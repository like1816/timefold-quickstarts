package org.acme.vehiclerouting.benchmark;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import org.acme.vehiclerouting.app.SolutionBuilder;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;

public class VehicleRoutingBenchmarkRunner {

    public static void main(String[] args) {
        // 从 XML 配置创建 benchmark factory
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(
                "vehicleRoutingBenchmarkConfig.xml");

        // 加载问题数据
        VehicleRoutePlan problem = SolutionBuilder.buildFromSolomon("r101");

        // 构建并运行 benchmark
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        benchmark.benchmark();

        System.out.println("Benchmark completed! Report in local/benchmarkReport directory.");
    }
}
