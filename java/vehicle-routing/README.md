# Vehicle Routing Auto Research

This project implements a Vehicle Routing Problem with Time Windows (VRPTW) solver using Timefold AI, with integrated Auto Research capabilities for automated solver optimization.

![Vehicle Routing Screenshot](./vehicle-routing-screenshot.png)

## Constraints

| Name                                | Level  | Description                                                                        |
|-------------------------------------|--------|------------------------------------------------------------------------------------|
| Vehicle capacity                    | Hard   | The total demand of all visits assigned to a vehicle must not exceed its capacity. |
| Service finished after max end time | Hard   | A visit must be serviced before its maximum end time.                              |
| Maximize visits assigned            | Medium | As many visits as possible should be assigned to a vehicle.                        |
| Minimize travel time                | Soft   | Minimize the total travel time of all vehicles.                                    |

## Table of Contents

- [How to Run](#how-to-run)
- [Auto Research](#auto-research)

> [!TIP]  
> <img src="https://docs.timefold.ai/_/img/models/field-service-routing.svg" align="right" width="50px" /> [Check out our off-the-shelf model for Field Service Routing](https://app.timefold.ai/models/field-service-routing/v1).

## How to Run

### Prerequisites

1. Install Java and Maven, for example with [Sdkman](https://sdkman.io):
   ```sh
   $ sdk install java
   $ sdk install maven
   ```

### Run the application (Dev Mode)

1. Git clone the repo and navigate to this directory:
   ```sh
   $ git clone https://github.com/like1816/timefold-quickstarts.git
   ...
   $ cd timefold-quickstarts/java/vehicle-routing
   ```

2. (Optional) For Plus/Enterprise Edition, set up your license key first.

3. Start the application:
   ```sh
   $ mvn quarkus:dev
   ```

4. Visit [http://localhost:8080](http://localhost:8080) and click **Solve**.

### Run the packaged application

```sh
$ mvn package
$ java -jar ./target/quarkus-app/quarkus-run.jar
```

### Run in a container

```sh
$ mvn package -Dcontainer
$ docker run -p 8080:8080 --rm $USER/vehicle-routing:1.0-SNAPSHOT
```

### Run natively

```sh
$ mvn package -Dnative
$ ./target/*-runner
```

## Auto Research

Auto Research enables automated optimization of solver configurations. It iteratively tests different heuristic strategies, parameters, and filters, automatically recording and comparing results.

### Key Files

- `src/main/resources/vehicleRoutingBenchmarkConfig.xml` - Solver configuration
- `src/main/java/org/acme/vehiclerouting/solver/` - Custom Move Selectors, Comparators, Filters
- `results.tsv` - Benchmark results (keep untracked by git)
- `skill.md` - Auto Research skill documentation

### Run Benchmark

```sh
$ cd timefold-quickstarts/java/vehicle-routing
$ mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"
```

### Results Format (results.tsv)

| Column | Description |
|--------|-------------|
| commit | Git commit ID |
| problem | Problem instance (e.g., r101) |
| config_name | Configuration name |
| final_score | Final soft score (minimizes travel time, closer to 0 = better) |
| run_time_ms | Run time in milliseconds |
| score_calc_count | Number of score calculations |
| convergence_time_ms | Time to last improvement |

### Auto Research Workflow

1. **Check git state**: Note current branch/commit
2. **Modify configurations**:
   - Edit `vehicleRoutingBenchmarkConfig.xml` for Local Search strategies
   - Add custom components in `src/main/java/org/acme/vehiclerouting/solver/`
   - Apply optimizations: incremental score, multi-threading, nearby selection
3. **git commit**: Record changes (for recovery)
4. **Run benchmark**: Execute the solver
5. **Check results**: Review output and `results.tsv`
6. **If crashed**: Debug using console output
7. **Record**: Results appended to `results.tsv` (keep untracked)
8. **Compare and decide**:
   - If score improved: Keep the commit, advance the branch
   - If score equal or worse: `git reset` to revert changes

## More information

Visit [timefold.ai](https://timefold.ai).
