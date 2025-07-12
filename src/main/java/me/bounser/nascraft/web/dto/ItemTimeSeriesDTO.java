package me.bounser.nascraft.web.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemTimeSeriesDTO {
    
    private String itemId;
    private String itemName;
    private List<Map<String, Object>> dataPoints;
    private String timeSpan;
    private String chartType;
    
    public ItemTimeSeriesDTO(String itemId, String itemName, List<Map<String, Object>> dataPoints, String timeSpan, String chartType) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.dataPoints = dataPoints;
        this.timeSpan = timeSpan;
        this.chartType = chartType;
    }
    
    /**
     * Backward compatibility constructor for older code
     * @param timestamp The timestamp for the data point
     * @param value The value for the data point
     * @param type The chart type as an integer
     */
    public ItemTimeSeriesDTO(long timestamp, double value, int type) {
        this.itemId = String.valueOf(timestamp);
        this.itemName = String.valueOf(value);
        this.dataPoints = new ArrayList<>();
        
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("timestamp", timestamp);
        dataPoint.put("value", value);
        this.dataPoints.add(dataPoint);
        
        this.timeSpan = "day"; // Default timespan
        this.chartType = getChartTypeFromInt(type);
    }
    
    private String getChartTypeFromInt(int type) {
        switch(type) {
            case 0: return "line";
            case 1: return "bar";
            case 2: return "pie";
            default: return "line";
        }
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public List<Map<String, Object>> getDataPoints() {
        return dataPoints;
    }
    
    public String getTimeSpan() {
        return timeSpan;
    }
    
    public String getChartType() {
        return chartType;
    }
} 