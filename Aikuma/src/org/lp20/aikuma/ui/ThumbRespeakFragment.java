/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.lp20.aikuma.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.lp20.aikuma.Aikuma;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.Segments;
import org.lp20.aikuma.util.AikumaSettings;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma.audio.Player;
import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.audio.record.Mapper;
import org.lp20.aikuma.audio.record.PCMWriter;
import org.lp20.aikuma.audio.record.ThumbRespeaker;
import org.lp20.aikuma.audio.record.Microphone.MicException;
import org.lp20.aikuma2.R;

import com.musicg.experiment.math.cluster.Segment;
import com.musicg.wave.Wave;

/**
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class ThumbRespeakFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.thumb_respeak_fragment, container, false);
		installButtonBehaviour(v);
		seekBar = (InterleavedSeekBar) v.findViewById(R.id.InterleavedSeekBar);
		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					int originalProgress;
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (fromUser) {
							seekBar.setProgress(originalProgress);
						}
					}
					public void onStopTrackingTouch(SeekBar _seekBar) {};
					public void onStartTrackingTouch(SeekBar _seekBar) {
						originalProgress = seekBar.getProgress();
					};
				});
		seekBar.invalidate();

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		listenFragment = new ListenFragment();
		ft.replace(R.id.respeak_latest_segment_player, listenFragment);
		ft.commit();
		
		return v;
	}

	/**
	 * Called when the fragment is destroyed; ensures resources are
	 * appropriately released.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (respeaker != null) {
			//If you hit the stop button really quickly, the player may not
			//have been initialized fully.
			respeaker.release();
		}
		
		String filename = FileIO.getOwnerPath().getAbsolutePath() +
				"/recordings/" + "temp.rspk.wav";
		if (listenFragment.getPlayer() != null) {
			listenFragment.getPlayer().release();
		}
		try {
			FileIO.delete(new File(filename));
			Log.i(TAG, "Deleting " + filename);
		} catch (IOException e) {
			Log.e(TAG, "failed to delete " + filename + " - maybe the file didn't exist");
		}
	}

	// Implements the behaviour for the play and respeak buttons.
	private void installButtonBehaviour(View v) {
		final ImageButton okButton = (ImageButton) 
				v.findViewById(R.id.saveButton);
		okButton.setImageResource(R.drawable.ok_disabled_48);
		okButton.setEnabled(false);
		
		final ImageButton playButton = (ImageButton)
				v.findViewById(R.id.PlayButton);
		final ImageButton respeakButton = (ImageButton)
				v.findViewById(R.id.RespeakButton);
		final int greyColor = 0xffd6d6d6;
		playButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if(count > 0) {
						respeaker.saveRespeaking();
					}
					
					// Color change
					//playButton.setBackgroundColor(0xff00d500);
					long previousGestureTime = gestureTime;
					gestureTime = System.currentTimeMillis();
					gestureTimeUpToDown = System.currentTimeMillis() - gestureTimeUpToDown;
					
					if(isCommented){ // After commentary is recorded(or At the start)
						//Rewind and Store the start-point
						respeaker.playOriginal(1); 
					} else { // After commentary is recorded(or At the start) and 
							 // green-arrow button is pressed more than once
						if(previousGestureTime < VALID_GESTURE_TIME) {
							respeaker.playOriginal(2); //Rewind
						} else if(gestureTimeUpToDown < VALID_GESTURE_TIME) {
							respeaker.playOriginal(0); //Continue
						} else {
							respeaker.playOriginal(1); //Rewind and Store the start-point
							isCommented = true;
						}
					}
					
					seekBarThread = new Thread(new Runnable() {
							public void run() {
								int currentPosition;
								while (true) {
									currentPosition =
											respeaker.getSimplePlayer().getCurrentMsec();
									seekBar.setProgress(
											(int)(((float)currentPosition/(float)
											respeaker.getSimplePlayer().getDurationMsec())*100));
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										return;
									}
								}
							}
					});
					seekBarThread.start();
				}
				if (event.getAction() == MotionEvent.ACTION_UP) {
					//playButton.setBackgroundColor(greyColor);
					respeaker.pauseOriginal();
					stopThread(seekBarThread);
					gestureTime = System.currentTimeMillis() - gestureTime;
					gestureTimeUpToDown = System.currentTimeMillis();
					Log.i("Thumb", ""+ gestureTime);
					if(gestureTime >= VALID_GESTURE_TIME) {
						isCommented = false;
					}
				}
				return false;
			}
		});

		respeakButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					//respeakButton.setBackgroundColor(0xffff2020);
					respeaker.pauseOriginal();
					respeaker.recordRespeaking();
					count++;
					
					String filename = FileIO.getOwnerPath().getAbsolutePath() +
							"/recordings/" + "temp.rspk.wav";
					if (listenFragment.getPlayer() != null) {
						listenFragment.getPlayer().release();
					}
					try {
						FileIO.delete(new File(filename));
						Log.i(TAG, "Deleting " + filename);
					} catch (IOException e) {
						Log.e(TAG, "failed to delete " + filename + " - maybe the file didn't exist");
					}
				}
				if (event.getAction() == MotionEvent.ACTION_UP) {
					Log.i("ThumbRespeak", "sleep: " + System.currentTimeMillis());
					try {
						Thread.sleep(AikumaSettings.EXTRA_AUDIO_DURATION);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						Log.e("ThumbRespeak", "sleep");
					}
					Log.i("ThumbRespeak", "sleep: " + System.currentTimeMillis());
					
					if(!okButton.isEnabled()) {
						okButton.setImageResource(R.drawable.ok_48);
						okButton.setEnabled(true);
					}
					//respeakButton.setBackgroundColor(greyColor);
					try {
						respeaker.pauseRespeaking();
						isCommented = true;
					} catch (MicException e) {
						ThumbRespeakFragment.this.getActivity().finish();
					}
					

					// save the latest segment in a temporary file 
					String filename = FileIO.getOwnerPath().getAbsolutePath() +
							"/recordings/" + "temp.rspk.wav";
					int rate = respeaker.getRecorder().getMicrophone().getSampleRate();
					PCMWriter file = PCMWriter.getInstance(rate, 
							respeaker.getRecorder().getMicrophone().getChannelConfiguration(), 
							respeaker.getRecorder().getMicrophone().getAudioFormat());
					file.prepare(filename);
					int bufferLength = respeaker.getRecorder().getAudioBufferLength();
					file.write(respeaker.getRecorder().getAudioBuffer(),bufferLength);
					file.close();
					Log.i(TAG, "Temporary segment saved into file " + filename);
					
					// TODO update the player with the latest segment
					try {
						SimplePlayer player = new SimplePlayer(new File(filename), 
								rate, true);
						Log.i(TAG, "New SimplePlater of" + filename + " (rate: " + rate + ")");
						listenFragment.setPlayer(player);
					} catch (IOException e) {
						Log.e(TAG, e.toString());
					} catch (NoSuchElementException e) {
						Log.e(TAG, e.toString());
					}
					
				}
				return false;
			}
		});
	}

	// Wrapper to more safely stop threads.
	private void stopThread(Thread thread) {
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * Recording mutator.
	 *
	 * @param	recording	The recording to be played.
	 */
	public void setRecording(Recording recording) {
		this.recording = recording;
	}

	/**
	 * sample rate mutator.
	 *
	 * @param	sampleRate	The sample rate of the recording.
	 */
	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * UUID mutator.
	 *
	 * @param	uuid	The uuid of the recording.
	 */
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * ThumbRespeaker mutator.
	 *
	 * @param	respeaker	The ThumbRespeaker to use.
	 */
	public void setThumbRespeaker(ThumbRespeaker respeaker) {
		this.respeaker = respeaker;
		respeaker.getSimplePlayer().setOnCompletionListener(onCompletionListener);
	}

	private Player.OnCompletionListener onCompletionListener =
			new Player.OnCompletionListener() {
				public void onCompletion(Player _player) {
					stopThread(seekBarThread);
					seekBar.setProgress(seekBar.getMax());
				}
			};
	private ThumbRespeaker respeaker;
	private ImageButton playButton;
	private ImageButton respeakButton;
	private InterleavedSeekBar seekBar;
	private Thread seekBarThread;
	private Recording recording;
	private UUID uuid;
	private int sampleRate;
	private ListenFragment listenFragment;
	
	private final int VALID_GESTURE_TIME = 250; //0.25sec
	private long gestureTime = VALID_GESTURE_TIME;
	private long gestureTimeUpToDown = VALID_GESTURE_TIME;
	private boolean isCommented = true;
	private int count = 0;
	
	public static final String TAG = "ThumbRespeakFragment";
}
