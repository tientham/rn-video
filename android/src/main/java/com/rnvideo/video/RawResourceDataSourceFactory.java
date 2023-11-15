/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */
package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.RawResourceDataSource;

public class RawResourceDataSourceFactory implements DataSource.Factory {

  private final String TAG = "RRDSF";
    private final Context context;

    RawResourceDataSourceFactory(Context context) {
      Log.d(TAG, "RawResourceDataSourceFactory");
      this.context = context;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public DataSource createDataSource() {
      Log.d(TAG, "createDataSource");
      return new RawResourceDataSource(context);
    }
}
