package org.lp20.aikuma.ui;

import org.lp20.aikuma2.R;

import android.os.Bundle;
import android.view.View;

public class TranslationSelection extends AikumaActivity {
	
	public static final String TAG = "TranslationSelection";

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.translation_selection);
	}
	
	public void onBackPressed(View v) {
		this.finish();
	}
	
}
