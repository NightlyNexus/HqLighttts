package com.ifttt.hqlighttts.appwidget;

import com.ifttt.hqlighttts.R;
import com.ifttt.hqlighttts.buildingstatus.BuildingStatus;
import com.ifttt.hqlighttts.buildingstatus.BuildingStatusNetworkUtils;
import com.nightlynexus.dokie.network.DokieHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LightProvider extends AppWidgetProvider {

    private static final String TAG = LightProvider.class.getSimpleName();
    private static final int REQUEST_CODE_CLICK_CONTENT = 0;
    private static final String PATTERN_DATE_FORMAT_TIMESTAMP_READABLE_12 = "h:mm:ss a";
    private static final String PATTERN_DATE_FORMAT_TIMESTAMP_READABLE_24 = "HH:mm:ss";

    private static DokieHttpClient.CallObject<BuildingStatus> buildingStatusCallObject = null;

    private static DateFormat getDateFormat(Context context) {
        boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
        String pattern = is24HourFormat ? PATTERN_DATE_FORMAT_TIMESTAMP_READABLE_24
                : PATTERN_DATE_FORMAT_TIMESTAMP_READABLE_12;
        DateFormat dateFormat = new SimpleDateFormat(pattern,
                context.getResources().getConfiguration().locale);
        dateFormat.setTimeZone(TimeZone.getDefault()); // best way to get current timezone, for now
        return dateFormat;
    }

    private static CharSequence formatReadable(DateFormat dateFormat, Date date) {
        String readable = dateFormat.format(date);
        readable = readable.replace("AM", "am").replace("PM", "pm");
        return readable;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (buildingStatusCallObject != null) {
            try {
                buildingStatusCallObject.cancel();
            } catch (NetworkOnMainThreadException e) {
                e.printStackTrace();
                // Strict Mode to be fixed in AOSP
                // https://github.com/square/okhttp/issues/1592
            }
        }
        // update all widgets
        appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, LightProvider.class));
        String packageName = context.getPackageName();
        RemoteViews contentViews = new RemoteViews(packageName,
                R.layout.appwidget_light);
        contentViews.removeAllViews(R.id.content);
        PendingIntent clickPendingIntent = getClickContentPendingIntent(context, appWidgetIds);
        contentViews.setOnClickPendingIntent(R.id.content, clickPendingIntent);
        addLoadingViews(contentViews, packageName);
        appWidgetManager.updateAppWidget(appWidgetIds, contentViews);
        buildingStatusCallObject = BuildingStatusNetworkUtils.newCallBuildingStatus();
        buildingStatusCallObject.enqueueObject(BuildingStatus.class, new WidgetCallback(
                buildingStatusCallObject, appWidgetManager, contentViews, packageName, appWidgetIds,
                clickPendingIntent, getDateFormat(context),
                context.getString(R.string.appwidget_timestamp)));
    }

    // simply update the whole widget
    static PendingIntent getClickContentPendingIntent(Context context, int[] appWidgetIds) {
        Intent updateIntent = new Intent(context, LightProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, REQUEST_CODE_CLICK_CONTENT, updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void addLoadingViews(RemoteViews contentViews, String packageName) {
        contentViews.addView(R.id.content, new RemoteViews(packageName,
                R.layout.appwidget_light_loading));
    }

    private static class WidgetCallback implements DokieHttpClient.CallbackObject<BuildingStatus> {

        @NonNull private final DokieHttpClient.CallObject<BuildingStatus> mCallObject;
        @NonNull private final AppWidgetManager mAppWidgetManager;
        @NonNull private final RemoteViews mContentViews;
        @NonNull private final String mPackageName;
        @NonNull private final int[] mAppWidgetIds;
        @NonNull private final PendingIntent mClickPendingIntent;
        @NonNull private final DateFormat mDateFormat;
        @NonNull private final String mTimestampFormat;

        public WidgetCallback(
                @NonNull DokieHttpClient.CallObject<BuildingStatus> callObject,
                @NonNull AppWidgetManager appWidgetManager, @NonNull RemoteViews contentViews,
                @NonNull String packageName, @NonNull int[] appWidgetIds,
                @NonNull PendingIntent clickPendingIntent, @NonNull DateFormat dateFormat,
                @NonNull String timestampFormat) {
            mCallObject = callObject;
            mAppWidgetManager = appWidgetManager;
            mContentViews = contentViews;
            mPackageName = packageName;
            mAppWidgetIds = appWidgetIds;
            mClickPendingIntent = clickPendingIntent;
            mDateFormat = dateFormat;
            mTimestampFormat = timestampFormat;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            Log.d(TAG, e.getMessage());
            if (mCallObject.isCanceled()) {
                // will update with a new request soon
                return;
            }
            RemoteViews failureViews = new RemoteViews(mPackageName,
                    R.layout.appwidget_light_failure);
            failureViews.setOnClickPendingIntent(R.id.failure, mClickPendingIntent);
            mAppWidgetManager.updateAppWidget(mAppWidgetIds, failureViews);
        }

        @Override
        public void onResponse(Response response, BuildingStatus buildingStatus) {
            mContentViews.setViewVisibility(R.id.loading, View.GONE);
            int roomsSize = buildingStatus.rooms.size();
            if (roomsSize > 0) {
                mContentViews.addView(R.id.content, new RemoteViews(mPackageName,
                        R.layout.appwidget_light_item_container));
                for (int i = 0; i < roomsSize; i++) {
                    int key = buildingStatus.rooms.keyAt(i);
                    int value = buildingStatus.rooms.get(key);
                    RemoteViews lightItemViews = new RemoteViews(mPackageName,
                            R.layout.appwidget_light_item);
                    int drawableResId;
                    switch (value) {
                        case BuildingStatus.OPEN:
                            drawableResId = R.drawable.ic_light_open;
                            break;
                        case BuildingStatus.CLOSED:
                            drawableResId = R.drawable.ic_light_closed;
                            break;
                        case BuildingStatus.CAUTION:
                            drawableResId = R.drawable.ic_light_caution;
                            break;
                        default:
                            drawableResId = R.drawable.ic_light_unknown;
                            break;
                    }
                    lightItemViews.setImageViewResource(R.id.item_icon, drawableResId);
                    lightItemViews.setTextViewText(R.id.item_name, Integer.toString(key));
                    mContentViews.addView(R.id.lights, lightItemViews);
                    mAppWidgetManager.updateAppWidget(mAppWidgetIds, mContentViews);
                }
            }
            if (buildingStatus.timestamp != null) {
                // DateFormat preferences from when the network call was originally made;
                // may not be current
                CharSequence timestampReadable = String.format(mTimestampFormat,
                        formatReadable(mDateFormat, buildingStatus.timestamp));
                RemoteViews updatedAgoViews = new RemoteViews(mPackageName,
                        R.layout.appwidget_light_updated_timestamp);
                updatedAgoViews.setTextViewText(R.id.timestamp, timestampReadable);
                mContentViews.addView(R.id.content, updatedAgoViews);
            }
            mAppWidgetManager.updateAppWidget(mAppWidgetIds, mContentViews);
        }
    }
}
