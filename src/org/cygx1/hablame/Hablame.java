package org.cygx1.hablame;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Hablame extends Activity implements View.OnClickListener {
	
	Button button;
	MediaRecorder recorder;
	AudioManager audioManager;
	
	static final int STATE_IDLE = 0;
	static final int STATE_RECORDING = 1;
	static final int STATE_SENDING = 2;
	static final int STATE_CONNECTING_BLUETOOTH = 2;
	int state = STATE_IDLE;
	
	private static final int PREFS_ID = 0;
	private static final int EMAIL_SEND_RESULT = 0;
	
	// To store the output file
	static File outputFile = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        BluetoothEnabledReceiver.setHablameActivity( this);
        
        recorder = new MediaRecorder();
	    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

	    button = (Button)findViewById(R.id.Button01);
        button.setOnClickListener(this);
    }
    
    void startRecording() {
		// create a File object for the parent directory
		File snapDirectory = new File("/sdcard/cygx1/hablame/");
		// have the object build the directory structure, if needed.
		snapDirectory.mkdirs();
		// create a File object for the output file
		outputFile = new File(snapDirectory, String.format(
				"h%d.3gp", System.currentTimeMillis()));

	    // could use setPreviewDisplay() to display a preview to suitable View here
	    
	    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	    //	Recording from bluetooth headset requires mono, 8kHz
	    recorder.setAudioChannels( 1);
	    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	    recorder.setOutputFile(outputFile.getAbsolutePath());

	    audioManager.setBluetoothScoOn(true);

	    try {
			recorder.prepare();
		    recorder.start();				
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		Toast.makeText( Hablame.this, "Recording started", Toast.LENGTH_SHORT).show();
		state = STATE_RECORDING;
	    button.setText("Stop recording");
	    button.setEnabled(true);
    }

	public void onClick(View v) {
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
								Hablame.this.startRecording();
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
			recorder.release();
			
			audioManager.stopBluetoothSco();

			Toast.makeText( Hablame.this, "Recording stopped", Toast.LENGTH_SHORT).show();
			state = STATE_IDLE;
		    button.setText("Start Recording");
		    
		    sendEmail();
		    // TODO: delete output file after sending
		    outputFile = null;
		}
	}
	
	public void sendEmail() {
		String path = outputFile.getAbsolutePath();		
		// Get my email address out of preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String recipient = prefs.getString(getString(R.string.recipientPref), null);
        String subject = prefs.getString(getString(R.string.subjectPref), null);
        
        // Format the subject using strftime escapes
        Time now = new Time();
        now.setToNow();
        subject = now.format(subject);
 
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("message/rfc822");
		emailIntent.putExtra(Intent.EXTRA_SUBJECT,
				subject);
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
		emailIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file://" + path));
		startActivityForResult(emailIntent, EMAIL_SEND_RESULT);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (EMAIL_SEND_RESULT == requestCode) {
			// Email sending finished
			// TODO: figure out why the result code is always 0 (RESULT_CANCELED)
			if (RESULT_OK == resultCode) {
				// The user sent successfully
				Toast.makeText( Hablame.this, "Email sent successfully", Toast.LENGTH_SHORT).show();
				return;
			} else {
				// They canceled sending
				Toast.makeText( Hablame.this, "Email send canceled", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		Toast.makeText( Hablame.this, "Unexpected activity result found", Toast.LENGTH_SHORT).show();
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