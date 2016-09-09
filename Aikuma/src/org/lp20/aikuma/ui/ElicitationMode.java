package org.lp20.aikuma.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.MainActivity;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma2.R;

import com.musicg.wave.Wave;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ElicitationMode extends AikumaActivity{
	
	public static final String TAG = "ElicitationMode";
	public static final String ELICITATION = "elicitation";
	public static final String importFileName = "elicitationFileName";
	private File mPath;
	private String[] mFileList;
	private String mChosenFile;
	private String fileType;
	private String title;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.elicitation_mode);

		onImportClick();
	}
	
	public void onImportClick() {
		//switch (_view.getId()) {
		//case R.id.button_byText:
			Log.i(TAG, "Import text; view id: " + R.id.button_byText); //_view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".txt";
			title = "Import text file";
			importContent();
		/*	break;
		case R.id.button_byImage:
			Log.i(TAG, "Import image; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".jpg";
			title = "Import image file";
			Toast.makeText(this, "This feature is not yet available...", Toast.LENGTH_LONG).show();
//			importContent();
			break;
		case R.id.button_byVideo:
			Log.i(TAG, "Import video; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".avi";
			title = "Import video file";
			Toast.makeText(this, "This feature is not yet available...", Toast.LENGTH_LONG).show();
//			importContent();
			break;
		}*/
		
	}
	
	private void importContent() {
		loadFileList(mPath,fileType);
		showAudioFilebrowserDialog();
	}

	/**
	 * Loads the list of files in the specified directory into mFileList
	 * (copied from RespeakingSelection.java)
	 *
	 * @param	dir	The directory to scan.
	 * @param	fileType	The type of file (other than directories) to look
	 * for.
	 */
	private void loadFileList(File dir, final String fileType) {
		if(dir.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(fileType) || sel.isDirectory();
				}
			};
			mFileList = mPath.list(filter);
		}
		else {
			mFileList= new String[0];
		}
	}

	/**
	 * Presents the dialog for choosing audio files to the user.
	 * Copied from RespeakingSelection.java
	 */
	private void showAudioFilebrowserDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		FilebrowserDialogFragment fbdf = new FilebrowserDialogFragment();
		fbdf.show(ft, "dialog");
	}

	/**
	 * Used to display audio files that the user can choose to load from.
	 * Copied from RespeakingSelection.java
	 */
	public class FilebrowserDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Dialog dialog = null;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(title);
			if(mFileList == null) {
				Log.e(TAG, "import file - Showing file picker before loading the file list");
				dialog = builder.create();
				return dialog;
			}
			builder.setItems(mFileList, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mChosenFile = mFileList[which];
					Log.i(TAG, "mChosenFile: " + mChosenFile);
					mPath = new File(mPath, mChosenFile);
					if (mPath.isDirectory()) {
						loadFileList(mPath, fileType);
						showAudioFilebrowserDialog();
					} else {
						if (fileType==".txt") {
							Toast.makeText(ElicitationMode.this, "Selected text: " + mPath, Toast.LENGTH_LONG).show();
						}
						if (fileType==".jpg") {
							Toast.makeText(ElicitationMode.this, "Selected image: " + mPath, Toast.LENGTH_LONG).show();
						}
						if (fileType==".avi") {
							Toast.makeText(ElicitationMode.this, "Selected video: " + mPath, Toast.LENGTH_LONG).show();
						}

						Intent intent = new Intent(getActivity(), RecordingMetadataLig.class);						
						intent.putExtra(importFileName, mPath.getAbsolutePath());	
						Log.i(TAG, "selected file: " + mPath.getAbsolutePath());
						intent.putExtra(ELICITATION, true);
						startActivity(intent);
							
					}
				}
			});
			dialog = builder.show();
			return dialog;
		}
	}

	public void onBackPressed(View v) {
		this.finish();
	}

}
