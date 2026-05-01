package org.acme.vehiclerouting.util;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

import java.io.InputStream;
import java.util.List;

public class SolomonSolverTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 开始测试 Solomon 算例求解 ===");
        System.out.println();

        // 1. 加载和解析算例
        String instanceName = "r101";
        String resourcePath = "/input/problems/" + instanceName + ".txt";
        InputStream inputStream = SolomonSolverTest.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            System.err.println("❌ 找不到算例文件: " + resourcePath);
            return;
        }

        System.out.println("✅ 找到算例文件: " + resourcePath);
        VehicleRoutePlan plan = SolomonParser.parse(instanceName, inputStream);

        System.out.println();
        System.out.println("算例信息:");
        System.out.println("  名称: " + plan.getName());
        System.out.println("  车辆: " + plan.getVehicles().size());
        System.out.println("  客户: " + plan.getVisits().size());

        Vehicle firstVehicle = plan.getVehicles().get(0);
        System.out.println();
        System.out.println("第一辆车信息:");
        System.out.println("  ID: " + firstVehicle.getId());
        System.out.println("  Depot 位置: (" + firstVehicle.getHomeLocation().getLatitude() + "," + firstVehicle.getHomeLocation().getLongitude() + ")");
        System.out.println("  出发时间: " + firstVehicle.getDepartureTime());
        System.out.println("  容量: " + firstVehicle.getCapacity());

        // 2. 创建求解器
        System.out.println();
        System.out.println("=== 创建求解器 ===");

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(VehicleRoutePlan.class)
                .withEntityClasses(org.acme.vehiclerouting.domain.Vehicle.class,
                        org.acme.vehiclerouting.domain.Visit.class)
                .withConstraintProviderClass(org.acme.vehiclerouting.solver.VehicleRoutingConstraintProvider.class);

        SolverFactory<VehicleRoutePlan> solverFactory = SolverFactory.create(solverConfig);
        Solver<VehicleRoutePlan> solver = solverFactory.buildSolver();

        // 3. 添加解监听器
        solver.addEventListener(event -> {
            VehicleRoutePlan bestSolution = event.getNewBestSolution();
            System.out.println();
            System.out.println("🏆 找到新的最佳解! Score: " + bestSolution.getScore());

            long assignedCount = bestSolution.getVisits().stream().filter(v -> v.getVehicle() != null).count();
            System.out.println("   已分配客户: " + assignedCount + " / " + bestSolution.getVisits().size());
        });

        // 4. 开始求解（运行 30 秒）
        System.out.println();
        System.out.println("=== 开始求解（运行 30 秒）===");
        long startTime = System.currentTimeMillis();

        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(30000);
                System.out.println();
                System.out.println("⏰ 30秒时间到，停止求解...");
                solver.terminateEarly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stopper.start();

        VehicleRoutePlan solution = solver.solve(plan);

        long endTime = System.currentTimeMillis();
        System.out.println();
        System.out.println("✅ 求解完成! 耗时: " + ((endTime - startTime) / 1000.0) + "秒");

        // 5. 打印第一辆车的路径详情
        System.out.println();
        System.out.println("=== 第一辆车路径详情 ===");
        Vehicle vehicle1 = solution.getVehicles().get(0);
        List<Visit> visits = vehicle1.getVisits();

        System.out.println("Depot 位置: (" + vehicle1.getHomeLocation().getLatitude() + "," + vehicle1.getHomeLocation().getLongitude() + ")");
        System.out.println("出发时间: " + vehicle1.getDepartureTime());
        System.out.println("分配客户数: " + visits.size());
        System.out.println();

        if (!visits.isEmpty()) {
            System.out.println("路径顺序:");
            Visit prevVisit = null;
            for (int i = 0; i < visits.size(); i++) {
                Visit visit = visits.get(i);
                System.out.println();

                // 位置和需求
                System.out.println("  客户 " + visit.getId() + ":");
                System.out.println("    位置: (" + visit.getLocation().getLatitude() + "," + visit.getLocation().getLongitude() + ")");
                System.out.println("    需求: " + visit.getDemand());

                // 时间窗
                System.out.println("    时间窗: " + visit.getMinStartTime() + " ~ " + visit.getMaxEndTime());
                System.out.println("    服务时长: " + visit.getServiceDuration().toMinutes() + " 分钟");

                // 到达和离开时间
                System.out.println("    到达时间: " + visit.getArrivalTime());
                System.out.println("    开始服务: " + visit.getStartServiceTime());
                System.out.println("    离开时间: " + visit.getDepartureTime());

                // 计算并显示距离
                if (prevVisit == null) {
                    double dist = calculateDistance(vehicle1.getHomeLocation(), visit.getLocation());
                    System.out.println("    行驶距离: " + dist + " (约 " + Math.round(dist) + " 分钟)");
                } else {
                    double dist = calculateDistance(prevVisit.getLocation(), visit.getLocation());
                    System.out.println("    行驶距离: " + dist + " (约 " + Math.round(dist) + " 分钟)");
                }

                // 检查是否违反时间窗
                if (visit.getArrivalTime() != null && visit.getStartServiceTime() != null && visit.getDepartureTime() != null) {
                    if (visit.getDepartureTime().isAfter(visit.getMaxEndTime())) {
                        System.out.println("    ⚠️ 违反时间窗! 服务结束时间超过最晚截止时间");
                    }
                    if (visit.getStartServiceTime().isBefore(visit.getMinStartTime())) {
                        System.out.println("    ⚠️ 等待! 提前到达，等待到 " + visit.getMinStartTime());
                    }
                }

                prevVisit = visit;

                // 只打印前5个客户，避免太长
                if (i >= 4) {
                    System.out.println();
                    System.out.println("  ... (还有 " + (visits.size() - 5) + " 个客户)");
                    break;
                }
            }
        } else {
            System.out.println("  没有分配客户!");
        }

        System.out.println();
        System.out.println("🎉 测试完成!");
    }

    private static double calculateDistance(Location from, Location to) {
        double dx = from.getLatitude() - to.getLatitude();
        double dy = from.getLongitude() - to.getLongitude();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
