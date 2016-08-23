package org.lp20.aikuma;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.zip.Inflater;

import org.lp20.aikuma.ui.CheckMode;
import org.lp20.aikuma.ui.CheckTranscription;
import org.lp20.aikuma.ui.CheckWordVariant;
import org.lp20.aikuma.ui.ElicitationMode;
import org.lp20.aikuma.ui.RecordElicitation;
import org.lp20.aikuma.ui.RecordingMetadataLig;
import org.lp20.aikuma.ui.RespeakingSelection;
import org.lp20.aikuma.ui.ThumbRespeakActivityLig;
import org.lp20.aikuma.ui.ThumbRespeakSummaryLig;
import org.lp20.aikuma.ui.sensors.LocationDetector;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ModeSelection extends Activity implements OnClickListener{
	
	private static final String TAG = "ModeSelection";
	public static final String TRANSLATE_MODE = "translate";
	private SharedPreferences prefsUserSession;
	private SharedPreferences.Editor ed;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.mode_selection);
	    
	    // set owner id with gmail account first but replaced with a random id now
	    SharedPreferences settings = 
				PreferenceManager.getDefaultSharedPreferences(this);
	    //AikumaSettings.setUserId(settings.getString(AikumaSettings.SETTING_OWNER_ID_KEY, null));
	    if (AikumaSettings.getCurrentUserId() == null) {
		    Random r = new Random();
		    AikumaSettings.setUserId(Integer.toString(r.nextInt()));
		    Log.i(TAG, AikumaSettings.getCurrentUserId());
	    }
	    
	    // DEBUG
	    Map<String,?> mapPrefs = settings.getAll();
	    for (String key : mapPrefs.keySet())
	    	Log.i(TAG, "DEBUG - Shared Preferences - " + key + " -> " + mapPrefs.get(key));
	    
	    // hides the action bar instead of removing it
//	    getActionBar().hide();
	    
		Aikuma.loadLanguages();
		
		Button btn_rec = (Button) findViewById(R.id.button_mode_record);
		btn_rec.setOnClickListener(this);
		Button btn_respk = (Button) findViewById(R.id.button_mode_respeak);
		btn_respk.setOnClickListener(this);
		Button btn_trad = (Button) findViewById(R.id.mainTradBtn);
		btn_trad.setOnClickListener(this);
		Button btn_elicit = (Button) findViewById(R.id.mainElicitBtn);
		btn_elicit.setOnClickListener(this);
		Button btn_check = (Button) findViewById(R.id.mainCheckBtn);
		btn_check.setOnClickListener(this);
//		Button btn_old_aikuma = (Button) findViewById(R.id.button_mode_slct_old_aikuma);
//		btn_old_aikuma.setOnClickListener(this);
		
		// Start gathering location data
		MainActivity.locationDetector = new LocationDetector(this);
	    
	    // check the existing session and show popup to propose retrieving it
	    prefsUserSession = getSharedPreferences(getString(R.string.userSession), MODE_PRIVATE);
	    ed = prefsUserSession.edit();
	    if (prefsUserSession.getBoolean(getString(R.string.sessionActive), false)) {
	    	showSessionDialog();
	    }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
			// DONE add a button for respeaking and link to the corresponding activity
			// DONE create a selection view with 2 buttons: one for recording and one for importing audio
			// DONE link the "recording" button to existing recording structure 
			//		(link to recordingMetadataLig activity and allow RecordActivityLig to link to Respeaking)
			// DONE study how resepaking is done and reuse it (only the respeaking view and its process)
			// 		(in particular, beware to check the storage location of the respeaking and mapping files)
			// TODO the last existing view of respeaking (summary) should allow to link back to the record/import selection view
			// TODO then adapt the respeaking interface to allow the edition of latest segments
			// TODO then adapt the summary view to allow the edition and playing of every segment
			case R.id.button_mode_respeak:
				startActivity(new Intent(ModeSelection.this, RespeakingSelection.class));
				Log.i(TAG, "Mode respeaking selected; view id: " + v.getId());
				break;
			case R.id.button_mode_record:
				startActivity(new Intent(ModeSelection.this, RecordingMetadataLig.class));
				break;
			case R.id.mainTradBtn:
				Intent intent = new Intent(ModeSelection.this, RespeakingSelection.class);
				intent.putExtra(TRANSLATE_MODE, true);
				startActivity(intent);
//				startActivity(new Intent(ModeSelection.this, TranslationSelection.class));
				Log.i(TAG, "Mode translation selected; view id: " + v.getId());
				break;
			case R.id.mainElicitBtn:
				startActivity(new Intent(ModeSelection.this, ElicitationMode.class));
				Log.i(TAG, "Mode elicitation selected; view id: " + v.getId());
				break;
			case R.id.mainCheckBtn:
				startActivity(new Intent(ModeSelection.this, CheckMode.class));
				Log.i(TAG, "Mode verification selected; view id: " + v.getId());
				break;
//			case R.id.button_mode_slct_old_aikuma:
//				startActivity(new Intent(this,MainActivity.class));
//				break;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MainActivity.locationDetector.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		MainActivity.locationDetector.start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity.locationDetector.stop();
	}
	
	public void showSessionDialog() {
        DialogFragment dialog = new UserSessionDiaglogFragment();
        dialog.show(getFragmentManager(), "SessionDialogFragment");
	}
	
	public class UserSessionDiaglogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
		    LayoutInflater inflater = getActivity().getLayoutInflater();		    
		    LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.session_dialog, null);
		    
		    setDialogDetails(ll);
		    
		    builder.setView(ll)
//					.setMessage("A session has been saved, would you like to retrieve it?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.i(TAG,"yes");
							retrieveSession();
						}
					})
					.setNegativeButton("No, but keep files", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.i(TAG,"no but keep");
						    prefsUserSession = getSharedPreferences(getString(R.string.userSession), MODE_PRIVATE);
						    // TODO complete -> what behaviour to adopt? clear sharedPrefs? removes files ?
						    ed.clear();
//							ed.putBoolean(getString(R.string.sessionActive), false);
							ed.commit();
						}
					})
					.setNeutralButton("No, but erase files", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.i(TAG,"no and erase");
						    prefsUserSession = getSharedPreferences(getString(R.string.userSession), MODE_PRIVATE);
						    // TODO complete -> what behaviour to adopt? clear sharedPrefs? removes files ?
						    ed.clear();
//							ed.putBoolean(getString(R.string.sessionActive), false);
							ed.commit();
						}
					});
			return builder.create();
		}
		
		public void retrieveSession() {
			String mode = prefsUserSession.getString(getString(R.string.sessionMode), null);
			Intent intent;
//			if (mode.compareTo(ThumbRespeakSummaryLig.TAG) == 0) {
//				
//			} else if (mode.compareTo(RecordElicitation.TAG) == 0) {
//				
//			} else 
				if (mode.compareToIgnoreCase(CheckTranscription.TAG) == 0) {
				intent = new Intent(getActivity(), CheckTranscription.class);
			} else if (mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0) {
				intent = new Intent(getActivity(), CheckWordVariant.class);
			} else {
				Toast.makeText(getActivity(), "An error ocurred, the session could not be retrieved", Toast.LENGTH_LONG).show();
				return;
			}
			startActivity(intent);
		}
		
		public void setDialogDetails(LinearLayout ll) {
			// retrieve prefs values and store display them on popup menu
			
			/*
			 * *** convert internal mode/submode to display mode/submode following the rule: ***
			 * internal mode 			/ internal submode 	----------> display mode / display submode
			 * CheckWordVariant 		/ null				----------> Check mode   / CheckWordVariant
			 * CheckTranscription 		/ null				----------> Check mode   / CheckTranscription
			 * RecordElicitation 		/ text				----------> Elicitation  / text
			 * 			""		 		/ image				----------> 	""		 / image
			 * 			""		 		/ video 			----------> 	""		 / video
			 * ThumbRespeakActivityLig 	/ respeaking		----------> Respeaking	 / null
			 * 			""				/ translation		----------> Translation	 / null
			 */
			String mode = prefsUserSession.getString(getString(R.string.sessionMode), "undefined");
			String submode = prefsUserSession.getString(getString(R.string.session_submode), "undefined");
			String modeDialog = null, submodeDialog = null;
			if (mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0 || mode.compareTo(CheckTranscription.TAG) == 0) {
				modeDialog = "Check mode";
				submodeDialog = (mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0) ? CheckWordVariant.TAG : CheckTranscription.TAG;
			} else if (mode.compareToIgnoreCase(RecordElicitation.TAG) == 0) {
				modeDialog = "Elicitation";
				if (submode.compareToIgnoreCase("text") == 0) { submodeDialog = "text"; }
				else if (submode.compareToIgnoreCase("image") == 0) { submodeDialog = "image"; }
				else if (submode.compareToIgnoreCase("video") == 0) { submodeDialog = "video"; }
			} else if (mode.compareToIgnoreCase(ThumbRespeakActivityLig.TAG) == 0) {
				if (submode.compareToIgnoreCase("respeaking") == 0) { modeDialog = "Respeaking"; }
				else if (submode.compareToIgnoreCase("translation") == 0) { modeDialog = "Translation"; }
				submodeDialog = "None";
			}
		    ((TextView) ll.findViewById(R.id.session_mode)).setText(modeDialog);
		    ((TextView) ll.findViewById(R.id.session_submode)).setText(submodeDialog);
		    
		    // display date in a more convenient way
		    try {
		    	Date date = new SimpleDateFormat().parse(prefsUserSession.getString(getString(R.string.sessionDate), "undefined"));
		    	// TODO parsing fails... 
			    ((TextView) ll.findViewById(R.id.session_date)).setText(new SimpleDateFormat("dd/MM/yyyy at HH:mm",Locale.FRANCE).format(date));
		    } catch (ParseException e) {
		    	Log.e(TAG, ""+e);
			    ((TextView) ll.findViewById(R.id.session_date)).setText(prefsUserSession.getString(getString(R.string.sessionDate), "undefined"));
		    }
		    
		    ((TextView) ll.findViewById(R.id.session_progress)).setText("" + prefsUserSession.getInt(getString(R.string.sessionProgress), 0));
		    
		    // display only filename
		    String file = new File(prefsUserSession.getString(getString(R.string.sessionInputFile), "undefined")).getName();
		    ((TextView) ll.findViewById(R.id.session_input_file)).setText(file);
		}
	}

}
