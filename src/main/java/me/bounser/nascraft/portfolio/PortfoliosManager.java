package me.bounser.nascraft.portfolio;

import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PortfoliosManager {


    private final HashMap<UUID, Portfolio> inventories = new HashMap<>();

    private static PortfoliosManager instance;

    public static PortfoliosManager getInstance() { return instance == null ? instance = new PortfoliosManager() : instance; }

    public Portfolio getPortfolio(UUID uuid) {

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new Portfolio(uuid));

        return inventories.get(uuid);
    }

    public Portfolio getPortfolio(String userid) {

        UUID uuid = LinkManager.getInstance().getUUID(userid);

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new Portfolio(uuid));

        return inventories.get(uuid);
    }

    public void savePortfoliosWorthOfOnlinePlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player == null) continue;

            double worth = getPortfolio(player.getUniqueId()).getValueOfDefaultCurrency() - DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId());
            double debt = DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId());

            if (worth == 0) continue;

            DatabaseManager.get().getDatabase().saveOrUpdateWorthToday(player.getUniqueId(), worth - debt);
        }
    }

    public void savePortfolioOfPlayer(Player player) {
        if (player == null) return;

        Portfolio portfolio = getPortfolio(player.getUniqueId());

        if (portfolio == null) return;

        double worth = portfolio.getValueOfDefaultCurrency();
        double debt = DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId());

        DatabaseManager.get().getDatabase().saveOrUpdateWorthToday(player.getUniqueId(), worth - debt);
    }

}
