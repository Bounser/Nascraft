package me.bounser.nascraft.web;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.market.MarketManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class WebServerManager {


    private final JavaPlugin plugin;
    private Javalin webServer;
    private final int port;

    private final String externalWebRootPath;

    public WebServerManager(JavaPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;

        externalWebRootPath = plugin.getDataFolder() + "/web";
    }

    public void startServer() {
        if (webServer != null) {
            plugin.getLogger().warning("Web server is already running!");
            return;
        }

        File webDir = new File(externalWebRootPath);
        if (!webDir.exists() || !webDir.isDirectory()) {
            plugin.getLogger().severe("-------------------------------------------------------");
            plugin.getLogger().severe("External web directory not found or is not a directory!");
            plugin.getLogger().severe("Path: " + externalWebRootPath);
            plugin.getLogger().severe("Please ensure the directory was created correctly (check permissions?).");
            plugin.getLogger().severe("Web server cannot start.");
            plugin.getLogger().severe("-------------------------------------------------------");
            return;
        }

        try {
            webServer = Javalin.create(config -> {
                config.showJavalinBanner = false;

                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = externalWebRootPath;
                    staticFiles.location = Location.EXTERNAL;
                });

            }).start(port);

            plugin.getLogger().info("Web server started successfully on port " + port);

            webServer.get("/api/items", ctx -> {
                List<ItemDTO> items = MarketManager.getInstance().getAllItemData();

                if (items == null) {
                    items = new ArrayList<>();
                }
                ctx.json(items);
            });

            webServer.get("/api/top-portfolios", ctx -> {
                List<PortfolioDTO> portfolios = MarketManager.getInstance().getTopPortfolios();

                if (portfolios == null) {
                    portfolios = new ArrayList<>();
                }
                ctx.json(portfolios);
            });

            webServer.get("/api/categories", ctx -> {
                List<CategoryDTO> categories = MarketManager.getInstance().getCategoriesDTO();

                if (categories == null) {
                    categories = new ArrayList<>();
                }
                ctx.json(categories);
            });

            webServer.get("/api/charts/cpi", ctx -> {
                List<TimeSeriesDTO> dataPoints = MarketManager.getInstance().getCPITimeSeries();

                if (dataPoints == null) {
                    dataPoints = new ArrayList<>();
                }

                ctx.json(dataPoints);
            });

            webServer.get("/api/popular-item", ctx -> {
                ItemDTO popularItem = MarketManager.getInstance().getPopularItem();
                if (popularItem != null) {
                    ctx.json(popularItem);
                } else {
                    ctx.status(404).result("Popular item data not available.");
                }
            });

            webServer.get("/api/charts/item/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");

                List<TimeSeriesDTO> dataPoints = MarketManager.getInstance().getItemTimeSeries(identifier);

                if (dataPoints == null) {
                    dataPoints = new ArrayList<>();
                }

                ctx.json(dataPoints);
            });

            webServer.get("/api/icons/{identifier}.png", ctx -> {
                String identifier = ctx.pathParam("identifier");
                BufferedImage image = ImagesManager.getInstance().getImage(identifier); // Your method call

                if (image != null) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        boolean written = ImageIO.write(image, "png", baos);

                        if (!written) {
                            plugin.getLogger().warning("ImageIO failed to find writer for PNG format.");
                            ctx.status(500).result("Failed to encode image to PNG.");
                            return;
                        }

                        byte[] imageBytes = baos.toByteArray();

                        ctx.header("Cache-Control", "public, max-age=" + TimeUnit.HOURS.toSeconds(1));
                        ctx.contentType("image/png");
                        ctx.result(imageBytes);

                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "IOException writing image " + identifier + " to byte array", e);
                        ctx.status(500).result("Error processing image.");
                    }
                } else {
                    plugin.getLogger().warning("No image found for identifier: " + identifier);
                    ctx.status(404).result("Image not found.");
                }
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,"Failed to start web server on port " + port + ": " + e.getMessage(), e);
            webServer = null;
        }
    }

    public void stopServer() {
        if (webServer != null) {
            try {
                webServer.stop();
                plugin.getLogger().info("Web server stopped.");
                webServer = null;
            } catch (Exception e) {
                plugin.getLogger().severe("Error stopping web server: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("Web server is not running.");
        }
    }

    public boolean isRunning() {
        return webServer != null;
    }

}
