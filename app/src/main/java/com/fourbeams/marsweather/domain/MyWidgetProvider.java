package com.fourbeams.marsweather.domain;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.fourbeams.marsweather.R;
import com.fourbeams.marsweather.persistence.MarsWeatherContentProvider;
import com.fourbeams.marsweather.presentation.MainActivity;

public class MyWidgetProvider extends AppWidgetProvider {

    private final String UPDATE_TEMPERATURE_BUTTON_PRESSED
            = "com.fourbeams.marsweather.intent.action.UPDATE_TEMPERATURE_BUTTON_PRESSED";
    private final String DATA_CHANGED_IN_PROVIDER
            = "com.fourbeams.marsweather.intent.action.DATA_CHANGED_IN_PROVIDER";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // obtaining new data from server

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        displayLoadingIndicator(remoteViews);
        ServiceHelper.getInstance(context.getApplicationContext()).runService(ServiceHelper.task.GET_NEW_WEATHER_DATA_FROM_SERVER);
        for (int i = 0; i < appWidgetIds.length; i++) {

            // open main activity when widget area clicked
            Intent startMainActivityIntent = new Intent(context, MainActivity.class);
            PendingIntent startMainActivityPendingIntent = PendingIntent.getActivity(context, 0, startMainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_top_layout, startMainActivityPendingIntent);

            Intent updateButtonPressedIntent = new Intent(context, MyWidgetProvider.class);
            updateButtonPressedIntent.setAction(UPDATE_TEMPERATURE_BUTTON_PRESSED);
            updateButtonPressedIntent.putExtra(appWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            PendingIntent updateButtonPressedPendingIntent = PendingIntent.getBroadcast(context, 0, updateButtonPressedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_button_refresh, updateButtonPressedPendingIntent);
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Intent intent = new Intent(context, MyWidgetProvider.class);
        intent.setAction(DATA_CHANGED_IN_PROVIDER);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        displayLoadingIndicator(remoteViews);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // if update data from server button where pressed
        // we start service to get data from server
        if (intent.getAction().equals(UPDATE_TEMPERATURE_BUTTON_PRESSED)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetId = intent.getIntExtra(appWidgetManager.EXTRA_APPWIDGET_ID, appWidgetManager.INVALID_APPWIDGET_ID);
            displayLoadingIndicator(remoteViews);
            ServiceHelper.getInstance(context.getApplicationContext()).runService(ServiceHelper.task.GET_NEW_WEATHER_DATA_FROM_SERVER);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        // if data changed at content provider
        // we update the UI using data from cursor via cursor loader
        if (intent.getAction().equals(DATA_CHANGED_IN_PROVIDER)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int [] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));
            String URL = "content://" + MarsWeatherContentProvider.PROVIDER_NAME + "/temperature/last_date";
            CursorLoader cursorLoader = new CursorLoader(
                    context,
                    Uri.parse(URL),
                    MarsWeatherContentProvider.TEMPERATURE_PROJECTION,
                    null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            if (cursor.moveToLast()) {
                String latestDateInContentProvider = cursor.getString(cursor.getColumnIndex(MarsWeatherContentProvider.TERRESTRIAL_DATE));
                Integer maxTempCInContentProvider = cursor.getInt(cursor.getColumnIndex(MarsWeatherContentProvider.MAX_TEMP_C));
                Integer minTempCInContentProvider = cursor.getInt(cursor.getColumnIndex(MarsWeatherContentProvider.MIN_TEMP_C));
                remoteViews.setTextViewText(R.id.textView, latestDateInContentProvider);
                remoteViews.setTextViewText(R.id.textView2, String.valueOf(maxTempCInContentProvider));
                remoteViews.setTextViewText(R.id.textView3, String.valueOf(minTempCInContentProvider));
                for (int i=0; i<appWidgetIds.length; i++) {
                    hideLoadingIndicator(remoteViews);
                    appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
                }
            }
        }
        // if service finished and returned, that no new data updated from server to content provider
        // we just stop the rotation of loading indicator
        if (intent.getAction().equals(Processor.PROCESSOR_RESPONDED_WITH_NO_NEW_DATA_AT_SERVER)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int [] appWidgetIds = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));
            for (int i=0; i<appWidgetIds.length; i++) {
                hideLoadingIndicator(remoteViews);
                // TODO we update app widget without setting actual values from content provider to remoteViews
                appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
            }
        }
        super.onReceive(context, intent);
    }

    private void hideLoadingIndicator(RemoteViews remoteViews){
        remoteViews.setViewVisibility(R.id.progress_spinner,View.GONE);
        remoteViews.setViewVisibility(R.id.widget_button_refresh,View.VISIBLE);
    }

    private void displayLoadingIndicator(RemoteViews remoteViews){
        remoteViews.setViewVisibility(R.id.progress_spinner,View.VISIBLE);
        remoteViews.setViewVisibility(R.id.widget_button_refresh,View.GONE);
    }

}
