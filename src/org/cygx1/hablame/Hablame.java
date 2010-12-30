package org.cygx1.hablame;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class Hablame extends Activity implements View.OnClickListener {
	
	Button button, playButton;
	
	private static final int PREFS_ID = 0;
	private static final int EMAIL_SEND_RESULT = 0;
	
	// To store the output file
	static File outputFile = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	    button = (Button)findViewById(R.id.Button01);
        button.setOnClickListener(this);
        
        playButton = (Button)findViewById(R.id.Playback);
        playButton.setOnClickListener(this);
    }

    public void onClick(View v) {
		if(v == button) {
			Intent iRecording = new Intent( this, HablameRecording.class);
			iRecording.setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity( iRecording);
		} else if(v == playButton) {
			Intent iPlayback = new Intent( this, HablamePlayback.class);
			iPlayback.setFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity( iPlayback);
		}
	}

	/**
	 * Handle the preferences menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, PREFS_ID, Menu.NONE, "Prefs")
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setAlphabeticShortcut('p');
		return (super.onCreateOptionsMenu(menu));
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case PREFS_ID:
			startActivity(new Intent(this, HablamePreferences.class));
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

}