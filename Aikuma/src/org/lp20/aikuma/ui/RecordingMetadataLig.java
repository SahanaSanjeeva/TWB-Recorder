package org.lp20.aikuma.ui;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.Attributes;

import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.model.MetadataSession;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.RecordingLig;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma2.R;
import org.lp20.aikuma2.R.id;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RecordingMetadataLig extends AikumaActivity implements OnClickListener {
	
	private static final String TAG = "RecordingMetadataLig";
	public static final int GENDER_MALE = 1;
	public static final int GENDER_FEMALE = 2;
	public static final int SELECT_LANG_ACT = 0;
	public static final String missFieldsMsg = "Some fields remain empty, do you want to continue?";
	public static final String validateMsg = "You are about to start recording. Do you want to continue?";
	
	
	public static final String metaLanguages = "languages";
	public static final String metaRecordLang = "recording_lang";
	public static final String metaMotherTong = "mother_tongue";
	public static final String metaOrigin = "origin";
	public static final String metaSpkrName = "speaker_name";
	public static final String metaSpkrAge = "speaker_age";
	public static final String metaSpkrGender = "speaker_gender";
	public static final String metaDate = "date";
	public static final String metaBundle = "metadataBundle";
	
	private boolean respeak = false;
	private boolean elicitation = false;
	private UUID importWavUUID;
	private int importWavRate;
	private int importWavDur;
	private String importWavFormat;
	private int importWavChannels;
	private int importBitsSample;
	private ListenFragment listenFragment;
	
	private Language recordLang;
	private Language motherTongue;
	private ArrayList<Language> selectedLanguages;
	private String regionOrigin;
	private String speakerName;
	private String age;
	private String msgError;
	private int speakerAge=0;
	private int speakerGender=0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.recording_metadata_lig);
//	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
	    respeak = getIntent().getBooleanExtra(RespeakingSelection.RESPEAK, false);
	    elicitation = getIntent().getBooleanExtra(ElicitationMode.ELICITATION, false);
	    if (respeak) {
	    	Toast.makeText(this, "Please fill in the details of the imported recording", 
	    			Toast.LENGTH_LONG)
	    			.show();
	    	// TODO add a frame layout for the listener
			TableLayout l_parent = (TableLayout) findViewById(R.id.title_layout);
//			LinearLayout l_parent = ((LinearLayout) findViewById(R.id.layout_spokenLanguages).getParent());
			FrameLayout playerLayout = new FrameLayout(this);
			playerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			playerLayout.setId(5555);
			l_parent.addView(playerLayout);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			listenFragment = new ListenFragment();
			ft.replace(5555, listenFragment);
			ft.commit();
			importWavUUID = UUID.fromString(getIntent().getStringExtra("tempRecUUID"));
			importWavRate = getIntent().getIntExtra("sampleRate", 16000);
			importWavDur = getIntent().getIntExtra("duration", 0);
			importWavFormat = getIntent().getStringExtra("format");
			importWavChannels = getIntent().getIntExtra("numChannels", 1);
			importBitsSample = getIntent().getIntExtra("bitspersample", 0);
	    } else if (elicitation) {
	    	Toast.makeText(this, "Please fill in details about the speaker", 
	    			Toast.LENGTH_LONG)
	    			.show();	    	
	    }
	    loadSession();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (respeak) {
			try {
				File recordingFile = new File(Recording.getNoSyncRecordingsPath(), 
						importWavUUID + ".wav");
				listenFragment.setPlayer(new SimplePlayer(
						recordingFile,
						importWavRate, true));
			} catch (IOException e) {
				//The SimplePlayer cannot be constructed, so let's end the
				//activity.
				Toast.makeText(this, "There has been an error in the creation of the audio file which prevents it from being read.", Toast.LENGTH_LONG).show();
				RecordingMetadataLig.this.finish();
			}
		}
	}
	
	/**
	 * Load the session if it exists
	 * 
	 */
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

			LinearLayout l_parent = (LinearLayout) findViewById(R.id.layout_languages);
//			int index = l_parent.getChildCount() - 2;
			Log.i(TAG, "# languages: " + (extraLanguages.size()+2) + "; # layouts: " + l_parent.getChildCount());
			if (extraLanguages.size() > l_parent.getChildCount()-2) {
				for (int i=l_parent.getChildCount()-2; i<extraLanguages.size(); i++) {
					onClickMoreLanguages(null);
				}
			}
			((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).setText(recordLanguage.getName());
			((TextView) findViewById(R.id.tv_selectedMotherTongue)).setText(motherTongue.getName());
			for (int i=0; i<extraLanguages.size(); i++) {
				((TextView) ((RelativeLayout) (l_parent.getChildAt(i+2))).getChildAt(1)).setText(extraLanguages.get(i).getName());
			}			
			((EditText) findViewById(R.id.edit_region_origin)).setText(region);
			((EditText) findViewById(R.id.edit_speaker_name)).setText(name);
			((EditText) findViewById(R.id.edit_speaker_age)).setText("" + age);
			if (gender == RecordingMetadataLig.GENDER_MALE)
				((RadioButton) findViewById(R.id.radio_gender_male)).setChecked(true);
			else if (gender == RecordingMetadataLig.GENDER_FEMALE)
				((RadioButton) findViewById(R.id.radio_gender_female)).setChecked(true);
		}
	}
	
	/**
	 * Called when a user press the "Select from list" button
	 * 
	 * @param _view
	 */
	public void onAddISOLanguageButton(View _view) {
		Intent intent = new Intent(this, LanguageFilterListLIG.class);
		RelativeLayout rl = (RelativeLayout) _view.getParent();
		int tv_id = rl.getChildAt(1).getId();
		intent.putExtra("textview_id", tv_id);
		Log.i("RecordingMetadataLig", "textview_id: " + tv_id);
		startActivityForResult(intent, SELECT_LANG_ACT);	// org.lp20.aikuma.ui.AddSpeakerActivity2.SELECT_LANGUAGE = 0
	}
	
	/**
	 * Called when a user press the "More Languages" button
	 * Update the view with a new field for inserting a new language
	 * 
	 * @param _view
	 */
	public void onClickMoreLanguages(View _view) {
		
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.layout_languages); //retrieval of the parent LinearLayout
		int index = l_parent.getChildCount()-1; //retrieval of the last LinearLayout position
		RelativeLayout l_language = (RelativeLayout) l_parent.getChildAt(index); //retrieval of the last LinearLayout
		RelativeLayout rl_more = new RelativeLayout(this);
		rl_more.setId(l_language.getId() + 1000);
		rl_more.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); 
		
		//retrieval of the style attributes
		int[] attrs = {android.R.attr.textSize, android.R.attr.textColor};
				
		//TextView Other Language
		TextView tv_sdLanguage = (TextView) findViewById(R.id.tv_secondLanguage); //retrieval of the last TextView
		TextView tv_otherLanguage = new TextView(this);
		tv_otherLanguage.setText("Other language");
		tv_otherLanguage.setWidth(tv_sdLanguage.getWidth());
		Log.i("set width is:", Integer.toString(tv_sdLanguage.getWidth()));
		rl_more.addView(tv_otherLanguage);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tv_otherLanguage.getLayoutParams();
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		tv_otherLanguage.setLayoutParams(params);
		//parsing style, using Context.obtainStyledAttributes()
		TypedArray ta = obtainStyledAttributes(R.style.TextviewMetadata2, attrs);
		String tsize = ta.getString(0); //fetching textSize from the style
		Log.i("Retrieved text:", tsize);
		float textSize = Float.parseFloat(tsize.split("sp")[0]);
		tv_otherLanguage.setTextSize(textSize); //set textSize
		//Log.i("set textSize is:", Float.toString(textSize));
		int tcolor = ta.getColor(1, Color.BLACK); //fetching color
		//Log.i("Retrieved textColor as hex:", Integer.toHexString(tcolor));
		String getcolorcode = Integer.toHexString(tcolor).substring(2);
		String color = "#"+getcolorcode;
		tv_otherLanguage.setTextColor(Color.parseColor(color)); //set textColor
		Log.i("set textColor in hexacode is:", color);
		//recycle the TypedArray
		ta.recycle();
		tv_otherLanguage.setTypeface(null, Typeface.BOLD);
		//Log.d(this.toString(), "" + l_language.getId());
		tv_otherLanguage.setId(rl_more.getId() + 1);
//		rl_more.addView(tv_otherLanguage);
				
		/* Old version : when user add a language by himself
		EditText et_sdLanguage = (EditText) findViewById(R.id.et_secondLanguage); //retrieval of the last EditText
		EditText et_otherLanguage = new EditText(l_parent.getContext());
		et_otherLanguage.setLayoutParams(new LayoutParams(et_sdLanguage.getWidth(), LayoutParams.WRAP_CONTENT));
		et_otherLanguage.setTextSize(12);
		et_otherLanguage.setHint(R.string.add_custom_language_or);
		et_otherLanguage.setId(ll_more.getId() + 10);
		ll_more.addView(et_otherLanguage);
		*/
		
		TextView tv_selectedSdLanguage = (TextView) findViewById(R.id.tv_selectedSecondLanguage);
		TextView tv_selectedOtherLanguage = new TextView(l_parent.getContext());
		RelativeLayout.LayoutParams tv_selected_params = (RelativeLayout.LayoutParams) new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tv_selected_params.addRule(RelativeLayout.RIGHT_OF, tv_otherLanguage.getId());
		tv_selectedOtherLanguage.setLayoutParams(tv_selected_params);
		//ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tv_selectedSdLanguage.getLayoutParams();
		//tv_selectedOtherLanguage.setGravity(Gravity.CENTER|Gravity.BOTTOM);
		//tv_selectedOtherLanguage.setPadding(20, 0, 20, 0);
		//setTextSize of the TextView (tv_selectedOtherLanguage.setTextSize(20))
		TypedArray ta_tvSelectedLang = obtainStyledAttributes(R.style.TextviewSelectedLanguage, attrs);
		String textSizetvSelectedLang = ta_tvSelectedLang.getString(0);
		float txtSzetv = Float.parseFloat(textSizetvSelectedLang.split("sp")[0]);
		tv_selectedOtherLanguage.setTextSize(txtSzetv); //set textSize of the TextView
		ta_tvSelectedLang.recycle();
		tv_selectedOtherLanguage.setId(rl_more.getId() + 10);		
		rl_more.addView(tv_selectedOtherLanguage);
		
		//Button add other language
		Button btn_otherLanguage = new Button(this);
		btn_otherLanguage.setText(getResources().getString(R.string.select_from_list));
		//retrieval of the button text size
		//btn_otherLanguage.setTextSize(18);
		TypedArray ta_btnChooseLang = obtainStyledAttributes(R.style.ButtonChooseLanguage, attrs);
		String btn_textsize = ta_btnChooseLang.getString(0);
		Log.i("Retrieved 'Select language' button TextSize:", btn_textsize);
		float buttontextSize = Float.parseFloat(btn_textsize.split("sp")[0]);
		btn_otherLanguage.setTextSize(buttontextSize); //set textSize of the button
		ta_btnChooseLang.recycle();
		Button btn_selectLanguageFromList = (Button) findViewById(R.id.btn_chooseLanguage);
		RelativeLayout.LayoutParams button_params = (RelativeLayout.LayoutParams)btn_selectLanguageFromList.getLayoutParams();
		button_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		btn_otherLanguage.setLayoutParams(button_params);
		btn_otherLanguage.setId(rl_more.getId() + 100);
		btn_otherLanguage.setOnClickListener(this);
		rl_more.addView(btn_otherLanguage);
								
		l_parent.addView(rl_more);
		
		
	}
	
	/**
	 * Called when a user press the "Less Languages" button
	 * Update the view with a suppression of the last added language
	 * 
	 * @param _view
	 */
	public void onClickLessLanguages(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.layout_languages); //Layout parent
		int index = l_parent.getChildCount()-1; 
		if (index<3) {
			Toast.makeText(this, "You can't delete the language of the recording, the mother tongue or the second language of the speaker.",Toast.LENGTH_LONG).show();	
		} else {
			l_parent.removeViewAt(index);
		}
	}
	
	/**
	 * Called when a user press the Ok button
	 * 
	 * @param _view
	 */
	
	public void onOkButtonPressed(View _view) {
		selectedLanguages = new ArrayList<Language>();
		Log.i(TAG, "DEBUG - selected languages before validation: " + selectedLanguages.size());
		Log.i(TAG, "DEBUG - selected languages before validation: " + selectedLanguages);
		// get spoken languages
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();
//		Log.i(TAG, "DEBUG - language code map keyset: " + language_code_map.keySet().toString());
		LinearLayout l_languages = (LinearLayout) findViewById(R.id.layout_languages);
		
		String lang = ((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).getText().toString();
		if (language_code_map.containsValue(lang))
			recordLang = getLanguageFromName(lang, language_code_map);
//			for (String lang_code : language_code_map.keySet()) {
//				if (language_code_map.get(lang_code).compareTo(lang) == 0) {
//					Log.i(TAG, "DEBUG - code: " + lang_code + "; name: " + language_code_map.get(lang_code)
//							+ "; extracted language name: " + lang);
//					recordLang = new Language(lang, lang_code);
//					break;
//				}
//			}
		
		lang = ((TextView) findViewById(R.id.tv_selectedMotherTongue)).getText().toString();
		if (language_code_map.containsValue(lang))
			motherTongue = getLanguageFromName(lang, language_code_map);
//			for (String lang_code : language_code_map.keySet()) {
//				if (language_code_map.get(lang_code).compareTo(lang) == 0) {
//					Log.i(TAG, "DEBUG - code: " + lang_code + "; name: " + language_code_map.get(lang_code)
//							+ "; extracted language name: " + lang);
//					motherTongue = new Language(lang, lang_code);
//					break;
//				}
//			}		
		
		for (int i=2; i<l_languages.getChildCount(); i++) {
			RelativeLayout l_lang = (RelativeLayout) l_languages.getChildAt(i);
			Log.i("RecordingMetadataLig", "layout id: " + l_lang.getId());
			String lang_name = ((TextView) l_lang.getChildAt(1)).getText().toString();
			if (!lang_name.isEmpty()) {
				Log.i("RecordingMetadataLig", "Language: " + lang_name);
				if (language_code_map.containsValue(lang_name))
					selectedLanguages.add(getLanguageFromName(lang_name, language_code_map));
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
		// get personal information
		String message;
		regionOrigin = ((EditText) findViewById(R.id.edit_region_origin)).getText().toString();
		if (regionOrigin.compareTo(getString(R.string.region_origin_edit)) == 0)
			regionOrigin = "";
		speakerName = ((EditText) findViewById(R.id.edit_speaker_name)).getText().toString();
		if (speakerName.compareTo(getString(R.string.speaker_name_edit)) == 0)	
			speakerName = "";
		age = ((EditText) findViewById(R.id.edit_speaker_age)).getText().toString();
		try {
			//speakerAge = new Integer(age);
			if (!age.isEmpty()) {
				speakerAge = Integer.parseInt(age);
				if (speakerAge < 3 || speakerAge > 151) {
					msgError = "Input age is implausible... please correct it.";
					new AlertDialog.Builder(this)
					.setMessage(msgError)
					.setPositiveButton("Ok", null)
					.show();
					return;
				}
			}
		} catch (NumberFormatException e) {
			new AlertDialog.Builder(this)
				.setMessage("Warning: Age is incorrect.")
				.setPositiveButton("Ok", null)
				.show();
			return;
		}
		
		RadioGroup rg = ((RadioGroup) findViewById(R.id.edit_radio_gender));
		if (rg.getCheckedRadioButtonId() != -1) {
			RadioButton rbtn = ((RadioButton)findViewById(rg.getCheckedRadioButtonId()));
			speakerGender = rbtn.getText().toString().compareTo(getString(R.string.radio_male)) == 0 ? 
					GENDER_MALE : GENDER_FEMALE;
		}
		
		if (recordLang == null || speakerName.isEmpty()) {
			msgError = "Some fields remain empty, please fill it.";
			new AlertDialog.Builder(this)
				.setMessage(msgError)
				.setPositiveButton("Ok", null)
				.show();
			return;
		}
		
		if (motherTongue == null || selectedLanguages.isEmpty() || regionOrigin.isEmpty()
				|| speakerGender == 0 || speakerAge == 0) {
			message = missFieldsMsg;
		} else {
			message = validateMsg;
		}
		
//		String record_lang = ((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).getText().toString();
//		if (record_lang.isEmpty() && speakerName .isEmpty()) {
//			msgError = "Some fields remain empty, please fill it.";
//			new AlertDialog.Builder(this)
//			.setMessage(msgError)
//			.setPositiveButton("Ok", null)
//			.show();
//			return;
//		} else if (selectedLanguages.isEmpty() || regionOrigin.isEmpty() || speakerGender == 0) {
//			message = missFieldsMsg;
//		} else { message = validateMsg; }
		
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MetadataSession session = MetadataSession.getMetadataSession();
				session.setSession(recordLang, motherTongue, selectedLanguages, regionOrigin, speakerName, speakerAge, speakerGender);
				Log.i(TAG, "Session saved - languages: " + selectedLanguages);
				Log.i(TAG, "Session saved - origin: " + regionOrigin);
				Log.i(TAG, "Session saved - speaker name: " + speakerName);
				Log.i(TAG, "Session saved - speaker age: " + speakerAge);
				Log.i(TAG, "Session saved - speaker gender: " + speakerGender);
				
				Log.i("RecordingMetadataLig", "languages: " + selectedLanguages.toString());
				Log.i("RecordingMetadataLig", "origin: " + regionOrigin );
				Log.i("RecordingMetadataLig", "speaker name: " + speakerName);
				Log.i("RecordingMetadataLig", "speaker age" + speakerAge);
				Log.i("RecordingMetadataLig", "speaker gender" + speakerGender);
				
				Date date = new Date();
				DateFormat dateformat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
				String metaDate = dateformat.format(date);
				
				Intent intent;
				if (!respeak && !elicitation) {
					// pass metadata to RecordActivityLig and start it
					Bundle metadataBundle = new Bundle();
					metadataBundle.putParcelable(metaRecordLang, recordLang);
					metadataBundle.putParcelable(metaMotherTong, motherTongue);
					metadataBundle.putParcelableArrayList(metaLanguages, selectedLanguages);
					metadataBundle.putString(metaOrigin, regionOrigin);
					metadataBundle.putString(metaSpkrName, speakerName);
					metadataBundle.putInt(metaSpkrAge, speakerAge);
					metadataBundle.putInt(metaSpkrGender, speakerGender);
					metadataBundle.putString(RecordingMetadataLig.metaDate, metaDate);
					intent = new Intent(RecordingMetadataLig.this, RecordActivityLig.class);
					intent.putExtra(metaBundle, metadataBundle);
					intent.putExtra(RespeakingSelection.RESPEAK, respeak);
					startActivity(intent);
				} else if (!respeak && elicitation) {
					intent = new Intent(RecordingMetadataLig.this, RecordElicitation.class);
					Bundle metadataBundle = new Bundle();
					metadataBundle.putParcelable(metaRecordLang, recordLang);
					metadataBundle.putParcelable(metaMotherTong, motherTongue);
					metadataBundle.putParcelableArrayList(metaLanguages, selectedLanguages);
					metadataBundle.putString(metaOrigin, regionOrigin);
					metadataBundle.putString(metaSpkrName, speakerName);
					metadataBundle.putInt(metaSpkrAge, speakerAge);
					metadataBundle.putInt(metaSpkrGender, speakerGender);
					metadataBundle.putString(RecordingMetadataLig.metaDate, metaDate);
					intent.putExtra(metaBundle, metadataBundle);
					intent.putExtra(ElicitationMode.importFileName, getIntent().getStringExtra(ElicitationMode.importFileName));
					Log.i(TAG, "Selected file: " + getIntent().getStringExtra(ElicitationMode.importFileName));
					startActivity(intent);
				} else if (respeak && !elicitation){
					String gender = "";
					if (speakerGender == RecordingMetadataLig.GENDER_MALE) { gender = "Male"; }
					else if (speakerGender == RecordingMetadataLig.GENDER_FEMALE) { gender = "Female"; }
					String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
							+ "_" + Aikuma.getDeviceName() + "_" + recordLang.getCode();
					Double latitude = MainActivity.locationDetector.getLatitude();
					Double longitude = MainActivity.locationDetector.getLongitude();
					RecordingLig recording = new RecordingLig(importWavUUID, name, date, 
							AikumaSettings.getLatestVersion(), 
							AikumaSettings.getCurrentUserId(), recordLang, motherTongue,
							selectedLanguages, new ArrayList<String>(), Aikuma.getDeviceName(),
							Aikuma.getAndroidID(), null, null, (long)importWavRate, importWavDur, 
							importWavFormat, importWavChannels, importBitsSample, latitude, longitude,
							regionOrigin, speakerName, speakerAge, gender);
					try {
						recording.write();
						intent = new Intent(RecordingMetadataLig.this, RespeakingMetadataLig.class);
						intent.putExtra(RecordActivityLig.intent_recordname, name);
						intent.putExtra("dirname", name);
						intent.putExtra(RecordActivityLig.intent_rewindAmount, 500);
					} catch (IOException e) {
						Toast.makeText(RecordingMetadataLig.this,
							"Failed to import the recording:\t" +
							e.getMessage(), Toast.LENGTH_LONG).show();
						Log.e(TAG, "Failed to import the recording: " + e);
						intent = new Intent(RecordingMetadataLig.this, RespeakingSelection.class);
					}
					startActivity(intent);
				}
				RecordingMetadataLig.this.finish();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private Language getLanguageFromName(String langName, Map<String,String> language_code_map) {
		for (String lang_code : language_code_map.keySet()) {
			if (language_code_map.get(lang_code).compareTo(langName) == 0) {
//				Log.i(TAG, "DEBUG - code: " + lang_code + "; name: " + language_code_map.get(lang_code)
//						+ "; extracted language name: " + lang);
				return new Language(langName, lang_code);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK && requestCode == SELECT_LANG_ACT) {
			Language language = intent.getParcelableExtra("language");
//			if (!selectedLanguages.contains(language)) {
//				selectedLanguages.add(language);
//			}
			Log.i("RecordingMetadataLig", "textview id: " + intent.getExtras().getInt("textview_id"));
			Log.i(TAG, "DEBUG - language: " + language.getCode() + ": " + language.getName());
			TextView tv = (TextView) findViewById(intent.getIntExtra("textview_id", -1));
			if (tv != null) {
				tv.setText(language.getName());
			} else {
				Toast.makeText(this, "Failed to retrieve language from selection", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		onAddISOLanguageButton(v);
		
	}
	
	public void onBackPressed(View prev) {
		this.finish();
	}
	

}
