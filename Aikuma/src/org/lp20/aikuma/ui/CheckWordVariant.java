package org.lp20.aikuma.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.model.Language;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CheckWordVariant extends AikumaActivity {
	
	public static final String TAG = "CheckWordVariant";
	//private UUID recordUUID;
	private BufferedReader reader;
	private BufferedWriter outputFile;
	private String variantTextFile;
	private int checkboxCount;
	private String variantchecked;
	private boolean wordvariant;
	private boolean append;
	private int nbReadLines;
	private String date;
	private SharedPreferences prefsUserSession;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_word_variant);
	    
	    date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.FRANCE).format(new Date());
	    prefsUserSession = getSharedPreferences(getString(R.string.userSession), MODE_PRIVATE);
	    //case: session already exists
	    if (prefsUserSession.getBoolean(getString(R.string.sessionActive), false)) {
	    	/*
	    	 * TODO rajouter une condition en && 
	    	 * soustraire date.now à date session : 
	    	 *  si <5min alors ed.clear
	    	 *  sinon continue;
	    	 */
	    	//retrieve selected file handled in the last session
	    	variantTextFile = prefsUserSession.getString(getString(R.string.sessionInputFile),null);
	    	Log.i(TAG, "Selected import file: " + variantTextFile);
	    	//retrieve result file handled in the last session
	    	variantchecked = prefsUserSession.getString(getString(R.string.sessionCheckExportFile),null);
	    	Log.i(TAG, "Selected export file: " + variantchecked);
	    	//retrieve number of lines already handled in the last session
	    	nbReadLines = prefsUserSession.getInt(getString(R.string.sessionProgress),0);
		    append = true;
		    // clear the current stored session
		    SharedPreferences.Editor ed = prefsUserSession.edit();
		    ed.clear(); ed.commit();
		//case: no session
	    } else {
	    	variantTextFile = getIntent().getStringExtra(CheckMode.importFileName);
		    Log.i(TAG, "Selected file: " + variantTextFile);
		    append = false;
		    //output filename
		    variantchecked = variantTextFile.replace(".txt", "_"+date+"_CHECKED.txt");
		    nbReadLines=0;
	    }

	    //creation of output file
	  	File checkedFile = new File(variantchecked);

	  	//new buffer to write in the output file
	    try {
			outputFile = new BufferedWriter(new FileWriter(checkedFile,append));
			Log.i("onCreate", "outputFile initialisé");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	  	
	  	//reading imported file and processing on
	    try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(variantTextFile)));
			// test on the lines read:
			// check that the file is not empty by ensuring that the line is not null (end of file reached)
			// check that the line is correctly formatted (no empty line and ', ' delimited found)
			String line;
			while ((line = reader.readLine()) != null && (line.isEmpty() || line.split(", ").length <= 1)) { continue; }
			if (line == null) { throw new IOException("Empty text file"); }
			for (int countLine=0;countLine<nbReadLines;countLine++) {
				line = reader.readLine();
			}
			//split on line. group1=word, group2=variant(s)
			String[] wordlist = line.split(", ");
			TextView lexeme = (TextView) findViewById(R.id.tv_verif_lexeme);
			lexeme.setText(wordlist[0]);
			
			LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
			llparent.removeAllViews();
			
			String[] variantlist = wordlist[1].split(" ; ");
			CheckBox variant;
			checkboxCount=0;
			final float scale = getResources().getDisplayMetrics().density;
		    int padding_in_px = (int) (10 * scale + 0.5f);
		    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
			for (String element : variantlist){
				//variant + checkbox
				variant = new CheckBox(this);
				variant.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				variant.setPadding(padding_in_px, 0, 0, padding_in_px);
				variant.setTextSize(tsizepx);
				//variant.setTypeface(null, Typeface.ITALIC);
				variant.setTypeface(Typeface.SERIF);
				variant.setText(element);
				variant.setId(checkboxCount);
				llparent.addView(variant);
				checkboxCount+=1;
			}
			/*
			 * TODO 3 radiogroup avec des radio buttons plutôt qu'une... + associer un label (=variant)
			 */
			
			nbReadLines++;
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

	}

	public void onNextClick(View _view) {
		if (saveFile()) {
		    //recordUUID = UUID.randomUUID();
			try {
				String line;
				while ((line = reader.readLine()) != null && (line.isEmpty() || line.split(", ").length <= 1)) { continue; }  
				if (line == null) { throw new IOException("Empty text file"); }
				//split on line. group1=word, group2=variant(s)
				String[] wordlist = line.split(", ");
				TextView lexeme = (TextView) findViewById(R.id.tv_verif_lexeme);
				lexeme.setText(wordlist[0]);
				
				LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
				llparent.removeAllViews();
				
				String[] variantlist = wordlist[1].split(" ; ");
				CheckBox variant;
				checkboxCount=0;
				final float scale = getResources().getDisplayMetrics().density;
			    int padding_in_px = (int) (10 * scale + 0.5f);
			    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
				for (String element : variantlist){
					//variant + checkbox
					variant = new CheckBox(this);
					variant.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					variant.setPadding(padding_in_px, 0, 0, padding_in_px);
					variant.setTextSize(tsizepx);
					//variant.setTypeface(null, Typeface.ITALIC);
					variant.setTypeface(Typeface.SERIF);
					variant.setText(element);
					variant.setId(checkboxCount);
					llparent.addView(variant);
					checkboxCount+=1;
				}
				/*
				 * TODO si rien n'est coché, désactiver le bouton next
				 */
				nbReadLines++;
			} catch (IOException e) {
				Log.e(TAG,"No more words to display or an error occurred: " + e);
				try {
					outputFile.flush();
					outputFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Toast.makeText(this, "No more words to display. File saved in "+variantchecked+".", Toast.LENGTH_LONG).show();
				this.finish();
			}
		}
	}
	
	public void onValidate(View _view) {
		if (saveFile()) {
			try {
				outputFile.flush();
				outputFile.close();
				Toast.makeText(this, "File saved in "+variantchecked+".", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			wordvariant = false;
			Log.i(TAG, "boolean value : "+wordvariant);
			//si fichier non terminé alors sauver session
			try {
				if (reader.readLine() != null) {
					/*
					 * sauver nb lignes lues: Boolean nbReadLines
					 * + mode: Boolean checkMode
					 * + submode: Boolean checkVariant
					 * + fichier import: String variantTextFile
					 * + fichier export: String variantchecked
					 * + date: String date
					 */
					 date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.FRANCE).format(new Date());
					 SharedPreferences.Editor ed = prefsUserSession.edit();
					 ed.putBoolean(getString(R.string.sessionActive), true); //session activated
					 ed.putInt(getString(R.string.sessionProgress), nbReadLines); //nb lines
					 ed.putString(getString(R.string.sessionInputFile), variantTextFile); //handled file
					 ed.putString(getString(R.string.sessionCheckExportFile), variantchecked); //resulting file
					 ed.putString(getString(R.string.sessionDate), date); //set date
					 ed.putString(getString(R.string.sessionMode),TAG); //set mode
//					 ed.putBoolean(getString(R.string.checkVariant), true); //set submode
					 //save infos
					 ed.commit();
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
		 * Write word with its variant(s) checked in a new file
		 */
		/*
		 * TODO si les radiogroup ne sont pas tous coché : 
		 * -> afficher un Toast "toutes les cases n'ont pas été cochées"
		 * -> return false
		 */
		try {
			Log.i("saveFile", "Writing on an existing file");
			//retrieve the word from the TextView
			outputFile.write(((TextView) findViewById(R.id.tv_verif_lexeme)).getText().toString());
			//retrieve variant checked
			for (int i=0; i<checkboxCount; i++) {
				//if checked
				CheckBox checkbox_x = (CheckBox) findViewById(i);
				if (checkbox_x.isChecked()) {
					/*
					 * TODO modifier pour prendre en compte le radiogroup
					 */
					Log.i("CHECKED", checkbox_x.toString());
					outputFile.write(", "+checkbox_x.getText());
				}
			}
			outputFile.write("\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return true;
	}
	
	public void onBackPressed() {
		onValidate(null);
	}
}
