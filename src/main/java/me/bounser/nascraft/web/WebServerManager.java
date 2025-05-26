package me.bounser.nascraft.web;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.Duration;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.bounser.nascraft.web.dto.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.mindrot.jbcrypt.BCrypt;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import com.google.gson.Gson; // Keep Gson for parsing Discord's JSON response
import com.google.gson.JsonObject; // Keep Gson for parsing Discord's JSON response


public class WebServerManager {

    private final JavaPlugin plugin;
    private Javalin webServer;
    private final int port;
    private final String externalWebRootPath;
    private final Database database;
    private final MarketManager marketManager;
    private final PortfoliosManager portfoliosManager;
    private final MoneyManager moneyManager;
    private final HttpClient httpClient;
    private final Gson gson; // Gson is still needed for parsing external JSON (Discord API)
    private final CodesManager codesManager;

    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";


    public WebServerManager(JavaPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
        this.externalWebRootPath = plugin.getDataFolder() + "/web";
        this.database = DatabaseManager.get().getDatabase();
        this.marketManager = MarketManager.getInstance();
        this.portfoliosManager = PortfoliosManager.getInstance();
        this.moneyManager = MoneyManager.getInstance();
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson(); // Initialize Gson
        this.codesManager = CodesManager.getInstance();
    }

    private static final Handler requireMinecraftLink = ctx -> {
        HttpSession session = ctx.req().getSession(false);
        if (session == null || session.getAttribute("minecraft-uuid") == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(new StatusResponse("Unauthorized: Minecraft account not logged in or session invalid. Please log in."));
            ctx.skipRemainingHandlers();
        }
    };

    public void startServer() {
        if (webServer != null) {
            plugin.getLogger().warning("Web server is already running!");
            return;
        }

        File webDir = new File(externalWebRootPath);
        if (!webDir.exists() || !webDir.isDirectory()) {
            plugin.getLogger().severe("External web directory not found: " + externalWebRootPath);
            return;
        }

        try {
            webServer = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = externalWebRootPath;
                    staticFiles.location = Location.EXTERNAL;
                    staticFiles.precompress = false;
                });
            }).start(port);

            plugin.getLogger().info("Web server started successfully on port " + port);

            // --- Public Endpoints ---
            // (Existing public endpoints - truncated for brevity)
            webServer.get("/api/items", ctx -> {
                List<ItemDTO> items = marketManager.getAllItemData();
                ctx.json(items != null ? items : new ArrayList<>());
            });
            webServer.get("/api/buy-projected/{quantity}/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                String quantityStr = ctx.pathParam("quantity");
                try {
                    int quantity = Integer.parseInt(quantityStr);
                    Item item = MarketManager.getInstance().getItem(identifier);
                    if (item == null) {
                        ctx.status(HttpStatus.NOT_FOUND).json(new StatusResponse("Item not found: " + identifier));
                        return;
                    }
                    double cost = item.getPrice().getProjectedCost(-quantity, 1);
                    ctx.json(cost);
                } catch (NumberFormatException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid quantity format."));
                }
            });
            webServer.get("/api/sell-projected/{quantity}/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                String quantityStr = ctx.pathParam("quantity");
                try {
                    int quantity = Integer.parseInt(quantityStr);
                    Item item = MarketManager.getInstance().getItem(identifier);
                    if (item == null) {
                        ctx.status(HttpStatus.NOT_FOUND).json(new StatusResponse("Item not found: " + identifier));
                        return;
                    }
                    double revenue = item.getPrice().getProjectedCost(quantity, 1);
                    ctx.json(revenue);
                } catch (NumberFormatException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid quantity format."));
                }
            });
            webServer.get("/api/top-portfolios", ctx -> {
                List<PortfolioDTO> portfolios = marketManager.getTopPortfolios();
                ctx.json(portfolios != null ? portfolios : new ArrayList<>());
            });
            webServer.get("/api/categories", ctx -> {
                List<CategoryDTO> categories = marketManager.getCategoriesDTO();
                ctx.json(categories != null ? categories : new ArrayList<>());
            });
            webServer.get("/api/charts/cpi", ctx -> {
                List<TimeSeriesDTO> dataPoints = marketManager.getCPITimeSeries();
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/charts/money-supply", ctx -> {
                List<TimeSeriesDTO> dataPoints = marketManager.getMoneySupply();
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/popular-item", ctx -> {
                ItemDTO popularItem = marketManager.getPopularItem();
                if (popularItem != null) {
                    ctx.json(popularItem);
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).json(new StatusResponse("Popular item data not available."));
                }
            });
            webServer.get("/api/charts/item/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                List<ItemTimeSeriesDTO> dataPoints = marketManager.getItemTimeSeries(identifier);
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/charts/item-day/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                List<ItemTimeSeriesDTO> dataPoints = marketManager.getItemTimeSeriesDay(identifier);
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/charts/item-month/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                List<ItemTimeSeriesDTO> dataPoints = marketManager.getItemTimeSeriesMonth(identifier);
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/charts/taxes", ctx -> {
                List<TimeSeriesDTO> dataPoints = marketManager.getAllTaxesCollected();
                ctx.json(dataPoints != null ? dataPoints : new ArrayList<>());
            });
            webServer.get("/api/limits", ctx -> {
                List<Duration> durationOptions = LimitOrdersManager.getInstance().getDurationOptions();
                ctx.json(durationOptions != null ? durationOptions : new ArrayList<>());
            });
            webServer.get("/api/portfolio-limit", ctx -> {
                ctx.json(Config.getInstance().getPortfolioMaxStorage());
            });
            webServer.get("/api/taxes/buy/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                double taxRate = Config.getInstance().getTaxBuyPercentage(identifier);
                ctx.json(taxRate);
            });
            webServer.get("/api/taxes/sell/{identifier}", ctx -> {
                String identifier = ctx.pathParam("identifier");
                double taxRate = Config.getInstance().getTaxSellPercentage(identifier);
                ctx.json(taxRate);
            });
            webServer.get("/api/icons/{identifier}.png", ctx -> {
                String identifier = ctx.pathParam("identifier");
                BufferedImage image = ImagesManager.getInstance().getImage(identifier);
                if (image != null) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        ImageIO.write(image, "png", baos);
                        byte[] imageBytes = baos.toByteArray();
                        ctx.header("Cache-Control", "public, max-age=" + TimeUnit.HOURS.toSeconds(1));
                        ctx.contentType("image/png").result(imageBytes);
                    } catch (IOException e) {
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Error processing image."));
                    }
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).json(new StatusResponse("Image not found."));
                }
            });
            webServer.get("/api/last-transactions", ctx -> {
                List<DetailedTransactionDTO> transactions = marketManager.getLastTransactions();
                ctx.json(transactions != null ? transactions : new ArrayList<>());
            });
            webServer.get("/api/server-time", ctx -> {
                HashMap<String, Long> timeMap = new HashMap<>(); // Jackson can serialize Maps
                timeMap.put("time", System.currentTimeMillis() / 1000);
                ctx.json(timeMap);
            });
            webServer.get("/api/code-time/{code}", ctx -> {
                String codeStr = ctx.pathParam("code");
                try {
                    int code = Integer.parseInt(codeStr);
                    long time = codesManager.getEpochTimeOfCode(code);
                    if (time == 0) {
                        ctx.status(HttpStatus.NOT_FOUND).json(new StatusResponse("Code not found or expired."));
                        return;
                    }
                    ctx.json(time); // Jackson can serialize primitive long
                } catch (NumberFormatException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid code format."));
                }
            });
            webServer.get("/api/code-expiration", ctx -> {
                int minutes = Config.getInstance().getWebCodeExpiration();
                ctx.json(minutes); // Jackson can serialize primitive int
            });


            // --- Minecraft Authentication ---
            webServer.post("/api/login", ctx -> {
                LoginRequest loginReq = ctx.bodyAsClass(LoginRequest.class);
                if (loginReq.getUsername() == null || loginReq.getUsername().isBlank() ||
                        loginReq.getPassword() == null || loginReq.getPassword().isBlank()) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Username and password are required."));
                    return;
                }

                String storedHash = database.retrieveHash(loginReq.getUsername());

                if (storedHash == null) {
                    ctx.status(HttpStatus.UNAUTHORIZED).json(new StatusResponse("Invalid username or password."));
                    return;
                }

                if (BCrypt.checkpw(loginReq.getPassword(), storedHash)) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(loginReq.getUsername());
                    UUID playerUUID = player.getUniqueId();

                    if (playerUUID == null) {
                        plugin.getLogger().warning("Could not resolve UUID for username: " + loginReq.getUsername() + " during web login.");
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Could not verify player identity."));
                        return;
                    }

                    HttpSession session = ctx.req().getSession(true);
                    String actualUsername = player.getName() != null ? player.getName() : loginReq.getUsername();
                    session.setAttribute("minecraft-uuid", playerUUID.toString());
                    session.setAttribute("minecraft-user-name", actualUsername);
                    session.setMaxInactiveInterval(Config.getInstance().getWebTimeout() * 60);

                    String discordUserId = database.getDiscordUserId(playerUUID);
                    if (discordUserId != null) {
                        session.setAttribute("discord-user-id", discordUserId);
                        String discordNickname = database.getNicknameFromUserId(discordUserId);
                        session.setAttribute("discord-user-nickname", discordNickname != null ? discordNickname : "Linked Discord");
                    }

                    plugin.getLogger().info("User logged in via Minecraft: " + actualUsername);
                    // Use the new POJO for the response
                    ctx.json(new LoginSuccessResponse(true, actualUsername));

                } else {
                    ctx.status(HttpStatus.UNAUTHORIZED).json(new StatusResponse("Invalid username or password."));
                }
            });

            // --- Discord Related Endpoints ---
            webServer.get("/api/discord-client-id", ctx -> {
                String clientId = Config.getInstance().getDiscordId();
                if (clientId == null || clientId.isBlank()) {
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Discord integration not configured."));
                    return;
                }
                // Use the new POJO for the response
                ctx.json(new DiscordClientIdResponse(clientId));
            });

            webServer.get("/api/auth/discord/login", ctx -> {
                String clientId = Config.getInstance().getDiscordId();
                if (clientId == null || clientId.isBlank()) {
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Discord login is not configured."));
                    return;
                }
                String state = generateSecureRandomString(32);
                HttpSession session = ctx.req().getSession(true);
                session.setAttribute("oauth-state", state);
                session.setMaxInactiveInterval(Config.getInstance().getWebTimeout() * 60);
                String redirectUri = ctx.scheme() + "://" + ctx.host() + "/auth/discord/callback";
                String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                String scope = URLEncoder.encode("identify email", StandardCharsets.UTF_8);
                String discordAuthUrl = DISCORD_API_BASE + "/oauth2/authorize?client_id=" + clientId +
                        "&redirect_uri=" + encodedRedirectUri + "&response_type=code&scope=" + scope + "&state=" + state;
                ctx.redirect(discordAuthUrl, HttpStatus.FOUND);
            });

            webServer.get("/auth/discord/callback", ctx -> {
                String code = ctx.queryParam("code");
                String state = ctx.queryParam("state");
                HttpSession session = ctx.req().getSession(false);
                String storedState = (session != null) ? (String) session.getAttribute("oauth-state") : null;

                if (state == null || storedState == null || !state.equals(storedState)) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid state."));
                    if (session != null) session.removeAttribute("oauth-state");
                    return;
                }
                if (session != null) session.removeAttribute("oauth-state");
                if (code == null) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Code missing."));
                    return;
                }

                String clientId = Config.getInstance().getDiscordId();
                String clientSecret = Config.getInstance().getDiscordSecret();
                String redirectUri = ctx.scheme() + "://" + ctx.host() + "/auth/discord/callback";

                String tokenRequestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                        "&grant_type=authorization_code&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(URI.create(DISCORD_API_BASE + "/oauth2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody)).build();
                try {
                    HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
                    // Use Gson here as we are parsing an external API's response
                    com.google.gson.JsonObject tokenJson = gson.fromJson(tokenResponse.body(), com.google.gson.JsonObject.class);
                    if (tokenResponse.statusCode() != 200 || !tokenJson.has("access_token")) {
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Discord token exchange failed."));
                        return;
                    }
                    String accessToken = tokenJson.get("access_token").getAsString();
                    HttpRequest userRequest = HttpRequest.newBuilder()
                            .uri(URI.create(DISCORD_API_BASE + "/users/@me"))
                            .header("Authorization", "Bearer " + accessToken).GET().build();
                    HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
                    com.google.gson.JsonObject userJson = gson.fromJson(userResponse.body(), com.google.gson.JsonObject.class);
                    if (userResponse.statusCode() != 200 || !userJson.has("id")) {
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Failed to fetch Discord user."));
                        return;
                    }

                    String discordUserId = userJson.get("id").getAsString();
                    String discordGlobalName = userJson.has("global_name") && !userJson.get("global_name").isJsonNull() ? userJson.get("global_name").getAsString() : userJson.get("username").getAsString();
                    String displayNickname = discordGlobalName;

                    HttpSession currentSession = ctx.req().getSession(true);
                    String minecraftUuidFromSessionStr = (String) currentSession.getAttribute("minecraft-uuid");

                    if (minecraftUuidFromSessionStr != null) {
                        UUID minecraftUUID = UUID.fromString(minecraftUuidFromSessionStr);
                        UUID discordAlreadyLinkedToMcUUID = database.getUUIDFromUserid(discordUserId);
                        if (discordAlreadyLinkedToMcUUID != null && !discordAlreadyLinkedToMcUUID.equals(minecraftUUID)) {
                            ctx.redirect("/?error=discord_already_linked_other", HttpStatus.FOUND);
                            return;
                        }
                        database.saveDiscordLink(minecraftUUID, discordUserId, displayNickname);
                        currentSession.setAttribute("discord-user-id", discordUserId);
                        currentSession.setAttribute("discord-user-nickname", displayNickname);
                        ctx.redirect("/?link_status=success", HttpStatus.FOUND);
                    } else {
                        UUID existingMinecraftUUID = database.getUUIDFromUserid(discordUserId);
                        if (existingMinecraftUUID != null) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(existingMinecraftUUID);
                            String minecraftUsername = player.getName() != null ? player.getName() : database.getNicknameFromUserId(discordUserId);
                            if (minecraftUsername == null) {
                                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Error retrieving linked MC account."));
                                return;
                            }
                            currentSession.setAttribute("minecraft-uuid", existingMinecraftUUID.toString());
                            currentSession.setAttribute("minecraft-user-name", minecraftUsername);
                            currentSession.setAttribute("discord-user-id", discordUserId);
                            currentSession.setAttribute("discord-user-nickname", displayNickname);
                            ctx.redirect("/", HttpStatus.FOUND);
                        } else {
                            currentSession.setAttribute("pending-discord-user-id", discordUserId);
                            currentSession.setAttribute("pending-discord-user-nickname", displayNickname);
                            String linkingCode = String.valueOf(codesManager.generateCode(discordUserId, displayNickname));
                            if (linkingCode == null || "null".equals(linkingCode)) {
                                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Failed to generate linking code."));
                                return;
                            }
                            ctx.redirect("/?view=linkAccount&code=" + URLEncoder.encode(linkingCode, StandardCharsets.UTF_8) +
                                    "&user=" + URLEncoder.encode(discordUserId, StandardCharsets.UTF_8) +
                                    "&displayName=" + URLEncoder.encode(displayNickname, StandardCharsets.UTF_8), HttpStatus.FOUND);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Discord auth error."));
                }
            });

            webServer.get("/api/auth/status", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                if (session == null) {
                    ctx.json(new AuthStatusResponse("unauthenticated", null, null, null, null, null, "Not authenticated."));
                    return;
                }
                String minecraftUuidStr = (String) session.getAttribute("minecraft-uuid");
                String minecraftUsername = (String) session.getAttribute("minecraft-user-name");
                String discordUserIdInSession = (String) session.getAttribute("discord-user-id");
                String discordNicknameInSession = (String) session.getAttribute("discord-user-nickname");

                if (minecraftUuidStr != null) {
                    UUID mcUUID = UUID.fromString(minecraftUuidStr);
                    String actualDiscordUserId = database.getDiscordUserId(mcUUID);
                    String actualDiscordNickname = null;
                    if (actualDiscordUserId != null) {
                        actualDiscordNickname = database.getNicknameFromUserId(actualDiscordUserId);
                        if(actualDiscordNickname == null && discordNicknameInSession !=null && actualDiscordUserId.equals(discordUserIdInSession)) {
                            actualDiscordNickname = discordNicknameInSession;
                        } else if (actualDiscordNickname == null) {
                            actualDiscordNickname = "Linked Discord";
                        }
                        if(!actualDiscordUserId.equals(discordUserIdInSession) || (actualDiscordNickname != null && !actualDiscordNickname.equals(discordNicknameInSession))) {
                            session.setAttribute("discord-user-id", actualDiscordUserId);
                            session.setAttribute("discord-user-nickname", actualDiscordNickname);
                        }
                    }
                    ctx.json(new AuthStatusResponse("minecraft_authenticated", minecraftUsername, actualDiscordUserId, actualDiscordNickname, actualDiscordUserId != null ? "linked" : "not_linked", null, "User authenticated via Minecraft."));
                } else {
                    String pendingDiscordUserId = (String) session.getAttribute("pending-discord-user-id");
                    String pendingDiscordNickname = (String) session.getAttribute("pending-discord-user-nickname");
                    if (pendingDiscordUserId != null) {
                        String linkingCode = String.valueOf(codesManager.getCodeForDiscordUser(pendingDiscordUserId));
                        if ("null".equals(linkingCode)) linkingCode = null;
                        ctx.json(new AuthStatusResponse("discord_auth_pending_minecraft_link", null, pendingDiscordUserId, pendingDiscordNickname, null, linkingCode, "Discord authenticated. Minecraft account linking is pending."));
                    } else {
                        ctx.json(new AuthStatusResponse("unauthenticated", null, null, null, null, null, "Not authenticated."));
                    }
                }
            });

            webServer.post("/api/discord/unlink", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String minecraftUuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID minecraftUUID = UUID.fromString(minecraftUuidStr);
                database.removeDiscordLink(minecraftUUID);
                session.removeAttribute("discord-user-id");
                session.removeAttribute("discord-user-nickname");
                plugin.getLogger().info("User " + session.getAttribute("minecraft-user-name") + " unlinked their Discord account.");
                ctx.json(new StatusResponse("Discord account unlinked successfully."));
            });

            webServer.post("/api/logout", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                if (session != null) session.invalidate();
                Cookie cookie = new Cookie("JSESSIONID", "");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                ctx.res().addCookie(cookie);
                ctx.json(new StatusResponse("Logged out successfully."));
            });

            // --- Endpoints Requiring Full Minecraft Account Link ---
            String[] protectedPaths = {
                    "/api/balance", "/api/portfolio-value", "/api/portfolio-capacity",
                    "/api/history", "/api/stats", "/api/debt", "/api/portfolio", "/api/trade",
                    "/api/buy-slot", "/api/discord/unlink"
            };
            for (String path : protectedPaths) {
                webServer.before(path, requireMinecraftLink);
            }

            // (Protected endpoints - truncated for brevity)
            webServer.get("/api/balance", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID uuid = UUID.fromString(uuidStr);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                double balance = moneyManager.getBalance(player, CurrenciesManager.getInstance().getDefaultCurrency());
                ctx.json(balance);
            });
            webServer.get("/api/portfolio-value", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID uuid = UUID.fromString(uuidStr);
                Portfolio portfolio = portfoliosManager.getPortfolio(uuid);
                double value = (portfolio != null) ? portfolio.getValueOfDefaultCurrency() : 0.0;
                ctx.json(value);
            });
            webServer.get("/api/portfolio-capacity", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID uuid = UUID.fromString(uuidStr);
                Portfolio portfolio = portfoliosManager.getPortfolio(uuid);
                int slots = (portfolio != null) ? portfolio.getCapacity() : 0;
                ctx.json(slots);
            });
            webServer.get("/api/history", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String username = (String) session.getAttribute("minecraft-user-name");
                List<TransactionDTO> transactions = marketManager.getHistoryPlayer(username);
                ctx.json(transactions != null ? transactions : new ArrayList<>());
            });
            webServer.get("/api/stats", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                List<PlayerStatsDTO> stats = marketManager.getPlayerStats(uuidStr);
                ctx.json(stats != null ? stats : new ArrayList<>());
            });
            webServer.get("/api/debt", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String username = (String) session.getAttribute("minecraft-user-name");
                DebtDTO debt = marketManager.getDebtPlayer(username);
                if (debt == null) {
                    ctx.json(new DebtDTO(0, 0, 0, 0, 0, 0, "00:00"));
                } else {
                    ctx.json(debt);
                }
            });
            webServer.get("/api/next-slot-price", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID uuid = UUID.fromString(uuidStr);
                Portfolio portfolio = portfoliosManager.getPortfolio(uuid);
                double price = (portfolio != null) ? portfolio.getNextSlotPrice() : 0;
                ctx.json(price);
            });
            webServer.get("/api/portfolio", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                UUID uuid = UUID.fromString(uuidStr);
                Portfolio portfolio = portfoliosManager.getPortfolio(uuid);
                HashMap<String, Integer> content = new HashMap<>();
                if (portfolio != null) {
                    HashMap<Item, Integer> portfolioContent = portfolio.getContent();
                    for (Item item : portfolioContent.keySet()) {
                        content.put(item.getIdentifier(), portfolioContent.get(item));
                    }
                }
                ctx.json(content);
            });
            webServer.post("/api/trade", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");
                SimplifiedTradeRequest tradeReq;
                try {
                    tradeReq = ctx.bodyAsClass(SimplifiedTradeRequest.class);
                    if (tradeReq.getIdentifier() == null || tradeReq.getIdentifier().trim().isEmpty() ||
                            tradeReq.getQuantity() <= 0 ||
                            (tradeReq.getType() == null ||
                                    (!tradeReq.getType().equalsIgnoreCase("BUY") && !tradeReq.getType().equalsIgnoreCase("SELL")))) {
                        ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid trade request parameters."));
                        return;
                    }
                    Item item = marketManager.getItem(tradeReq.getIdentifier());
                    if (item == null) {
                        ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Item not found: " + tradeReq.getIdentifier()));
                        return;
                    }
                } catch (Exception e) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(new StatusResponse("Invalid request format."));
                    return;
                }
                java.util.concurrent.Future<TradeResponse> future = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    UUID playerUUID = UUID.fromString(uuidStr);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    try {
                        Item item = marketManager.getItem(tradeReq.getIdentifier());
                        if (item == null) return new TradeResponse(false, "Item not found (sync).", null);
                        Portfolio portfolio = portfoliosManager.getPortfolio(playerUUID);
                        if (portfolio == null) return new TradeResponse(false, "Portfolio not found.", null);

                        switch (tradeReq.getType().toLowerCase()) {
                            case "buy":
                                if (!MarketManager.getInstance().getActive()) return new TradeResponse(false, "Market closed.", null);
                                if (!portfolio.hasSpace(item, tradeReq.getQuantity())) return new TradeResponse(false, "Insufficient portfolio space.", null);
                                double requiredBalance = item.getPrice().getProjectedCost(-tradeReq.getQuantity(), item.getPrice().getBuyTaxMultiplier());
                                if (!moneyManager.hasEnoughMoney(player, item.getCurrency(), requiredBalance)) return new TradeResponse(false, "Insufficient funds.", null);
                                double buyResult = item.buy(tradeReq.getQuantity(), playerUUID, false);
                                if (buyResult != 0) {
                                    portfolio.addItem(item, tradeReq.getQuantity());
                                    return new TradeResponse(true, "Buy successful.", buyResult);
                                } else return new TradeResponse(false, "Buy failed. Market conditions may have changed.", null);
                            case "sell":
                                if (!MarketManager.getInstance().getActive()) return new TradeResponse(false, "Market closed.", null);
                                if (DebtManager.getInstance().getDebtOfPlayer(playerUUID) > 0) return new TradeResponse(false, "Cannot sell with debt.", null);
                                if (!portfolio.hasItem(item, tradeReq.getQuantity())) return new TradeResponse(false, "Insufficient items.", null);
                                double sellResult = item.sell(tradeReq.getQuantity(), playerUUID, false);
                                if (sellResult != -1) {
                                    portfolio.removeItem(item, tradeReq.getQuantity());
                                    return new TradeResponse(true, "Sell successful.", sellResult);
                                } else return new TradeResponse(false, "Sell failed. Market conditions may have changed.", null);
                            default: return new TradeResponse(false, "Invalid trade type.", null);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error during sync trade for " + playerUUID, e);
                        return new TradeResponse(false, "Server error during trade.", null);
                    }
                });
                try {
                    TradeResponse tradeResult = future.get(10, TimeUnit.SECONDS);
                    ctx.status(tradeResult.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).json(tradeResult);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error processing trade future for " + uuidStr, e);
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Trade processing error."));
                }
            });

            webServer.post("/api/buy-slot", ctx -> {
                HttpSession session = ctx.req().getSession(false);
                String uuidStr = (String) session.getAttribute("minecraft-uuid");

                java.util.concurrent.Future<BuySlotResponse> future = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    UUID playerUUID = UUID.fromString(uuidStr);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    try {
                        Portfolio portfolio = portfoliosManager.getPortfolio(playerUUID);
                        if (portfolio == null) return new BuySlotResponse(false, "Portfolio not found.", null);

                        if (portfolio.getCapacity() >= 40)
                            return new BuySlotResponse(false, "Maximum portfolio size reached.", null);

                        double price = portfolio.getNextSlotPrice();

                        if (!MoneyManager.getInstance().hasEnoughMoney(player, CurrenciesManager.getInstance().getDefaultCurrency(), price))
                            return new BuySlotResponse(false, "You can't afford the expansion.", null);

                        MoneyManager.getInstance().simpleWithdraw(player, CurrenciesManager.getInstance().getDefaultCurrency(), price);

                        if (!MoneyManager.getInstance().hasEnoughMoney(player, CurrenciesManager.getInstance().getDefaultCurrency(), price))
                            return new BuySlotResponse(true, "You have expanded your portfolio.", price);

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error during sync trade for " + playerUUID, e);
                        return new BuySlotResponse(false, "Server error during trade.", null);
                    }
                    return new BuySlotResponse(false, "Error processing slot buy.", null);
                });
                try {
                    BuySlotResponse result = future.get(10, TimeUnit.SECONDS);
                    ctx.status(result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).json(result);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error processing slot buy for " + uuidStr, e);
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new StatusResponse("Slot buy processing error."));
                }
            });


        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start web server: " + e.getMessage(), e);
            webServer = null;
        }
    }

    public void stopServer() {
        if (webServer != null) {
            try {
                webServer.stop();
                plugin.getLogger().info("Web server stopped.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error stopping web server: " + e.getMessage(), e);
            } finally {
                webServer = null;
            }
        }
    }

    public boolean isRunning() {
        return webServer != null;
    }

    private String generateSecureRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[(length * 6 + 7) / 8];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // --- Static Inner Classes for DTOs ---

    public static class StatusResponse {
        private final String message;
        public StatusResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // New POJO for successful login response
    public static class LoginSuccessResponse {
        private final boolean success;
        private final String username;
        public LoginSuccessResponse(boolean success, String username) {
            this.success = success;
            this.username = username;
        }
        public boolean isSuccess() { return success; }
        public String getUsername() { return username; }
    }

    // New POJO for Discord Client ID response
    public static class DiscordClientIdResponse {
        private final String clientId;
        public DiscordClientIdResponse(String clientId) {
            this.clientId = clientId;
        }
        public String getClientId() { return clientId; }
    }


    public static class AuthStatusResponse {
        private final String authStatus;
        private final String minecraftUsername;
        private final String discordUserId;
        private final String discordNickname;
        private final String discordLinkStatus;
        private final String minecraftLinkingCode;
        private final String message;

        public AuthStatusResponse(String authStatus, String mcUsername, String dUserId, String dNickname, String dLinkStatus, String mcLinkCode, String msg) {
            this.authStatus = authStatus; this.minecraftUsername = mcUsername; this.discordUserId = dUserId;
            this.discordNickname = dNickname; this.discordLinkStatus = dLinkStatus; this.minecraftLinkingCode = mcLinkCode; this.message = msg;
        }
        public String getAuthStatus() { return authStatus; }
        public String getMinecraftUsername() { return minecraftUsername; }
        public String getDiscordUserId() { return discordUserId; }
        public String getDiscordNickname() { return discordNickname; }
        public String getDiscordLinkStatus() { return discordLinkStatus; }
        public String getMinecraftLinkingCode() { return minecraftLinkingCode; }
        public String getMessage() { return message; }
    }

    public static class SimplifiedTradeRequest {
        private String identifier;
        private int quantity;
        private String type;
        public String getIdentifier() { return identifier; }
        public int getQuantity() { return quantity; }
        public String getType() { return type; }
    }

    public static class TradeResponse {
        private final boolean success;
        private final String message;
        private final Double value;

        public TradeResponse(boolean success, String message, Double value) {
            this.success = success; this.message = message; this.value = value;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Double getValue() { return value; }
    }

    public static class BuySlotResponse {
        private final boolean success;
        private final String message;
        private final Double value;

        public BuySlotResponse(boolean success, String message, Double value) {
            this.success = success; this.message = message; this.value = value;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Double getValue() { return value; }
    }
}
