/**
 * ShortcutHelper.java
 * Implements the ShortcutHelper class
 * A ShortcutHelper creates and handles station shortcuts on the Home screen
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;


/**
 * ShortcutHelper class
 */
public class ShortcutHelper {

    /* Define log tag */
    private static final String LOG_TAG = ShortcutHelper.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String STREAM_URI = "streamUri";
    private static final String STATION_NAME = "stationName";
    private static final String STATION_ID = "stationID";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String STATION_ID_LAST = "stationIDLast";
    private static final String PLAYBACK = "playback";


    /* Main class variables */
    private Activity mActivity;
    private Collection mCollection;


    /* Constructor */
    public ShortcutHelper(Activity activity, Collection collection) {
        mActivity = activity;
        mCollection = collection;
    }


    /* Creates shortcut on Home screen */
    private void createShortcut(Station station) {

        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(mActivity, MainActivity.class);
        shortcutIntent.putExtra(STREAM_URI, station.getStreamUri().toString());
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shortcutIntent.setAction(ACTION_PLAY);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.getStationName());

        // set image for station
        if (station.getStationImageFile().exists()) {
            // station image
            Bitmap icon = BitmapFactory.decodeFile(station.getStationImageFile().toString());
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        } else {
            // default image
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(mActivity, R.mipmap.ic_launcher));
        }

        addIntent.putExtra("duplicate", false);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        mActivity.getApplicationContext().sendBroadcast(addIntent);
    }


    /* Handles incoming intent from Home screen shortcut  */
    private void handleShortcutIntent(Intent intent, Bundle savedInstanceState) {
        String streamUri = intent.getStringExtra(STREAM_URI);

        // check if there is a previous saved state to detect if the activity is restored
        // after being destroyed and that playback should not be resumed
        if (ACTION_PLAY.equals(intent.getAction()) && savedInstanceState == null) {

            // find the station corresponding to the stream URI
            int stationID = mCollection.findStationID(streamUri);
            if (stationID != -1) {
                String stationName = mCollection.getStations().get(stationID).getStationName();

                // get current playback state
                // TODO replace with loadAppState
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                int stationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
                boolean playback = settings.getBoolean(PLAYBACK, false);
                int stationIDLast = stationIDCurrent;

                // check if this station is not already playing
                if (!playback || stationIDCurrent != stationID) {
                    // start playback service
                    PlayerService playerService = new PlayerService();
                    playerService.startActionPlay(mActivity, streamUri, stationName);

                    stationIDLast = stationIDCurrent;
                    stationIDCurrent = stationID;
                    playback = true;
                }

                // save station name and ID
                // TODO replace with saveAppState
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(STATION_ID_CURRENT, stationIDCurrent);
                editor.putInt(STATION_ID_LAST, stationIDLast);
                editor.putBoolean(PLAYBACK, playback);
                editor.apply();

                // add name, url and id of station to intent
                Intent startIntent = new Intent(mActivity, PlayerActivity.class);
                startIntent.putExtra(STATION_NAME, stationName);
                startIntent.putExtra(STREAM_URI, streamUri);
                startIntent.putExtra(STATION_ID, stationID);

                // start activity with intent
                mActivity.startActivity(startIntent);
            }
            else {
                Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_stream_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

}