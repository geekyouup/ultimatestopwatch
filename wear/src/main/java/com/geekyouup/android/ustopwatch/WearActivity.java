package com.geekyouup.android.ustopwatch;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class WearActivity extends FragmentActivity {

    public static final String MSG_UPDATE_COUNTER_TIME = "msg_update_counter";
    public static final String MSG_NEW_TIME_DOUBLE = "msg_new_time_double";
    public static final String MSG_STATE_CHANGE = "msg_state_change";
    public static final String MSG_RESET = "msg_reset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
    }
}
