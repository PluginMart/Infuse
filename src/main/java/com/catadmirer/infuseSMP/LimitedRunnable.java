package com.catadmirer.infuseSMP;

import java.util.function.IntConsumer;
import org.bukkit.scheduler.BukkitRunnable;

public class LimitedRunnable extends BukkitRunnable {
    private final IntConsumer task;
    private final int iterations;
    private int iteration = 0;

    public LimitedRunnable(int iterations, Runnable task) {
        this(iterations, i -> task.run());
    }

    public LimitedRunnable(int iterations, IntConsumer task) {
        this.iterations = iterations;
        this.task = task;
    }

    @Override
    public void run() {
        if (iteration == iterations) {
            cancel();
            return;
        }

        task.accept(iteration);
        iteration++;
    }
}
