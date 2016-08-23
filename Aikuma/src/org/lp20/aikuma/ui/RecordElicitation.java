package org.lp20.aikuma.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.audio.record.Recorder;
import org.lp20.aikuma.audio.record.Microphone.MicException;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.RecordingLig;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class RecordElicitation extends AikumaActivity {
	
	public static final String TAG = "RecordElicitation";
	private UUID recordUUID;
	private Recorder recorder;
	protected long sampleRate = 16000l;
	private boolean recording = false;
	private BufferedReader reader;
	private int phraseId = 1;
	private String eliciTextFile;
	private ListenFragment fragment;

	private Language recordLang;
	private Language motherTong;
	private ArrayList<Language> selectedLanguages = new ArrayList<Language>();
	private String regionOrigin;
	private String speakerName;
	private int speakerAge=0;
	private int speakerGender=0;
	private Date date;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.elicitation);
	    recordUUID = UUID.randomUUID();
	    eliciTextFile = getIntent().getStringExtra(ElicitationMode.importFileName);
	    Log.i(TAG, "Selected file: " + eliciTextFile);
	    
	    // retrieving metadata
		Bundle bundle = getIntent().getBundleExtra(RecordingMetadataLig.metaBundle);
		recordLang = bundle.getParcelable(RecordingMetadataLig.metaRecordLang);
		motherTong = bundle.getParcelable(RecordingMetadataLig.metaMotherTong);
		selectedLanguages = bundle.getParcelableArrayList(RecordingMetadataLig.metaLanguages);
		regionOrigin = bundle.getString(RecordingMetadataLig.metaOrigin);
		speakerName = bundle.getString(RecordingMetadataLig.metaSpkrName);
		speakerAge = bundle.getInt(RecordingMetadataLig.metaSpkrAge, 0);
		speakerGender = bundle.getInt(RecordingMetadataLig.metaSpkrGender, 0);
		try {
			date = new SimpleDateFormat().parse(bundle.getString(RecordingMetadataLig.metaDate));
//			date = dateformat.parse(metaIntent.getStringExtra(RecordingMetadataLig.metaDate));
		} catch (ParseException e1) {
			date = new Date();
		} catch (Exception e) {
			Log.e(TAG, "Exception caught: " + e); 
		}
	    
	    try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(eliciTextFile)));
//			String phrase = reader.readLine();
			// test on the lines read:
			// check that the file is not empty by ensuring that the phrase is not null (end of file reached)
			// check that the sentence is correctly formatted (no empty line and '##' delimited found)
			String phrase;
			while ((phrase = reader.readLine()) != null && (phrase.isEmpty() || phrase.split("##").length <= 1)) { continue; }  
			if (phrase == null) { throw new IOException("Empty text file"); }
			String[] phrasePair = phrase.split("##");
			TextView tvFrPhrase = (TextView) findViewById(R.id.tv_elicit_phrase);
			tvFrPhrase.setText(phrasePair[0]);
			tvFrPhrase = (TextView) findViewById(R.id.tv_orig_phrase);
			tvFrPhrase.setText(phrasePair[1]);
			TextView tv = (TextView) findViewById(R.id.phrase_x);
			tv.setText("Phrase" + phraseId);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			Log.e(TAG,"text file could not be found: " + e1);
			Toast.makeText(this, "An error occurred, the text file could not be found", Toast.LENGTH_LONG).show();
			this.finish();
		} catch (IOException e) {
			Toast.makeText(this, "Something weird happened. It might be that the text file was empty.", Toast.LENGTH_LONG).show();
			Log.e(TAG,"No more sentences to display or an error occurred: " + e);
			this.finish();
		}

	    fragment = (ListenFragment) getFragmentManager().findFragmentById(R.id.phrase_player);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(recorder != null) {
			recorder.release();
		}
	}
	
	public void onRecordClick(View _view) {
		if (recording) {
			pause();
		} else {
			record();
		}
	}
	
	public void onNextClick(View _view) {
		if (saveRecording()) {
		    recordUUID = UUID.randomUUID();
			try {
				File f = new File(Recording.getNoSyncRecordingsPath(),
						recordUUID.toString() + ".wav");
				if (recorder != null) { recorder.release(); }
				recorder = new Recorder(0, f, sampleRate);
				Log.i(TAG, "recorder filename: " + recorder.getWriter().getFullFileName());
				fragment.releasePlayer();
				// test on the lines read:
				// check that the file is not empty by ensuring that the phrase is not null (end of file reached)
				// check that the sentence is correctly formatted (no empty line and '##' delimited found)
				String phrase;
				while ((phrase = reader.readLine()) != null && (phrase.isEmpty() || phrase.split("##").length <= 1)) { continue; }  
				if (phrase == null) { throw new IOException("End of text file reached."); }
				String[] phrasePair = phrase.split("##");
				TextView tvFrPhrase = (TextView) findViewById(R.id.tv_elicit_phrase);
				tvFrPhrase.setText(phrasePair[0]);
				tvFrPhrase = (TextView) findViewById(R.id.tv_orig_phrase);
				tvFrPhrase.setText(phrasePair[1]);
				phraseId++;
				TextView tv = (TextView) findViewById(R.id.phrase_x);
				tv.setText("Phrase" + phraseId);
			} catch (MicException e) {
				this.finish();
				Toast.makeText(getApplicationContext(),
						"Error setting up microphone.",
						Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(this, "No more sentences to display", Toast.LENGTH_LONG).show();
				Log.e(TAG,"No more sentences to display or an error occurred: " + e);
				this.finish();
			}
		} else {
			Toast.makeText(this, "Going to next sentence...", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onValidate(View _view) {
		if (saveRecording())
			this.finish();
	}
	
	private boolean saveRecording() {
		try {
			recorder.stop();
		} catch (MicException e) {
			Log.e(TAG, "error when saving the recroding: " + e);
			Toast.makeText(this, "An error occurred when saving the recording. Please try again.", Toast.LENGTH_LONG).show();
			return false;
		}

		int duration = recorder.getCurrentMsec();
		String deviceName = Aikuma.getDeviceName();
		String androidID = Aikuma.getAndroidID();
		ArrayList<String> speakerIds = new ArrayList<String>(); 
		Double latitude = MainActivity.locationDetector.getLatitude();
		Double longitude = MainActivity.locationDetector.getLongitude();
		String suffix = new File(eliciTextFile).getName().replace(".txt", "").length() >= 10 ? new File(eliciTextFile).getName().substring(0, 10)
				: new File(eliciTextFile).getName().replace(".txt", "");
		String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)
						+ "_" + deviceName + "_" + recordLang.getCode()
						+ "_elicit_" + suffix;
//						+ "_elicit_" + new File(eliciTextFile).getName().substring(0,10);
		// 		+ "_" + eliciTextFile.substring(0, 10) + "_" + phraseId
		Log.i(TAG, "recording name: " + name);
		String gender = "";
		if (speakerGender == RecordingMetadataLig.GENDER_MALE)
			gender = "Male";
		else if (speakerGender == RecordingMetadataLig.GENDER_FEMALE)
			gender = "Female";
		
		RecordingLig recording = new RecordingLig(recordUUID, name, date, 
				AikumaSettings.getLatestVersion(), 
				AikumaSettings.getCurrentUserId(), recordLang, motherTong,
				selectedLanguages, speakerIds, deviceName, androidID, 
				null, null, sampleRate, duration, 
				recorder.getFormat(), recorder.getNumChannels(), 
				recorder.getBitsPerSample(), latitude, longitude,
				regionOrigin, speakerName, speakerAge, gender);

		try {
			// Move the wave file from the nosync directory to
			// the synced directory and write the metadata
			recording.write();
			String filename = recording.getFile().getAbsolutePath();
			FileUtils.copyFile(recording.getFile(), 
					new File(filename.replace(".wav", "_" + phraseId + ".wav")));
			FileIO.delete(new File(filename));
			FileIO.delete(new File(filename.replace(".wav", "-preview.wav")));
			FileUtils.copyFile(new File(filename.replace(".wav", "-metadata.json")), 
					new File(filename.replace(".wav", "_" + phraseId + "-metadata.json")));
			FileIO.delete(new File(filename.replace(".wav", "-metadata.json")));
			recorder.release();
			recorder = null;
		} catch (IOException e) {
			Log.e(TAG, "error when saving the recroding: " + e);
			Toast.makeText(this, "An error occurred when saving the recording. Please try again.", Toast.LENGTH_LONG).show();
			return false;
		}
		Toast.makeText(this, name+"_"+phraseId, Toast.LENGTH_LONG).show();
		return true;
	}
	
	// Pauses the recording.
	private void pause() {
		if (recording) {
			recording = false;
			ImageButton recordButton =
					(ImageButton) findViewById(R.id.btn_record_elicit);
			recordButton.setImageResource(R.drawable.record);
			ImageButton nextButton = (ImageButton) findViewById(R.id.btn_next);
			nextButton.setEnabled(true);
			ImageButton validateButton = (ImageButton) findViewById(R.id.btn_validate);
			validateButton.setEnabled(true);
			try {
				recorder.pause();
				recorder.stop();
				// the player can only be set when the file is closed (hence the recorder stopped)
				// indeed, they both require a file descriptor (fd) on the same file;
				// when i tried to set up the player while the recorder was still holding the fd,
				// there was an error about the failure to set the fd; which i interpreted this way
				fragment.releasePlayer();
				SimplePlayer player = new SimplePlayer(new File(recorder.getWriter().getFullFileName()), sampleRate, true);
				fragment.setPlayer(player);
			} catch (MicException e) {
				// Maybe make a recording metadata file that refers to the error so
				// that the audio can be salvaged.
			} 
			catch (IOException e) {
				Log.e(TAG, "Could not start the fragment of the file " 
						+ new File(Recording.getNoSyncRecordingsPath(),	recordUUID.toString() + ".wav")
						+ " : " + e);
			}
		}
	}
		
	// Activates recording
	private void record() {		
		if (!recording) {
			recording = true;
			ImageButton recordButton =
					(ImageButton) findViewById(R.id.btn_record_elicit);
			recordButton.setImageResource(R.drawable.pause);
			ImageButton nextButton = (ImageButton) findViewById(R.id.btn_next);
			nextButton.setEnabled(false);
			ImageButton validateButton = (ImageButton) findViewById(R.id.btn_validate);
			validateButton.setEnabled(false);
			if (recorder != null) {	recorder.release(); }				
			try {
				File f = new File(Recording.getNoSyncRecordingsPath(),
						"/" + recordUUID.toString() + ".wav");
				recorder = new Recorder(0, f, sampleRate);
			} catch (MicException e) {
				this.finish();
				Toast.makeText(getApplicationContext(),
						"Error setting up microphone.",
						Toast.LENGTH_LONG).show();
			} 
			recorder.listen();
		}
	}
	
	public void onBackPressed(View v) {
		this.finish();
	}
}
