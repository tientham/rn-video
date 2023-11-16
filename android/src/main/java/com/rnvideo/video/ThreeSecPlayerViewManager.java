/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import javax.annotation.Nullable;

public class ThreeSecPlayerViewManager extends SimpleViewManager<ThreeSecPlayerView> {
  private static final String REACT_PACKAGE = "RnVideo-3sp";
  private final String SET_SOURCE = "source";
  private final String SET_PLAY = "play";
  private final String SET_REPLAY = "replay";
  private final String SET_VOLUME = "volume";

  private ThemedReactContext reactContext;

  public ThreeSecPlayerViewManager() {
  }

  @Override
  public String getName() {
    return REACT_PACKAGE;
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  protected ThreeSecPlayerView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;
    return new ThreeSecPlayerView(reactContext);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_SOURCE)
  public void setSource(final ThreeSecPlayerView playerView, @Nullable String source) {
    if (source == null) {
      return;
    }
    playerView.setSource(source);
  }
}
