package org.cygx1.hablame;

import java.io.IOException;

import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HablamePlayback extends Activity implements View.OnClickListener {
	
	//	Amount of time to rewind before resuming a pause
	static final int REW_MILLIS = 2000;
	
	//	Visual elements
	Button playHardcoded, stopAndRecord;
	
	//	Non-visual classes
	MediaPlayer player;
	
	//	Keep current playback status
	String currentPlayback = null;
	int currentPosition = -1;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.playback);

	    stopAndRecord = (Button)findViewById(R.id.PauseRecord);
	    stopAndRecord.setOnClickListener(this);
	    
	    player = new MediaPlayer();
	    
	    //	TODO: use file from parameter instead of hardcoded test
	    currentPlayback = "/sdcard/cygx1/hablame/test.3gp";
	}
	protected void onDestroy() {
		super.onDestroy();
		if( player.isPlaying()) {
			player.stop();
		}
		player.release();
	}
	
	protected void onStart() {
		Log.d(getClass().getName(),"onStart");
		super.onStart();
	    startPlayback();
	}

	public void onClick(View v) {
		if( v == stopAndRecord) {
			stopPlayback();
			goToRecording();
		} else {
			Log.d( this.getClass().getName(),"Unknown button clicked: " + v);
		}
	}

	private void stopPlayback() {
		if( player.isPlaying()) {
			player.pause();
			currentPosition = player.getCurrentPosition();
		} else {
			currentPosition = -1;
		}
	}

	private void goToRecording() {
		Intent iRecording = new Intent(this,HablameRecording.class);
		iRecording.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		iRecording.setAction( Hablame.ACTION_RECORDING_RESUME);
		startActivity(iRecording);
	}

	private void startPlayback() {
		//	Ignore a 'play' button if we're already playing
		if( player.isPlaying()) 
			return;
		
		//	Drop here if we're playing a new file
		if( currentPosition == -1) {
		    try {
				player.reset();
				player.setDataSource( currentPlayback);
				player.prepare();
				player.start();
			} catch (IOException e) {
				Log.e( this.getClass().getName(), "Exception trying to play file", e);
			}
			
		//	We had already started playback before
		} else {
			//	Rewind a little to regain context from last pause
			player.seekTo(Math.max(0, currentPosition-REW_MILLIS));
			player.start();
		}	
	}
}
