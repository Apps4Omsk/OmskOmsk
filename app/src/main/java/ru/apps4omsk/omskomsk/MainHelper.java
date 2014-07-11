package ru.apps4omsk.omskomsk;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * MainHelper
 */
public class MainHelper {

    private Context context;

    public MainHelper(Context context) {
        this.context = context;
    }

    public long getTimeOfLastCheckInMs() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        final long timeOfLastCheckIn = sharedPreferences.getLong("timeOfLastCheckIn", -1);
        return timeOfLastCheckIn;
    }

    public void updateTimeOfLastCheckInMs(long timeMs) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong("timeOfLastCheckIn", timeMs).commit();
    }
}
