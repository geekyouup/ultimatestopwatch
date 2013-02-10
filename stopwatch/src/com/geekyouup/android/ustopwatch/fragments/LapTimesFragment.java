package com.geekyouup.android.ustopwatch.fragments;

import java.util.ArrayList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.geekyouup.android.ustopwatch.R;
import com.geekyouup.android.ustopwatch.UltimateStopwatchActivity;

public class LapTimesFragment extends SherlockListFragment implements LapTimeListener {

	private LapTimesBaseAdapter mAdapter;
	private ArrayList<LapTimeBlock> mLapTimes = new ArrayList<LapTimeBlock>();
	private LapTimeRecorder mLapTimeRecorder;
    private ArrayList<Integer> mCheckedItems;

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
        final LapTimesFragment ltf = this;
        ListView listView = getListView();
        listView.setCacheColorHint(Color.WHITE);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        //MultiMode Choice is only available in Honeycomb+
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(android.view.ActionMode actionMode, int i, long l, boolean checked) {
                    if(mCheckedItems==null) mCheckedItems=new ArrayList<Integer>();
                    if(checked)
                    {
                        mCheckedItems.add(new Integer(i));
                        Log.d("USW", "Item clicked " + i + ", total " + mCheckedItems.size());
                    }
                    else
                    {
                        mCheckedItems.remove(new Integer(i));
                        Log.d("USW", "Item clicked " + i + ", total " + mCheckedItems.size());
                    }
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
                            mLapTimeRecorder.deleteLapTimes(mCheckedItems,ltf);
                            actionMode.finish(); // Action picked, so close the CAB
                            mCheckedItems.clear();
                            mCheckedItems=null;
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(android.view.ActionMode actionMode) {
                }
            });
        }

        mAdapter=new LapTimesBaseAdapter(getActivity(), mLapTimes);
		setListAdapter(mAdapter);

        ((UltimateStopwatchActivity)getActivity()).registerLapTimeFragment(this);

        //on long touch start the contextual actionbar
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
                final ListView listView = getListView();
                getSherlockActivity().startActionMode(new ActionMode.Callback(){
                    @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {return false;}
                    @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
                    @Override public void onDestroyActionMode(ActionMode mode) {}
                    @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {return false;}
                });
                return true;
            }
        });
	}

    @Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// if vars stored then use them
        mLapTimes.clear();
        mLapTimes.addAll(mLapTimeRecorder.getTimes());
        mAdapter.notifyDataSetChanged();
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

	@Override
	public void lapTimesUpdated() {
        if(mLapTimeRecorder==null) mLapTimeRecorder = LapTimeRecorder.getInstance();
        if(mLapTimes==null) mLapTimes=new ArrayList<LapTimeBlock>() ;

        mLapTimes.clear();
        mLapTimes.addAll(mLapTimeRecorder.getTimes());
		notifyDataSetChanged();
	}
}
