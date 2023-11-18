/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.facebook.react.uimanager.ThemedReactContext;

import jp.wasabeef.glide.transformations.BlurTransformation;

import javax.annotation.Nullable;

@SuppressLint("ViewConstructor")
@UnstableApi public class BlurGifView extends FrameLayout {

  private ImageView thumbnail;
  private final String TAG = "RnVideo-3sbg";

  RequestOptions requestOptions;
  private Context context;

  private ViewGroup.LayoutParams layoutParams;
  private FrameLayout.LayoutParams aspectRatioParams;
  private final AspectRatioFrameLayout layout;


  public BlurGifView(Context context) {
    this(context, null);
  }

  public BlurGifView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BlurGifView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    Log.d(TAG, "INIT GIF VIEW");
    this.context = context;
    layoutParams = new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT);

    aspectRatioParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT);
    aspectRatioParams.gravity = Gravity.CENTER;
    layout = new AspectRatioFrameLayout(context);
    layout.setLayoutParams(aspectRatioParams);

    MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
      new CenterCrop(),
      new BlurTransformation(10, 1));

    requestOptions = new RequestOptions().bitmapTransform(multiTransformation).override(250).set(GifOptions.DISABLE_ANIMATION, false);

    thumbnail = new ImageView(context);
  }

  public void setSource(@Nullable String source) {
    Log.d(TAG, "setSource: " + source);

    ((ThemedReactContext) context).getCurrentActivity().runOnUiThread(new Runnable() {
      public void run() {
        try {
          Log.d(TAG, "setSource runOnUiThread run");
          Glide.with(((ThemedReactContext) context).getCurrentActivity())
            .asGif()
            .load(source)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            //.thumbnail(0.25f)
            .priority(Priority.IMMEDIATE)
            .listener(new RequestListener<GifDrawable>() {
              @Override
              public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                Log.d(TAG, "Glide onLoadFailed to initialize Player!");
                e.printStackTrace();
                return false;
              }

              @Override
              public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                Log.d(TAG, "Glide onResourceReady to initialize Player!");
                return false;
              }
            })
            .into(thumbnail);

          // thumbnail.setLayoutParams(layoutParams);
          layout.addView(thumbnail, 0, layoutParams);
          addViewInLayout(layout, 0, aspectRatioParams);

          reLayout(BlurGifView.this);
        } catch (Exception ex) {
          Log.e(TAG, "Failed to initialize Player!");
          Log.e(TAG, ex.toString());
        }
      }
    });

  }

  private void reLayout(View view) {
    Log.d(TAG, "reLayout");
    if (view == null) return;

    Log.d(TAG, "reLayout with view # null");
    view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
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
}
