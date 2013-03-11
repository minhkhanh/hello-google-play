package com.fantageek.hello;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Application;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		EasyTracker.getInstance().setContext(this);
		super.onCreate();
	}
}
