/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.util.Log;

import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import java.util.Map;

import javax.annotation.Nullable;

public class VideoViewManager extends SimpleViewManager<VideoView> {
  private static final String REACT_PACKAGE = "RnVideo";
  private final String SET_SOURCE = "source";
  private final String SET_PLAY = "play";
  private final String SET_REPLAY = "replay";
  private final String SET_VOLUME = "volume";

  private ThemedReactContext reactContext;

  private RnVideoConfig config;

  public VideoViewManager(RnVideoConfig config) {
    this.config = config;
  }

  @Override
  public String getName() {
    return REACT_PACKAGE;
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  protected VideoView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;
    return new VideoView(reactContext, config);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_SOURCE)
  public void setSource(final VideoView playerView, @Nullable String source) {
    if (source == null) {
      return;
    }
    playerView.setSource(source);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_PLAY, defaultBoolean = true)
  public void setPlay(final VideoView playerView, boolean shouldPlay) {
    Log.d(VideoViewManager.REACT_PACKAGE, "setPlay: " + String.valueOf(shouldPlay));
    playerView.setPlay(shouldPlay);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_REPLAY, defaultBoolean = true)
  public void setReplay(final VideoView playerView, boolean replay) {
    Log.d(VideoViewManager.REACT_PACKAGE, "set replay: " + String.valueOf(replay));
    playerView.setReplay(replay);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_VOLUME, defaultFloat = 0f)
  public void setVolume(final VideoView playerView, float volume) {
    Log.d(VideoViewManager.REACT_PACKAGE, "set volume: " + String.valueOf(volume));
    playerView.setMediaVolume(volume);
  }
}
