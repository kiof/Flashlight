package com.kiof.flashlight;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetFlashlight extends AppWidgetProvider {
	private static final String WIDGET = "widget";

	public void onEnabled(Context context) {
		super.onEnabled(context);
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App WidgetFlashlight that belongs to this provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];

			// Create an Intent to launch PlaySound
			Intent intentFlashlight = new Intent(context, Flashlight.class);
			intentFlashlight.putExtra(WIDGET, true);
			PendingIntent pendingIntentFlashlight = PendingIntent.getActivity(context, 0, intentFlashlight, PendingIntent.FLAG_CANCEL_CURRENT);

			// Get the layout for the App WidgetFlashlight and attach an on-click listener to the button
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widgeticon, pendingIntentFlashlight);

			// Tell the AppWidgetManager to perform an update on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}
