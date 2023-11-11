/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

package com.rnvideo.video;

import com.facebook.react.uimanager.ThemedReactContext;
import androidx.media3.ui.PlayerView;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.ExoPlayer;


public class VideoView extends PlayerView {

  private ThemedReactContext themedReactContext;
  private String source;
  private String LOG_TAG = "RnVideo - VideoView";

  public ExoPlayer player;

  public VideoView(ThemedReactContext ctx) {
    super(ctx);

    themedReactContext = ctx;
  }

  public void initPlayer(final String uri) {
    DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
    trackSelector.setParameters(trackSelector
            .buildUponParameters()
            .setMaxVideoSize(256, 144)
            .setForceHighestSupportedBitrate(true));
    ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(this.themedReactContext).setTrackSelector(trackSelector);
    this.player = playerBuilder.build();

    setPlayer(this.player);

    this.player.setRepeatMode(Player.REPEAT_MODE_ALL);
    this.player.setPlayWhenReady(true);
    DefaultHttpDataSource.Factory d = new DefaultHttpDataSource.Factory();
    MediaItem mediaItem = MediaItem.fromUri(uri);
    ProgressiveMediaSource p = new ProgressiveMediaSource.Factory(d).createMediaSource(mediaItem);
    this.player.setMediaSource(p);
    this.player.prepare();
  }

  public void setPlay(final boolean shouldPlay) {
    Log.d(LOG_TAG, "setPlay: " + shouldPlay);
  }

  public void setReplay(final boolean shouldReplay) {
    Log.d(LOG_TAG, "setReplay: " + shouldReplay);
  }

  public void setMediaVolume(float volume) {
    Log.d(LOG_TAG, "setMediaVolume: " + volume);
  }

  public void setSource(final String uri) {
    Log.d(LOG_TAG, "setSource: " + uri);
    initPlayer(uri);
  }

}
