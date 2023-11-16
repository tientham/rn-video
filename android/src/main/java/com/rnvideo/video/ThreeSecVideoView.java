/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.dash.DashUtil;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.Player;


import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SuppressLint("ViewConstructor")
@UnstableApi public class ThreeSecVideoView extends PlayerView implements
  BandwidthMeter.EventListener,
  LifecycleEventListener,
  Player.Listener {

  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private final ThemedReactContext themedReactContext;
  private String source;
  private final String TAG = "RnVideo-3s";

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
  private final RnVideoConfig config;
  private DefaultBandwidthMeter bandwidthMeter;

  private ExoPlayer player;
  private DefaultTrackSelector trackSelector;
  private boolean playerNeedsSource;
  private Map<String, String> requestHeaders;

  private Uri srcUri;
  private String extension;
  private int minLoadRetryCount = 3;
  private float rate = 1f;

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


  public ThreeSecVideoView(ThemedReactContext ctx, RnVideoConfig config) {
    super(ctx);
    Log.d(TAG, "INIT THREE SEC VIDEO VIEW");
    themedReactContext = ctx;
    this.config = config;
    this.bandwidthMeter = config.getBandwidthMeter();
    setUseController(false);
    setControllerAutoShow(false);
    setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    this.mediaDataSourceFactory = buildDataSourceFactory();
    themedReactContext.addLifecycleEventListener(this);
  }

  @Override
  protected void onAttachedToWindow() {
    Log.d(TAG, "onAttachedToWindow");
    super.onAttachedToWindow();
    initPlayer();
  }

  @Override
  protected void onDetachedFromWindow() {
    Log.d(TAG, "onDetachedFromWindow");
    super.onDetachedFromWindow();
  }

  private void initPlayer() {
    Log.d(TAG, "initPlayer");
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(TAG, "initPlayer Handler run");
          if (player == null) {
            // Initialize core configuration and listeners
            initializePlayerCore();
          }
          Log.d(TAG, "initPlayer srcUri" + srcUri);
          if (playerNeedsSource && srcUri != null) {
            invalidate();
          } else if (srcUri != null) {
            Log.d(TAG, "initPlayer runOnUiThread run with playerNeedsSource is false");
            initializePlayerSource();
          }
        } catch (Exception ex) {
          playerNeedsSource = true;
          ex.printStackTrace();
          Log.d(TAG, "initPlayer " + ex);
        }
      }
    }, 1);
  }

  @Override
  public void onBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
    Log.d(TAG, "onBandwidthSample");
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

  @Override
  public void onPlayerError(@NonNull PlaybackException e) {
    Log.d(TAG, "onPlayerError");
    String errorString = "ExoPlaybackException: " + PlaybackException.getErrorCodeName(e.errorCode);
    String errorCode = "2" + String.valueOf(e.errorCode);
    switch(e.errorCode) {
      case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED:
      case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
      case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED:
      case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR:
      case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED:
      default:
        break;
    }
    setPlayWhenReady(true);
    playerNeedsSource = true;
    initPlayer();
  }

  private void initializePlayerCore() {
    Log.d(TAG, "initializePlayerCore");
    ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
    trackSelector = new DefaultTrackSelector(this.themedReactContext, videoTrackSelectionFactory);
    // TODO: set max bit rate here flexible
    trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoBitrate(Integer.MAX_VALUE));

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

    MediaSource.Factory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory);

    this.player = new ExoPlayer.Builder(this.themedReactContext, renderersFactory)
      .setTrackSelector(trackSelector)
      .setBandwidthMeter(bandwidthMeter)
      .setLoadControl(loadControl)
      .setMediaSourceFactory(mediaSourceFactory)
      .build();

    this.player.setRepeatMode(Player.REPEAT_MODE_ALL);
    this.player.setVolume(0.f);
    setPlayWhenReady(true);
    playerNeedsSource = true;

    PlaybackParameters params = new PlaybackParameters(rate, 1f);
    player.setPlaybackParameters(params);
  }

  private void initializePlayerSource() {
    Log.d(TAG, "initializePlayerSource");
    MediaSource mediaSource = buildMediaSource(srcUri, startTimeMs, endTimeMs);

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
    finishPlayerInitialization();
  }

  private void finishPlayerInitialization() {
    Log.d(TAG, "finishPlayerInitialization");
    // applyModifiers();
  }

  private void reLayout(View view) {
    Log.d(TAG, "reLayout");
    if (view == null) return;

    Log.d(TAG, "reLayout with view # null");
    view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
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

  private DataSource.Factory buildDataSourceFactory() {
    Log.d(TAG, "buildDataSourceFactory useBandwidthMeter");
    return DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext, bandwidthMeter, requestHeaders);
  }

  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    Log.d(TAG, "buildHttpDataSourceFactory useBandwidthMeter: " + useBandwidthMeter);
    return DataSourceUtil.getDefaultHttpDataSourceFactory(
      this.themedReactContext, bandwidthMeter, requestHeaders);
  }

  public void setBackBufferDurationMs(int backBufferDurationMs) {
    Log.d(TAG, "setBackBufferDurationMs backBufferDurationMs: " + backBufferDurationMs);
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
    Log.d(TAG, "setPlayWhenReady playWhenReady: " + playWhenReady + " player is: " + player);
    if (player == null) {
      return;
    }

    player.setPlayWhenReady(playWhenReady);
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
    this.srcUri = Uri.parse(uri);
    initPlayer();
  }

  @Override
  public void onHostResume() {
    Log.d(TAG, "onHostResume");
    this.setPlayWhenReady(true);
  }

  @Override
  public void onHostPause() {
    Log.d(TAG, "onHostPause");
    this.setPlayWhenReady(false);
  }

  @Override
  public void onHostDestroy() {
    Log.d(TAG, "onHostDestroy");
    releasePlayer();
  }

  private void releasePlayer() {
    Log.d(TAG, "releasePlayer");
    player.release();
    player.removeListener(this);
    trackSelector = null;
    player = null;
    themedReactContext.removeLifecycleEventListener(this);
    bandwidthMeter.removeEventListener(this);
  }

  @Override
  public void onEvents(@NonNull Player player, Player.Events events) {
    Log.d(TAG, "onEvents");
    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
      int playbackState = player.getPlaybackState();
      boolean playWhenReady = player.getPlayWhenReady();
      String text = TAG + " onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=";
      switch (playbackState) {
        case Player.STATE_IDLE:
          text += "idle";
          if (!player.getPlayWhenReady()) {
            setKeepScreenOn(false);
          }
          break;
        case Player.STATE_BUFFERING:
          text += "buffering";
          onBuffering(true);
          setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
          break;
        case Player.STATE_READY:
          text += "ready";
          onBuffering(false);
          videoLoaded();
          if (selectTrackWhenReady && isUsingContentResolution) {
            selectTrackWhenReady = false;
            setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
          }
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

  private void onBuffering(boolean buffering) {
    Log.d(TAG, "onBuffering");
    if (isBuffering == buffering) {
      return;
    }

    isBuffering = buffering;
  }

  public void setBufferConfig(int newMinBufferMs, int newMaxBufferMs, int newBufferForPlaybackMs, int newBufferForPlaybackAfterRebufferMs, double newMaxHeapAllocationPercent, double newMinBackBufferMemoryReservePercent, double newMinBufferMemoryReservePercent) {
    Log.d(TAG, "setBufferConfig");
    minBufferMs = newMinBufferMs;
    maxBufferMs = newMaxBufferMs;
    bufferForPlaybackMs = newBufferForPlaybackMs;
    bufferForPlaybackAfterRebufferMs = newBufferForPlaybackAfterRebufferMs;
    maxHeapAllocationPercent = newMaxHeapAllocationPercent;
    minBackBufferMemoryReservePercent = newMinBackBufferMemoryReservePercent;
    minBufferMemoryReservePercent = newMinBufferMemoryReservePercent;
    releasePlayer();
    initPlayer();
  }

  private void videoLoaded() {
    Log.d(TAG, "videoLoaded");
    if (!player.isPlayingAd() && loadVideoStarted) {
      loadVideoStarted = false;
      if (videoTrackType != null) {
        setSelectedVideoTrack(videoTrackType, videoTrackValue);
      }
      Format videoFormat = player.getVideoFormat();
      int width = videoFormat != null ? videoFormat.width : 0;
      int height = videoFormat != null ? videoFormat.height : 0;
      String trackId = videoFormat != null ? videoFormat.id : "-1";

      // Properties that must be accessed on the main thread
      long duration = player.getDuration();
      long currentPosition = player.getCurrentPosition();

      if (this.contentStartTime != -1L) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(new Runnable() {
          @Override
          public void run() {
            // To prevent ANRs caused by getVideoTrackInfo we run this on a different thread and notify the player only when we're done
            ArrayList<VideoTrack> videoTracks = getVideoTrackInfoFromManifest();
            if (videoTracks != null) {
              isUsingContentResolution = true;
            }

          }
        });
        return;
      }

      ArrayList<VideoTrack> videoTracks = getVideoTrackInfo();
    }
  }

  public void setSelectedVideoTrack(String type, Dynamic value) {
    Log.d(TAG, "setSelectedVideoTrack");
    videoTrackType = type;
    videoTrackValue = value;
    setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
  }

  public void setSelectedTrack(int trackType, String type, Dynamic value) {
    Log.d(TAG, "setSelectedTrack");
    if (player == null) return;
    int rendererIndex = getTrackRendererIndex(trackType);
    if (rendererIndex == C.INDEX_UNSET) {
      return;
    }
    MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
    if (info == null) {
      return;
    }

    TrackGroupArray groups = info.getTrackGroups(rendererIndex);
    int groupIndex = C.INDEX_UNSET;
    List<Integer> tracks = new ArrayList<>();
    tracks.add(0);

    if (TextUtils.isEmpty(type)) {
      type = "default";
    }

    if (type.equals("disabled")) {
      disableTrack(rendererIndex);
      return;
    } else if (type.equals("language")) {
      for (int i = 0; i < groups.length; ++i) {
        Format format = groups.get(i).getFormat(0);
        if (format.language != null && format.language.equals(value.asString())) {
          groupIndex = i;
          break;
        }
      }
    } else if (type.equals("title")) {
      for (int i = 0; i < groups.length; ++i) {
        Format format = groups.get(i).getFormat(0);
        if (format.id != null && format.id.equals(value.asString())) {
          groupIndex = i;
          break;
        }
      }
    } else if (type.equals("index")) {
      if (value.asInt() < groups.length) {
        groupIndex = value.asInt();
      }
    } else if (type.equals("resolution")) {
      int height = value.asInt();
      for (int i = 0; i < groups.length; ++i) { // Search for the exact height
        TrackGroup group = groups.get(i);
        Format closestFormat = null;
        int closestTrackIndex = -1;
        boolean usingExactMatch = false;
        for (int j = 0; j < group.length; j++) {
          Format format = group.getFormat(j);
          if (format.height == height) {
            groupIndex = i;
            tracks.set(0, j);
            closestFormat = null;
            closestTrackIndex = -1;
            usingExactMatch = true;
            break;
          } else if (isUsingContentResolution) {
            // When using content resolution rather than ads, we need to try and find the closest match if there is no exact match
            if (closestFormat != null) {
              if ((format.bitrate > closestFormat.bitrate || format.height > closestFormat.height) && format.height < height) {
                // Higher quality match
                closestFormat = format;
                closestTrackIndex = j;
              }
            } else if(format.height < height) {
              closestFormat = format;
              closestTrackIndex = j;
            }
          }
        }
        // This is a fallback if the new period contains only higher resolutions than the user has selected
        if (closestFormat == null && isUsingContentResolution && !usingExactMatch) {
          // No close match found - so we pick the lowest quality
          int minHeight = Integer.MAX_VALUE;
          for (int j = 0; j < group.length; j++) {
            Format format = group.getFormat(j);
            if (format.height < minHeight) {
              minHeight = format.height;
              groupIndex = i;
              tracks.set(0, j);
            }
          }
        }
        // Selecting the closest match found
        if (closestFormat != null && closestTrackIndex != -1) {
          // We found the closest match instead of an exact one
          groupIndex = i;
          tracks.set(0, closestTrackIndex);
        }
      }
    }

    if (groupIndex == C.INDEX_UNSET && trackType == C.TRACK_TYPE_VIDEO && groups.length != 0) { // Video auto
      // Add all tracks as valid options for ABR to choose from
      TrackGroup group = groups.get(0);
      tracks = new ArrayList<>(group.length);
      ArrayList<Integer> allTracks = new ArrayList<>(group.length);
      groupIndex = 0;
      for (int j = 0; j < group.length; j++) {
        allTracks.add(j);
      }

      // Valiate list of all tracks and add only supported formats
      int supportedFormatLength = 0;
      ArrayList<Integer> supportedTrackList = new ArrayList<>();
      for (int g = 0; g < allTracks.size(); g++) {
        Format format = group.getFormat(g);
        if (isFormatSupported(format)) {
          supportedFormatLength++;
        }
      }
      if (allTracks.size() == 1) {
        // With only one tracks we can't remove any tracks so attempt to play it anyway
        tracks = allTracks;
      } else {
        tracks =  new ArrayList<>(supportedFormatLength + 1);
        for (int k = 0; k < allTracks.size(); k++) {
          Format format = group.getFormat(k);
          if (isFormatSupported(format)) {
            tracks.add(allTracks.get(k));
            supportedTrackList.add(allTracks.get(k));
          }
        }
      }
    }

    if (groupIndex == C.INDEX_UNSET) {
      disableTrack(rendererIndex);
      return;
    }

    TrackSelectionOverride selectionOverride = new TrackSelectionOverride(groups.get(groupIndex), tracks);

    DefaultTrackSelector.Parameters selectionParameters = trackSelector.getParameters()
      .buildUpon()
      .setRendererDisabled(rendererIndex, false)
      .clearOverridesOfType(selectionOverride.getType())
      .addOverride(selectionOverride)
      .build();
    trackSelector.setParameters(selectionParameters);
  }

  public void disableTrack(int rendererIndex) {
    Log.d(TAG, "disableTrack");
    DefaultTrackSelector.Parameters disableParameters = trackSelector.getParameters()
      .buildUpon()
      .setRendererDisabled(rendererIndex, true)
      .build();
    trackSelector.setParameters(disableParameters);
  }

  private ArrayList<VideoTrack> getVideoTrackInfo() {
    Log.d(TAG, "getVideoTrackInfo");
    ArrayList<VideoTrack> videoTracks = new ArrayList<>();
    if (trackSelector == null) {
      // Likely player is unmounting so no audio tracks are available anymore
      return videoTracks;
    }
    MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
    int index = getTrackRendererIndex(C.TRACK_TYPE_VIDEO);
    if (info == null || index == C.INDEX_UNSET) {
      return videoTracks;
    }

    TrackGroupArray groups = info.getTrackGroups(index);
    for (int i = 0; i < groups.length; ++i) {
      TrackGroup group = groups.get(i);

      for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
        Format format = group.getFormat(trackIndex);
        if (isFormatSupported(format)) {
          VideoTrack videoTrack = exoplayerVideoTrackToGenericVideoTrack(format, trackIndex);
          videoTracks.add(videoTrack);
        }
      }
    }
    return videoTracks;
  }

  private boolean isFormatSupported(Format format) {
    Log.d(TAG, "isFormatSupported");
    int width = format.width == Format.NO_VALUE ? 0 : format.width;
    int height = format.height == Format.NO_VALUE ? 0 : format.height;
    float frameRate = format.frameRate == Format.NO_VALUE ? 0 : format.frameRate;
    String mimeType = format.sampleMimeType;
    if (mimeType == null) {
      return true;
    }
    boolean isSupported;
    try {
      MediaCodecInfo codecInfo = MediaCodecUtil.getDecoderInfo(mimeType, false, false);
      isSupported = codecInfo.isVideoSizeAndRateSupportedV21(width, height, frameRate);
    } catch (Exception e) {
      // Failed to get decoder info - assume it is supported
      isSupported = true;
    }
    return isSupported;
  }

  private VideoTrack exoplayerVideoTrackToGenericVideoTrack(Format format, int trackIndex) {
    Log.d(TAG, "exoplayerVideoTrackToGenericVideoTrack");
    VideoTrack videoTrack = new VideoTrack();
    videoTrack.setWidth(format.width == Format.NO_VALUE ? 0 : format.width);
    videoTrack.setHeight(format.height == Format.NO_VALUE ? 0 : format.height);
    videoTrack.setBitrate(format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
    if (format.codecs != null) videoTrack.setCodecs(format.codecs);
    videoTrack.setTrackId(format.id == null ? String.valueOf(trackIndex) : format.id);;
    return videoTrack;
  }

  public int getTrackRendererIndex(int trackType) {
    Log.d(TAG, "getTrackRendererIndex");
    if (player != null) {
      int rendererCount = player.getRendererCount();
      for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
        if (player.getRendererType(rendererIndex) == trackType) {
          return rendererIndex;
        }
      }
    }
    return C.INDEX_UNSET;
  }

  private ArrayList<VideoTrack> getVideoTrackInfoFromManifest() {
    Log.d(TAG, "getVideoTrackInfoFromManifest");
    return this.getVideoTrackInfoFromManifest(0);
  }

  // We need retry count to in case where minefest request fails from poor network conditions
  @WorkerThread
  private ArrayList<VideoTrack> getVideoTrackInfoFromManifest(int retryCount) {
    Log.d(TAG, "getVideoTrackInfoFromManifest");
    ExecutorService es = Executors.newSingleThreadExecutor();
    final DataSource dataSource = this.mediaDataSourceFactory.createDataSource();
    final Uri sourceUri = this.srcUri;
    final long startTime = this.contentStartTime * 1000 - 100; // s -> ms with 100ms offset

    Future<ArrayList<VideoTrack>> result = es.submit(new Callable<ArrayList<VideoTrack>>() {
      final DataSource ds = dataSource;
      final Uri uri = sourceUri;
      final long startTimeUs = startTime * 1000; // ms -> us

      public ArrayList<VideoTrack> call() {
        ArrayList<VideoTrack> videoTracks = new ArrayList<>();
        try  {
          DashManifest manifest = DashUtil.loadManifest(this.ds, this.uri);
          int periodCount = manifest.getPeriodCount();
          for (int i = 0; i < periodCount; i++) {
            Period period = manifest.getPeriod(i);
            for (int adaptationIndex = 0; adaptationIndex < period.adaptationSets.size(); adaptationIndex++) {
              AdaptationSet adaptation = period.adaptationSets.get(adaptationIndex);
              if (adaptation.type != C.TRACK_TYPE_VIDEO) {
                continue;
              }
              boolean hasFoundContentPeriod = false;
              for (int representationIndex = 0; representationIndex < adaptation.representations.size(); representationIndex++) {
                Representation representation = adaptation.representations.get(representationIndex);
                Format format = representation.format;
                if (isFormatSupported(format)) {
                  if (representation.presentationTimeOffsetUs <= startTimeUs) {
                    break;
                  }
                  hasFoundContentPeriod = true;
                  VideoTrack videoTrack = exoplayerVideoTrackToGenericVideoTrack(format, representationIndex);
                  videoTracks.add(videoTrack);
                }
              }
              if (hasFoundContentPeriod) {
                return videoTracks;
              }
            }
          }
        } catch (Exception e) {
          Log.w(TAG, "error in getVideoTrackInfoFromManifest:" + e.getMessage());
        }
        return null;
      }
    });

    try {
      ArrayList<VideoTrack> results = result.get(3000, TimeUnit.MILLISECONDS);
      if (results == null && retryCount < 1) {
        return this.getVideoTrackInfoFromManifest(++retryCount);
      }
      es.shutdown();
      return results;
    } catch (Exception e) {
      Log.w(TAG, "error in getVideoTrackInfoFromManifest handling request:" + e.getMessage());
    }

    return null;
  }
}
