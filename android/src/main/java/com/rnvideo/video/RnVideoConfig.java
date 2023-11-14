package com.rnvideo.video;

import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

public interface RnVideoConfig {

    LoadErrorHandlingPolicy buildLoadErrorHandlingPolicy(int minLoadRetryCount);

    void setDisableDisconnectError(boolean disableDisconnectError);
    boolean getDisableDisconnectError();

    DefaultBandwidthMeter getBandwidthMeter();
    
}
