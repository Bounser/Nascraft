package me.bounser.nascraft.web.dto;

import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;
import java.util.Map;

public class PortfolioDTO {
    private final String ownerName;
    private final Double value;
    private final HashMap<String, Integer> content;

    public PortfolioDTO(String ownerName, Double value, Map<Item, Integer> portfolioContent) {
        this.ownerName = ownerName;
        this.value = value;
        this.content = new HashMap<>();

        if (portfolioContent != null) {
            for (Map.Entry<Item, Integer> entry : portfolioContent.entrySet()) {
                if (entry.getKey() != null) this.content.put(entry.getKey().getIdentifier(), entry.getValue());
            }
        }
    }

    public String getOwnerName() { return ownerName; }
    public Double getValue() { return value; }
    public HashMap<String, Integer> getContent() { return content; }
}
