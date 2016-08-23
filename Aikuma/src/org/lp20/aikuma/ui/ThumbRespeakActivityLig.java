package org.lp20.aikuma.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.audio.record.Recorder;
import org.lp20.aikuma.audio.record.ThumbRespeaker;
import org.lp20.aikuma.audio.record.Microphone.MicException;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.RecordingLig;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class ThumbRespeakActivityLig extends AikumaActivity {
	
	public static final String TAG = "ThumbRespeakActivityLig";
	private final int code = 1000;

	private boolean translateMode = false;
	private ThumbRespeakFragment fragment;
	private ThumbRespeaker respeaker;
	private String sourceId;
	private UUID respeakingUUID;
	private long sampleRate;
	private RecordingLig recording;
	private boolean edited = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
		setContentView(R.layout.thumb_respeak);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		//Lets method in superclass know to ask user if they are willing to
		//discard new data on an activity transition via the menu.
		safeActivityTransition = true;
		fragment = (ThumbRespeakFragment)
				getFragmentManager().findFragmentById(R.id.ThumbRespeakFragment);
		setUpThumbRespeaker();
		fragment.setThumbRespeaker(respeaker);
		translateMode = getIntent().getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);
	}

	private void setUpThumbRespeaker() {
		Intent intent = getIntent();
		String recordName = intent.getStringExtra(RecordActivityLig.intent_recordname);
		String dirName = intent.getStringExtra("dirname");
//		sourceId = (String) intent.getExtras().get("sourceId");
		int rewindAmount = intent.getExtras().getInt(RecordActivityLig.intent_rewindAmount);
		sampleRate = intent.getLongExtra(RecordActivityLig.intent_sampleRate, 16000);
		
		respeakingUUID = UUID.randomUUID();
		
		try {
			File metadataFile = new File(FileIO.getOwnerPath(),
					RecordingLig.RECORDINGS + dirName + "/"
					+ recordName + RecordingLig.METADATA_SUFFIX);
//			File metadataFile = new File(recordName.split(".")[0] 
//					+ "." + RecordingLig.METADATA_SUFFIX);
			recording = RecordingLig.read(metadataFile);
			sourceId = recording.getId();
			Log.i(TAG, "Initiating activity - metadatafile to read: " + metadataFile.getAbsolutePath());
			respeaker = new ThumbRespeaker(recording, respeakingUUID, rewindAmount);
		} catch (IOException e) {
			Log.e(TAG, "initiating Thumbrespeaker - IOException e: " + e);
			ThumbRespeakActivityLig.this.finish();
		} catch (MicException e) {
			Log.e(TAG, "initiating Thumbrespeaker - MicException e: " + e);
			ThumbRespeakActivityLig.this.finish();
		}
	}
	
	/**
	 * When the save respeaking button is called, stop the activity and send
	 * the relevant data to the RecordingMetadataActivity
	 * LIG version
	 *
	 * @param	view	The save respeaking button.
	 */
	public void onSaveRespeakingButton(View view) {
		if (!edited) { 
			respeaker.saveRespeaking();
			
			try {
				respeaker.stop();
			} catch (MicException e) {
				Toast.makeText(this, "There has been an error stopping the microphone.",
						Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(this, "There has been an error writing the mapping between original and respeaking to file",
						Toast.LENGTH_LONG).show();
			}
			startActivityForResult(new Intent(this, ThumbRespeakSummaryLig.class), code);
			return;
		}
		
		Date date = new Date();
		DateFormat dateformat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		String metaDate = dateformat.format(date);
		
		int duration = respeaker.getCurrentMsec();
		String deviceName = Aikuma.getDeviceName();
		String androidID = Aikuma.getAndroidID();
		ArrayList<String> speakerIds = new ArrayList<String>(); 
		Double latitude = MainActivity.locationDetector.getLatitude();
		Double longitude = MainActivity.locationDetector.getLongitude();
//		String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
//						+ "_" + deviceName + "_" + recording.getLanguages().get(0).getCode(); 
		
//				selectedLanguages.get(0).getCode();
		Intent metadataIntent = getIntent();
		Bundle respeakingBundle = metadataIntent.getBundleExtra(RespeakingMetadataLig.metabundle);
		String gender = respeakingBundle.getString(RecordingMetadataLig.metaSpkrGender);
		Language recordLang = respeakingBundle.getParcelable(RecordingMetadataLig.metaRecordLang);
		Language motherTong = respeakingBundle.getParcelable(RecordingMetadataLig.metaMotherTong);
		ArrayList<Language> rspkLanguages = respeakingBundle.getParcelableArrayList(RecordingMetadataLig.metaLanguages);
		String regionOrigin = respeakingBundle.getString(RecordingMetadataLig.metaOrigin);
		String speakerName = respeakingBundle.getString(RecordingMetadataLig.metaSpkrName);
		int speakerAge = respeakingBundle.getInt(RecordingMetadataLig.metaSpkrAge);
		
		String mode = translateMode ? "_translate_" : "_respeak_";
		String name = metadataIntent.getStringExtra("dirname")
				+ mode + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
				+ "_" + deviceName + "_" + recordLang.getCode();
//				+ "_respeak_" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
//				+ "_" + deviceName + "_" + rspkLanguages.get(0).getCode();
//		if (speakerGender == RecordingMetadataLig.GENDER_MALE)
//			gender = "Male";
//		else if (speakerGender == RecordingMetadataLig.GENDER_FEMALE)
//			gender = "Female";
		
		String groupId = Recording.getGroupIdFromId(sourceId);
		String sourceVerId = recording.getVersionName() + "-" + recording.getId();
		Recorder recorder = respeaker.getRecorder();
		RecordingLig recordingRspk = new RecordingLig(respeakingUUID, name, date, 
				AikumaSettings.getLatestVersion(), 
				AikumaSettings.getCurrentUserId(), recordLang, motherTong,
				rspkLanguages, new ArrayList<String>(), deviceName, androidID, 
				groupId, sourceVerId, sampleRate, duration, 
				recorder.getFormat(), recorder.getNumChannels(), 
				recorder.getBitsPerSample(), latitude, longitude,
				regionOrigin, speakerName, speakerAge, gender);

		try {
			// Move the wave file from the nosync directory to
			// the synced directory and write the metadata
			recordingRspk.write();
			Toast.makeText(ThumbRespeakActivityLig.this, 
					"Respeaking saved into the file " + name,
					Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(ThumbRespeakActivityLig.this,
				"Failed to write the respoken recording metadata:\t" +
				e.getMessage(), Toast.LENGTH_LONG).show();
			Log.e(TAG, "failed when saving files: " + e);
//			Intent intent;
//			if (!respeak)
//				intent = new Intent(RecordActivityLig.this, ModeSelection.class);
//			else
//				intent = new Intent(RecordActivityLig.this, RespeakingSelection.class);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//			return;
		}
		
		Intent intent = new Intent(this, RespeakingSelection.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		ThumbRespeakActivityLig.this.finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK && requestCode == this.code) {
			edited = true;
			onSaveRespeakingButton(null);
		} else if (resultCode == RESULT_CANCELED && requestCode == this.code) {
			Toast.makeText(this, "Error when saving the respeaking: please try again", Toast.LENGTH_LONG).show();
			finish();
		}
	}
}
