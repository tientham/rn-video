/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashUtil;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.mp4.Track;
import androidx.media3.ui.AspectRatioFrameLayout;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.uimanager.ThemedReactContext;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
  private long endTimeMs = -1;
  private boolean disableDisconnectError;
  private boolean isBuffering;
  private boolean preventsDisplaySleepDuringVideoPlayback = true;
  private boolean isUsingContentResolution = false;
  private boolean selectTrackWhenReady = false;
  private String videoTrackType;
  private Dynamic videoTrackValue;
  private String audioTrackType;
  private Dynamic audioTrackValue;
  private long contentStartTime = -1L;
  private boolean loadVideoStarted;

  private ViewGroup.LayoutParams layoutParams;
  private final FrameLayout adOverlayFrameLayout;
  private final AspectRatioFrameLayout layout;
  private int maxBitRate = 0;
  private float audioVolume = 1f;
  private long seekTime = C.TIME_UNSET;
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

//    layout.addView(shutterView, 1, layoutParams);
//    layout.addView(subtitleLayout, 2, layoutParams);
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
    super.requestLayout();
    post(measureAndLayout);
  }

  public void setPlayer(ExoPlayer player) {
    Log.d(TAG, "setPlayer player: " + player);
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
    playerNeedsSource = true;
    player.setRepeatMode(Player.REPEAT_MODE_ALL);
    config.setDisableDisconnectError(this.disableDisconnectError);

    MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(uri);

    MediaItem mediaItem = mediaItemBuilder.build();
    MediaSource mediaSource = new ProgressiveMediaSource
      .Factory(mediaDataSourceFactory)
      .setLoadErrorHandlingPolicy(config.buildLoadErrorHandlingPolicy(minLoadRetryCount))
      .createMediaSource(mediaItem);

    ClippingMediaSource clippingMediaSource = new ClippingMediaSource(mediaSource, 0, 3 * 1000);
    player.setMediaSource(mediaSource);
    player.prepare();

    reLayout(this);
    loadVideoStarted = true;
    PlaybackParameters params = new PlaybackParameters(rate, 1f);
    player.setPlaybackParameters(params);
  }

  @Override
  public void onBandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
    Log.d(TAG, "onBandwidthSample");
  }

  private DataSource.Factory buildDataSourceFactory() {
    Log.d(TAG, "buildDataSourceFactory");
    return DataSourceUtil.getDefaultDataSourceFactory((ThemedReactContext) context, bandwidthMeter, requestHeaders);
  }

  private void releasePlayer() {
    Log.d(TAG, "releasePlayer");
    player.release();
    player.removeListener(innerPlayerListener);
    trackSelector = null;
    player = null;
    bandwidthMeter.removeEventListener(this);
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
            videoLoaded();
            if (selectTrackWhenReady && isUsingContentResolution) {
              selectTrackWhenReady = false;
              setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            }

//            if (audioTrackType != null) {
//              setSelectedAudioTrack(audioTrackType, audioTrackValue);
//            }
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

    public void setSelectedVideoTrack(String type, Dynamic value) {
      videoTrackType = type;
      videoTrackValue = value;
      setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
    }

    private void videoLoaded() {
      if (!player.isPlayingAd() && loadVideoStarted) {
        loadVideoStarted = false;
        if (audioTrackType != null) {
          setSelectedAudioTrack(audioTrackType, audioTrackValue);
        }
        if (videoTrackType != null) {
          setSelectedVideoTrack(videoTrackType, videoTrackValue);
        }
        isUsingContentResolution = false;


      }
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
      if (playbackState == Player.STATE_READY && seekTime != C.TIME_UNSET) {
        seekTime = C.TIME_UNSET;
        if (isUsingContentResolution) {
          // We need to update the selected track to make sure that it still matches user selection if track list has changed in this period
          setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
        }
      }
    }
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
    } else if (rendererIndex == C.TRACK_TYPE_AUDIO) { // Audio default
      groupIndex = getGroupIndexForDefaultLocale(groups);
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

  public void setSelectedAudioTrack(String type, Dynamic value) {
    audioTrackType = type;
    audioTrackValue = value;
    setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
  }

  private int getGroupIndexForDefaultLocale(TrackGroupArray groups) {
    if (groups.length == 0){
      return C.INDEX_UNSET;
    }

    int groupIndex = 0; // default if no match
    String locale2 = Locale.getDefault().getLanguage(); // 2 letter code
    String locale3 = Locale.getDefault().getISO3Language(); // 3 letter code
    for (int i = 0; i < groups.length; ++i) {
      Format format = groups.get(i).getFormat(0);
      String language = format.language;
      if (language != null && (language.equals(locale2) || language.equals(locale3))) {
        groupIndex = i;
        break;
      }
    }
    return groupIndex;
  }
}
