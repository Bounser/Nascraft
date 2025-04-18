package me.bounser.nascraft.web.dto;

import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;

public class PortfolioDTO {

    private String ownerName;
    private Double value;
    private HashMap<String, Integer> content;

    public PortfolioDTO(String ownerName, Double value, HashMap<Item, Integer> content) {
        this.ownerName = ownerName;
        this.value = value;

        this.content = new HashMap<>();

        for (Item item : content.keySet()) {
            this.content.put(item.getIdentifier(), content.get(item));
        }
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Double getValue() {
        return value;
    }

    public HashMap<String, Integer> getContent() {
        return content;
    }

}
