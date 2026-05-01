package org.acme.vehiclerouting.util;

import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VerifyOptimalRoute {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 验证 r101 Route 14 最优解 ===");
        System.out.println();

        // 加载 r101 算例
        String instanceName = "r101";
        String resourcePath = "/input/problems/" + instanceName + ".txt";
        InputStream inputStream = VerifyOptimalRoute.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            System.err.println("❌ 找不到算例文件: " + resourcePath);
            return;
        }

        VehicleRoutePlan plan = SolomonParser.parse(instanceName, inputStream);

        // 建立客户 ID -> Visit 的映射
        Map<Integer, Visit> visitMap = new HashMap<>();
        for (Visit visit : plan.getVisits()) {
            visitMap.put(Integer.parseInt(visit.getId()), visit);
        }

        // Depot 信息
        Location depot = plan.getVehicles().get(0).getHomeLocation();
        System.out.println("Depot 位置: (" + depot.getLatitude() + ", " + depot.getLongitude() + ")");
        System.out.println("车辆出发时间: " + plan.getVehicles().get(0).getDepartureTime());
        System.out.println();

        // Route 14: 62 -> 11 -> 90 -> 20 -> 32 -> 70
        int[] route14 = {62, 11, 90, 20, 32, 70};

        System.out.println("Route 14 路径验证:");
        System.out.println("========================================");

        Location prevLocation = depot;
        java.time.LocalDateTime currentTime = plan.getVehicles().get(0).getDepartureTime();

        for (int i = 0; i < route14.length; i++) {
            Visit visit = visitMap.get(route14[i]);

            // 计算行驶距离和时间
            double dx = prevLocation.getLatitude() - visit.getLocation().getLatitude();
            double dy = prevLocation.getLongitude() - visit.getLocation().getLongitude();
            double distance = Math.sqrt(dx * dx + dy * dy);
            long travelTimeMinutes = Math.round(distance);

            // 到达时间
            currentTime = currentTime.plusMinutes(travelTimeMinutes);

            // 开始服务时间（如果早于时间窗开始，等待）
            java.time.LocalDateTime startServiceTime = currentTime.isBefore(visit.getMinStartTime())
                    ? visit.getMinStartTime()
                    : currentTime;

            // 结束服务时间
            java.time.LocalDateTime departureTime = startServiceTime.plus(visit.getServiceDuration());

            System.out.println();
            System.out.println("客户 " + visit.getId() + ":");
            System.out.println("  位置: (" + visit.getLocation().getLatitude() + ", " + visit.getLocation().getLongitude() + ")");
            System.out.println("  前一位置: (" + prevLocation.getLatitude() + ", " + prevLocation.getLongitude() + ")");
            System.out.println("  行驶距离: " + distance + " km (" + travelTimeMinutes + " 分钟)");
            System.out.println("  到达时间: " + currentTime);
            System.out.println("  时间窗: " + visit.getMinStartTime() + " ~ " + visit.getMaxEndTime());
            System.out.println("  开始服务: " + startServiceTime + (currentTime.isBefore(visit.getMinStartTime()) ? " (等待 " + java.time.Duration.between(currentTime, visit.getMinStartTime()).toMinutes() + " 分钟)" : ""));
            System.out.println("  离开时间: " + departureTime);

            // 检查是否违反时间窗
            if (departureTime.isAfter(visit.getMaxEndTime())) {
                System.out.println("  ❌ 违反时间窗! 结束时间超过最晚截止时间 " + visit.getMaxEndTime());
            } else {
                System.out.println("  ✅ 时间窗满足");
            }

            prevLocation = visit.getLocation();
            currentTime = departureTime;
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("验证完成！");
    }
}