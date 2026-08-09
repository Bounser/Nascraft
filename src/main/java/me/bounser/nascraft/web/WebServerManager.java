package me.bounser.nascraft.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import me.bounser.nascraft.Services;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.web.dto.CategoryDTO;
import me.bounser.nascraft.web.dto.ItemDTO;
import me.bounser.nascraft.web.dto.PortfolioDTO;
import me.bounser.nascraft.web.dto.TimeSeriesDTO;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Self-hosted market website restored from the Nascraft 1.9.1 release.
 *
 * The public source repository did not contain this implementation even though
 * it was present in the published 1.9.1 JAR. The API surface intentionally
 * remains compatible with that release.
 */
public final class WebServerManager {

    private final JavaPlugin plugin;
    private final int port;
    private final String externalWebRootPath;
    private final WebConfig webConfig;
    private Javalin webServer;

    public WebServerManager(JavaPlugin plugin, WebConfig webConfig) {
        this.plugin = plugin;
        this.webConfig = webConfig;
        this.port = webConfig.port();
        this.externalWebRootPath = new File(plugin.getDataFolder(), "web").getAbsolutePath();
    }

    public synchronized void startServer() {
        if (webServer != null) {
            plugin.getLogger().warning("Web server is already running!");
            return;
        }

        File webRoot = new File(externalWebRootPath);
        if (!webRoot.isDirectory()) {
            plugin.getLogger().severe("-------------------------------------------------------");
            plugin.getLogger().severe("External web directory not found or is not a directory!");
            plugin.getLogger().severe("Expected: " + externalWebRootPath);
            plugin.getLogger().severe("Web server cannot start.");
            plugin.getLogger().severe("-------------------------------------------------------");
            return;
        }

        try {
            Javalin app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.staticFiles.add(files -> {
                    files.hostedPath = "/";
                    files.directory = externalWebRootPath;
                    files.location = Location.EXTERNAL;
                });
            });

            registerRoutes(app);
            webServer = app.start(port);
            plugin.getLogger().info("Web server started successfully on port " + port + ".");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to start web server on port " + port + ": " + exception.getMessage(), exception);
            webServer = null;
        }
    }

    private void registerRoutes(Javalin app) {
        app.get("/api/items", ctx -> ctx.json(getAllItemData()));
        app.get("/api/top-portfolios", ctx -> ctx.json(getTopPortfolios()));
        app.get("/api/categories", ctx -> ctx.json(getCategories()));
        app.get("/api/charts/cpi", ctx -> ctx.json(getCpiTimeSeries()));
        app.get("/api/config", ctx -> ctx.json(getPublicConfig()));

        app.get("/api/popular-item", ctx -> {
            ItemDTO popular = getPopularItem();
            if (popular == null) ctx.status(404).result("Popular item data not available.");
            else ctx.json(popular);
        });

        app.get("/api/charts/item/{identifier}", ctx -> {
            List<TimeSeriesDTO> data = getItemTimeSeries(ctx.pathParam("identifier"));
            if (data == null) ctx.status(404).result("Unknown market item.");
            else ctx.json(data);
        });

        app.get("/api/icons/{identifier}.png", this::serveIcon);
    }

    private List<ItemDTO> getAllItemData() {
        List<ItemDTO> result = new ArrayList<>();
        for (Item item : Services.get().market().getAllParentItems()) result.add(toItemDto(item));
        return result;
    }

    private ItemDTO getPopularItem() {
        List<Item> items = Services.get().market().getMostTraded(1);
        return items.isEmpty() ? null : toItemDto(items.getFirst());
    }

    private ItemDTO toItemDto(Item item) {
        double change = Math.round(item.getPrice().getValueChangeLastHour() * 10.0d) / 10.0d;
        return new ItemDTO(
                item.getIdentifier(),
                item.getName(),
                item.getPrice().getValue(),
                item.getPrice().getBuyPrice(),
                item.getPrice().getSellPrice(),
                item.getOperations(),
                change
        );
    }

    private List<TimeSeriesDTO> getCpiTimeSeries() {
        List<TimeSeriesDTO> result = new ArrayList<>();
        List<CPIInstant> history = DatabaseManager.get().getDatabase().getCPIHistory();
        if (history == null) return result;

        for (CPIInstant instant : history) {
            if (instant == null || instant.getLocalDateTime() == null) continue;
            result.add(new TimeSeriesDTO(
                    instant.getLocalDateTime().toEpochSecond(ZoneOffset.UTC),
                    instant.getIndexValue()
            ));
        }
        return result;
    }

    private List<TimeSeriesDTO> getItemTimeSeries(String identifier) {
        Item item = Services.get().market().getItem(identifier);
        if (item == null) return null;

        List<TimeSeriesDTO> result = new ArrayList<>();
        Set<Long> timestamps = new HashSet<>();
        List<Instant> prices = DatabaseManager.get().getDatabase().getAllPrices(item);
        if (prices == null) return result;

        for (Instant instant : prices) {
            if (instant == null || instant.getLocalDateTime() == null || instant.getPrice() == 0) continue;
            long time = instant.getLocalDateTime().toEpochSecond(ZoneOffset.UTC);
            if (timestamps.add(time)) result.add(new TimeSeriesDTO(time, instant.getPrice()));
        }
        return result;
    }

    private List<CategoryDTO> getCategories() {
        List<CategoryDTO> result = new ArrayList<>();
        for (Category category : Services.get().market().getCategories()) {
            double change = 0.0d;
            try {
                if (!category.getItems().isEmpty()) change = category.getDayChange();
            } catch (RuntimeException ignored) {
                // A fresh database may not have enough day-history yet.
            }
            if (!Double.isFinite(change)) change = 0.0d;
            result.add(new CategoryDTO(category.getIdentifier(), category.getDisplayName(), change));
        }
        return result;
    }

    private List<PortfolioDTO> getTopPortfolios() {
        List<PortfolioDTO> result = new ArrayList<>();
        Database database = DatabaseManager.get().getDatabase();
        HashMap<UUID, Portfolio> top = database.getTopWorth(5);
        if (top == null) return result;

        for (Map.Entry<UUID, Portfolio> entry : top.entrySet()) {
            UUID uuid = entry.getKey();
            Portfolio portfolio = entry.getValue();
            if (uuid == null || portfolio == null) continue;

            String name = database.getNameByUUID(uuid);
            if (name == null || name.isBlank()) name = uuid.toString();
            double netValue = portfolio.getInventoryValue() - database.getDebt(uuid);
            result.add(new PortfolioDTO(name, netValue, portfolio.getContent()));
        }
        return result;
    }

    private Map<String, Object> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("title", webConfig.title());
        config.put("accent", webConfig.accent());
        config.put("defaultMode", webConfig.defaultMode());
        config.put("lockMode", webConfig.lockMode());
        config.put("defaultTheme", webConfig.defaultTheme());
        return config;
    }

    private void serveIcon(Context ctx) {
        String identifier = ctx.pathParam("identifier");
        BufferedImage image = Services.get().images().getImage(identifier);

        if (image == null && (identifier.equals("logo") || identifier.equals("logo-color") || identifier.equals("fire"))) {
            File file = new File(plugin.getDataFolder(), "images/" + identifier + ".png");
            if (file.isFile()) {
                try {
                    image = ImageIO.read(file);
                } catch (IOException exception) {
                    plugin.getLogger().log(Level.WARNING, "Could not read web image " + file, exception);
                }
            }
        }

        if (image == null) {
            ctx.status(404).result("Image not found.");
            return;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                ctx.status(500).result("Failed to encode image to PNG.");
                return;
            }
            ctx.header("Cache-Control", "public, max-age=" + TimeUnit.HOURS.toSeconds(1));
            ctx.contentType("image/png");
            ctx.result(output.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error processing image " + identifier, exception);
            ctx.status(500).result("Error processing image.");
        }
    }

    public synchronized void stopServer() {
        if (webServer == null) return;
        try {
            webServer.stop();
            plugin.getLogger().info("Web server stopped.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Error stopping web server.", exception);
        } finally {
            webServer = null;
        }
    }

    public synchronized boolean isRunning() {
        return webServer != null;
    }
}
