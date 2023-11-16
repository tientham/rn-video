/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import javax.annotation.Nullable;

public class GifViewManager extends ViewGroupManager<GifView> {
  private static final String REACT_PACKAGE = "RnVideo-3sg";
  private final String SET_SOURCE = "source";

  private ThemedReactContext reactContext;

  public GifViewManager() {
  }

  @Override
  public String getName() {
    return REACT_PACKAGE;
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  protected GifView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;
    return new GifView(reactContext);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_SOURCE)
  public void setSource(final GifView playerView, @Nullable String source) {
    if (source == null) {
      return;
    }
    playerView.setSource(source);
  }
}
