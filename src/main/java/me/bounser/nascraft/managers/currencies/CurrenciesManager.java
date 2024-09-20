package me.bounser.nascraft.managers.currencies;

import me.bounser.nascraft.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CurrenciesManager {

    public static CurrenciesManager instance;

    private final HashMap<String, Currency> currencies = new HashMap<>();

    public Currency defaultCurrency;

    public static CurrenciesManager getInstance() { return instance == null ? instance = new CurrenciesManager() : instance; }

    private CurrenciesManager() {
        for (Currency currency : Config.getInstance().getCurrencies()) {
            currencies.put(currency.getCurrencyIdentifier(), currency);
        }
        defaultCurrency = currencies.get(Config.getInstance().getDefaultCurrencyIdentifier());
    }

    public Currency getCurrency(String currencyIdentifier) { return currencies.get(currencyIdentifier); }

    public Currency getVaultCurrency() {
        return currencies.get("vault");
    }

    public List<Currency> getCurrencies() { return new ArrayList<>(currencies.values()); }

    public Currency getDefaultCurrency() { return defaultCurrency; }

}
