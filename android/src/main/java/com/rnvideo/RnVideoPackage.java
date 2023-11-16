package com.rnvideo;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import com.rnvideo.video.GifViewManager;
import com.rnvideo.video.RnVideoConfig;
import com.rnvideo.video.RnVideoConfigImpl;
import com.rnvideo.video.ThreeSecPlayerViewManager;
import com.rnvideo.video.ThreeSecVideoViewManager;
import com.rnvideo.video.VideoViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RnVideoPackage implements ReactPackage {

  private RnVideoConfig config;

  public RnVideoPackage() {
  }

  public RnVideoPackage(RnVideoConfig config) {
    this.config = config;
  }

  @Override
  public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    if (config == null) {
      config = new RnVideoConfigImpl(reactContext);
    }

    return Arrays.<ViewManager>asList(
      new VideoViewManager(config),
      new ThreeSecVideoViewManager(config),
      new ThreeSecPlayerViewManager(),
      new GifViewManager()
    );
  }
}
