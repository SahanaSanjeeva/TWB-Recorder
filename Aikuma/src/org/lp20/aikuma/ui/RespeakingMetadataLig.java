package org.lp20.aikuma.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.model.MetadataSession;
import org.lp20.aikuma.model.RecordingLig;
import org.lp20.aikuma.model.WaveFile;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RespeakingMetadataLig extends AikumaActivity implements OnClickListener {
	
	public static final String TAG = "RespeakingMetadataLig";
	public static final String metabundle = "RespeakingMetadataBundle";
	public static final String msgTranslate = "In translation mode, the respeaking language should be different from the original language.";
	
	private Language recordLang;
	private Language motherTong;
	private ArrayList<Language> languages;
	private String regionOrigin;
	private String speakerName;
	private int speakerAge = 0;
	private String speakerGender;
	
	private String recordSourceId;
	private String recordOwnerId;
	private String versionName;
	private long recordSampleRate;
	private int rewindAmount;
	private String OrigDirName;
	private String origRecName;
	private String msgError;
	private boolean translateMode = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.respeaking_metadata);
	    origRecName = getIntent().getStringExtra(RecordActivityLig.intent_recordname);
	    OrigDirName = getIntent().getStringExtra("dirname");
	    translateMode = getIntent().getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);
	    JSONObject metaJSON;
	    try {
	    	metaJSON = FileIO.readJSONObject(new File(FileIO.getOwnerPath(), 
										RecordingLig.RECORDINGS + OrigDirName 
										+ "/" + origRecName + RecordingLig.METADATA_SUFFIX));
	    	recordSampleRate = (Long) metaJSON.get("sampleRate");
	    		
//	    	JSONObject metaJSON = FileIO.readJSONObject(new File(recordPath.split(".")[0]
//	    			+ RecordingLig.METADATA_SUFFIX));
	    	// fill in the recording details about languages
		    TextView tv = (TextView) findViewById(R.id.record_edit_recording_lang);
	    	String code = (String) metaJSON.get(RecordingMetadataLig.metaRecordLang);
		    tv.setText(new Language(Aikuma.getLanguageCodeMap().get(code), code).getName());
//	    	recordLang = new Language(Aikuma.getLanguageCodeMap().get(code), code);
		    tv = (TextView) findViewById(R.id.record_edit_mother_tongue);
	    	code = (String) metaJSON.get(RecordingMetadataLig.metaMotherTong);
		    tv.setText(new Language(Aikuma.getLanguageCodeMap().get(code), code).getName());
//	    	motherTong = new Language(Aikuma.getLanguageCodeMap().get(code), code);
	    	List<Language> languages= (List<Language>) Language.decodeJSONArray((JSONArray)metaJSON.get("languages"));
//	    	ArrayList<Language> languages = (ArrayList<Language>)metaJSON.get("languages");
		    tv = (TextView) findViewById(R.id.record_edit_extra_lang);
		    if (languages.size() > 1) {
			    StringBuilder s = new StringBuilder();
//			    for (Language l : languages.subList(0, languages.size()))
			    for (Language l : languages)
			    	s.append(l.getName() + ";");
			    tv.setText(s.toString());
		    } else {
		    	tv.setText("None");
		    }
		    // fill in the recording details about speaker
		    tv = (TextView) findViewById(R.id.record_edit_spkr_name);
		    tv.setText((String)metaJSON.get(RecordingMetadataLig.metaSpkrName));
		    tv = (TextView) findViewById(R.id.record_edit_spkr_age);
//		    Integer age = (Integer)metaJSON.get(RecordingMetadataLig.metaSpkrAge);
		    tv.setText(Long.toString((Long)metaJSON.get(RecordingMetadataLig.metaSpkrAge)));
		    tv = (TextView) findViewById(R.id.record_edit_spkr_gender);
		    tv.setText((String)metaJSON.get(RecordingMetadataLig.metaSpkrGender));
		    tv = (TextView) findViewById(R.id.record_edit_spkr_region_orig);
		    tv.setText((String)metaJSON.get(RecordingMetadataLig.metaOrigin));
	    } catch (IOException e) {
	    	Toast.makeText(RespeakingMetadataLig.this, 
	    			"Error: failed to load the recording. Please try again.", 
	    			Toast.LENGTH_LONG)
	    			.show();
	    	startActivity(new Intent(RespeakingMetadataLig.this, RespeakingSelection.class)
	    						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	    	RespeakingMetadataLig.this.finish();
	    	Log.e(TAG, "Exception caught when reading json metadata file - " + e);
	    }
	    
	    loadSession();
	    retrieveRecordIntent();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK && requestCode == RecordingMetadataLig.SELECT_LANG_ACT) {
			Language language = intent.getParcelableExtra("language");
			Log.i(TAG, "textview id: " + intent.getExtras().getInt("textview_id"));
//			Log.i(TAG, "DEBUG - language: " + language.getCode() + ": " + language.getName());
			TextView tv = (TextView) findViewById(intent.getIntExtra("textview_id", -1));
			if (tv != null) {
				tv.setText(language.getName());
			} else {
				Toast.makeText(this, "Failed to retrieve language from selection", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void onAddMoreLanguagesField(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages);
		int index = l_parent.getChildCount()-1; 
		LinearLayout l_language = (LinearLayout) l_parent.getChildAt(index);
		LinearLayout ll_more = new LinearLayout(this);
		ll_more.setId(l_language.getId() + 1000);
		ll_more.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		//retrieval of the style attributes
		int[] attrs = {android.R.attr.textSize, android.R.attr.textColor};
		
		TextView tv = (TextView) findViewById(R.id.respeak_tv_secondlanguage);
		TextView tv_otherlang = new TextView(this);
		tv_otherlang.setText("Other language");
		tv_otherlang.setWidth(tv.getWidth());
		tv_otherlang.setId(ll_more.getId() + 1001);
		ll_more.addView(tv_otherlang);
		tv_otherlang.setLayoutParams(new LinearLayout.LayoutParams(tv.getLayoutParams()));
		TypedArray ta = obtainStyledAttributes(R.style.respeak_TextviewMetadata2, attrs);
		String tsize = ta.getString(0);
		Log.i("Retrieved text:", tsize);
		float textSize = Float.parseFloat(tsize.split("sp")[0]);
		tv_otherlang.setTextSize(textSize); 
		int tcolor = ta.getColor(1, Color.BLACK);
		String getcolorcode = Integer.toHexString(tcolor).substring(2);
		String color = "#"+getcolorcode;
		tv_otherlang.setTextColor(Color.parseColor(color));
		Log.i("set textColor in hexacode is:", color);
		ta.recycle();
		tv_otherlang.setTypeface(null, Typeface.BOLD);
		//Log.d(this.toString(), "" + l_language.getId());
				
		TextView tv_selected_language = new TextView(l_language.getContext());
//		tv_selected_language.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		tv_selected_language.setLayoutParams(((TextView)findViewById(R.id.respeak_tv_selectedsecondlanguage)).getLayoutParams());
		//tv_selected_language.setEms(10);
		//tv_selected_language.setTextSize(14);
		TypedArray ta_tvSelectedLang = obtainStyledAttributes(R.style.respeak_TextViewSelectedLang, attrs);
		String textSizetvSelectedLang = ta_tvSelectedLang.getString(0);
		float txtSzetv = Float.parseFloat(textSizetvSelectedLang.split("sp")[0]);
		tv_selected_language.setTextSize(txtSzetv); //set textSize of the TextView
		ta_tvSelectedLang.recycle();
		tv_selected_language.setId(ll_more.getId() + 1010);
		ll_more.addView(tv_selected_language);
		
		Button btn = (Button)findViewById(R.id.respeak_btn_secondlanguage);
		Button btn_otherlang = new Button(this);
		btn_otherlang.setLayoutParams(btn.getLayoutParams());
		btn_otherlang.setText(getResources().getString(R.string.select_from_list));
		//btn_otherlang.setTextSize(14);
		TypedArray ta_btnChooseLang = obtainStyledAttributes(R.style.ButtonChooseLanguage, attrs);
		String btn_textsize = ta_btnChooseLang.getString(0);
		Log.i("Retrieved 'Select language' button TextSize:", btn_textsize);
		float buttontextSize = Float.parseFloat(btn_textsize.split("sp")[0]);
		btn_otherlang.setTextSize(buttontextSize); //set textSize of the button
		ta_btnChooseLang.recycle();
		btn_otherlang.setId(l_language.getId() + 1100);
		btn_otherlang.setOnClickListener(this);
		ll_more.addView(btn_otherlang);
		
		l_parent.addView(ll_more);
	}
	
	/**
	 * Called when a user press the "Less Languages" button
	 * Update the view with a suppression of the last added language
	 * 
	 * @param _view
	 */
	public void onDelLanguagesField(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages); //Layout parent
		int index = l_parent.getChildCount()-1; 
		if (index<3) {
			Toast.makeText(this, "You can't delete the language of the respeaking, the mother tongue or the second language of the speaker.",Toast.LENGTH_LONG).show();	
		} else {
			l_parent.removeViewAt(index);
		}
	}
	
	public void onPickLanguage(View _view) {
		Intent intent = new Intent(this, LanguageFilterListLIG.class);
		LinearLayout ll = (LinearLayout) _view.getParent();
		int tv_id = ll.getChildAt(1).getId();
		intent.putExtra("textview_id", tv_id);
		Log.i(TAG, "textview_id: " + tv_id);
		startActivityForResult(intent, RecordingMetadataLig.SELECT_LANG_ACT);	// org.lp20.aikuma.ui.AddSpeakerActivity2.SELECT_LANGUAGE = 0
	}
	
	
	public void onOkButtonClick(View _view) {
//		Log.i(TAG, "DEBUG - selected languages before validation: " + selectedLanguages.size());
		// get spoken languages
		recordLang = retrieveRecordLang();
		motherTong = retrieveMotherTong();
		languages = retrieveLanguages();
		// get personal information
		regionOrigin = ((EditText) findViewById(R.id.respeak_edit_spkr_region)).getText().toString();
		speakerName = ((EditText) findViewById(R.id.respeak_edit_spkr_name)).getText().toString();
		String age = ((EditText) findViewById(R.id.respeak_edit_spkr_age)).getText().toString();
		try {
			//speakerAge = new Integer(age);
			speakerAge = Integer.parseInt(age);
			if (speakerAge < 3 || speakerAge > 151) {
				msgError = "Input age is implausible... please correct it.";
				new AlertDialog.Builder(this)
				.setMessage(msgError)
				.setPositiveButton("Ok", null)
				.show();
				return;
			}
		} catch (NumberFormatException e) {
			new AlertDialog.Builder(this)
				.setMessage("Warning: age is incorrect")
				.setPositiveButton("Ok", null)
				.show();
			return;
		}
		RadioGroup rg = ((RadioGroup) findViewById(R.id.respeak_edit_gender));
		if (rg.getCheckedRadioButtonId() != -1) {
			RadioButton rbtn = ((RadioButton)findViewById(rg.getCheckedRadioButtonId()));
			speakerGender = rbtn.getText().toString().compareTo(getString(R.string.radio_male)) == 0 ? 
					"Male" : "Female";
		}
		
		String message;
		String respeak_lang = ((TextView) findViewById(R.id.respeak_tv_selectedlangRespeaking)).getText().toString();
		/*if (languages.isEmpty() || regionOrigin.isEmpty() || speakerName .isEmpty()
				|| speakerAge == 0 || speakerGender.isEmpty()) {
			message = RecordingMetadataLig.missFieldsMsg;
		} else { message = RecordingMetadataLig.validateMsg; }*/
		
		if (respeak_lang.isEmpty() && speakerName .isEmpty()) {
			msgError = "Some fields remain empty, please fill it.";
			new AlertDialog.Builder(this)
			.setMessage(msgError)
			.setPositiveButton("Ok", null)
			.show();
			return;
		} else if (languages.isEmpty() || regionOrigin.isEmpty()) {
					message = RecordingMetadataLig.missFieldsMsg;
		} else if (translateMode 
				&& recordLang.getName().compareTo(((TextView) findViewById(R.id.record_edit_recording_lang)).getText().toString()) == 0) {
			message = msgTranslate;
		} else { message = RecordingMetadataLig.validateMsg; }
		
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// update session
				MetadataSession session = MetadataSession.getMetadataSession();
				int gender = speakerGender.compareTo("Male") == 0 ? RecordingMetadataLig.GENDER_MALE 
						: RecordingMetadataLig.GENDER_FEMALE;
				session.setSession(recordLang, motherTong, languages, regionOrigin, 
						speakerName, speakerAge, gender);

				Log.i(TAG, "languages: " + recordLang + "; " + motherTong + "; " + languages.toString());
				Log.i(TAG, "origin: " + regionOrigin );
				Log.i(TAG, "speaker name: " + speakerName);
				Log.i(TAG, "speaker age" + speakerAge);
				Log.i(TAG, "speaker gender" + speakerGender);
				
				Date date = new Date();
				DateFormat dateformat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
				String metaDate = dateformat.format(date);
				
				Bundle metadataBundle = new Bundle();
				metadataBundle.putParcelable(RecordingMetadataLig.metaRecordLang, recordLang);
				metadataBundle.putParcelable(RecordingMetadataLig.metaMotherTong, motherTong);
				metadataBundle.putParcelableArrayList(RecordingMetadataLig.metaLanguages, languages);
				metadataBundle.putString(RecordingMetadataLig.metaOrigin, regionOrigin);
				metadataBundle.putString(RecordingMetadataLig.metaSpkrName, speakerName);
				metadataBundle.putInt(RecordingMetadataLig.metaSpkrAge, speakerAge);
				metadataBundle.putString(RecordingMetadataLig.metaSpkrGender, speakerGender);
				
				Intent intent = new Intent(RespeakingMetadataLig.this, ThumbRespeakActivityLig.class);
				intent.putExtra(metabundle, metadataBundle);
				// add metadata from the original recording
//				intent.putExtra(RecordActivityLig.intent_ownerId, recordOwnerId);
				intent.putExtra(RecordActivityLig.intent_recordname, origRecName);
				intent.putExtra("dirname", OrigDirName);
				intent.putExtra(RecordActivityLig.intent_rewindAmount, rewindAmount);
				intent.putExtra(RecordActivityLig.intent_sampleRate, recordSampleRate);
				intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
//				intent.putExtra(RecordActivityLig.intent_sourceId, recordSourceId);
//				intent.putExtra(RecordActivityLig.intent_versionName, versionName);
//				intent.putExtra(RespeakingSelection.RESPEAK, respeak);
				startActivity(intent);
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
		
	}
	
	private Language getLanguageFromName(String lang) {
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();
		for (String lang_code : language_code_map.keySet()) {
			if (language_code_map.get(lang_code).compareTo(lang) == 0) {
//				Log.i(TAG, "DEBUG - code: " + lang_code + "; name: " + language_code_map.get(lang_code)
//						+ "; extracted language name: " + lang_name);
				return new Language(lang, lang_code);
			}
		}
		return null;
	}
	
	private Language retrieveRecordLang() {
		String lang_name = ((TextView)findViewById(R.id.respeak_tv_selectedlangRespeaking)).getText().toString();
		return getLanguageFromName(lang_name);
	}
	
	private Language retrieveMotherTong() {
		String lang_name = ((TextView)findViewById(R.id.respeak_tv_selectedmotherTongue)).getText().toString();
		return getLanguageFromName(lang_name);
	}
	
	private ArrayList<Language> retrieveLanguages() {
		ArrayList<Language> selectedLanguages = new ArrayList<Language>();
		LinearLayout l_languages = (LinearLayout) findViewById(R.id.respeak_layout_languages);
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();
//		Log.i(TAG, "DEBUG - language code map keyset: " + language_code_map.keySet().toString());
		for (int i=2; i<l_languages.getChildCount(); i++) {
			LinearLayout l_lang = (LinearLayout) l_languages.getChildAt(i);
			Log.i(TAG, "layout id: " + l_lang.getId());
			String lang_name = ((TextView) l_lang.getChildAt(1)).getText().toString();
			if (!lang_name.isEmpty()) {
				Log.i(TAG, "Language: " + lang_name);
				if (language_code_map.containsValue(lang_name))
					selectedLanguages.add(getLanguageFromName(lang_name));
//					for (String lang_code : language_code_map.keySet()) {
//						if (language_code_map.get(lang_code).compareTo(lang_name) == 0) {
//							Log.i(TAG, "DEBUG - code: " + lang_code + "; name: " + language_code_map.get(lang_code)
//									+ "; extracted language name: " + lang_name);
//							selectedLanguages.add(new Language(lang_name, lang_code));
//							break;
//						}
//					}
				else {
					selectedLanguages.add(new Language(lang_name, lang_name.substring(0, 3)));
					Log.i(TAG, "DEBUG - 'else if map does not contain value'; selected languages size: " + selectedLanguages.size());
				}
			}
			Log.i(TAG, "DEBUG - selected languages: " + selectedLanguages.size());
		}
		return selectedLanguages;
	}

	@Override
	public void onClick(View v) {
		onPickLanguage(v);
	}
	
	public void onBackPressed(View v) {
		this.finish();
	}
	
	private void retrieveRecordIntent() {
//		recordSampleRate = getIntent().getLongExtra(RecordActivityLig.intent_sampleRate, 16000);
		rewindAmount = getIntent().getIntExtra(RecordActivityLig.intent_rewindAmount, 500);
//		origRecName = getIntent().getStringExtra(RecordActivityLig.intent_recordname);
//		recordSourceId = getIntent().getStringExtra(RecordActivityLig.intent_sourceId);
//		recordOwnerId = getIntent().getStringExtra(RecordActivityLig.intent_ownerId);
//		versionName = getIntent().getStringExtra(RecordActivityLig.intent_versionName);
	}
	
	private void loadSession() {
		MetadataSession session = MetadataSession.getMetadataSession();
		if (!session.isEmpty()) {
			Language recordLanguage = session.getRecordLanguage();
			Language motherTongue = session.getMotherTongue();
			ArrayList<Language> extraLanguages = session.getExtraLanguages();
			String region = session.getRegionOrigin();
			int age = session.getSpeakerAge();
			int gender = session.getSpeakerGender();
			String name = session.getSpeakerName();
			Log.i(TAG,"load session: " + recordLanguage + "; " + motherTongue + "; " + extraLanguages);
			Log.i(TAG,"load session: " + region);
			Log.i(TAG,"load session: " + age);
			Log.i(TAG,"load session: " + gender);
			Log.i(TAG,"load session: " + name);

			LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages);
//			int index = l_parent.getChildCount() - 2;
			Log.i(TAG, "# languages: " + (extraLanguages.size()+2) + "; # layouts: " + l_parent.getChildCount());
			if (extraLanguages.size() > l_parent.getChildCount()-2) {
				for (int i=l_parent.getChildCount()-2; i<extraLanguages.size(); i++) {
					onAddMoreLanguagesField(null);
				}
			}
			((TextView) findViewById(R.id.respeak_tv_selectedlangRespeaking)).setText(recordLanguage.getName());
			((TextView) findViewById(R.id.respeak_tv_selectedmotherTongue)).setText(motherTongue.getName());
			for (int i=0; i<extraLanguages.size(); i++) {
				((TextView) ((LinearLayout) (l_parent.getChildAt(i+2))).getChildAt(1)).setText(extraLanguages.get(i).getName());
			}			
			((EditText) findViewById(R.id.respeak_edit_spkr_region)).setText(region);
			((EditText) findViewById(R.id.respeak_edit_spkr_name)).setText(name);
			((EditText) findViewById(R.id.respeak_edit_spkr_age)).setText("" + age);
			if (gender == RecordingMetadataLig.GENDER_MALE)
				((RadioButton) findViewById(R.id.radio_gender_male)).setChecked(true);
			else if (gender == RecordingMetadataLig.GENDER_FEMALE)
				((RadioButton) findViewById(R.id.radio_gender_female)).setChecked(true);
		}
	}

}
