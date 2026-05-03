#!/usr/bin/env python3
"""
Agent-driven autoresearch benchmark runner.
Automates the experiment loop: modify config → compile → run → analyze → decide.
"""
import subprocess
import os
import sys
import json
import time
import re
from pathlib import Path
from datetime import datetime

PROJECT_DIR = Path(__file__).parent
RESULTS_TSV = PROJECT_DIR / "results.tsv"
CONFIG_FILE = PROJECT_DIR / "src" / "main" / "resources" / "vehicleRoutingBenchmarkConfig.xml"

# Environment setup
ENV = os.environ.copy()
ENV["JAVA_HOME"] = "/usr/lib/jvm/java-21-alibaba-dragonwell-21.0.10.0.10-1.1.al8.x86_64"
ENV["PATH"] = f"{ENV['JAVA_HOME']}/bin:/home/admin/apache-maven-3.9.9/bin:{ENV['PATH']}"


def run_cmd(cmd, cwd=None, timeout=300):
    """Run a command and return (stdout, stderr, exit_code)."""
    result = subprocess.run(
        cmd, shell=True, cwd=cwd or PROJECT_DIR,
        env=ENV, capture_output=True, text=True, timeout=timeout
    )
    return result.stdout, result.stderr, result.returncode


def get_git_commit():
    """Get current short commit hash."""
    out, _, _ = run_cmd("git rev-parse --short HEAD")
    return out.strip()


def read_config():
    """Read current benchmark config."""
    return CONFIG_FILE.read_text()


def write_config(content):
    """Write benchmark config."""
    CONFIG_FILE.write_text(content)


def compile_project():
    """Compile the project. Returns True on success."""
    stdout, stderr, code = run_cmd("mvn compile -q", timeout=120)
    return code == 0


def run_benchmark(problem="r101", timeout=300):
    """Run benchmark and parse results."""
    stdout, stderr, code = run_cmd(
        f'mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"',
        timeout=timeout
    )
    
    # Parse result from stdout
    for line in stdout.splitlines():
        if line.startswith("Logged:"):
            return parse_logged_line(line)
    
    # If no Logged line, check for errors
    if "BUILD FAILURE" in stdout or "BUILD FAILURE" in stderr:
        error_msg = stderr if stderr else stdout
        return {"status": "crash", "error": error_msg[-500:]}
    
    return {"status": "unknown", "raw": stdout[-500:]}


def parse_logged_line(line):
    """Parse a 'Logged:' line from benchmark output."""
    # Format: commit\tproblem\tbenchmark_time\tconfig_name\tfinal_score\trun_time_ms\tscore_calc_count\tmove_eval_count\tinitial_score\tconvergence_time_ms\tstatus\treport_dir
    parts = line.replace("Logged: ", "").split("\t")
    if len(parts) >= 12:
        score_str = parts[4]  # e.g., "0hard/0medium/-100920soft"
        soft_match = re.search(r'-?(\d+)soft', score_str)
        soft_score = int(soft_match.group(1)) if soft_match else 0
        
        return {
            "commit": parts[0],
            "problem": parts[1],
            "benchmark_time_ms": int(parts[2]),
            "config_name": parts[3],
            "final_score": score_str,
            "run_time_ms": int(parts[5]),
            "score_calc_count": int(parts[6]),
            "move_eval_count": int(parts[7]),
            "initial_score": float(parts[8]),
            "convergence_time_ms": int(parts[9]),
            "status": parts[10],
            "report_dir": parts[11],
            "soft_score": soft_score,
        }
    return {"status": "parse_error", "raw": line}


def read_results():
    """Read all results from results.tsv."""
    if not RESULTS_TSV.exists():
        return []
    results = []
    with open(RESULTS_TSV) as f:
        lines = f.readlines()
    if not lines:
        return []
    header = lines[0].strip().split("\t")
    for line in lines[1:]:
        parts = line.strip().split("\t")
        if len(parts) == len(header):
            results.append(dict(zip(header, parts)))
    return results


def git_commit(message):
    """Commit current changes."""
    run_cmd("git add -A")
    run_cmd(f'git commit -m "{message}"')
    return get_git_commit()


def git_reset_hard(commit):
    """Reset to a specific commit."""
    run_cmd(f"git reset --hard {commit}")


def git_stash():
    """Stash current changes."""
    run_cmd("git stash")


def git_stash_pop():
    """Pop stashed changes."""
    run_cmd("git stash pop")


def compare_scores(score_a, score_b):
    """Compare two soft scores. Returns >0 if a is better, <0 if b is better."""
    # Higher (closer to 0) is better
    return score_a - score_b


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 agent_benchmark_runner.py <command> [args]")
        print("Commands:")
        print("  baseline          - Run baseline benchmark")
        print("  test <config_xml> - Test with given config (from file)")
        print("  results           - Show all results")
        print("  best              - Show best result")
        print("  status            - Show git status and current commit")
        sys.exit(0)
    
    cmd = sys.argv[1]
    
    if cmd == "baseline":
        print("=== Running Baseline Benchmark ===")
        commit = get_git_commit()
        print(f"Commit: {commit}")
        
        print("Compiling...")
        if not compile_project():
            print("COMPILE FAILED")
            sys.exit(1)
        
        print("Running benchmark (60s solver)...")
        result = run_benchmark(timeout=300)
        print(json.dumps(result, indent=2))
        
    elif cmd == "results":
        results = read_results()
        if not results:
            print("No results yet.")
        else:
            print(f"{'Commit':<10} {'Config':<30} {'Score':<25} {'Status':<10}")
            print("-" * 75)
            for r in results:
                print(f"{r.get('commit','?'):<10} {r.get('config_name','?'):<30} {r.get('final_score','?'):<25} {r.get('status','?'):<10}")
    
    elif cmd == "best":
        results = read_results()
        if not results:
            print("No results yet.")
        else:
            successful = [r for r in results if r.get('status') == 'success']
            if not successful:
                print("No successful results.")
            else:
                best = min(successful, key=lambda r: int(re.search(r'-?(\d+)soft', r.get('final_score','0soft')).group(1)))
                print(f"Best result:")
                print(json.dumps(best, indent=2))
    
    elif cmd == "status":
        commit = get_git_commit()
        stdout, _, _ = run_cmd("git status --short")
        print(f"Commit: {commit}")
        print(f"Changes: {stdout.strip() or '(clean)'}")
    
    elif cmd == "test":
        if len(sys.argv) < 3:
            print("Usage: python3 agent_benchmark_runner.py test <config_file>")
            sys.exit(1)
        
        config_file = Path(sys.argv[2])
        if not config_file.exists():
            print(f"Config file not found: {config_file}")
            sys.exit(1)
        
        print(f"=== Testing with config: {config_file} ===")
        
        # Save current config
        original_config = read_config()
        
        # Apply new config
        write_config(config_file.read_text())
        
        commit = get_git_commit()
        print(f"Commit: {commit}")
        
        print("Compiling...")
        if not compile_project():
            print("COMPILE FAILED")
            write_config(original_config)
            sys.exit(1)
        
        print("Running benchmark...")
        result = run_benchmark(timeout=300)
        print(json.dumps(result, indent=2))
        
        # Restore original config
        write_config(original_config)
    
    else:
        print(f"Unknown command: {cmd}")
        sys.exit(1)
