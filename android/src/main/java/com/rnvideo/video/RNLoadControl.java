/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */
package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.upstream.DefaultAllocator;

@SuppressLint("UnsafeOptInUsageError")
public class RNLoadControl extends DefaultLoadControl {
    private final String TAG = "RNLoadControl";
    private int availableHeapInBytes;
    private double minBufferMemoryReservePercent;
    private final Runtime runtime;

    @SuppressLint("UnsafeOptInUsageError")
    public RNLoadControl(
            DefaultAllocator allocator,
            Context context,
            double maxHeapAllocationPercent,
            double minBufferMemoryReservePercent,
            int minBufferMs,
            int maxBufferMs,
            int bufferForPlaybackMs,
            int bufferForPlaybackAfterRebufferMs,
            int targetBufferBytes,
            boolean prioritizeTimeOverSizeThresholds,
            int backBufferDurationMs,
            boolean retainBackBufferFromKeyframe ) {
        super(
                allocator,
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs,
                targetBufferBytes,
                prioritizeTimeOverSizeThresholds,
                backBufferDurationMs,
                retainBackBufferFromKeyframe);

        this.minBufferMemoryReservePercent = minBufferMemoryReservePercent;
        runtime = Runtime.getRuntime();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        availableHeapInBytes = (int) Math.floor(activityManager.getMemoryClass() * maxHeapAllocationPercent * 1024 * 1024);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
        int loadedBytes = getAllocator().getTotalBytesAllocated();
        boolean isHeapReached = availableHeapInBytes > 0 && loadedBytes >= availableHeapInBytes;
        if (isHeapReached) {
            return false;
        }

        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeMemory = runtime.maxMemory() - usedMemory;
        long reserveMemory = (long) this.minBufferMemoryReservePercent * runtime.maxMemory();
        long bufferedMs = bufferedDurationUs / (long)1000;

        if (reserveMemory > freeMemory && bufferedMs > 2000) {
            // do not have memory => stop buffering to open rooms for others
            return false;
        }

        if (runtime.freeMemory() == 0) {
            Log.d(TAG, "Free memory = 0 => forcing garbage collection");
            runtime.gc();
            return false;
        }
        return super.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
    }
}
