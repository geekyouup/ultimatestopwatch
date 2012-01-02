package com.geekyouup.android.ustopwatch.fragments;

import com.geekyouup.android.ustopwatch.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class LapTimesActivity extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.laptimes_layout);
		
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.laptimes_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_reset) {
			LapTimeRecorder.getInstance().reset(this);
			
			LapTimesFragment ltf = (LapTimesFragment) getFragmentManager().findFragmentById(R.id.laptimes_fragment_from_layout);
			if(ltf != null) ltf.reset();
		}else if(item.getItemId() == android.R.id.home){
            // app icon in action bar clicked; go home
            finish();
	    }
			
		return true;
	}
	
}
