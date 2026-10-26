package vn.hcmute.rebook.dto;

import java.util.List;

/** Dữ liệu cho biểu đồ Chart.js */
public record ChartData(List<String> labels, List<Long> values) {}
