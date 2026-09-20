package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstract class that helps with handling data asynchronously.
 */
@NullMarked
public abstract class AsyncDataManager implements DataManager {
    protected ExecutorService executorService;

    public AsyncDataManager() {
        executorService = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());
    }

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallySetExistingCount(effect, count));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallySetExistingCount(effect, count);
    }

    protected abstract void reallySetExistingCount(InfuseEffect effect, int count);

    @Override
    public void setTrusted(OfflinePlayer player, Set<OfflinePlayer> trusted) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallySetTrusted(player, trusted));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallySetTrusted(player, trusted);
    }

    protected abstract void reallySetTrusted(OfflinePlayer player, Set<OfflinePlayer> trusted);

    @Override
    public void addTrust(OfflinePlayer player, OfflinePlayer trusted) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallyAddTrust(player, trusted));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallyAddTrust(player, trusted);
    }

    protected abstract void reallyAddTrust(OfflinePlayer player, OfflinePlayer trusted);

    @Override
    public void removeTrust(OfflinePlayer player, OfflinePlayer untrusted) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallyRemoveTrust(player, untrusted));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallyRemoveTrust(player, untrusted);
    }

    protected abstract void reallyRemoveTrust(OfflinePlayer player, OfflinePlayer untrusted);

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallySetEffect(player, slot, effect));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallySetEffect(player, slot, effect);
    }

    protected abstract void reallySetEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect);

    @Override
    public void setControlMode(OfflinePlayer player, String controlMode) {
        // Attempts to update the database asynchronously, but falls back to synchronous execution if there is an error or the ExecutorService is closed.
        if (!executorService.isShutdown()) {
            executorService.execute(() -> reallySetControlMode(player, controlMode));
            return;
        }

        Infuse.LOGGER.warn("Could not write data to the database asynchronously");
        reallySetControlMode(player, controlMode);
    }

    protected abstract void reallySetControlMode(OfflinePlayer player, String controlMode);
}
