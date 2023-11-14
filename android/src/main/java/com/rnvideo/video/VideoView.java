/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import static androidx.media3.common.C.CONTENT_TYPE_OTHER;
import static androidx.media3.common.C.TIME_END_OF_SOURCE;

import com.facebook.react.uimanager.ThemedReactContext;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.drm.UnsupportedDrmException;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.Player;


import java.util.UUID;
import java.util.Map;


public class VideoView extends PlayerView implements BandwidthMeter.EventListener {

  private ThemedReactContext themedReactContext;
  private String source;
  private String TAG = "RnVideo VideoView";

  public static final double DEFAULT_MAX_HEAP_ALLOCATION_PERCENT = 1;
  public static final double DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE = 0;
  public static final double DEFAULT_MIN_BUFFER_MEMORY_RESERVE = 0;
  @SuppressLint("UnsafeOptInUsageError")
  private int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
  @SuppressLint("UnsafeOptInUsageError")
  private int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
  @SuppressLint("UnsafeOptInUsageError")
  private int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
  @SuppressLint("UnsafeOptInUsageError")
  private int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
  private double maxHeapAllocationPercent = VideoView.DEFAULT_MAX_HEAP_ALLOCATION_PERCENT;
  private double minBackBufferMemoryReservePercent = VideoView.DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE;
  private double minBufferMemoryReservePercent = VideoView.DEFAULT_MIN_BUFFER_MEMORY_RESERVE;

  private int backBufferDurationMs = DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS;

  private DataSource.Factory mediaDataSourceFactory;
  private DefaultBandwidthMeter bandwidthMeter;

  private ExoPlayer player;
  private DefaultTrackSelector trackSelector;

  private Map<String, String> requestHeaders;

  private Uri srcUri;
  private String extension;
  private int minLoadRetryCount = 3;

  private long startTimeMs = -1;
  private long endTimeMs = -1;
  private boolean hasDrmFailed = false;

  
  public VideoView(ThemedReactContext ctx) {
    super(ctx);
    Log.d(TAG, "INIT VIDEO VIEW");

    setUseController(false);
    setControllerAutoShow(false);
    setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    themedReactContext = ctx;

    this.mediaDataSourceFactory = buildDataSourceFactory(true);
  }

  @Override
  protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      initPlayer();
  }

  @Override
  protected void onDetachedFromWindow() {
      super.onDetachedFromWindow();
  }

  private void initPlayer() {
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        try {
          if (player == null) {
            // Initialize core configuration and listeners
            initializePlayerCore(VideoView.this);
          }

          if (srcUri != null) {
            initializePlayerSource(VideoView.this, null);
          }
        } catch (Exception ex) {
          Log.d(TAG, ex.toString());
        }
      }
    }, 1);
  }

  @Override
  public void onBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
    // if (mReportBandwidth) {
    if (false) {
      if (player == null) {
        // eventEmitter.bandwidthReport(bitrateEstimate, 0, 0, "-1");
      } else {
        @SuppressLint("UnsafeOptInUsageError") Format videoFormat = player.getVideoFormat();
        int width = videoFormat != null ? videoFormat.width : 0;
        int height = videoFormat != null ? videoFormat.height : 0;
        String trackId = videoFormat != null ? videoFormat.id : "-1";
        // eventEmitter.bandwidthReport(bitrateEstimate, height, width, trackId);
      }
    }
  }

  private void initializePlayerCore(VideoView self) {
    ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
    self.trackSelector = new DefaultTrackSelector(this.themedReactContext, videoTrackSelectionFactory);
    // TODO: set max bit rate here flexiblely
    self.trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoBitrate(Integer.MAX_VALUE));

    DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
    RNLoadControl loadControl = new RNLoadControl(
      allocator,
      this.themedReactContext,
      maxHeapAllocationPercent,
      minBufferMemoryReservePercent,
      minBufferMs,
      maxBufferMs,
      bufferForPlaybackMs,
      bufferForPlaybackAfterRebufferMs,
      -1,
      true,
      backBufferDurationMs,
      DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME);

    DefaultRenderersFactory renderersFactory =
      new DefaultRenderersFactory(this.themedReactContext)
      .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

    this.player = new ExoPlayer.Builder(this.themedReactContext, renderersFactory)
      .setTrackSelector(self.trackSelector)
      .setBandwidthMeter(bandwidthMeter)
      .setLoadControl(loadControl)
      .setMediaSourceFactory(mediaDataSourceFactory)
      .build();

    this.player.setRepeatMode(Player.REPEAT_MODE_ALL);
    this.player.setVolume(0.f);

    setPlayWhenReady(true);
  }

  private void initializePlayerSource(VideoView self, DrmSessionManager drmSessionManager) {
    MediaSource mediaSource = buildMediaSource(self.srcUri, self.extension, drmSessionManager, startTimeMs, endTimeMs);

    // wait for player to be set
    while (player == null) {
      try {
        wait();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        Log.e(TAG, ex.toString());
      }
    }
    player.setMediaSource(mediaSource);
    player.prepare();

    reLayout(this);

    finishPlayerInitialization();
  }

  private void finishPlayerInitialization() {
    // applyModifiers();
  }

  private void reLayout(View view) {
    if (view == null) return;
    view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
  }

  private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray) throws UnsupportedDrmException {
      return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, 0);
  }

  private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, int retryCount) throws UnsupportedDrmException {
    try {
      HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
              buildHttpDataSourceFactory(false));
      if (keyRequestPropertiesArray != null) {
        for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
          drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1]);
        }
      }
      FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
      if (hasDrmFailed) {
        // When DRM fails using L1 we want to switch to L3
        mediaDrm.setPropertyString("securityLevel", "L3");
      }
      return new DefaultDrmSessionManager(uuid, mediaDrm, drmCallback, null, false, 3);
    } catch(UnsupportedDrmException ex) {
      // Unsupported DRM exceptions are handled by the calling method
      throw ex;
    } catch (Exception ex) {
      if (retryCount < 3) {
        // Attempt retry 3 times in case where the OS Media DRM Framework fails for whatever reason
        return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, ++retryCount);
      }
      return null;
    }
  }

  private MediaSource buildMediaSource(Uri uri, String overrideExtension, DrmSessionManager drmSessionManager, long startTimeMs, long endTimeMs) {
    if (uri == null) {
      throw new IllegalStateException("Invalid video uri");
    }
    config.setDisableDisconnectError(this.disableDisconnectError);

    MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);

    MediaItem mediaItem = mediaItemBuilder.build();
    MediaSource mediaSource;
    DrmSessionManagerProvider drmProvider;
    if (drmSessionManager != null) {
      drmProvider = new DrmSessionManagerProvider() {
        @Override
        public DrmSessionManager get(MediaItem mediaItem) {
            return drmSessionManager;
        }
      };
    } else {
      drmProvider = new DefaultDrmSessionManagerProvider();
    }
    mediaSource = new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                    .setDrmSessionManagerProvider(drmProvider)
                    .setLoadErrorHandlingPolicy(
                      config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                    ).createMediaSource(mediaItem);

    if (startTimeMs >= 0 && endTimeMs >= 0){
      return new ClippingMediaSource(mediaSource, startTimeMs * 1000, endTimeMs * 1000);
    } else if (startTimeMs >= 0) {
      return new ClippingMediaSource(mediaSource, startTimeMs * 1000, TIME_END_OF_SOURCE);
    } else if (endTimeMs >= 0) {
      return new ClippingMediaSource(mediaSource, 0, endTimeMs * 1000);
    }

    return mediaSource;
  }

  private MediaSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext,
      useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
  }

  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return DataSourceUtil.getDefaultHttpDataSourceFactory(this.themedReactContext, useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
  }

  public void setBackBufferDurationMs(int backBufferDurationMs) {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    long freeMemory = runtime.maxMemory() - usedMemory;
    long reserveMemory = (long)minBackBufferMemoryReservePercent * runtime.maxMemory();
    if (reserveMemory > freeMemory) {
      // We don't have enough memory in reserve so we will
      Log.d(TAG, "Not enough reserve memory, setting back buffer to 0ms to reduce memory pressure!");
      this.backBufferDurationMs = 0;
      return;
    }
    this.backBufferDurationMs = backBufferDurationMs;
  }

  private void setPlayWhenReady(boolean playWhenReady) {
    if (player == null) {
        return;
    }

    if (playWhenReady) {
        player.setPlayWhenReady(true);
    } else {
        player.setPlayWhenReady(false);
    }
  }

  public void setPlay(final boolean shouldPlay) {
    Log.d(TAG, "setPlay: " + shouldPlay);
  }

  public void setReplay(final boolean shouldReplay) {
    Log.d(TAG, "setReplay: " + shouldReplay);
  }

  public void setMediaVolume(float volume) {
    Log.d(TAG, "setMediaVolume: " + volume);
  }

  public void setSource(final String uri) {
    Log.d(TAG, "setSource: " + uri);
    initPlayer(uri);
  }

}
