package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.ChartData;

import java.util.List;
import java.util.Map;

public interface StatisticsService {
    /** period: day (30 ngày) | month (12 tháng) | year (5 năm) */
    ChartData storeRevenue(Long storeId, String period);

    Map<String, Object> storeSummary(Long storeId);

    List<Object[]> storeTopProducts(Long storeId);

    ChartData systemRevenue(String period);

    ChartData newUsers(String period);

    ChartData newStores(String period);

    Map<String, Object> systemSummary();

    Map<String, Object> shipperSummary(Long shipperId);

    ChartData shipperDelivered(Long shipperId);
}
