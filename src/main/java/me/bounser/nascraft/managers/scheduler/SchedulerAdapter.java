package me.bounser.nascraft.managers.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides a unified API for scheduling tasks in both Folia and regular Bukkit environments.
 */
public interface SchedulerAdapter {
    /**
     * Run a task asynchronously without any region context.
     * 
     * @param task The task to run
     * @return A CompletableFuture that completes when the task is done
     */
    CompletableFuture<Void> runAsync(Runnable task);
    
    /**
     * Run a task on the global region (main thread in Bukkit).
     * 
     * @param task The task to run
     * @return A CompletableFuture that completes when the task is done
     */
    CompletableFuture<Void> runGlobal(Runnable task);
    
    /**
     * Run a task in the region of the specified entity.
     * 
     * @param entity The entity whose region should run the task
     * @param task The task to run
     * @return A CompletableFuture that completes when the task is done
     */
    CompletableFuture<Void> runForEntity(Entity entity, Consumer<Entity> task);
    
    /**
     * Run a task in the region of the specified location.
     * 
     * @param location The location whose region should run the task
     * @param task The task to run
     * @return A CompletableFuture that completes when the task is done
     */
    CompletableFuture<Void> runAtLocation(Location location, Consumer<Location> task);
    
    /**
     * Schedule a repeating task to run asynchronously.
     * 
     * @param task The task to run
     * @param initialDelayTicks Initial delay in ticks before first execution
     * @param periodTicks Period between successive executions in ticks
     * @return A task ID that can be used to cancel the task
     */
    int scheduleAsyncRepeating(Runnable task, long initialDelayTicks, long periodTicks);
    
    /**
     * Schedule a repeating task to run on the global region (main thread in Bukkit).
     * 
     * @param task The task to run
     * @param initialDelayTicks Initial delay in ticks before first execution
     * @param periodTicks Period between successive executions in ticks
     * @return A task ID that can be used to cancel the task
     */
    int scheduleGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks);
    
    /**
     * Cancel a scheduled task.
     * 
     * @param taskId The ID of the task to cancel
     */
    void cancelTask(int taskId);
    
    /**
     * Check if the current thread is the main server thread.
     * 
     * @return true if this is the main server thread, false otherwise
     */
    boolean isMainThread();
    
    /**
     * Check if Folia is available.
     * 
     * @return true if Folia is available, false otherwise
     */
    boolean isFolia();
} 