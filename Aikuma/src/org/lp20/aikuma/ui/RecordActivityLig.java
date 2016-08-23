package org.lp20.aikuma.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.audio.Beeper;
import org.lp20.aikuma.audio.record.Recorder;
import org.lp20.aikuma.audio.record.Microphone.MicException;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.model.MetadataSession;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.RecordingLig;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma2.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class RecordActivityLig extends RecordActivity {
	
	public static final String intent_sourceId = "sourceId";
	public static final String intent_ownerId = "ownerId";
	public static final String intent_versionName = "versionName";
	public static final String intent_sampleRate = "sampleRate";
	public static final String intent_rewindAmount = "rewindAmount";
	public static final String intent_recordname = "recordname";
	static final String TAG = "RecordActivityLig";
	
	private boolean respeak = false;
	
	private Language recordLang;
	private Language motherTong;
	private ArrayList<Language> selectedLanguages = new ArrayList<Language>();
	private String regionOrigin;
	private String speakerName;
	private int speakerAge=0;
	private int speakerGender=0;
	private Date date;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		respeak = getIntent().getBooleanExtra(RespeakingSelection.RESPEAK, false);
		// retrieve metadata from the intent
		Bundle bundle = getIntent().getBundleExtra(RecordingMetadataLig.metaBundle);
		recordLang = bundle.getParcelable(RecordingMetadataLig.metaRecordLang);
		motherTong = bundle.getParcelable(RecordingMetadataLig.metaMotherTong);
		selectedLanguages = bundle.getParcelableArrayList(RecordingMetadataLig.metaLanguages);
		regionOrigin = bundle.getString(RecordingMetadataLig.metaOrigin);
		speakerName = bundle.getString(RecordingMetadataLig.metaSpkrName);
		speakerAge = bundle.getInt(RecordingMetadataLig.metaSpkrAge, 0);
		speakerGender = bundle.getInt(RecordingMetadataLig.metaSpkrGender, 0);
		DateFormat dateformat = DateFormat.getDateTimeInstance();
		try {
			date = new SimpleDateFormat().parse(bundle.getString(RecordingMetadataLig.metaDate));
//			date = dateformat.parse(metaIntent.getStringExtra(RecordingMetadataLig.metaDate));
		} catch (ParseException e1) {
			date = new Date();
		} catch (Exception e) {
			Log.e(TAG, "Exceptopn caught: " + e); 
		}
		
		// DEBUG
		Log.i(TAG, "start intent - selected languages: " + selectedLanguages.toString());
		Log.i(TAG, "start intent - region of origin: " + regionOrigin);
		Log.i(TAG, "start intent - speaker name: " + speakerName);
		Log.i(TAG, "start intent - speaker age: " + speakerAge);
		Log.i(TAG, "start intent - speaker gender: " + speakerGender);
		Log.i(TAG, "start intent - date: " + date);
		
		// code from inherited class RecordActivity		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_activity_lig);
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		super.soundUUID = UUID.randomUUID();
		// Disable the stopButton(saveButton) before the recording starts
		ImageButton stopButton = 
				(ImageButton) findViewById(R.id.stopButton);
		stopButton.setImageResource(R.drawable.ok_disabled_48);
		stopButton.setEnabled(false);
		
		// Set up the Beeper that will make beeps when recording starts and
		// pauses
		super.beeper = new Beeper(this, new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer _) {
					// There is a reason for this conditional. The beeps take
					// time to be played. If the user reaches the far state 
					// before recording actually starts, then recording will 
					// be then be triggered when the display suggests otherwise.
					Log.i("RecordActivity", "in onCompletion, recording:" + recording);
					if (recording) {
						recorder.listen();
						ImageButton recordButton =
								(ImageButton) findViewById(R.id.recordButton);
						ImageButton pauseButton =
								(ImageButton) findViewById(R.id.pauseButton);
						recordButton.setVisibility(View.GONE);
						pauseButton.setVisibility(View.VISIBLE);
					}
				}
			});
		
		try {
			File f = new File(Recording.getNoSyncRecordingsPath(),
					soundUUID.toString() + ".wav");
			recorder = new Recorder(0, f, sampleRate);
		} catch (MicException e) {
			this.finish();
			Toast.makeText(getApplicationContext(),
					"Error setting up microphone.",
					Toast.LENGTH_LONG).show();
		}
		timeDisplay = (TextView) findViewById(R.id.timeDisplay);
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					timeDisplay.post(new Runnable() {
						public void run() {
							int time = recorder.getCurrentMsec();
							/*
							BigDecimal bd = new
									BigDecimal(recorder.getCurrentMsec()/1000f);
							bd = bd.round(new MathContext(1));
							*/
							//Lets method in superclass know to ask user if
							//they are willing to discard audio if time>250msec
							if(time > 250) {
								safeActivityTransition = true;
								safeActivityTransitionMessage = "Discard Audio?";
							}
								
							timeDisplay.setText(Float.toString(time/1000f) + "s");
						}
					});
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}).start();
	}
	
	@Override
	public void onStopButton(View view) {
		try {
			super.recorder.stop();
		} catch (MicException e) {
			// Maybe make a recording metadata file that refers to the error so
			// that the audio can be salvaged.
		}

		int duration = super.recorder.getCurrentMsec();
		String deviceName = Aikuma.getDeviceName();
		String androidID = Aikuma.getAndroidID();
		ArrayList<String> speakerIds = new ArrayList<String>(); 
		Double latitude = MainActivity.locationDetector.getLatitude();
		Double longitude = MainActivity.locationDetector.getLongitude();
		String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
						+ "_" + deviceName + "_" + recordLang.getCode();
//						+ "_" + deviceName + "_" + selectedLanguages.get(0).getCode();
		String gender = "";
		if (speakerGender == RecordingMetadataLig.GENDER_MALE)
			gender = "Male";
		else if (speakerGender == RecordingMetadataLig.GENDER_FEMALE)
			gender = "Female";
		
		RecordingLig recording = new RecordingLig(soundUUID, name, date, 
				AikumaSettings.getLatestVersion(), 
				AikumaSettings.getCurrentUserId(), recordLang, motherTong,
				selectedLanguages, speakerIds, deviceName, androidID, 
				null, null, super.sampleRate, duration, 
				super.recorder.getFormat(), super.recorder.getNumChannels(), 
				super.recorder.getBitsPerSample(), latitude, longitude,
				regionOrigin, speakerName, speakerAge, gender);

		try {
			// Move the wave file from the nosync directory to
			// the synced directory and write the metadata
			recording.write();
		} catch (IOException e) {
			Toast.makeText(RecordActivityLig.this,
				"Failed to write the Recording metadata:\t" +
				e.getMessage(), Toast.LENGTH_LONG).show();
			Intent intent;
			if (!respeak)
				intent = new Intent(RecordActivityLig.this, ModeSelection.class);
			else
				intent = new Intent(RecordActivityLig.this, RespeakingSelection.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return;
		}
		
		Intent intent;
		if (!respeak) {
			intent = new Intent(this, ModeSelection.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			Toast.makeText(RecordActivityLig.this, String.format("%s       %s (%s)", 
							recording.getNameAndLang(), 
							new SimpleDateFormat("yyyy-MM-dd").format(recording.getDate()),
							(duration / 1000) + ""), 
							Toast.LENGTH_LONG).show();
		} else {
			// TODO allow for the second mode of respeaking, phone near to ear?
			intent = new Intent(this, RespeakingMetadataLig.class);
			SharedPreferences preferences =	PreferenceManager.getDefaultSharedPreferences(this);
			String respeakingMode = preferences.getString(
					AikumaSettings.RESPEAKING_MODE_KEY, "nothing");
			int rewind = preferences.getInt("respeaking_rewind", 500);
			Log.i(TAG, "respeakingMode: " + respeakingMode +", rewindAmount: " + rewind);
			intent.putExtra(intent_sourceId, recording.getId());
			intent.putExtra(intent_ownerId, AikumaSettings.getCurrentUserId());
			intent.putExtra(intent_versionName, AikumaSettings.getLatestVersion());
			intent.putExtra(intent_sampleRate, super.sampleRate);
			intent.putExtra(intent_rewindAmount, rewind);
			intent.putExtra(intent_recordname, name);
			Toast.makeText(RecordActivityLig.this, "Please fill in the details related of the respeaking speaker."
					+ recording.getNameAndLang(), Toast.LENGTH_LONG)
					.show();
		}
		
		startActivity(intent);
		this.finish();
	}
	
	public void onBackPressed(View v) {
		this.finish();
	}

}
