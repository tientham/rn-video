/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.util.Log;

import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import javax.annotation.Nullable;

public class ThreeSecVideoViewManager extends SimpleViewManager<ThreeSecVideoView> {
  private static final String REACT_PACKAGE = "RnVideo-3sv";
  private final String SET_SOURCE = "source";
  private final String SET_PLAY = "play";
  private final String SET_REPLAY = "replay";
  private final String SET_VOLUME = "volume";

  private ThemedReactContext reactContext;

  private RnVideoConfig config;

  public ThreeSecVideoViewManager(RnVideoConfig config) {
    this.config = config;
  }

  @Override
  public String getName() {
    return REACT_PACKAGE;
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  protected ThreeSecVideoView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;
    return new ThreeSecVideoView(reactContext, config);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_SOURCE)
  public void setSource(final ThreeSecVideoView playerView, @Nullable String source) {
    if (source == null) {
      return;
    }
    playerView.setSource(source);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_PLAY, defaultBoolean = true)
  public void setPlay(final ThreeSecVideoView playerView, boolean shouldPlay) {
    Log.d(REACT_PACKAGE, "setPlay: " + String.valueOf(shouldPlay));
    playerView.setPlay(shouldPlay);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_REPLAY, defaultBoolean = true)
  public void setReplay(final ThreeSecVideoView playerView, boolean replay) {
    Log.d(REACT_PACKAGE, "set replay: " + String.valueOf(replay));
    playerView.setReplay(replay);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_VOLUME, defaultFloat = 0f)
  public void setVolume(final ThreeSecVideoView playerView, float volume) {
    Log.d(REACT_PACKAGE, "set volume: " + String.valueOf(volume));
    playerView.setMediaVolume(volume);
  }
}
