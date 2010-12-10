package org.cygx1.hablame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Hablame extends Activity {
	
	Button recordButton;
	Button stopButton;
	
	MediaRecorder recorder;
	AudioManager am;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        BluetoothEnabledReceiver.setHablameActivity( this);
        
        recorder = new MediaRecorder();
	    am = (AudioManager) getSystemService(AUDIO_SERVICE);

	    recordButton = (Button)findViewById(R.id.Record);
        stopButton = (Button)findViewById(R.id.Stop);
        
        recordButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//	Start connection to bluetooth headset. The rest
				//	will happen when we receive the notification that the
				//	connection succeeded
			    am.startBluetoothSco();
			}
		});
        
        stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				recorder.stop();
				recorder.release();
				
				am.stopBluetoothSco();

				Toast.makeText( Hablame.this, "Recording stopped", Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    void startRecording() {
		// create a File object for the parent directory
		File snapDirectory = new File("/sdcard/cygx1/hablame/");
		// have the object build the directory structure, if needed.
		snapDirectory.mkdirs();
		// create a File object for the output file
		final File outputFile = new File(snapDirectory, String.format(
				"h%d.3gp", System.currentTimeMillis()));

	    // could use setPreviewDisplay() to display a preview to suitable View here
	    
	    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	    //	Recording from bluetooth headset requires mono, 8kHz
	    //recorder.setAudioSamplingRate( 8*1024);
	    recorder.setAudioChannels( 1);
	    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	    recorder.setOutputFile(outputFile.getAbsolutePath());

	    //am.setMicrophoneMute(true);
	    am.setBluetoothScoOn(true);

	    try {
			recorder.prepare();
		    recorder.start();				
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Toast.makeText( Hablame.this, "Recording started", Toast.LENGTH_SHORT).show();
    }
}