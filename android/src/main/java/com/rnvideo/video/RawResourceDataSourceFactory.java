/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */
package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.RawResourceDataSource;

public class RawResourceDataSourceFactory implements DataSource.Factory {

    private final Context context;

    RawResourceDataSourceFactory(Context context) {
        this.context = context;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public DataSource createDataSource() {
        return new RawResourceDataSource(context);
    }
}
