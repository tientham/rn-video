/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.AspectRatioFrameLayout;
import com.facebook.react.bridge.Dynamic;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;

import javax.annotation.Nullable;

@SuppressLint("ViewConstructor")
@UnstableApi public class ThreeSecPlayerView extends FrameLayout implements
  BandwidthMeter.EventListener {

  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private View surfaceView;
  private String source;
  private final String TAG = "RnVideo-3sp";

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

  private Context context;
  private ExoPlayer player;
  private final InnerPlayerListener innerPlayerListener;
  private DefaultTrackSelector trackSelector;
  private boolean playerNeedsSource;
  private Map<String, String> requestHeaders;

  private Uri srcUri;
  private String extension;
  private int minLoadRetryCount = 3;
  private float rate = 1f;
  private final RnVideoConfig config;
  private long startTimeMs = 0;
  private long endTimeMs = 3;
  private boolean disableDisconnectError;
  private boolean isBuffering;
  private boolean preventsDisplaySleepDuringVideoPlayback = true;
  private boolean isUsingContentResolution = false;
  private boolean selectTrackWhenReady = false;
  private String videoTrackType;
  private Dynamic videoTrackValue;
  private long contentStartTime = -1L;
  private boolean loadVideoStarted;

  private ViewGroup.LayoutParams layoutParams;
  private final FrameLayout adOverlayFrameLayout;
  private final AspectRatioFrameLayout layout;
  private int maxBitRate = 0;

  public ThreeSecPlayerView(Context context) {
    this(context, null);
  }

  public ThreeSecPlayerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ThreeSecPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    this.context = context;

    layoutParams = new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT);

    innerPlayerListener = new InnerPlayerListener();

    FrameLayout.LayoutParams aspectRatioParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT);
    aspectRatioParams.gravity = Gravity.CENTER;
    layout = new AspectRatioFrameLayout(context);
    layout.setLayoutParams(aspectRatioParams);

//    shutterView = new View(getContext());
//    shutterView.setLayoutParams(layoutParams);
//    shutterView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black));

//    subtitleLayout = new SubtitleView(context);
//    subtitleLayout.setLayoutParams(layoutParams);
//    subtitleLayout.setUserDefaultStyle();
//    subtitleLayout.setUserDefaultTextSize();

    updateSurfaceView();

    adOverlayFrameLayout = new FrameLayout(context);

//    layout.addView(shutterView, 1, layoutParams);
//    layout.addView(subtitleLayout, 2, layoutParams);
    layout.addView(adOverlayFrameLayout, 1, layoutParams);

    addViewInLayout(layout, 0, aspectRatioParams);

    this.config = new RnVideoConfigImpl(context);

    initializePlayerCore();
  }

  private void clearVideoView() {
    if (surfaceView instanceof SurfaceView) {
      this.player.clearVideoSurfaceView((SurfaceView) surfaceView);
    }
  }

  private void setVideoView() {
    if (surfaceView instanceof SurfaceView) {
      player.setVideoSurfaceView((SurfaceView) surfaceView);
    }
  }

  private void updateSurfaceView() {
    View view = new SurfaceView(context);
    view.setLayoutParams(layoutParams);

    surfaceView = view;
    if (layout.getChildAt(0) != null) {
      layout.removeViewAt(0);
    }
    layout.addView(surfaceView, 0, layoutParams);

    if (this.player != null) {
      setVideoView();
    }
  }

  @Override
  public void requestLayout() {
    super.requestLayout();
    post(measureAndLayout);
  }

  public void setPlayer(ExoPlayer player) {
    if (this.player == player) {
      return;
    }
    if (this.player != null) {
      this.player.removeListener(innerPlayerListener);
      clearVideoView();
    }
    this.player = player;
    if (player != null) {
      setVideoView();
      player.addListener(innerPlayerListener);
    }
  }

  public void setResizeMode(int resizeMode) {
    if (layout.getResizeMode() != resizeMode) {
      layout.setResizeMode(resizeMode);
      post(measureAndLayout);
    }

  }

  public View getVideoSurfaceView() {
    return surfaceView;
  }

  private final Runnable measureAndLayout = new Runnable() {
    @Override
    public void run() {
      measure(
        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
      layout(getLeft(), getTop(), getRight(), getBottom());
    }
  };

  public void setSource(@Nullable String source) {
    Log.d(TAG, "initializePlayerSource");
    MediaSource mediaSource = buildMediaSource(Uri.parse(source), startTimeMs, endTimeMs);
    // wait for player to be set
    while (player == null) {
      try {
        Log.d(TAG, "initializePlayerSource with player is null");
        wait();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        Log.e(TAG, ex.toString());
      }
    }
    player.setMediaSource(mediaSource);
    player.prepare();
    playerNeedsSource = false;
    reLayout(this);
    loadVideoStarted = true;
  }

  private MediaSource buildMediaSource(Uri uri, long startTimeMs, long endTimeMs) {
    Log.d(TAG, "buildMediaSource - uri: " + uri);
    if (uri == null) {
      throw new IllegalStateException("Invalid video uri");
    }
    config.setDisableDisconnectError(this.disableDisconnectError);

    MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);

    MediaItem mediaItem = mediaItemBuilder.build();
    MediaSource mediaSource = new ProgressiveMediaSource
      .Factory(mediaDataSourceFactory)
      .setLoadErrorHandlingPolicy(config.buildLoadErrorHandlingPolicy(minLoadRetryCount))
      .createMediaSource(mediaItem);

    if (startTimeMs >= 0 && endTimeMs >= 3) {
      return new ClippingMediaSource(mediaSource, startTimeMs * 1000, 3 * 1000);
    } else if (startTimeMs >= 0) {
      return new ClippingMediaSource(mediaSource, startTimeMs * 1000, endTimeMs * 1000);
    }
    throw new IllegalStateException("wrong start time");
  }

  private void reLayout(View view) {
    Log.d(TAG, "reLayout");
    if (view == null) return;

    Log.d(TAG, "reLayout with view # null");
    view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
  }

  private void initializePlayerCore() {
    Log.d(TAG, "initializePlayerCore");
    ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
    trackSelector = new DefaultTrackSelector(context, videoTrackSelectionFactory);
    trackSelector.setParameters(trackSelector.buildUponParameters()
      .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));

    DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
    RNLoadControl loadControl = new RNLoadControl(
      allocator,
      context,
      maxHeapAllocationPercent,
      minBufferMemoryReservePercent,
      minBufferMs,
      maxBufferMs,
      bufferForPlaybackMs,
      bufferForPlaybackAfterRebufferMs,
      -1,
      true,
      backBufferDurationMs,
      DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME
    );
    DefaultRenderersFactory renderersFactory =
      new DefaultRenderersFactory(getContext())
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

    MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory);

    player = new ExoPlayer.Builder(context, renderersFactory)
      .setTrackSelector(trackSelector)
      .setBandwidthMeter(bandwidthMeter)
      .setLoadControl(loadControl)
      .setMediaSourceFactory(mediaSourceFactory)
      .build();
    player.addListener(innerPlayerListener);
    player.setVolume(0.f);
    setPlayer(player);
    bandwidthMeter.addEventListener(new Handler(), this);
    player.setPlayWhenReady(true);
    playerNeedsSource = true;

    PlaybackParameters params = new PlaybackParameters(rate, 1f);
    player.setPlaybackParameters(params);
  }

  @Override
  public void onBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
    Log.d(TAG, "onBandwidthSample");
  }

  private final class InnerPlayerListener implements Player.Listener {


  }
}