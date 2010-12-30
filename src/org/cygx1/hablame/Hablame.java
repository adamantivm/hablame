package org.cygx1.hablame;

import java.io.File;
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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Hablame extends Activity implements View.OnClickListener {
	
	Button button, stopRecording;
	Chronometer clock;
	MediaRecorder recorder;
	MediaPlayer player;
	AudioManager audioManager;
	ProgressBar amplitude;
	Timer ampTimer;
	
	static final int STATE_IDLE = 0;
	static final int STATE_RECORDING = 1;
	static final int STATE_SENDING = 2;
	static final int STATE_CONNECTING_BLUETOOTH = 2;
	int state = STATE_IDLE;
	
	private static final int PREFS_ID = 0;
	private static final int EMAIL_SEND_RESULT = 0;
	
	// To store the output file
	static File outputFile = null;
	
	// For updating the amplitude bar while recording
	private Runnable updateRecordAmplitudeTask = new Runnable() {
		public void run() {
    		int max = recorder.getMaxAmplitude();
    		int amp = (int) Math.round(100.0*max / 32767);
    		amplitude.setProgress(amp);
    		Log.d("Hablame", "Max amplitude: " + max);
    		amplitudeHandler.postDelayed(updateRecordAmplitudeTask, AMPLITUDE_UPDATE_FREQ);
		}
	};
	private Handler amplitudeHandler = new Handler();
	private static final int AMPLITUDE_UPDATE_FREQ = 100;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d( this.getClass().getName(), "onCreate");

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        BluetoothEnabledReceiver.setHablameActivity( this);
        
        recorder = new MediaRecorder();
        player = new MediaPlayer();
	    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

	    button = (Button)findViewById(R.id.Button01);
        button.setOnClickListener(this);
    }
	protected void onDestroy() {
		//	TODO: Trap possible accidental "back" button and ask for confirmation of deleted recording?
		super.onDestroy();
    	// TL: attempt to prevent some force closes by cleaning up after
    	// ourselves. Shut down the recorder
    	recorder.release();
	}

	protected void onStart() {
		super.onStart();
	}
    public void onStop() {
    	super.onStop();
    }

    void startRecording( boolean withBluetooth) {
        setContentView(R.layout.record);
        clock = (Chronometer)findViewById(R.id.Chronometer01);
		stopRecording = (Button)findViewById(R.id.StopRecording);
		stopRecording.setOnClickListener(this);
		amplitude = (ProgressBar)findViewById(R.id.RecordingAmplitude);
		amplitude.setProgress(50);

		// create a File object for the parent directory
		File snapDirectory = new File("/sdcard/cygx1/hablame/");
		// have the object build the directory structure, if needed.
		snapDirectory.mkdirs();

		// create a File object for the output file
		outputFile = new File(snapDirectory, String.format(
				"h%d.3gp", System.currentTimeMillis()));

		// Update informational widgets
		TextView recordingSource = (TextView)findViewById(R.id.RecordingSource);
		if (withBluetooth) {
			recordingSource.setText("bluetooth");
		} else {
			recordingSource.setText("built-in mic");
		}
		
	    audioManager.setBluetoothScoOn( withBluetooth);

	    // could use setPreviewDisplay() to display a preview to suitable View here
	    recorder.reset();
	    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	    //	Recording from bluetooth headset requires mono, 8kHz
	    recorder.setAudioChannels( 1);
	    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	    recorder.setOutputFile(outputFile.getAbsolutePath());
	    
	    // Schedule a periodic timer to display the audio amplitude
	    amplitudeHandler.removeCallbacks(updateRecordAmplitudeTask);
	    amplitudeHandler.postDelayed(updateRecordAmplitudeTask, AMPLITUDE_UPDATE_FREQ);
	    
	    try {
			recorder.prepare();
		    recorder.start();				
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Start the timer counting up
		clock.start();

		state = STATE_RECORDING;
	    button.setEnabled(true);
    }

	public void onClick(View v) {
		Log.d("Hablame", "In onClick, state is " + state);
		// 	TODO: Decouple / generalize / clean-up state machine implementation
		if( state == STATE_IDLE) {
			//	Start connection to bluetooth headset. The rest
			//	will happen when we receive the notification that the
			//	connection succeeded
		    audioManager.startBluetoothSco();
		    
		    new Timer().schedule(new TimerTask() {
				public void run() {
					//	After the timeout, bluetooth didn't connect. So give up
					if( state == STATE_CONNECTING_BLUETOOTH) {
						Log.d("Hablame","timed out waiting for bluetooth. Using built-in mic");
						runOnUiThread(new Runnable() {
							public void run() {
								Hablame.this.startRecording( false);
							}
						});
					}
				}
			}, 3000);
		    
		    state = STATE_CONNECTING_BLUETOOTH;
		    button.setText("[Connecting to bluetooth]");
		    button.setEnabled(false);

		} else if( state == STATE_RECORDING) {
			recorder.stop();
			//	Logic would indicate reset here. But even if you do, the underlying native code
			//	will fail to reset because it didn't finish stopping, despite having returned from the stop() call
			audioManager.setBluetoothScoOn(false);
			audioManager.stopBluetoothSco();
			
			// Stop UI displays
			clock.stop();
			amplitudeHandler.removeCallbacks(updateRecordAmplitudeTask);

			Toast.makeText( Hablame.this, "Recording stopped", Toast.LENGTH_SHORT).show();
			state = STATE_IDLE;
		    
		    sendEmail();
		    // TODO: delete output file after sending
		    outputFile = null;
		    
		    // Switch back to the main view
	        setContentView(R.layout.main);
	        button = (Button)findViewById(R.id.Button01);
		    button.setText("Start Recording");
		    button.setEnabled(true);
	        button.setOnClickListener(this);
		}
	}
	
	public void sendEmail() {
		String path = outputFile.getAbsolutePath();		
		// Get my email address out of preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String recipient = prefs.getString(getString(R.string.recipientPref), null);
        
		//	Peek file duration
        int duration = 0;
		try {
			player.setDataSource(path);
			player.prepare();
			duration = player.getDuration();
			player.reset();
		} catch (IOException e) {
			Log.e( this.getClass().getName(), "Error getting recording duration", e);
		}
		int seconds = duration / 1000;
		int minutes = seconds / 60;
		seconds -= minutes*60;

		/* JAC: Format with recording duration */
		String subject = String.format("%s%d:%02d",
				recipient.substring(0, 1).toUpperCase(), minutes, seconds);
 
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("audio/3gpp");
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
		// Initialize the body of the message with some text so that
		// it doesn't prompt on send
		emailIntent.putExtra(Intent.EXTRA_TEXT, "<3");
		emailIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file://" + path));
		startActivityForResult(emailIntent, EMAIL_SEND_RESULT);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (EMAIL_SEND_RESULT == requestCode) {
			// Email sending finished
			// TODO: figure out why the result code is always 0 (RESULT_CANCELED)
			if (RESULT_OK == resultCode) {
				// The user sent successfully
				//Toast.makeText( Hablame.this, "Email sent successfully", Toast.LENGTH_SHORT).show();
				return;
			} else {
				// They canceled sending
				//Toast.makeText( Hablame.this, "Email send canceled", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		//Toast.makeText( Hablame.this, "Unexpected activity result found", Toast.LENGTH_SHORT).show();
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