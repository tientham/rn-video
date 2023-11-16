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

import com.facebook.react.uimanager.ThemedReactContext;

@SuppressLint("UnsafeOptInUsageError")
public class RNLoadControl extends DefaultLoadControl {
    private final String TAG = "RNLoadControl";
    private final int availableHeapInBytes;
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
        Log.d(TAG, "RNLoadControl START");
        this.minBufferMemoryReservePercent = minBufferMemoryReservePercent;
        runtime = Runtime.getRuntime();
        ActivityManager activityManager = (ActivityManager) ((ThemedReactContext)context).getSystemService(ThemedReactContext.ACTIVITY_SERVICE);
        availableHeapInBytes = (int) Math.floor(activityManager.getMemoryClass() * maxHeapAllocationPercent * 1024 * 1024);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
      Log.d(TAG, "shouldContinueLoading");
      int loadedBytes = getAllocator().getTotalBytesAllocated();
      boolean isHeapReached = availableHeapInBytes > 0 && loadedBytes >= availableHeapInBytes;
      if (isHeapReached) {
        Log.d(TAG, "shouldContinueLoading isHeapReached true");
        return false;
      }

      long usedMemory = runtime.totalMemory() - runtime.freeMemory();
      long freeMemory = runtime.maxMemory() - usedMemory;
      long reserveMemory = (long) this.minBufferMemoryReservePercent * runtime.maxMemory();
      long bufferedMs = bufferedDurationUs / (long)1000;

      if (reserveMemory > freeMemory && bufferedMs > 2000) {
        Log.d(TAG, "shouldContinueLoading stop buffering to open rooms for others");
        // do not have memory => stop buffering to open rooms for others
        return false;
      }

      if (runtime.freeMemory() == 0) {
        Log.d(TAG, "shouldContinueLoading Free memory = 0 => forcing garbage collection");
        runtime.gc();
        return false;
      }
      Log.d(TAG, "shouldContinueLoading with playbackPositionUs: " + playbackPositionUs + " bufferedDurationUs: " + bufferedDurationUs + " playbackSpeed: " + playbackSpeed);
      return super.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
    }
}
