/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;

import jp.wasabeef.glide.transformations.BlurTransformation;

import javax.annotation.Nullable;

@SuppressLint("ViewConstructor")
@UnstableApi public class GifView extends FrameLayout {

  private ImageView thumbnail;
  private final String TAG = "RnVideo-3sg";

  RequestOptions requestOptions;
  private Context context;

  private ViewGroup.LayoutParams layoutParams;
  private final FrameLayout adOverlayFrameLayout;
  private final AspectRatioFrameLayout layout;


  public GifView(Context context) {
    this(context, null);
  }

  public GifView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public GifView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.context = context;
    layoutParams = new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT);

    FrameLayout.LayoutParams aspectRatioParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT);
    aspectRatioParams.gravity = Gravity.CENTER;
    layout = new AspectRatioFrameLayout(context);
    layout.setLayoutParams(aspectRatioParams);

    adOverlayFrameLayout = new FrameLayout(context);

    MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
      new CenterCrop(),
      new BlurTransformation(10, 1));

    requestOptions = new RequestOptions().bitmapTransform(multiTransformation).override(250);

    thumbnail = new ImageView(context);
    thumbnail.setLayoutParams(layoutParams);

    addViewInLayout(layout, 0, aspectRatioParams);
  }

  public void setSource(@Nullable String source) {
    Log.d(TAG, "setSource");
    Glide.with(context)
      .asGif()
      .load(source)
      .apply(requestOptions)
      .dontAnimate()
      .diskCacheStrategy(DiskCacheStrategy.ALL)
      //.thumbnail(0.25f)
      .priority(Priority.IMMEDIATE)
      .into(thumbnail);

    layout.addView(thumbnail, 1, layoutParams);
  }

}
