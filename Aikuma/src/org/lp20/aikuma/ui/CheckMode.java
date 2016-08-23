package org.lp20.aikuma.ui;

import java.io.File;
import java.io.FilenameFilter;

import org.lp20.aikuma2.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CheckMode extends AikumaActivity{
	public static final String TAG = "CheckMode";
	public static final String importFileName = "checkFileName";
	private File mPath;
	private String[] mFileList;
	private String mChosenFile;
	private String fileType;
	private String title;
	private boolean wordvariant;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_mode);
	    wordvariant = false;
	    Log.i(TAG, "boolean value : "+wordvariant);
	}
	
	public void onImportClick(View _view) {
		switch (_view.getId()) {
		case R.id.button_checkvariant:
			wordvariant = true;
			Log.i(TAG, "Import word with variant file; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".txt";
			title = "Import word with variant text file";
			importContent();
			break;
		case R.id.button_checktranscript:
			wordvariant = false;
			Log.i(TAG, "Import transcription file; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".txt";
			title = "Import transcription file";
			importContent();
			break;
		}
		
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
						/*
						 * TODO
						 */
						Intent intent;
						Log.i(TAG, "boolean value : "+wordvariant);
						if (wordvariant) {
							Log.i(TAG, "Go to CheckWordVariant.class");
							intent = new Intent(getActivity(), CheckWordVariant.class);
							intent.putExtra(importFileName, mPath.getAbsolutePath());	
							Log.i(TAG, "selected file: " + mPath.getAbsolutePath());
							startActivity(intent);
						} else {
							Log.i(TAG, "Go to CheckTranscription.class");
							intent = new Intent(getActivity(), CheckTranscription.class);
							intent.putExtra(importFileName, mPath.getAbsolutePath());	
							Log.i(TAG, "selected file: " + mPath.getAbsolutePath());
							startActivity(intent);
						}
					}
				}
			});
			dialog = builder.show();
			return dialog;
		}
	}

	public void onBackPressed(View v) {
		Log.i(TAG, "boolean value : "+wordvariant);
		this.finish();
	}

}
