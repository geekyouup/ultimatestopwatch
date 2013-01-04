package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;


public class LapTimesFragment extends SherlockListFragment implements LapTimeListener {

	private LapTimesBaseAdapter mAdapter;
	private ArrayList<LapTimeBlock> mLapTimes = new ArrayList<LapTimeBlock>();
	private static final String PREFS_NAME_LAPTIMESFRAG = "usw_prefs_laptimesfrag";
	private static final String KEY_CURRENT_VIEW = "current_view";
	private ViewFlipper mViewFlipper;
	private LapTimeRecorder mLapTimeRecorder;
    private ActionMode mActionMode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLapTimeRecorder = LapTimeRecorder.getInstance();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.laptimes_fragment, container, false);
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getListView().setCacheColorHint(Color.WHITE);
		mAdapter=new LapTimesBaseAdapter(getActivity(), mLapTimes);
		setListAdapter(mAdapter);

        ((UltimateStopwatchActivity)getActivity()).registerLapTimeFragment(this);

        /*final ActionMode.Callback actionModeCallback = new ActionMode.Callback(){

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_laptimes_contextual, menu);
                return true;
            }

            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public void onDestroyActionMode(ActionMode mode) {}

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_context_delete:

                        mode.finish();
                        return true;
                    default:
                        mode.finish();
                        return false;
                }
            }
        };

        AdapterView.OnItemLongClickListener listener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
                final ListView listView = getListView();



                mActionMode = getSherlockActivity().startActionMode(actionModeCallback);
                view.setSelected(true);
                return true;
            }
        };

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode actionMode, int i, long l, boolean b) {
                Toast.makeText(getSherlockActivity(),"Item clicked " + i,Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onCreateActionMode(android.view.ActionMode actionMode, android.view.Menu menu) {
                android.view.MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.menu_laptimes_contextual, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode actionMode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode actionMode, android.view.MenuItem menuItem) {
                // Respond to clicks on the actions in the CAB
                switch (menuItem.getItemId()) {
                    case R.id.menu_context_delete:
                        //deleteSelectedItems();
                        actionMode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode actionMode)
            {
                mActionMode = null;
            }
        });
        getListView().setOnItemLongClickListener(listener);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mActionMode!=null) view.setSelected(true);
            }
        });    */
	}


    @Override
	public void onPause() {
		super.onPause();
		
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMESFRAG, Context.MODE_PRIVATE);
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			if (editor != null) {
				if(mViewFlipper!=null) editor.putInt(KEY_CURRENT_VIEW, mViewFlipper.getDisplayedChild());
				editor.commit();
			}
		}
		
		Log.d("USW","LaptimesFragment.onPause()");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.d("USW","LaptimesFragment.onResume()");
		
		// if vars stored then use them
		SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME_LAPTIMESFRAG, Context.MODE_PRIVATE);
		if (settings != null) {
            mLapTimes.clear();
            mLapTimes.addAll(mLapTimeRecorder.getTimes());
            mAdapter.notifyDataSetChanged();
            
            setMode(settings.getInt(KEY_CURRENT_VIEW, 0));
		}
	}

	public void reset()
	{
		mLapTimes.clear();
		mAdapter.notifyDataSetChanged();
	}

	public void notifyDataSetChanged()
	{
		mAdapter.notifyDataSetChanged();
	}
	
	public void setMode(int mode)
	{
		if(mViewFlipper!= null)
		{
			mViewFlipper.setDisplayedChild(mode);
		}
	}

	@Override
	public void lapTimesUpdated() {
        if(mLapTimeRecorder==null) mLapTimeRecorder = LapTimeRecorder.getInstance();
        if(mLapTimes==null) mLapTimes=new ArrayList<LapTimeBlock>() ;

        mLapTimes.clear();
        mLapTimes.addAll(mLapTimeRecorder.getTimes());
		notifyDataSetChanged();
	}
}
