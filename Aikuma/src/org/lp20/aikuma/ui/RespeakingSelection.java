package org.lp20.aikuma.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.ModeSelection;
import org.lp20.aikuma.MainActivity.FilebrowserDialogFragment;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import com.musicg.wave.Wave;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class RespeakingSelection extends AikumaActivity {
	
	private boolean translateMode = false;
	public static final String TAG = "RespeakingSelection";
	public static final String FILE_TYPE = ".wav";
	public static final String RESPEAK = "respeak";
	private File mPath;
	private String[] mFileList;
	private String mChosenFile;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.respeaking_selection);
	    translateMode = getIntent().getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);
	    if (translateMode) {
	    	((TextView) findViewById(R.id.respeaking_title)).setText(R.string.translatemode);
	    }
	}
	
	public void onImportAudioClick(View _view) {
		if (_view.getId() == R.id.btn_importFromAikuma) {
			mPath = new File(FileIO.getAppRootPath(),
					"/recordings");
		} else if (_view.getId() == R.id.btn_importFromPhone) {
			mPath = Environment.getExternalStorageDirectory();
		}
		audioImport(null);
	}

	/**
	 * Called when the import button is pressed; starts the import process.
	 *
	 * @param	_view	the audio import button.
	 */
	public void audioImport(View _view) {
		loadFileList(mPath, FILE_TYPE);
		showAudioFilebrowserDialog();
	}

	/**
	 * Loads the list of files in the specified directory into mFileList
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
	 */
	private void showAudioFilebrowserDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		FilebrowserDialogFragment fbdf = new FilebrowserDialogFragment();
		fbdf.show(ft, "dialog");
	}

	/**
	 * Used to display audio files that the user can choose to load from.
	 */
	public class FilebrowserDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Dialog dialog = null;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle("Import audio file");
			if(mFileList == null) {
				Log.e("importfile", "Showing file picker before loading the file list");
				dialog = builder.create();
				return dialog;
			}
			builder.setItems(mFileList, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mChosenFile = mFileList[which];
					Log.i("importfile", "mChosenFile: " + mChosenFile);
					mPath = new File(mPath, mChosenFile);
					if (mPath.isDirectory()) {
						loadFileList(mPath, ".wav");
						showAudioFilebrowserDialog();
					} else {
						SharedPreferences preferences =	PreferenceManager
								.getDefaultSharedPreferences(RespeakingSelection.this);
						File aikumaFile = mPath.getParentFile().getParentFile().getParentFile();
						Intent intent;

						if (aikumaFile != null && aikumaFile.getAbsolutePath().contains("aikuma")) {
							// if recording, send to RespeakingMetadataLig
							intent = new Intent(getActivity(), RespeakingMetadataLig.class);
							String recordName;
							try { recordName = mPath.getName().substring(0,mPath.getName().length()-4); }
							catch (Exception e) {recordName = mPath.getName(); }
							String dirName = mPath.getParentFile().getName();
							intent.putExtra(RecordActivityLig.intent_recordname, recordName);
							intent.putExtra("dirname", dirName);
							Log.i(TAG, "Respeaking on an existing aikuma file - original record name: "	+ recordName);
							Log.i(TAG, "Respeaking on an existing aikuma file - original record directory name: " + dirName);
							int rewind = preferences.getInt("respeaking_rewind", 500);
							intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
							intent.putExtra(RecordActivityLig.intent_rewindAmount, rewind);
//							intent.putExtra(RecordActivityLig.intent_sampleRate, (long) sampleRate);
							startActivity(intent);
						} else {
							UUID uuid = UUID.randomUUID();
							try {
								Wave wave = new Wave(
										new FileInputStream(mPath));
								String format = wave.getWaveHeader().getFormat();
								int sampleRate = wave.getWaveHeader().
										getSampleRate();
								int durationMsec = (int) wave.length() * 1000;
								int bitsPerSample = wave.getWaveHeader().
										getBitsPerSample();
								int numChannels = wave.getWaveHeader().
										getChannels();
								FileUtils.copyFile(mPath,
										new File(Recording.getNoSyncRecordingsPath(),
										uuid.toString() + ".wav"));
								FileUtils.copyFile(mPath,
										new File(Recording.getNoSyncRecordingsPath(),
										uuid.toString() + "-preview.wav"));
								Log.i(TAG, "Copying original recording file to: " + 
										Recording.getNoSyncRecordingsPath() + "/" + uuid.toString() + ".wav");
								intent = new Intent(getActivity(), RecordingMetadataLig.class);
								intent.putExtra(RESPEAK, true);
								intent.putExtra("tempRecUUID", uuid.toString());
								intent.putExtra("sampleRate", sampleRate);
								intent.putExtra("duration", durationMsec);
								intent.putExtra("format", format);
								intent.putExtra("numChannels", numChannels);
								intent.putExtra("bitspersample", bitsPerSample);
								intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
								startActivity(intent);
							} catch (FileNotFoundException e) {
								Toast.makeText(getActivity(),
										"Failed to import the recording.",
										Toast.LENGTH_LONG).show();
								Log.e(TAG, "Failed to import the file: " + e);
							} catch (IOException e) {
								Toast.makeText(getActivity(),
										"Failed to import the recording.",
										Toast.LENGTH_LONG).show();
								Log.e(TAG, "Failed to copy the file to the temporary directory: "
										+ e);
							}
						}
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
