package org.cygx1.hablame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class BluetoothEnabledReceiver extends BroadcastReceiver {
	//	JAC: I wish I knew what was the right pattern for accessing 
	//	the Activity object instance from a Receiver. Meanwhile, I'm using
	//	a static setter
	
	static Hablame hablame;
	
	static void setHablameActivity( Hablame hablame) {
		BluetoothEnabledReceiver.hablame = hablame;
	}
	
	public void onReceive(Context context, Intent intent) {
		Object state = intent.getExtras().get( AudioManager.EXTRA_SCO_AUDIO_STATE);
		if( state != null && state.equals(AudioManager.SCO_AUDIO_STATE_CONNECTED)) {
			hablame.startRecording();
		}
	}
}