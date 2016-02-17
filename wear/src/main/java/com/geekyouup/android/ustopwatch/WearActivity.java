package com.geekyouup.android.ustopwatch;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.geekyouup.android.ustopwatch.wear.R;

public class WearActivity extends FragmentActivity {

    private TextView mTextView;
    public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
    public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
    public static final String MSG_STATE_CHANGE = "msg_state_change";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
    }
}
