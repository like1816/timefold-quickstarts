package org.acme.vehiclerouting.util;

import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VerifyOptimalRouteFixed {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 验证 r101 Route 14 最优解 (修复版) ===");
        System.out.println();

        // 加载 r101 算例
        String instanceName = "r101";
        String resourcePath = "/input/problems/" + instanceName + ".txt";
        InputStream inputStream = VerifyOptimalRouteFixed.class.getResourceAsStream(resourcePath);

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

        // 打印客户 11 的原始数据
        System.out.println("客户 11 原始数据验证:");
        Visit visit11 = visitMap.get(11);
        System.out.println("  ID: " + visit11.getId());
        System.out.println("  位置: (" + visit11.getLocation().getLatitude() + ", " + visit11.getLocation().getLongitude() + ")");
        System.out.println("  minStartTime: " + visit11.getMinStartTime());
        System.out.println("  maxEndTime: " + visit11.getMaxEndTime());
        System.out.println("  serviceDuration: " + visit11.getServiceDuration());
        System.out.println();

        // 打印客户 62 的原始数据
        System.out.println("客户 62 原始数据验证:");
        Visit visit62 = visitMap.get(62);
        System.out.println("  ID: " + visit62.getId());
        System.out.println("  位置: (" + visit62.getLocation().getLatitude() + ", " + visit62.getLocation().getLongitude() + ")");
        System.out.println("  minStartTime: " + visit62.getMinStartTime());
        System.out.println("  maxEndTime: " + visit62.getMaxEndTime());
        System.out.println("  serviceDuration: " + visit62.getServiceDuration());
        System.out.println();

        // 手动计算距离
        System.out.println("手动计算距离:");
        double dist62_11 = Math.sqrt(
            Math.pow(visit62.getLocation().getLatitude() - visit11.getLocation().getLatitude(), 2) +
            Math.pow(visit62.getLocation().getLongitude() - visit11.getLocation().getLongitude(), 2)
        );
        System.out.println("  62 -> 11 距离: " + dist62_11 + " km");
        System.out.println("  四舍五入分钟: " + Math.round(dist62_11));
        System.out.println();

        // Route 14: 62 -> 11 -> 90 -> 20 -> 32 -> 70
        int[] route14 = {62, 11, 90, 20, 32, 70};

        System.out.println("Route 14 路径验证 (使用 DrivingTimeCalculator):");
        System.out.println("========================================");

        Location prevLocation = depot;
        java.time.LocalDateTime currentTime = plan.getVehicles().get(0).getDepartureTime();

        for (int i = 0; i < route14.length; i++) {
            Visit visit = visitMap.get(route14[i]);

            // 使用 DrivingTimeCalculator 计算行驶时间（秒）
            long drivingTimeSeconds = prevLocation.getDrivingTimeTo(visit.getLocation());
            long drivingTimeMinutes = drivingTimeSeconds / 60;

            // 到达时间
            currentTime = currentTime.plusSeconds(drivingTimeSeconds);

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
            System.out.println("  行驶时间(DrivingTimeCalculator): " + drivingTimeSeconds + " 秒 = " + drivingTimeMinutes + " 分钟");
            System.out.println("  到达时间: " + currentTime);
            System.out.println("  时间窗: " + visit.getMinStartTime() + " ~ " + visit.getMaxEndTime());
            System.out.println("  开始服务: " + startServiceTime);
            if (currentTime.isBefore(visit.getMinStartTime())) {
                System.out.println("    (等待 " + java.time.Duration.between(currentTime, visit.getMinStartTime()).toMinutes() + " 分钟)");
            }
            System.out.println("  离开时间: " + departureTime);

            // 检查是否违反时间窗（服务结束时间不能晚于 maxEndTime）
            if (departureTime.isAfter(visit.getMaxEndTime())) {
                System.out.println("  ❌ 违反时间窗! 服务结束时间 " + departureTime + " 超过最晚截止时间 " + visit.getMaxEndTime());
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
