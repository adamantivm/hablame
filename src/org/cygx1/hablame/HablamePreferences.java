package org.cygx1.hablame;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class HablamePreferences extends PreferenceActivity {
	   public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        addPreferencesFromResource(R.xml.hablameprefs);
	    }
}
