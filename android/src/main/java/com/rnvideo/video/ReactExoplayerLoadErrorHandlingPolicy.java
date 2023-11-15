package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

@UnstableApi public final class ReactExoplayerLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {
  private final String TAG = "RELEHP";
  private final int minLoadRetryCount;

  public @OptIn(markerClass = UnstableApi.class) ReactExoplayerLoadErrorHandlingPolicy(int minLoadRetryCount) {
    super(minLoadRetryCount);
    Log.d(TAG, "ReactExoplayerLoadErrorHandlingPolicy");
    this.minLoadRetryCount = minLoadRetryCount;
  }

  @Override
  public long getRetryDelayMsFor(LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo) {
    Log.d(TAG, "getRetryDelayMsFor");
    @SuppressLint("UnsafeOptInUsageError") String errorMessage = loadErrorInfo.exception.getMessage();

    if (
      loadErrorInfo.exception instanceof HttpDataSource.HttpDataSourceException &&
        errorMessage != null && (errorMessage.equals("Unable to connect") || errorMessage.equals("Software caused connection abort"))
    ) {
      // Capture the error we get when there is no network connectivity and keep retrying it
      return 1000; // Retry every second
    } else if(loadErrorInfo.errorCount < this.minLoadRetryCount) {
      return Math.min((loadErrorInfo.errorCount - 1) * 1000, 5000); // Default timeout handling
    } else {
      return C.TIME_UNSET; // Done retrying and will return the error immediately
    }
  }

  @Override
  public int getMinimumLoadableRetryCount(int dataType) {
    Log.d(TAG, "getMinimumLoadableRetryCount");
    return Integer.MAX_VALUE;
  }
}

