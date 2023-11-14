package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

public class RnVideoConfigImpl implements RnVideoConfig {

  private final DefaultBandwidthMeter bandwidthMeter;
  private boolean disableDisconnectError = false;

  public @OptIn(markerClass = UnstableApi.class) RnVideoConfigImpl(Context context) {
    this.bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  public LoadErrorHandlingPolicy buildLoadErrorHandlingPolicy(int minLoadRetryCount) {
    if (this.disableDisconnectError) {
      // Use custom error handling policy to prevent throwing an error when losing network connection
      return new ReactExoplayerLoadErrorHandlingPolicy(minLoadRetryCount);
    }
    return new DefaultLoadErrorHandlingPolicy(minLoadRetryCount);
  }

  @Override
  public void setDisableDisconnectError(boolean disableDisconnectError) {

  }

  @Override
  public boolean getDisableDisconnectError() {
    return false;
  }

  @Override
  public DefaultBandwidthMeter getBandwidthMeter() {
    return null;
  }
}
