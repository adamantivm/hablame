package org.cygx1.hablame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

public class HablameRecording extends Activity implements View.OnClickListener {

	Button stopRecording, backToPlayback;
	Chronometer clock;
	TextView recordingSource;
	ProgressBar amplitude;
	Timer ampTimer;
	
	MediaRecorder recorder;
	MediaPlayer player;
	AudioManager audioManager;
	
	static final int STATE_IDLE = 0;
	static final int STATE_RECORDING = 1;
	static final int STATE_SENDING = 2;
	static final int STATE_CONNECTING_BLUETOOTH = 2;
	int state = STATE_IDLE;
	
	private static final int EMAIL_SEND_RESULT = 0;
	
	// To store the output file
	private String fileNameBase = null;
	private int sequenceNumber = -1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.record);
        
        clock = (Chronometer)findViewById(R.id.Chronometer01);
		recordingSource = (TextView)findViewById(R.id.RecordingSource);
		stopRecording = (Button)findViewById(R.id.StopRecording);
		stopRecording.setOnClickListener(this);
		backToPlayback = (Button)findViewById(R.id.ToPlayback);
		backToPlayback.setOnClickListener(this);
		amplitude = (ProgressBar)findViewById(R.id.RecordingAmplitude);
		amplitude.setProgress(50);

		BluetoothEnabledReceiver.setHablameRecordingActivity( this);
        
        recorder = new MediaRecorder();
        player = new MediaPlayer();
	    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }
	protected void onDestroy() {
		//	TODO: Trap possible accidental "back" button and ask for confirmation of deleted recording?
		super.onDestroy();
    	// TL: attempt to prevent some force closes by cleaning up after
    	// ourselves. Shut down the recorder
    	recorder.release();
	}	
	
	protected void onResume() {
		Log.d(getClass().getName(),"onResume");
		super.onResume();
	}
	protected void onNewIntent( Intent newIntent) {
		super.onNewIntent(newIntent);
		setIntent(newIntent);
	}

	/**
	 * This is called whenever the application is called. Either for the first time
	 * or during subsequent times
	 */
	protected void onStart() {
		super.onStart();
	    launchRecording();
	}
    public void onStop() {
    	super.onStop();
    }

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
    
    /**
     * Called when the bluetooth connection is already resolved
     * to actually start the recording process
     * 
     * @param withBluetooth	use bluetooth or not (internal mic)
     */
    void startRecording( boolean withBluetooth) {
		// create a File object for the parent directory
		File snapDirectory = new File("/sdcard/cygx1/hablame/");
		// have the object build the directory structure, if needed.
		snapDirectory.mkdirs();
		
		
		if( getIntent().getAction() == Hablame.ACTION_RECORDING_START || sequenceNumber == -1) {
			fileNameBase = snapDirectory + File.separator + String.format("h%d", System.currentTimeMillis());
		}

		// create a File object for the output file
		String path = fileNameBase + "-" + (++sequenceNumber) + ".3gp";
		Log.d(getClass().getName(), "File name = " + path);

		// Update informational widgets
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
	    recorder.setOutputFile( path);

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
    }
    
    /**
     * We want to start recording. This will start the bluetooth connection, etc.
     */
	private void launchRecording() {
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
							HablameRecording.this.startRecording( false);
						}
					});
				}
			}
		}, 3000);

	    state = STATE_CONNECTING_BLUETOOTH;
	    recordingSource.setText("[Connecting to bluetooth]");
	}

	public void onClick(View v) {
		Log.d("Hablame", "In onClick, state is " + state);		

		if( v == stopRecording) {
			recorder.stop();
			//	Logic would indicate reset here. But even if you do, the underlying native code
			//	will fail to reset because it didn't finish stopping, despite having returned from the stop() call
			audioManager.setBluetoothScoOn(false);
			audioManager.stopBluetoothSco();

			// Stop UI displays
			clock.stop();
			amplitudeHandler.removeCallbacks(updateRecordAmplitudeTask);

			Toast.makeText( HablameRecording.this, "Recording stopped", Toast.LENGTH_SHORT).show();
			state = STATE_IDLE;

		    sendEmail();
		    // TODO: delete output file after sending
		    sequenceNumber = -1;
		    fileNameBase = null;

		} else if(v == backToPlayback) {
			//	TODO: Leave everything ready for a resume
			recorder.stop();

			Intent iPlayback = new Intent( this, HablamePlayback.class);
			iPlayback.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity( iPlayback);
			
		// } else if(v == review) {
			//  TODO: Implemente codepath for reviewing the recording
		}
	}

	public void sendEmail() {
		if( sequenceNumber == -1) {
			Log.w(getClass().getName(), "sendEmail called with no saved messages? sequenceNumber == -1");
			return;
		}
		
		// Get my email address out of preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String recipient = prefs.getString(getString(R.string.recipientPref), null);

		final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		if (null != recipient) {
			emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
		}
		emailIntent.setType("audio/3gpp");
		// Initialize the body of the message with some text so that
		// it doesn't prompt on send
		emailIntent.putExtra(Intent.EXTRA_TEXT, "<3");
        
		//	Peek file duration
        int duration = 0;
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for(int i=0;i<=sequenceNumber;i++) {
    		String path = fileNameBase + "-" + i + ".3gp";
			try {
				player.setDataSource(path);
				player.prepare();
				duration += player.getDuration();
				player.reset();
			} catch (IOException e) {
				Log.e( this.getClass().getName(), "Error getting recording duration", e);
			}
			uris.add(Uri.parse( "file://" + path));
        }
		int seconds = duration / 1000;
		int minutes = seconds / 60;
		seconds -= minutes*60;

		/* JAC: Format with recording duration */
		String prefix;
		if (null == recipient) {
			prefix = "?";
		} else {
			prefix = recipient.substring(0, 1).toUpperCase(); 
		}
		String subject = String.format("%s%d:%02d", prefix, minutes, seconds);
 		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris);
		startActivityForResult(emailIntent, EMAIL_SEND_RESULT);
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (EMAIL_SEND_RESULT == requestCode) {
			// Email sending finished
			// TODO: figure out why the result code is always 0 (RESULT_CANCELED)
			if (RESULT_OK == resultCode) {
				// The user sent successfully
				//Toast.makeText( Hablame.this, "Email sent successfully", Toast.LENGTH_SHORT).show();
			} else {
				// They canceled sending
				//Toast.makeText( Hablame.this, "Email send canceled", Toast.LENGTH_SHORT).show();
			}
			
			//	File sent, go back to where we were before
			finish();
		}
		//Toast.makeText( Hablame.this, "Unexpected activity result found", Toast.LENGTH_SHORT).show();
	}
}
