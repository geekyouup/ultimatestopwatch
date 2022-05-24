package com.geekyouup.android.ustopwatch;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class TabsAdapter extends FragmentPagerAdapter {
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private AppCompatActivity mAppCompatActivity;

    static final class TabInfo {
        private final Class<?> clss;
        private final Bundle args;
        private String title;

        TabInfo(String _title, Class<?> _class, Bundle _args) {
            title = _title;
            clss = _class;
            args = _args;
        }
    }

    public TabsAdapter(AppCompatActivity activity) {
        super(activity.getSupportFragmentManager());
        mAppCompatActivity = activity;
    }

    public void addTab(String title, Class<?> clss, Bundle args) {
        TabInfo info = new TabInfo(title, clss, args);
        mTabs.add(info);
        notifyDataSetChanged();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabs.get(position).title;
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(position);
        return Fragment.instantiate(mAppCompatActivity,
                info.clss.getName(), info.args);
    }
}