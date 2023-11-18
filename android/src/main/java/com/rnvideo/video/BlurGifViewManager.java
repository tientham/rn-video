/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import javax.annotation.Nullable;

public class BlurGifViewManager extends ViewGroupManager<BlurGifView> {
  private static final String REACT_PACKAGE = "RnVideo-3sbg";
  private final String SET_SOURCE = "source";

  private ThemedReactContext reactContext;

  public BlurGifViewManager() {
  }

  @Override
  public String getName() {
    return REACT_PACKAGE;
  }

  @SuppressLint("UnsafeOptInUsageError")
  @Override
  protected BlurGifView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;
    return new BlurGifView(reactContext);
  }

  @SuppressLint("UnsafeOptInUsageError")
  @ReactProp(name = SET_SOURCE)
  public void setSource(final BlurGifView playerView, @Nullable String source) {
    if (source == null) {
      return;
    }
    playerView.setSource(source);
  }
}
