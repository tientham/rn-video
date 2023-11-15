/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */
package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.util.Log;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.network.CookieJarContainer;
import com.facebook.react.modules.network.ForwardingCookieHandler;
import com.facebook.react.modules.network.OkHttpClientProvider;

import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;

import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.JavaNetCookieJar;

public class DataSourceUtil {

  private final static String TAG = "DataSourceUtil";
  private static DataSource.Factory rawDataSourceFactory = null;
  private static DataSource.Factory defaultDataSourceFactory = null;
  private static HttpDataSource.Factory defaultHttpDataSourceFactory = null;
  private static String userAgent = null;

  public static void setUserAgent(String userAgent) {
    Log.d(TAG, "setUserAgent");
    DataSourceUtil.userAgent = userAgent;
  }

  @SuppressLint("UnsafeOptInUsageError")
  public static String getUserAgent(ReactContext context) {
    Log.d(TAG, "getUserAgent");
    if (userAgent == null) {
      userAgent = Util.getUserAgent(context, "RnVideo");
    }
    return userAgent;
  }

  public static DataSource.Factory getRawDataSourceFactory(ReactContext context) {
    Log.d(TAG, "getRawDataSourceFactory");
    if (rawDataSourceFactory == null) {
      rawDataSourceFactory = buildRawDataSourceFactory(context);
    }
    return rawDataSourceFactory;
  }

  public static void setRawDataSourceFactory(DataSource.Factory factory) {
    Log.d(TAG, "setRawDataSourceFactory");
    DataSourceUtil.rawDataSourceFactory = factory;
  }


  public static DataSource.Factory getDefaultDataSourceFactory(
    ReactContext context,
    DefaultBandwidthMeter bandwidthMeter,
    Map<String, String> requestHeaders) {
    Log.d(TAG, "getDefaultDataSourceFactory");
    if (defaultDataSourceFactory == null || (requestHeaders != null && !requestHeaders.isEmpty())) {
      defaultDataSourceFactory = buildDataSourceFactory(context, bandwidthMeter, requestHeaders);
    }
    return defaultDataSourceFactory;
  }

  public static void setDefaultDataSourceFactory(DataSource.Factory factory) {
    Log.d(TAG, "setDefaultDataSourceFactory");
    DataSourceUtil.defaultDataSourceFactory = factory;
  }

  public static HttpDataSource.Factory getDefaultHttpDataSourceFactory(
    ReactContext context,
    DefaultBandwidthMeter bandwidthMeter,
    Map<String, String> requestHeaders) {
    Log.d(TAG, "getDefaultHttpDataSourceFactory");
    if (defaultHttpDataSourceFactory == null || (requestHeaders != null && !requestHeaders.isEmpty())) {
      defaultHttpDataSourceFactory = buildHttpDataSourceFactory(context, bandwidthMeter, requestHeaders);
    }
    return defaultHttpDataSourceFactory;
  }

  public static void setDefaultHttpDataSourceFactory(HttpDataSource.Factory factory) {
    Log.d(TAG, "setDefaultHttpDataSourceFactory");
    DataSourceUtil.defaultHttpDataSourceFactory = factory;
  }

  private static DataSource.Factory buildRawDataSourceFactory(ReactContext context) {
    Log.d(TAG, "buildRawDataSourceFactory");
    return new RawResourceDataSourceFactory(context.getApplicationContext());
  }

  private static DataSource.Factory buildDataSourceFactory(
    ReactContext context,
    DefaultBandwidthMeter bandwidthMeter,
    Map<String, String> requestHeaders) {
    Log.d(TAG, "buildDataSourceFactory");
    return new DefaultDataSource.Factory(
      context,
      buildHttpDataSourceFactory(context, bandwidthMeter, requestHeaders));
  }

  @SuppressLint("UnsafeOptInUsageError")
  private static HttpDataSource.Factory buildHttpDataSourceFactory(
    ReactContext context,
    DefaultBandwidthMeter bandwidthMeter,
    Map<String, String> requestHeaders) {
    Log.d(TAG, "buildHttpDataSourceFactory");
    OkHttpClient client = OkHttpClientProvider.getOkHttpClient();
    CookieJarContainer container = (CookieJarContainer) client.cookieJar();
    ForwardingCookieHandler handler = new ForwardingCookieHandler(context);
    container.setCookieJar(new JavaNetCookieJar(handler));
    @SuppressLint("UnsafeOptInUsageError")
    OkHttpDataSource.Factory okHttpDataSourceFactory =
      new OkHttpDataSource.Factory((Call.Factory) client)
        .setUserAgent(getUserAgent(context))
        .setTransferListener(bandwidthMeter);

    if (requestHeaders != null)
      okHttpDataSourceFactory.setDefaultRequestProperties(requestHeaders);

    return okHttpDataSourceFactory;
  }
}
