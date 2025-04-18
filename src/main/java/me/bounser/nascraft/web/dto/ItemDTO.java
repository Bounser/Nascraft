package me.bounser.nascraft.web.dto;

public class ItemDTO {

    private String identifier;
    private String name;
    private Double price;
    private Double buy;
    private Double sell;
    private Double changePercent;
    private Integer operations;

    public ItemDTO(String identifier, String name, double price, double buy, double sell, Integer operations, Double changePercent) {
        this.identifier = identifier;
        this.name = name;
        this.price = price;
        this.buy = buy;
        this.sell = sell;
        this.operations = operations;
        this.changePercent = changePercent;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public double getBuy() {
        return buy;
    }

    public double getSell() {
        return sell;
    }

    public int getOperations() { return operations; }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setId(String identifier) {
        this.identifier = identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

}