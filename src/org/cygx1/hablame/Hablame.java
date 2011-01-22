package org.cygx1.hablame;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class Hablame extends Activity implements View.OnClickListener {
	
	Button button, playButton;
	
	private static final int PREFS_ID = 0;
	
	static final String ACTION_RECORDING_RESUME = "recording_resume";
	static final String ACTION_RECORDING_START = "recording_start";
	
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
			iRecording.setAction( ACTION_RECORDING_START);
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