package me.bounser.nascraft.managers.scheduler;

import me.bounser.nascraft.Nascraft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.lang.reflect.Method;

public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Nascraft plugin;
    private final Object asyncScheduler;
    private final Object globalRegionScheduler;
    private final Method runAsync;
    private final Method runGlobal;
    private final Method runDelayedAsync;
    private final Method runDelayedGlobal;
    private final Method runAtFixedRateAsync;
    private final Method runAtFixedRateGlobal;
    private final boolean usesConsumer;

    public FoliaSchedulerAdapter(Nascraft plugin) {
        this.plugin = plugin;
        
        try {
            Method getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            asyncScheduler = getAsyncScheduler.invoke(null);
            
            Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            globalRegionScheduler = getGlobalRegionScheduler.invoke(null);
            
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            
            plugin.getLogger().info("=== AsyncScheduler available methods ===");
            for (Method method : asyncSchedulerClass.getMethods()) {
                if (method.getDeclaringClass() == asyncSchedulerClass) {
                    plugin.getLogger().info("Method: " + method.getName() + "(" + 
                        java.util.Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") + ")");
                }
            }
            
            plugin.getLogger().info("=== GlobalRegionScheduler available methods ===");
            for (Method method : globalRegionSchedulerClass.getMethods()) {
                if (method.getDeclaringClass() == globalRegionSchedulerClass) {
                    plugin.getLogger().info("Method: " + method.getName() + "(" + 
                        java.util.Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") + ")");
                }
            }
            
            boolean hasConsumerRunNow = false;
            boolean hasRunnableRunNow = false;
            
            try {
                asyncSchedulerClass.getMethod("runNow", Plugin.class, Consumer.class);
                hasConsumerRunNow = true;
            } catch (NoSuchMethodException ignored) {}
            
            try {
                asyncSchedulerClass.getMethod("runNow", Plugin.class, Runnable.class);
                hasRunnableRunNow = true;
            } catch (NoSuchMethodException ignored) {}
            
            usesConsumer = hasConsumerRunNow;
            
            if (usesConsumer) {
                plugin.getLogger().info("Detected LightingLuminol Consumer-based API");
                
                runAsync = asyncSchedulerClass.getMethod("runNow", Plugin.class, Consumer.class);
                runDelayedAsync = asyncSchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                runAtFixedRateAsync = asyncSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                
                runGlobal = globalRegionSchedulerClass.getMethod("execute", Plugin.class, Runnable.class);
                runDelayedGlobal = globalRegionSchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                runAtFixedRateGlobal = globalRegionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                
            } else if (hasRunnableRunNow) {
                plugin.getLogger().info("Detected standard Folia Runnable-based API");
                
                runAsync = asyncSchedulerClass.getMethod("runNow", Plugin.class, Runnable.class);
                runDelayedAsync = asyncSchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class, TimeUnit.class);
                runAtFixedRateAsync = asyncSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class, TimeUnit.class);
                
                runGlobal = globalRegionSchedulerClass.getMethod("run", Plugin.class, Runnable.class);
                runDelayedGlobal = globalRegionSchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class);
                runAtFixedRateGlobal = globalRegionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class);
                
            } else {
                throw new RuntimeException("No compatible Folia scheduler methods found");
            }
            
            plugin.getLogger().info("Successfully initialized Folia scheduler adapter (usesConsumer: " + usesConsumer + ")");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Folia scheduler adapter");
            throw new RuntimeException("Failed to initialize Folia scheduler", e);
        }
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (usesConsumer) {
                Consumer<Object> consumer = (scheduledTask) -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                };
                runAsync.invoke(asyncScheduler, plugin, consumer);
            } else {
                Runnable wrappedTask = () -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                };
                runAsync.invoke(asyncScheduler, plugin, wrappedTask);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runGlobal(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (usesConsumer && !runGlobal.getName().equals("execute")) {
                Consumer<Object> consumer = (scheduledTask) -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                };
                runGlobal.invoke(globalRegionScheduler, plugin, consumer);
            } else {
                Runnable wrappedTask = () -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                };
                runGlobal.invoke(globalRegionScheduler, plugin, wrappedTask);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runForEntity(Entity entity, Consumer<Entity> task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            Runnable wrappedTask = () -> {
                try {
                    task.accept(entity);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            };
            
            if (usesConsumer && !runGlobal.getName().equals("execute")) {
                Consumer<Object> consumer = (scheduledTask) -> wrappedTask.run();
                runGlobal.invoke(globalRegionScheduler, plugin, consumer);
            } else {
                runGlobal.invoke(globalRegionScheduler, plugin, wrappedTask);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runAtLocation(Location location, Consumer<Location> task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            Runnable wrappedTask = () -> {
                try {
                    task.accept(location);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            };
            
            if (usesConsumer && !runGlobal.getName().equals("execute")) {
                Consumer<Object> consumer = (scheduledTask) -> wrappedTask.run();
                runGlobal.invoke(globalRegionScheduler, plugin, consumer);
            } else {
                runGlobal.invoke(globalRegionScheduler, plugin, wrappedTask);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public int scheduleAsyncRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        try {
            long initialDelayMs = initialDelayTicks * 50;
            long periodMs = periodTicks * 50;
            
            if (usesConsumer) {
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                Object taskObj = runAtFixedRateAsync.invoke(asyncScheduler, plugin, consumer, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
                return taskObj.hashCode();
            } else {
                Object taskObj = runAtFixedRateAsync.invoke(asyncScheduler, plugin, task, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
                return taskObj.hashCode();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule async repeating task: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public int scheduleGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        try {
            if (usesConsumer && !runAtFixedRateGlobal.getName().equals("execute")) {
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                Object taskObj = runAtFixedRateGlobal.invoke(globalRegionScheduler, plugin, consumer, initialDelayTicks, periodTicks);
                return taskObj.hashCode();
            } else {
                Object taskObj = runAtFixedRateGlobal.invoke(globalRegionScheduler, plugin, task, initialDelayTicks, periodTicks);
                return taskObj.hashCode();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule global repeating task: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public void cancelTask(int taskId) {
        plugin.getLogger().warning("Task cancellation not implemented for Folia scheduler (taskId: " + taskId + ")");
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }
} 