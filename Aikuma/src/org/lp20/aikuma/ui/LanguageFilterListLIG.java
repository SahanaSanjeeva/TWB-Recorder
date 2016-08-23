package org.lp20.aikuma.ui;

import org.lp20.aikuma.model.Language;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class LanguageFilterListLIG extends LanguageFilterList {
	
	private int textview_id;
	private final String textview_str = "textview_id";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Intent intent = getIntent();
	    textview_id = intent.getIntExtra(textview_str, -1);	
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent();
		intent.putExtra(textview_str, textview_id);
		intent.putExtra("language", (Language)l.getItemAtPosition(position));
		setResult(RESULT_OK, intent);
		this.finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
