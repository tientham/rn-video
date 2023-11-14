/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */
package com.rnvideo.video;

import android.annotation.SuppressLint;
import android.content.Context;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.network.CookieJarContainer;
import com.facebook.react.modules.network.ForwardingCookieHandler;
import com.facebook.react.modules.network.OkHttpClientProvider;

import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;

import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.JavaNetCookieJar;

public class DataSourceUtil {

    private static DataSource.Factory rawDataSourceFactory = null;
    private static DataSource.Factory defaultDataSourceFactory = null;
    private static HttpDataSource.Factory defaultHttpDataSourceFactory = null;
    private static String userAgent = null;

    public static void setUserAgent(String userAgent) {
        DataSourceUtil.userAgent = userAgent;
    }

    @SuppressLint("UnsafeOptInUsageError")
    public static String getUserAgent(Context context) {
        if (userAgent == null) {
            userAgent = Util.getUserAgent(context, "RnVideo");
        }
        return userAgent;
    }

    public static DataSource.Factory getRawDataSourceFactory(Context context) {
        if (rawDataSourceFactory == null) {
            rawDataSourceFactory = buildRawDataSourceFactory(context);
        }
        return rawDataSourceFactory;
    }

    public static void setRawDataSourceFactory(DataSource.Factory factory) {
        DataSourceUtil.rawDataSourceFactory = factory;
    }


    public static DataSource.Factory getDefaultDataSourceFactory(Context context, DefaultBandwidthMeter bandwidthMeter, Map<String, String> requestHeaders) {
        if (defaultDataSourceFactory == null || (requestHeaders != null && !requestHeaders.isEmpty())) {
            defaultDataSourceFactory = buildDataSourceFactory(context, bandwidthMeter, requestHeaders);
        }
        return defaultDataSourceFactory;
    }

    public static void setDefaultDataSourceFactory(DataSource.Factory factory) {
        DataSourceUtil.defaultDataSourceFactory = factory;
    }

    public static HttpDataSource.Factory getDefaultHttpDataSourceFactory(Context context, DefaultBandwidthMeter bandwidthMeter, Map<String, String> requestHeaders) {
        if (defaultHttpDataSourceFactory == null || (requestHeaders != null && !requestHeaders.isEmpty())) {
            defaultHttpDataSourceFactory = buildHttpDataSourceFactory(context, bandwidthMeter, requestHeaders);
        }
        return defaultHttpDataSourceFactory;
    }

    public static void setDefaultHttpDataSourceFactory(HttpDataSource.Factory factory) {
        DataSourceUtil.defaultHttpDataSourceFactory = factory;
    }

    private static DataSource.Factory buildRawDataSourceFactory(Context context) {
        return new RawResourceDataSourceFactory(context.getApplicationContext());
    }

    private static DataSource.Factory buildDataSourceFactory(Context context, DefaultBandwidthMeter bandwidthMeter, Map<String, String> requestHeaders) {
        return new DefaultDataSource.Factory(context,
                buildHttpDataSourceFactory(context, bandwidthMeter, requestHeaders));
    }

    private static HttpDataSource.Factory buildHttpDataSourceFactory(Context context, DefaultBandwidthMeter bandwidthMeter, Map<String, String> requestHeaders) {
        OkHttpClient client = OkHttpClientProvider.getOkHttpClient();
        CookieJarContainer container = (CookieJarContainer) client.cookieJar();
        ForwardingCookieHandler handler = new ForwardingCookieHandler(context);
        container.setCookieJar(new JavaNetCookieJar(handler));
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory((Call.Factory) client)
                .setUserAgent(getUserAgent(context))
                .setTransferListener(bandwidthMeter);

        if (requestHeaders != null)
            okHttpDataSourceFactory.setDefaultRequestProperties(requestHeaders);

        return okHttpDataSourceFactory;
    }
}
