package org.acme.vehiclerouting.app;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

import java.util.List;

public class App {

    private static final String INSTANCE_NAME = "r101";
    private static final int DURATION_SECONDS = 60;

    private static final String[][] SOLOMON_INSTANCES = {
        {"C1", "c101", "c102", "c103", "c104", "c105", "c106", "c107", "c108", "c109"},
        {"C2", "c201", "c202", "c203", "c204", "c205", "c206", "c207", "c208"},
        {"R1", "r101", "r102", "r103", "r104", "r105", "r106", "r107", "r108", "r109", "r110", "r111", "r112"},
        {"R2", "r201", "r202", "r203", "r204", "r205", "r206", "r207", "r208", "r209", "r210", "r211"},
        {"RC1", "rc101", "rc102", "rc103", "rc104", "rc105", "rc106", "rc107", "rc108"},
        {"RC2", "rc201", "rc202", "rc203", "rc204", "rc205", "rc206", "rc207", "rc208"}
    };

    public static void main(String[] args) {
        String instanceName = INSTANCE_NAME;
        int durationSeconds = DURATION_SECONDS;

        System.out.println("=== Vehicle Routing Solver ===");
        System.out.println("Instance: " + instanceName);
        System.out.println("Duration: " + durationSeconds + " seconds");
        System.out.println();

        VehicleRoutePlan problem = SolutionBuilder.buildFromSolomon(instanceName);

        System.out.println("Problem loaded:");
        System.out.println("  Vehicles: " + problem.getVehicles().size());
        System.out.println("  Visits: " + problem.getVisits().size());
        System.out.println();

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(VehicleRoutePlan.class)
                .withEntityClasses(Vehicle.class, Visit.class)
                .withConstraintProviderClass(org.acme.vehiclerouting.solver.VehicleRoutingConstraintProvider.class);

        SolverFactory<VehicleRoutePlan> solverFactory = SolverFactory.create(solverConfig);
        Solver<VehicleRoutePlan> solver = solverFactory.buildSolver();

        solver.addEventListener(event -> {
            VehicleRoutePlan bestSolution = event.getNewBestSolution();
            System.out.println("New best solution found! Score: " + bestSolution.getScore());
            printSolutionSummary(bestSolution);
        });

        System.out.println("Starting solver...");
        long startTime = System.currentTimeMillis();

        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(durationSeconds * 1000L);
                System.out.println();
                System.out.println("Time limit reached. Terminating solver...");
                solver.terminateEarly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stopper.start();

        VehicleRoutePlan solution = solver.solve(problem);

        long endTime = System.currentTimeMillis();
        System.out.println();
        System.out.println("=== Solver finished ===");
        System.out.println("Time: " + (endTime - startTime) / 1000.0 + " seconds");
        System.out.println("Final Score: " + solution.getScore());
        System.out.println();

        System.out.println("=== Solution ===");
        SolutionExporter.print(solution);
    }

    private static void printSolutionSummary(VehicleRoutePlan solution) {
        long assignedCount = solution.getVisits().stream()
                .filter(v -> v.getVehicle() != null)
                .count();
        long totalVisits = solution.getVisits().size();
        System.out.println("  Assigned visits: " + assignedCount + " / " + totalVisits);

        long usedVehicles = solution.getVehicles().stream()
                .filter(v -> !v.getVisits().isEmpty())
                .count();
        System.out.println("  Used vehicles: " + usedVehicles + " / " + solution.getVehicles().size());
    }

    private static void printUsage() {
        System.out.println("Usage: java App <instance_name> [duration_seconds]");
        System.out.println("Example: java App c101 30");
        System.out.println();
        System.out.println("Available Solomon instances:");
        for (String[] group : SOLOMON_INSTANCES) {
            System.out.print("  " + group[0] + ": ");
            for (int i = 1; i < group.length; i++) {
                System.out.print(group[i]);
                if (i < group.length - 1) System.out.print(", ");
            }
            System.out.println();
        }
    }
}