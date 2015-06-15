package com.ifttt.hqlighttts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.ifttt.hqlighttts.buildingstatus.BuildingStatus;
import com.ifttt.hqlighttts.buildingstatus.BuildingStatusNetworkUtils;
import com.ifttt.hqlighttts.util.datastructure.sortedintmap.SortedIntMap;
import com.nightlynexus.dokie.network.DokieHttpClient;
import com.nightlynexus.dokie.network.image.LruCacheImage;
import com.nightlynexus.dokie.network.object.GsonConverter;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.ActivityManager;
import android.app.Application;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class HqLightttsApp extends Application {

    @NonNull public static final DokieHttpClient HTTP_CLIENT;

    @NonNull private static final String REQUEST_HEADER_USER_AGENT
            = "HQ Lighttts Android \u2014 " + BuildConfig.VERSION_CODE + " \u2014 "
            + Build.VERSION.RELEASE + " \u2014 " + Build.MODEL;
    @NonNull private static final Gson GSON;
    @NonNull private static final Interceptor NETWORK_INTERCEPTOR;

    static {
        GSON = new GsonBuilder()
                .registerTypeAdapter(BuildingStatus.class,
                        new BuildingStatusNetworkUtils.BuildingStatusTypeAdapter())
                .create();
        NETWORK_INTERCEPTOR = new Interceptor() {

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request.Builder result = originalRequest.newBuilder();
                String headerNameUserAgent = "User-Agent";
                if (originalRequest.header(headerNameUserAgent) == null) {
                    result.header(headerNameUserAgent, REQUEST_HEADER_USER_AGENT);
                }
                return chain.proceed(result.build());
            }
        };
        HTTP_CLIENT = new DokieHttpClient();
        HTTP_CLIENT.setConverter(new GsonConverter(GSON));
        HTTP_CLIENT.networkInterceptors().add(NETWORK_INTERCEPTOR);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SortedIntMap s = new SortedIntMap(4);
        for (int i = 1; i <= 4; i++) {
            s.put(i, i);
        }
        HTTP_CLIENT.setCache(new Cache(new File(getCacheDir().getPath()), 20 * 1024 * 1024));
        HTTP_CLIENT.setCacheImage(new LruCacheImage(calculateMemoryCacheSize()));
    }

    private int calculateMemoryCacheSize() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target a seventh of the available heap
        return 1024 * 1024 * memoryClass / 7;
    }
}
