package com.example.myfirstapp;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

	/*
	 * public MediaRecorder mRecorder;
	 * 
	 * @Override protected void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState);
	 * setContentView(R.layout.activity_main);
	 * 
	 * 
	 * }
	 * 
	 * @Override protected void onStart(){ mRecorder = new MediaRecorder();
	 * mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	 * mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	 * mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	 * mRecorder.setOutputFile("/dev/null"); try { mRecorder.prepare(); } catch
	 * (IllegalStateException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } mRecorder.start(); }
	 * 
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.activity_main, menu); return true; }
	 * 
	 * public double getAmplitude() { if (mRecorder != null) return
	 * (mRecorder.getMaxAmplitude()); else return 0;
	 * 
	 * }
	 */
	private static final String LOG_TAG = "AudioRecordTest";
	private static String mFileName = null;

	private RecordButton mRecordButton = null;
	private MediaRecorder mRecorder = null;

	private PlayButton mPlayButton = null;
	private MediaPlayer mPlayer = null;

	private void onRecord(boolean start) {
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(mFileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
	}

	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mFileName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
			e.printStackTrace();
		}

		mRecorder.start();
		printAmplitude();
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}

	class RecordButton extends Button {
		boolean mStartRecording = true;

		OnClickListener clicker = new OnClickListener() {
			int i = 0;
			int maxVol = Integer.MIN_VALUE;
			int minVol = Integer.MAX_VALUE;
			
			int beats = 0;
			long startTime;
			
			
			public void reset(){
				i = 0;
				beats = 0;
				maxVol = Integer.MIN_VALUE;
				minVol = Integer.MAX_VALUE;
				startTime = SystemClock.elapsedRealtime();
			}


			Handler mUpdater = new Handler();

			Runnable mUpdateView = new Runnable() {
				
				boolean beatPrevious = false;
				double heartRate = -1;
				
				//Loop called every sampleRate milliseconds
				int sampleRate = 50;
				
				@Override
				public void run() {
					int volume = getAmplitude();
					String prefix = "Min vol: " + minVol + " Max vol: "
							+ maxVol + " Volume: " + volume;
					if (i <= 2 * 1000 / sampleRate && i > 1) {
						if (volume > maxVol)
							maxVol = volume;
						if (volume < minVol)
							minVol = volume;

						setText(prefix);
					}
					else if(volume > (maxVol-minVol)*-0.2 + (maxVol + minVol)*0.5 ){
						if(! beatPrevious){
							beats ++;
							int beatsCounted = 6;
							if(beats % beatsCounted == 0){
								long timeNow = SystemClock.elapsedRealtime();
								long period = timeNow - startTime;
								startTime = timeNow;
								// (5 beats / x msec) * (1000 msec /sec) * (60 sec / min)
								heartRate = beatsCounted * 1000.0 * 60  / period;
							}
						}
						prefix = "Bumb " + prefix;
						beatPrevious = true;
					}
					else{
						beatPrevious = false;
					}
					prefix = "Rate:" + Math.round(heartRate) + "Beat: " + beats + prefix;
					setText(prefix);
					mUpdater.postDelayed(this, sampleRate);
					i++;
				}
			};

			public void onClick(View v) {
				if (mStartRecording) {
					setText("Stop recording");
					reset();
					mUpdateView.run();
				} else {
					printAmplitude();
					mUpdater.removeCallbacksAndMessages(null);
					setText("Start recording");
				}
				onRecord(mStartRecording);
				mStartRecording = !mStartRecording;
			}
		};

		public RecordButton(Context ctx) {
			super(ctx);
			setText("Start recording");
			setOnClickListener(clicker);
		}
	}

	class PlayButton extends Button {
		boolean mStartPlaying = true;

		OnClickListener clicker = new OnClickListener() {
			public void onClick(View v) {

				if (mStartPlaying) {

					setText("Stop playing");
				} else {
					printAmplitude();
					setText("Start playing");
				}
				onPlay(mStartPlaying);
				mStartPlaying = !mStartPlaying;
			}
		};

		public PlayButton(Context ctx) {
			super(ctx);
			setText("Start playing");
			setOnClickListener(clicker);
		}
	}

	public MainActivity() {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		mFileName += "/audiorecordtest.3gp";
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		LinearLayout ll = new LinearLayout(this);
		mRecordButton = new RecordButton(this);
		ll.addView(mRecordButton, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		mPlayButton = new PlayButton(this);
		ll.addView(mPlayButton, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		setContentView(ll);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void printAmplitude() {
		Log.d("Stuffy Stuffy", getAmplitudeString());
	}

	private String getAmplitudeString() {
		return Integer.toString(getAmplitude());
	}

	public int getAmplitude() {
		if (mRecorder != null) {
			return mRecorder.getMaxAmplitude();
		} else {
			return -1;
		}
	}
}
