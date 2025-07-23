package me.bounser.nascraft.managers.scheduler;

import me.bounser.nascraft.Nascraft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Implementation of SchedulerAdapter for traditional Bukkit servers.
 */
public class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Nascraft plugin;

    public BukkitSchedulerAdapter(Nascraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> runGlobal(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runForEntity(Entity entity, Consumer<Entity> task) {
        // In Bukkit, all entities are on the main thread, so we just run on the main thread
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.accept(entity);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.accept(entity);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runAtLocation(Location location, Consumer<Location> task) {
        // In Bukkit, all locations are on the main thread, so we just run on the main thread
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.accept(location);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.accept(location);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public int scheduleAsyncRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks).getTaskId();
    }

    @Override
    public int scheduleGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isFolia() {
        return false;
    }
} 