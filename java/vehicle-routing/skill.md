# Vehicle Routing Auto Research

This is an Auto Research experiment for Vehicle Routing Problem with Time Windows (VRPTW) based on Timefold AI. Let LLM automatically improve the algorithm.

## Setup

When setting up a new experiment, work with the user:

1. **Agree on a run tag**: Suggest a tag based on today's date (e.g., `may5`). The branch `autoresearch/<tag>` must not exist - this is a brand new run.
2. **Create branch**: Execute `git checkout -b autoresearch/<tag> autoresearch_alpha`.
3. **Read files in scope**:
   - `README.md` – Project overview
   - `pom.xml` – Maven dependency configuration
   - `results.tsv` – Previous experiment results
   - `src/main/java/org/acme/vehiclerouting/benchmark/VehicleRoutingBenchmarkRunner.java` – Benchmark runner
   - `src/main/resources/vehicleRoutingBenchmarkConfig.xml` – Solver configuration,used together with the solver
   - `src/main/java/org/acme/vehiclerouting/score/VehicleRoutingConstraintProvider.java` – Constraint definition
   - `src/main/java/org/acme/vehiclerouting/solver/` – All custom components for Auto Research (comparators, filters, etc.), used together with the benchmark config
   - `src/main/java/org/acme/vehiclerouting/domain/` – Domain model (Vehicle, Visit, Location, Depot)
4. **Utilize knowledge base**: If your agent has access to an LLM wiki with Timefold documentation, leverage it. Otherwise, consult online Timefold documentation for topics including but not limited to:
   - Local Search strategies (Late Acceptance, Simulated Annealing, Tabu Search)
   - Move selection (Nearby Selection, SubList moves)
   - Score calculation (incremental scoring, constraint matches)
   - Construction heuristics (First Fit, Best Fit, Cheapest Insertion)
5. **Verify data exists**: Check if `src/main/resources/input/problems/` contains Solomon problem instances (e.g., r101.txt, c101.txt).
6. **Verify results.tsv exists**: This file is git-ignored and persists locally. Do NOT recreate it – just append new results during experiments. (See ## Output Format and ## Logging Results below for details.)
7. **Confirm and start**: Confirm Setup is OK.

After confirmation, start the experiment.

## Experimentation

Each experiment's running time is configured via `<secondsSpentLimit>` in `vehicleRoutingBenchmarkConfig.xml`, default ~5 minutes. Run command:

```bash
cd java/vehicle-routing
mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"
```

**What you can do:**
- Modify `src/main/resources/vehicleRoutingBenchmarkConfig.xml` – Adjust Local Search strategy, Move Selectors, etc.
- Add new custom Java components (comparators, filters, etc.) to `src/main/java/org/acme/vehiclerouting/solver/`

**What you cannot do:**
- Modify `src/main/java/org/acme/vehiclerouting/benchmark/` – Benchmark runner
- Modify `src/main/java/org/acme/vehiclerouting/domain/` – Domain model
- Modify `src/main/java/org/acme/vehiclerouting/score/` – Score constraints (evaluation standard)
- Modify `src/main/java/org/acme/vehiclerouting/rest/` – REST API
- Modify `src/main/java/org/acme/vehiclerouting/util/` – Utilities
- Modify `src/main/java/org/acme/vehiclerouting/app/` – Application
- Modify evaluation suite (`results.tsv` format)
- Install new packages or add dependencies. You can only use what's already in `pom.xml`.
- Modify problem instance data

**Goal:** Maximize Soft score (travel time), i.e., make it closer to 0 (less negative). In Timefold, scores are negative (0 is best). For example, -96000 is better than -100000.
- Hard=0: All hard constraints satisfied (capacity, time windows)
- Medium=0: All visits assigned
- Soft: Minimize travel time (closer to 0 = better)

## Principles

**1. Think Before Coding**: Before making any changes, understand the codebase and the problem. Ask: why would this change help? What do I expect the outcome to be?

**2. Simplicity First**: All else being equal, simpler is better. A small improvement that adds ugly complexity is not worth it. Removing something and getting equal or better results is a great simplification win. For example: a 0.01% score improvement that adds 50 lines of complex config? Probably not worth it. An improvement from simplifying the config? Definitely keep.

**3. Surgical Changes**: Make one change at a time, verify the result, then move to the next. Don't change multiple things simultaneously - it makes it impossible to know what worked and what didn't.

**4. Goal-Driven Execution**: Always remember the goal: Hard=0 (constraints satisfied), Medium=0 (all visits assigned), Soft closer to 0 (less travel time). If a change doesn't move you toward this goal, revert it.

**First run**: Your first run should always establish the baseline.

## Output Format

After completion, the script prints a summary like:

```
Benchmark completed! Report in local/benchmarkReport/2026-05-02_160405
Results logged to: results.tsv
Logged: 5708c51	r101	30556	Visit Waiting Time Sorter	0hard/0medium/-100920soft	30003	2534407	2534404	0	29050	success	local\benchmarkReport\2026-05-02_160405\2026-05-02_160406
```

## Logging Results

After experiment completion, results are automatically logged to `results.tsv` (tab-separated).

TSV header:

```
commit	problem	benchmark_time	config_name	final_score	run_time_ms	score_calc_count	move_eval_count	initial_score	convergence_time_ms	status	report_dir
```

Field description:
1. `commit` – Git commit hash (7 characters)
2. `problem` – Problem instance (e.g., r101)
3. `benchmark_time` – Total time in milliseconds
4. `config_name` – Configuration name
5. `final_score` – Final score (format: hard/medium/soft)
6. `run_time_ms` – Solver run time in milliseconds
7. `score_calc_count` – Number of score calculations
8. `move_eval_count` – Number of move evaluations
9. `initial_score` – Score after construction heuristic
10. `convergence_time_ms` – Convergence time (time of last improvement)
11. `status` – `success` or `crash`
12. `report_dir` – Report directory path

Example:

```
5708c51	r101	30556	Visit Waiting Time Sorter	0hard/0medium/-100920soft	30003	2534407	2534404	0	29050	success	local/benchmarkReport/2026-05-02_160405/2026-05-02_160406
```

## The Experiment Loop

Experiments run on dedicated branches (e.g., `autoresearch/may5`).

LOOP (up to 50 iterations):

1. **Check git state**: Note current branch/commit
2. **Modify one thing at a time** (Surgical Changes principle):
   - Edit `vehicleRoutingBenchmarkConfig.xml` for Local Search strategies
   - Or add custom components in `src/main/java/org/acme/vehiclerouting/solver/` (create subfolders if needed)
3. **`git commit`**: Record changes (for recovery)
4. **Compile + Run benchmark**:
   ```bash
   mvn compile -q
   mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"
   ```
   > Note: The benchmark runner reads from `target/classes/`, not `src/main/resources/`. Always run `mvn compile` before benchmark, or it silently uses the cached config.
5. **Check results**: Review console output and `results.tsv`
6. **If crashed**: Debug using console output, try to fix; if idea is fundamentally broken, record "crash" and continue
7. **Record**: Results automatically appended to `results.tsv` (keep untracked by git)
8. **Compare and decide**:
   - If score improved (Soft closer to 0, with Hard=0, Medium=0): Keep commit, advance branch
   - If score same or worse: `git reset` back to starting point

**Timeout**: The solver's running time is configured in `vehicleRoutingBenchmarkConfig.xml` via `<secondsSpentLimit>`. The default is ~5 minutes. Normally, let the solver run to completion automatically – only intervene if it crashes or behaves unexpectedly. If a run exceeds the configured time significantly, kill it and treat as failed.

**Crash**: If run crashes (OOM, bug, etc.), use your judgment: if it's something silly and easily fixable (e.g., typo, missing import), fix it and re-run. If the idea is fundamentally broken, skip it, record status as "crash", and continue.

**Iteration limit**: The experiment loop should run for a maximum of **50 iterations**. Each iteration takes ~2 minutes (1 min solver + 1 min compile, stats, commit). So ~100 minutes total. If you reach 50 iterations without finding improvements, stop and report to the user.