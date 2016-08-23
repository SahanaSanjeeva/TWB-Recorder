package org.lp20.aikuma.ui;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.lp20.aikuma.audio.SegmentPlayer;
import org.lp20.aikuma.audio.SimplePlayer;
import org.lp20.aikuma.audio.record.Recorder;
import org.lp20.aikuma.audio.record.ThumbRespeaker;
import org.lp20.aikuma.audio.record.Microphone.MicException;
import org.lp20.aikuma.model.Recording;
import org.lp20.aikuma.model.Segments;
import org.lp20.aikuma.model.Segments.Segment;
import org.lp20.aikuma.model.WaveFile;
import org.lp20.aikuma.util.FileIO;
import org.lp20.aikuma2.R;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ThumbRespeakSummaryLig extends AikumaActivity implements OnClickListener {
	
	public static final String TAG = "ThumbRespeakSummaryLig";
	private final String tempRspk = FileIO.getAppRootPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
	private final String tempSeg = FileIO.getAppRootPath().getAbsolutePath() + "/recordings/temp_seg.wav";
	private final int okButtonId = 5;
	public int idCurrentPlayer;
	
	private ThumbRespeaker respeaker;
	private Segments segments;
	private Segments tempSegments;
	private Recorder recorder;
	private boolean recording = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.thumb_respeak_summary_lig);
	    respeaker = ThumbRespeaker.getThumbRespeaker();
	    segments = respeaker.getMapper().getSegments();
	    tempSegments = new Segments();
	    Iterator<Segment> it = segments.getOriginalSegmentIterator();
	    while (it.hasNext()) {
	    	Segment s = it.next();
	    	Segment origSeg = new Segment(s.getStartSample(), s.getEndSample());
	    	Segment rspkSeg = new Segment(segments.getRespeakingSegment(origSeg).getStartSample(),
	    			segments.getRespeakingSegment(origSeg).getEndSample());
	    	tempSegments.put(origSeg,rspkSeg);
	    }
	    setSummaryView();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
	    // TODO make the copy in a working thread
	    try {
			FileUtils.copyFile(new File (respeaker.getRecorder().getWriter().getFullFileName()), 
				new File(tempRspk));
		} catch (IOException e) {
			Log.e(TAG, "Could not initiate (copy) the respeaking file");
			
		}
//	    Thread t = new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//	    t.start();
	    
		int i=0;
		for (Segment origSeg : tempSegments.getSegmentMap().keySet()) {
//			LinearLayout l_orig_seg = (LinearLayout) findViewById((i+1)*1000);
			ListenFragment playerOrigFragment = (ListenFragment) getFragmentManager().findFragmentById((i+1)*1000);
			ListenFragment playerRspkFragment = (ListenFragment) getFragmentManager().findFragmentById((i+1)*1000+1);
			Segment rspkSeg = segments.getRespeakingSegment(origSeg);
			Log.i(TAG, "segment " + i + ": (orig) listenFragment #" + (i+1)*1000);
			Log.i(TAG, "segment (orig): " + origSeg);
			Log.i(TAG, "segment " + i + ": (rspk) listenFragment #" + (i+1)*1000);
			Log.i(TAG, "segment (rspk): " + rspkSeg);

			try {
				SegmentPlayer origPlayer = new SegmentPlayer(respeaker.getSimplePlayer(),origSeg, true, this);
				Log.i(TAG, "orig player: " + origPlayer);
				playerOrigFragment.setPlayer(origPlayer);
				SegmentPlayer rspkPlayer = new SegmentPlayer(
						new File(tempRspk),
						respeaker.getRecorder().getWriter().getSampleRate(),
						true, rspkSeg, this);
//				rspkPlayer.setSegment(rspkSeg);
				Log.i(TAG, "rspk player: " + rspkPlayer);
				playerRspkFragment.setPlayer(rspkPlayer);
			} catch (IOException e) {
				Log.e(TAG, "Could not initiate segment because of IOException: " + e);
			}
			i++;
		}
	}
	
	private void setSummaryView() {
		LayoutInflater inflater = (LayoutInflater)
				this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout l_activity = (LinearLayout) findViewById(R.id.thumb_respeak_summary_layout);
		l_activity.setGravity(Gravity.CENTER_HORIZONTAL);
//		l_activity.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

		int i=0;
		for (Segment origSeg : segments.getSegmentMap().keySet()) {
			Segment rspkSeg = segments.getRespeakingSegment(origSeg);
			
			LinearLayout l_segment = new LinearLayout(this);
			l_segment.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			l_segment.setOrientation(LinearLayout.HORIZONTAL);
			l_activity.addView(l_segment);
			
			LinearLayout l_seg_orig = new LinearLayout(this);
			l_seg_orig.setLayoutParams(l_segment.getLayoutParams());
			l_seg_orig.setOrientation(LinearLayout.VERTICAL);
			l_seg_orig.setId((i+1)*1000);
			l_segment.addView(l_seg_orig);
			TextView tvSegment = new TextView(this);
			tvSegment.setText("Segment " + i);
			l_seg_orig.addView(tvSegment);
			ListenFragment playerOrigFragment = new ListenFragment();
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(l_seg_orig.getId(), playerOrigFragment);
			ft.commit();
			
			LinearLayout l_seg_rspk = new LinearLayout(this);
			l_seg_rspk.setLayoutParams(l_segment.getLayoutParams());
			l_seg_rspk.setOrientation(LinearLayout.VERTICAL);
			l_segment.addView(l_seg_rspk);
			tvSegment = new TextView(this);
			tvSegment.setText("Segment " + i);
			l_seg_rspk.addView(tvSegment);
			LinearLayout l_rspk_player = new LinearLayout(this);
			l_rspk_player.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			l_rspk_player.setOrientation(LinearLayout.HORIZONTAL);
			l_rspk_player.setId((i+1)*1000+1);
			l_seg_rspk.addView(l_rspk_player);
			ListenFragment playerRspkFragment = new ListenFragment();
			ft = getFragmentManager().beginTransaction();
			ft.add(l_rspk_player.getId(), playerRspkFragment);
			ft.commit();
			ImageButton recBtn = new ImageButton(this);
			recBtn.setImageResource(R.drawable.record);
			recBtn.setOnClickListener(this);
			recBtn.setId(10+i);
			l_rspk_player.addView(recBtn);
			ImageButton cancelBtn = new ImageButton(this);
			cancelBtn.setImageResource(R.drawable.cancel_32);
			cancelBtn.setOnClickListener(this);
			cancelBtn.setId(100+i);
			l_rspk_player.addView(cancelBtn);

			i++;
		}
		ImageButton okButton = new ImageButton(this);
		okButton.setImageResource(R.drawable.ok_32);
		okButton.setOnClickListener(this);
		okButton.setId(okButtonId);
		l_activity.addView(okButton);
	}

	@Override
	public void onClick(View v) {
//		Toast.makeText(this, "Record Button activates", Toast.LENGTH_SHORT).show();
		Log.i(TAG, "id: " + v.getId());
		if (v.getId() == okButtonId)  {
			try {
				// update wave file
				FileUtils.copyFile(new File(tempRspk), new File(respeaker.getRecorder().getWriter().getFullFileName()));
				Log.i(TAG, "Move temp file " + tempRspk 
						+ " to respeaking file " + respeaker.getRecorder().getWriter().getFullFileName());
				// update mapper
				Log.i(TAG, "Updated the mapper file");
				Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
				Segments segments = respeaker.getMapper().getSegments();
				while (it.hasNext()) {
					Segment origSeg = it.next();
					segments.put(origSeg, tempSegments.getRespeakingSegment(origSeg));
					Log.i(TAG, "Orig segment '" + origSeg 
							+ ") -> respeaking segment (" + segments.getRespeakingSegment(origSeg) + ")");
				}
				segments.write(respeaker.getMapper().getMapFile());
				setResult(RESULT_OK);
			} catch (IOException e) {
				Log.e(TAG, "IOException: might have failed to update the respeaking file with the temporary one: " + e);
				setResult(RESULT_CANCELED);
			}
			try {
				FileIO.delete(new File(tempRspk));
				FileIO.delete(new File(tempSeg));
				FileIO.delete(new File(tempSeg.replace(".wav", "-preview.wav")));
			} catch (IOException e) {
				Log.e(TAG, "IOException: might be because of a file to be deleted didn't exist: " + e);
			}
			ThumbRespeakSummaryLig.this.finish();
		} else if (v.getId() >= 100) {		// cancel button
			// cancel button
			int i = v.getId() - 100;
			Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
			while (it.hasNext() && i > 0) {
				it.next();
				i--;
			}
			Segment origSeg = it.next();
			Segment rspkSeg = tempSegments.getRespeakingSegment(origSeg);
			try {
				long delta = segments.getRespeakingSegment(origSeg).getDuration() 
						- tempSegments.getRespeakingSegment(origSeg).getDuration();
//				long delta = recorder.getCurrentSample() - rspkSeg.getDuration();
				updateFile(origSeg,delta,false);
				updateSegmentList(v.getId()-100,false);
				updateView(v.getId(), origSeg, false);
				Toast.makeText(this, "Back to the initial respoken segment...", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Log.e(TAG, "Error when canceling the segment");
				Toast.makeText(this, "Problem when cancelling the segment", Toast.LENGTH_SHORT).show();
			}
		} else if (v.getId() >= 10 && v.getId() < 100) {		// record button
			if (!recording) {
				try {
					FileIO.delete(new File(tempSeg));
				} catch (IOException e) { }
				try {
					File f = new File(tempSeg);
					recorder = new Recorder(0, new File(tempSeg), 
							respeaker.getRecorder().getWriter().getSampleRate());
					recorder.listen();
					((ImageButton) v).setImageResource(R.drawable.pause);
					recording = true;
					int i = v.getId() - 10;
					ImageButton cancelBtn = (ImageButton) findViewById(100+i);
					cancelBtn.setEnabled(false);
					idCurrentPlayer = (i+1)*1000+1;
					Toast.makeText(this, "Recording...", Toast.LENGTH_LONG).show();
				} catch (MicException e) {
					this.finish();
					Toast.makeText(getApplicationContext(),
							"Error setting up microphone.",
							Toast.LENGTH_LONG).show();
				}
			} else {
				try {
					recorder.pause();
					recorder.stop();
					recording = false;
					((ImageButton) v).setImageResource(R.drawable.record);
					int i = v.getId() - 10;
					ImageButton cancelBtn = (ImageButton) findViewById(100+i);
					cancelBtn.setEnabled(true);
					// update segment list
					Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
					while (it.hasNext() && i > 0) {
						it.next();
						i--;
					}
					Segment origSeg = it.next();
					Segment rspkSeg = tempSegments.getRespeakingSegment(origSeg);
					// save and update temp respeaking file
					long delta = recorder.getCurrentSample() - rspkSeg.getDuration();
					updateFile(origSeg,delta,true);
					// update segment list
					updateSegmentList(v.getId()-10,true);
					// update view
					updateView(v.getId(), origSeg, true);
				} catch (MicException e) {
					Log.e(TAG, "Problemn with the recorder, unable to stop it");
					Toast.makeText(this, "Problem with microphone: unable to stop it", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Log.e(TAG, "Exception when saving the new segment: " + e);
					Toast.makeText(this, "Problem when saving the new segment", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	private long updateSegmentList(int index, boolean update) {
		// if boolean == true -> edit (add); else -> cancel (remove)
		for (Segment s : tempSegments.getSegmentMap().keySet()) {
			Log.i(TAG, "Orig seg: " + s + "; rspk seg: " + tempSegments.getRespeakingSegment(s));
		}
		Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
		while (it.hasNext() && index > 0) {
			it.next();
			index--;
		}
		Segment origSeg = it.next();
		Segment rspkSeg = tempSegments.getRespeakingSegment(origSeg);
		long delta;
		if (update) {
			tempSegments.put(origSeg, new Segment(rspkSeg.getStartSample(), 
					rspkSeg.getStartSample() + recorder.getCurrentSample()));
			Log.i(TAG, "Recorder duration (sample): " + recorder.getCurrentSample());
			delta = recorder.getCurrentSample() - rspkSeg.getDuration();
		} else {
			tempSegments.put(origSeg, new Segment(rspkSeg.getStartSample(), 
					rspkSeg.getStartSample() + segments.getRespeakingSegment(origSeg).getDuration()));
			Log.i(TAG, "Back to respeaking; segment duration: " + segments.getRespeakingSegment(origSeg).getDuration());
			delta = segments.getRespeakingSegment(origSeg).getDuration() - rspkSeg.getDuration();
		}
		Log.i(TAG, "delta: " + delta);
		while (it.hasNext()) {
			Segment orig = it.next();
			Segment rspk = tempSegments.getRespeakingSegment(orig);
			tempSegments.put(orig, new Segment(rspk.getStartSample() + delta, rspk.getEndSample() + delta));
		}
		for (Segment s : tempSegments.getSegmentMap().keySet()) {
			Log.i(TAG, "Orig seg: " + s + "; rspk seg: " + tempSegments.getRespeakingSegment(s));
		}
		return delta;
	}
	
	private void updateFile(Segment origSeg, long delta, boolean update) throws IOException {
		// if update -> edit (add) segment; else -> remove (cancel) segment
		byte[] byteSegment;
		if (update) {
			RandomAccessFile f = new RandomAccessFile(new File(tempSeg), "r");
			byteSegment = new byte[(int)f.length()-44];		// skip header and keep only raw data
			f.read(byteSegment, 44, byteSegment.length-44);
			f.close();
		} else {
			Segment rspkSeg = segments.getRespeakingSegment(origSeg);
			RandomAccessFile f = new RandomAccessFile(new File(respeaker.getRecorder().getWriter().getFullFileName()), "r");
			f.seek(rspkSeg.getStartSample().intValue()*2+44);
			byteSegment = new byte[rspkSeg.getDuration().intValue()*2]; // skip header, keep only raw data
			f.read(byteSegment, 0, byteSegment.length);
			f.close();
		}
		byte[] byteRspkUpdt = new byte[(int)(new File(tempRspk).length()) + (int)delta*2];
		Log.i(TAG, "delta: " + delta);
		Log.i(TAG, "byteRspkUpdt size: " + byteRspkUpdt.length);
		RandomAccessFile f = new RandomAccessFile(tempRspk, "r");
		int startSeg = tempSegments.getRespeakingSegment(origSeg).getStartSample().intValue()*2;
		int endSeg = tempSegments.getRespeakingSegment(origSeg).getEndSample().intValue()*2;
		f.read(byteRspkUpdt, 0, startSeg+44);
		System.arraycopy(byteSegment, 0, byteRspkUpdt, 44+startSeg, byteSegment.length);
		f.seek(44+endSeg);
		f.read(byteRspkUpdt,44 + startSeg + byteSegment.length, (int)f.length() - 44 - endSeg);
		f.close();
		for (int j=4; j<8; j++) {
			byteRspkUpdt[j] = (byte) (byteRspkUpdt.length-44+36 >> (j-4)*8);
			byteRspkUpdt[j+36] = (byte) (byteRspkUpdt.length-44 >> (j-4)*8);
		}
		FileUtils.writeByteArrayToFile(new File(tempRspk), byteRspkUpdt);
	}
	
	private void updateView(int index, Segment origSeg, boolean update) throws IOException {
		int i=1;
		ListenFragment frag = (ListenFragment) getFragmentManager().findFragmentById(i*1000+1);
		Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
		while (frag != null && it.hasNext()) {
			SegmentPlayer rspkPlayer = (SegmentPlayer)frag.getPlayer();
			rspkPlayer.release();
			Segment rspkSeg = tempSegments.getRespeakingSegment(it.next());
			rspkPlayer = new SegmentPlayer(new File(tempRspk),
					respeaker.getRecorder().getWriter().getSampleRate(),
					true, rspkSeg, this);
			frag.setPlayer(rspkPlayer);
			i++;
			frag = (ListenFragment) getFragmentManager().findFragmentById(i*1000+1);
		}
	}
	
	public void pauseFragment(Segment seg) {
		Iterator<Segment> it = tempSegments.getOriginalSegmentIterator();
		int i=0;
		if (tempSegments.getSegmentMap().containsKey(seg)) {			// segment is original
			while (it.hasNext() && !it.next().equals(seg)) {	i++; }
		} else if (tempSegments.getSegmentMap().containsValue(seg)) {	// segment is respoken
			while (it.hasNext() && !(tempSegments.getRespeakingSegment(it.next())).equals(seg)) { i++; }
		}
		final int j = i;
		final boolean original = tempSegments.getSegmentMap().containsKey(seg) ? true : false;
		Log.i(TAG, "id fragment player: " + j + "original: " + original);
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				int shift = original ? 0 : 1;		
				Log.i(TAG, "j: " + j + "; shift: " + shift + "; fragmen id: " + (j+1)*1000+shift);
				((ListenFragment) ThumbRespeakSummaryLig.this.getFragmentManager().findFragmentById((j+1)*1000+shift)).onMarkedReachedListener();
			}
		});
	}
	
}
