package com.geekyouup.android.ustopwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

public class UltimateStopwatchLauncher extends Activity {

	private static boolean IS_HONEYCOMB = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent startActivityIntent = null;
		if (IS_HONEYCOMB && isXLargeScreen())
		{
			startActivityIntent = new Intent(this, UltimateStopwatchTabletActivity.class);
		}
		else
			startActivityIntent = new Intent(this, UltimateStopwatch.class);

		startActivity(startActivityIntent);
		finish();
	}

	private boolean isXLargeScreen() {
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {

			return true;
		}
		return false;
	}

}
