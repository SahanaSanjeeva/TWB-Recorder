package org.lp20.aikuma.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.audio.record.Recorder;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma2.R;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CheckTranscription extends AikumaActivity {
	
	public static final String TAG = "CheckTranscription";
	private BufferedReader reader;
	private BufferedWriter outputFile;
	private String transcripTextFile;
	private String transcriptChecked;
	private TextView transcriptID;
	private CheckBox transcriptOK;
	protected long sampleRate = 16000l;
	private boolean append;
	private int nbReadLines;
	private String date;
	private SharedPreferences prefsUserSession;
	private String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
	private String dataPath = externalStoragePath+"/Download/";
    

	private ListenFragment fragment;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_transcription);
	    
	    prefsUserSession = getSharedPreferences(getString(R.string.userSession), MODE_PRIVATE);
	    date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.FRANCE).format(new Date());
	    //case: session already exists
	    if (prefsUserSession.getBoolean(getString(R.string.sessionActive), false)) {
	    	//retrieve selected file handled in the last session
	    	transcripTextFile = prefsUserSession.getString(getString(R.string.sessionInputFile),null);
	    	Log.i(TAG, "Selected import file: " + transcripTextFile);
	    	//retrieve result file handled in the last session
	    	transcriptChecked = prefsUserSession.getString(getString(R.string.sessionCheckExportFile),null);
	    	Log.i(TAG, "Selected export file: " + transcriptChecked);
	    	//retrieve number of lines already handled in the last session
	    	nbReadLines = prefsUserSession.getInt(getString(R.string.sessionProgress),0);
	    	append = true;
	    	// clear the current stored session
	    	SharedPreferences.Editor ed = prefsUserSession.edit();
	    	ed.clear();
	    	ed.commit();
		//case: no session
	    } else {
		    transcripTextFile = getIntent().getStringExtra(CheckMode.importFileName);
		    Log.i(TAG, "no session");
		    Log.i(TAG, "Selected file: " + transcripTextFile);
		    append = false;
		    //output filename
		    transcriptChecked = transcripTextFile.replace(".txt", "_"+date+"_CHECKED.txt");
		    nbReadLines=0;
	    }
	    
	    //creation
	  	File checkedFile = new File(transcriptChecked);
	  	try {
	  		//new buffer to write in
			outputFile = new BufferedWriter(new FileWriter(checkedFile,append));
			Log.i("onCreate", "outputFile initialisé");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	  	
	  	
		//reading imported file and processing on
	    try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(transcripTextFile)));
			// test on the lines read:
			// check that the file is not empty by ensuring that the phrase is not null (end of file reached)
			// check that the sentence is correctly formatted (no empty line and ' : ' delimited found)
			String line;
			while ((line = reader.readLine()) != null && (line.isEmpty() || line.split(" : ").length <= 1)) { continue; }  
			if (line == null) { throw new IOException("Empty text file"); }
			for (int countLine=0;countLine<nbReadLines;countLine++) {
				line = reader.readLine();
			}
			//split on line. group1=id, group2=transcription
			String[] splittedline = line.split(" : ");
			//group1
			transcriptID = (TextView) findViewById(R.id.transcription_id);
			transcriptID.setText(splittedline[0]);
			//group2
			String transcription = splittedline[1];
			
			//retrieve layout to put element in
			LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
			llparent.removeAllViews();
			
			final float scale = getResources().getDisplayMetrics().density;
		    int padding_in_px = (int) (10 * scale + 0.5f);
		    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
			//transcription + checkbox
			transcriptOK = new CheckBox(this);
			transcriptOK.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			transcriptOK.setPadding(padding_in_px, 0, 0, padding_in_px);
			transcriptOK.setTextSize(tsizepx);
			transcriptOK.setTypeface(Typeface.SERIF);
			transcriptOK.setText(transcription);
			llparent.addView(transcriptOK);
			nbReadLines++;
			Log.i(TAG, "current line: " + nbReadLines);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			Log.e(TAG,"transcript file could not be found: " + e1);
			Toast.makeText(this, "An error occurred, the transcript file could not be found", Toast.LENGTH_LONG).show();
			this.finish();
		} catch (IOException e) {
			Toast.makeText(this, "Something weird happened. It might be that the transcript file is empty.", Toast.LENGTH_LONG).show();
			Log.e(TAG,"No more sentences to display or an error occurred: " + e);
			this.finish();
		}
	    
	    /*
	     * TODO: load the audio file corresponding to transcriptID.getText()
	     */
	    fragment = (ListenFragment) getFragmentManager().findFragmentById(R.id.phrase_player);
	    
	    SimplePlayer player;
		try {
			player = new SimplePlayer(new File(dataPath+transcriptID.getText().toString()+".wav"),sampleRate, true);
			fragment.setPlayer(player);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(this, "The corresponding audio file is unobtainable. Please proceed to the next transcript."+transcriptID.getText(), Toast.LENGTH_LONG).show();
			transcriptOK.setEnabled(false);
		}
	}
	
	public void onNextClick(View _view) {
		if (saveFile()) {
		    try {
				String line;
				while ((line = reader.readLine()) != null && (line.isEmpty() || line.split(" : ").length <= 1)) { continue; }  
				if (line == null) { throw new IOException("Empty text file"); }
				//split on line. group1=id, group2=transcription
				String[] splittedline = line.split(" : ");
				//group1
				transcriptID = (TextView) findViewById(R.id.transcription_id);
				transcriptID.setText(splittedline[0]);
				//group2
				String transcription = splittedline[1];
				
				LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
				llparent.removeAllViews();
				
				final float scale = getResources().getDisplayMetrics().density;
			    int padding_in_px = (int) (10 * scale + 0.5f);
			    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
				//transcription + checkbox
				transcriptOK = new CheckBox(this);
				transcriptOK.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				transcriptOK.setPadding(padding_in_px, 0, 0, padding_in_px);
				transcriptOK.setTextSize(tsizepx);
				//transcriptionOK.setTypeface(null, Typeface.ITALIC);
				transcriptOK.setTypeface(Typeface.SERIF);
				transcriptOK.setText(transcription);
				llparent.addView(transcriptOK);
				nbReadLines++;
				Log.i(TAG, "current line: " + nbReadLines);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"No more transcript to display or an error occurred: " + e);
				try {
					outputFile.flush();
					outputFile.close();
					fragment.releasePlayer();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Toast.makeText(this, "No more transcript to display. File saved in "+transcriptChecked+".", Toast.LENGTH_LONG).show();
				this.finish();
			}
		    try {
		    	//player release
				fragment.releasePlayer();
				SimplePlayer player = new SimplePlayer(new File(dataPath+transcriptID.getText().toString()+".wav"), sampleRate, true);
				//allocation of the player to the fragment
				fragment.setPlayer(player);
		    } catch (IOException e) {
		    	Toast.makeText(this, "The corresponding audio file is unobtainable. Please proceed to the next transcript.", Toast.LENGTH_LONG).show();
		    	transcriptOK.setEnabled(false);
		    }
		} else {
			Toast.makeText(this, "Going to next transcript...", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onValidate(View _view) {
		if (saveFile()) {
			try {
				outputFile.flush();
				outputFile.close();
				Toast.makeText(this, "File saved in "+transcriptChecked+".", Toast.LENGTH_LONG).show();
				fragment.releasePlayer();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//si fichier non terminé alors sauver session
			try {
				if (reader.readLine() != null) {
					/*
					 * sauver nb lignes lues: Boolean nbReadLines
					 * + mode: Boolean checkMode
					 * + submode: Boolean checkTranscript
					 * + fichier import: String transcripTextFile
					 * + fichier export: String transcriptChecked
					 * + date: String date
					 */
					 date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.FRANCE).format(new Date());
					 SharedPreferences.Editor ed = prefsUserSession.edit();
					 ed.putBoolean(getString(R.string.sessionActive), true); //session activated
					 ed.putInt(getString(R.string.sessionProgress), nbReadLines); //nb lines
					 ed.putString(getString(R.string.sessionInputFile), transcripTextFile); //handled file
					 ed.putString(getString(R.string.sessionCheckExportFile), transcriptChecked); //resulting file
					 ed.putString(getString(R.string.sessionDate), date); //set date
					 ed.putString(getString(R.string.sessionMode),TAG); //set mode
//					 ed.putBoolean(getString(R.string.checkTranscript), true); //set submode
					 //save infos
					 ed.commit();

					 Log.i(TAG, "progress: " + nbReadLines);
					 Log.i(TAG, "input file: " + transcripTextFile);
					 Log.i(TAG, "export file: " + transcriptChecked);
					 Log.i(TAG, "date: " + date);
					 Log.i(TAG, "mode: " + TAG);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//sinon fin de fichier (buffer=vide) alors finish
			this.finish();
		}
	}
	
	private boolean saveFile() {
		/*
		 * Write transcription checked and its id in a new file
		 */
		try {
		Log.i("saveFile", "Writing on file");
		//if checked
		if (transcriptOK.isChecked()) {
			outputFile.write(transcriptID.getText()+" : "+transcriptOK.getText()+"\n");
		}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return true;
	}
	
	public void onBackPressed() {
		onValidate(null);
		/*
		 * TODO
		 * popup 3 choix :
		 * - oui je veux quitter et enregistrer ma session
		 * - oui je veux quitter mais ne pas enregistrer
		 * - annuler (je me suis trompé)
		 */
	}
}
