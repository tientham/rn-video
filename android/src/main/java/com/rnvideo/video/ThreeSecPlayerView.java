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
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
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

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import javax.annotation.Nullable;

@SuppressLint("ViewConstructor")
@UnstableApi public class ThreeSecPlayerView extends FrameLayout implements
  BandwidthMeter.EventListener,
  LifecycleEventListener {

  private View surfaceView;
  private ImageView thumbnail;
  private final String TAG = "RnVideo-3sp";
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
  private int minLoadRetryCount = 3;
  private float rate = 1f;
  private final RnVideoConfig config;
  private boolean preventsDisplaySleepDuringVideoPlayback = true;

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

    updateSurfaceView();

    adOverlayFrameLayout = new FrameLayout(context);
    layout.addView(adOverlayFrameLayout, 1, layoutParams);

    addViewInLayout(layout, 0, aspectRatioParams);

    config = new RnVideoConfigImpl(context);
    bandwidthMeter = config.getBandwidthMeter();

    mediaDataSourceFactory = buildDataSourceFactory();
  }

  private void clearVideoView() {
    Log.d(TAG, "clearVideoView");
    if (surfaceView instanceof TextureView) {
      player.clearVideoTextureView((TextureView) surfaceView);
    } else if (surfaceView instanceof SurfaceView) {
      this.player.clearVideoSurfaceView((SurfaceView) surfaceView);
    }
  }

  private void setVideoView() {
    Log.d(TAG, "setVideoView");
    if (surfaceView instanceof TextureView) {
      player.setVideoTextureView((TextureView) surfaceView);
    } else if (surfaceView instanceof SurfaceView) {
      player.setVideoSurfaceView((SurfaceView) surfaceView);
    }
  }

  private void updateSurfaceView() {
    Log.d(TAG, "updateSurfaceView");
    View view = new SurfaceView(context);
    view.setLayoutParams(layoutParams);

    surfaceView = view;
    if (layout.getChildAt(0) != null) {
      layout.removeViewAt(0);
    }
    layout.addView(surfaceView, 0, layoutParams);
  }

  @Override
  public void requestLayout() {
    Log.d(TAG, "requestLayout");
    super.requestLayout();
    post(measureAndLayout);
  }

  public void setPlayer(ExoPlayer player) {
    Log.d(TAG, "setPlayer");
    if (this.player != null) {
      this.player.removeListener(innerPlayerListener);
      clearVideoView();
    }
    this.player = player;
    if (player != null) {
      Log.d(TAG, "==> setVideoView");
      setVideoView();
      player.addListener(innerPlayerListener);
    }
  }

  public void setResizeMode(int resizeMode) {
    Log.d(TAG, "setResizeMode mode: " + resizeMode);
    if (layout.getResizeMode() != resizeMode) {
      layout.setResizeMode(resizeMode);
      post(measureAndLayout);
    }
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
    Log.d(TAG, "setSource");
    initializePlayerCore(Uri.parse(source));
  }

  private void reLayout(View view) {
    Log.d(TAG, "reLayout");
    if (view == null) return;

    Log.d(TAG, "reLayout with view # null");
    view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
  }

  private void initializePlayerCore(Uri uri) {
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
    player.setRepeatMode(Player.REPEAT_MODE_ALL);
    config.setDisableDisconnectError(false);

    MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);

    MediaItem mediaItem = mediaItemBuilder.build();
    MediaSource mediaSource = new ProgressiveMediaSource
      .Factory(mediaDataSourceFactory)
      .setLoadErrorHandlingPolicy(config.buildLoadErrorHandlingPolicy(minLoadRetryCount))
      .createMediaSource(mediaItem);

    player.setMediaSource(mediaSource);
    player.prepare();

    reLayout(this);
    PlaybackParameters params = new PlaybackParameters(rate, 1f);
    player.setPlaybackParameters(params);
  }

  @Override
  public void onBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
    Log.d(TAG, "onBandwidthSample");
  }

  private DataSource.Factory buildDataSourceFactory() {
    Log.d(TAG, "buildDataSourceFactory");
    return DataSourceUtil.getDefaultDataSourceFactory((ThemedReactContext) context, bandwidthMeter, null);
  }

  private void releasePlayer() {
    Log.d(TAG, "releasePlayer");
    player.release();
    player.removeListener(innerPlayerListener);
    trackSelector = null;
    player = null;
    bandwidthMeter.removeEventListener(this);
  }

  @Override
  public void onHostResume() {
    Log.d(TAG, "onHostResume");
    // NOTHING TO DO FOR NOW
  }

  @Override
  public void onHostPause() {
    Log.d(TAG, "onHostPause");
    // NOTHING TO DO FOR NOW
  }

  @Override
  public void onHostDestroy() {
    Log.d(TAG, "onHostDestroy");
    releasePlayer();
  }

  private final class InnerPlayerListener implements Player.Listener {
    @Override
    public void onEvents(@NonNull Player player, Player.Events events) {
      Log.d(TAG, "InnerPlayerListener onEvents: " + player.getPlaybackState());
      if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
        int playbackState = player.getPlaybackState();
        boolean playWhenReady = player.getPlayWhenReady();
        String text = TAG + "InnerPlayerListener onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
          case Player.STATE_IDLE:
            text += "idle";
            if (!player.getPlayWhenReady()) {
              setKeepScreenOn(false);
            }
            break;
          case Player.STATE_BUFFERING:
            text += "buffering";
            setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
            break;
          case Player.STATE_READY:
            text += "ready";
            setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
            break;
          case Player.STATE_ENDED:
            text += "ended";
            releasePlayer();
            setKeepScreenOn(false);
            break;
          default:
            text += "unknown";
            break;
        }
        Log.d(TAG, text);
      }
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
      Log.d(TAG, "onPlaybackStateChanged: playbackState - " + playbackState);
    }
  }

}
