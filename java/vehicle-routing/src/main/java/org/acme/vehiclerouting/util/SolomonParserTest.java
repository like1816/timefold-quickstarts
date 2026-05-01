package org.acme.vehiclerouting.util;

import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

import java.io.InputStream;

public class SolomonParserTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 开始加载 Solomon 算例 ===");

        // 1. 加载 r101.txt
        String instanceName = "r101";
        String resourcePath = "/input/problems/" + instanceName + ".txt";
        InputStream inputStream = SolomonParserTest.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            System.err.println("❌ 找不到算例文件: " + resourcePath);
            return;
        }

        System.out.println("✅ 找到算例文件: " + resourcePath);

        // 2. 解析算例
        System.out.println("=== 开始解析 ===");
        VehicleRoutePlan plan = SolomonParser.parse(instanceName, inputStream);

        // 3. 打印关键信息
        System.out.println("=== 解析完成 ===");
        System.out.println("名称: " + plan.getName());
        System.out.println("车辆数量: " + plan.getVehicles().size());
        System.out.println("客户数量: " + plan.getVisits().size());
        System.out.println("起始时间: " + plan.getStartDateTime());
        System.out.println("结束时间: " + plan.getEndDateTime());

        // 4. 打印 Depot 信息
        System.out.println("\n=== Depot 位置 ===");
        Location depotLocation = plan.getVehicles().get(0).getHomeLocation();
        System.out.println("Depot 坐标 (X,Y): " + depotLocation.getLatitude() + ", " + depotLocation.getLongitude());
        System.out.println("所有车辆的 Depot:");
        for (Vehicle v : plan.getVehicles()) {
            Location vHome = v.getHomeLocation();
            System.out.println("  " + v.getId() + ": " + vHome.getLatitude() + "," + vHome.getLongitude() +
                    " (是否相同? " + depotLocation.equals(vHome) + ")");
        }

        // 5. 打印前3个客户
        System.out.println("\n=== 前3个客户 ===");
        for (int i = 0; i < Math.min(3, plan.getVisits().size()); i++) {
            Visit visit = plan.getVisits().get(i);
            Location loc = visit.getLocation();
            System.out.println("  " + visit.getId() + ": " +
                    "(" + loc.getLatitude() + "," + loc.getLongitude() + "), " +
                    "需求: " + visit.getDemand() + ", " +
                    "时间窗: " + visit.getMinStartTime() + "~" + visit.getMaxEndTime() + ", " +
                    "服务时长: " + visit.getServiceDuration().getSeconds() + "秒");
        }

        // 6. 测试距离计算
        System.out.println("\n=== 距离测试 ===");
        if (plan.getVisits().size() >= 2) {
            Visit visit1 = plan.getVisits().get(0);
            Visit visit2 = plan.getVisits().get(1);

            Location loc1 = visit1.getLocation();
            Location loc2 = visit2.getLocation();

            long drivingTime = loc1.getDrivingTimeTo(loc2);
            System.out.println("客户 " + visit1.getId() + " → 客户 " + visit2.getId() + " 耗时: " + drivingTime + "秒");
            System.out.println("  坐标1: " + loc1.getLatitude() + "," + loc1.getLongitude());
            System.out.println("  坐标2: " + loc2.getLatitude() + "," + loc2.getLongitude());

            // 手动计算距离
            double dx = loc1.getLatitude() - loc2.getLatitude();
            double dy = loc1.getLongitude() - loc2.getLongitude();
            double distanceKm = Math.sqrt(dx * dx + dy * dy);
            double expectedTime = distanceKm / 50 * 3600;
            System.out.println("  直线距离: " + distanceKm + "km");
            System.out.println("  预期时间 (50km/h): " + Math.round(expectedTime) + "秒");
        }

        System.out.println("\n✅ 测试完成，可以在 IDE 中调试!");
    }
}