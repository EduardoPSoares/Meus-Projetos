package com.midgard.core.particle;

import com.midgard.core.MidgardCore;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/**
 * Animated particle effect that runs over time.
 */
public class ParticleAnimation {

    private final Consumer<AnimationContext> frameHandler;
    private final int totalFrames;
    private final long intervalTicks;
    private BukkitTask task;

    public ParticleAnimation(Consumer<AnimationContext> frameHandler, int totalFrames, long intervalTicks) {
        this.frameHandler = frameHandler;
        this.totalFrames = totalFrames;
        this.intervalTicks = intervalTicks;
    }

    public void start(Location origin) {
        AnimationContext context = new AnimationContext(origin, totalFrames);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (context.getCurrentFrame() >= totalFrames) {
                    cancel();
                    return;
                }
                frameHandler.accept(context);
                context.nextFrame();
            }
        }.runTaskTimer(MidgardCore.getInstance(), 0L, intervalTicks);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public static class AnimationContext {
        private final Location origin;
        private final int totalFrames;
        private int currentFrame = 0;

        public AnimationContext(Location origin, int totalFrames) {
            this.origin = origin;
            this.totalFrames = totalFrames;
        }

        public Location getOrigin() { return origin; }
        public int getCurrentFrame() { return currentFrame; }
        public int getTotalFrames() { return totalFrames; }
        public double getProgress() { return (double) currentFrame / totalFrames; }

        void nextFrame() { currentFrame++; }
    }
}
