package me.bounser.nascraft.web.dto;

public class CategoryDTO {

    private String identifier;
    private String name;
    private Double changePercent;

    public CategoryDTO(String identifier, String name, Double changePercent) {
        this.identifier = identifier;
        this.name = name;
        this.changePercent = changePercent;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setId(String identifier) {
        this.identifier = identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

}
