package com.gbr.video;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.gbr.bbkdemo.R;
import com.gbr.bluetooth.BluetoothLeService;
import com.gbr.unity.Unity3DActivity;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlayActivity extends Activity {
	private static final String TAG="PlayActivity";
	Unity3DActivity activity;
	// 声音调节Toast

	// 音频管理器
	private AudioManager mAudioManager;
	// 屏幕宽高
	private float width;
	private float height;
	private  TextView mPlayTime;
	private TextView mDurationTime;
	private SeekBar seekBar;
	private int currentPosition = 0;
	private int currentPosition_l = 0;
	private boolean isPlaying;
	private int pos; // 记录传过来的值,来选择播放的视频
	// 三个surfaceview
	private SurfaceView surfaceView_l;
	private SurfaceView surfaceView_r;
	private SurfaceView surfaceView_all;
	// 切换按钮
	private Button button_vR;
	private MediaPlayer mplayer_l;
	private MediaPlayer mplayer_r;
	
	private ImageView play_imv;
	private boolean isVR = true;
	// 底部View
	private RelativeLayout mBottomView,title;
	// 控制屏幕大小
	private DisplayMetrics dm;
	// 屏幕的宽高
	private int srceenWidth;
	private int srceenHeight;
	// 自动隐藏顶部和底部View的时间
	private static final int HIDE_TIME = 5000;
	// 记录传过来的值,来选择播放的视频
	private String filePath;
	private ImageView mBack;
	private  LightController lightController;
	private  VolumeController volumeController;
	private int i;
	private Timer mTimer;
	private TimerTask mTask;
   // private Handler mHandler;
	// 设置接收蓝牙数据
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String data = intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA);
				if (data.length() == 2){
					if(data.equals("04")){
						forward(50);
					}
					if(data.equals("01")){
						backward(50);
					}
				}
			}
			if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.play_activity);
		activity = new Unity3DActivity();
		
		Bundle extras = getIntent().getExtras();
		filePath = extras.getString("filePath");
		
		//mHandler=new Handler();
		
		mPlayTime = (TextView) findViewById(R.id.play_time);
		mDurationTime = (TextView) findViewById(R.id.total_time);
		mBottomView = (RelativeLayout) findViewById(R.id.bottom_layout);
		title=(RelativeLayout) findViewById(R.id.title);
		seekBar = (SeekBar) findViewById(R.id.seekbar);

		mBack = (ImageView) findViewById(R.id.backA);
		button_vR=(Button) findViewById(R.id.button);
		play_imv = (ImageView) findViewById(R.id.play_btn);
		mBack.setOnClickListener(click);
        button_vR.setOnClickListener(click);
		play_imv.setOnClickListener(click);
		mTimer=new Timer();
		screenSizeInit(); // 得到屏幕的大小
		height = DensityUtil.getWidthInPx(this);
		width = DensityUtil.getHeightInPx(this);
		threshold = DensityUtil.dip2px(this, 18);
		
		mAudioManager=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
	    lightController=new LightController(this);
	    volumeController=new VolumeController(this);
		
		surfaceView_l = (SurfaceView) findViewById(R.id.surfaceView_l);
		surfaceView_r = (SurfaceView) findViewById(R.id.surfaceView_r);
		surfaceView_all = (SurfaceView) findViewById(R.id.surfaceView_all);
		halfScreen(surfaceView_l); // 左边半视频
		halfScreen(surfaceView_r); // 右边半视频
		fullScreen(surfaceView_all); // 全屏视频
		// MediaPlayer新建
		mplayer_l = new MediaPlayer();
		mplayer_r = new MediaPlayer();
	
		initSurfceView_all();
		initSurfaceView_l();
		initSurfaceView_r();
        seekBar.setOnSeekBarChangeListener(change);
     
	
	}
	private OnClickListener click=new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.backA:  //退出
				if (mTimer!=null) {
					mTimer.cancel();
					mTimer=null;
				}
				if (mTask!=null) {
					mTask.cancel();
					mTask=null;
				}

		    finish();
				break;
			case R.id.play_btn:             //暂停 播放
				if (mplayer_l.isPlaying()) {
					mplayer_l.pause();
					mplayer_r.pause();
					play_imv.setImageResource(R.drawable.video_btn_down);
				} else {
					mplayer_l.start();
					mplayer_r.start();
					play_imv.setImageResource(R.drawable.video_btn_on);
				}
				break;
			case R.id.button:                //切换模式
				if (!isVR) {
					surfaceView_l.setVisibility(SurfaceView.GONE);
					surfaceView_r.setVisibility(SurfaceView.GONE);
					surfaceView_all.setVisibility(SurfaceView.VISIBLE);
					isVR = !isVR;
				} else {
					surfaceView_all.setVisibility(SurfaceView.GONE);
					surfaceView_l.setVisibility(SurfaceView.VISIBLE);
					surfaceView_r.setVisibility(SurfaceView.VISIBLE);
					isVR = !isVR;
				}
				break;
			}
			
			
		}
	};
	private OnSeekBarChangeListener change=new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			int progress = seekBar.getProgress();
			if (mplayer_r != null && mplayer_r.isPlaying()) {
			
				mplayer_r.seekTo(progress);
				mplayer_l.seekTo(progress);
			}
		}
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		}
	};
	private void initSurfceView_all() {
		surfaceView_all.getHolder().addCallback(new Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (mplayer_r != null && mplayer_r.isPlaying()) {
					currentPosition = mplayer_r.getCurrentPosition();
					mplayer_r.stop();
					mplayer_l.stop();
				
				}
			
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mplayer_r.setDisplay(surfaceView_all.getHolder());
				startMovie(currentPosition_l);
			}
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
			}
		});
	}
	private void initSurfaceView_r() {
		surfaceView_r.getHolder().addCallback(new Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (mplayer_r != null && mplayer_r.isPlaying()) {
					mplayer_r.stop();
					
				}
			
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mplayer_r.setDisplay(surfaceView_r.getHolder());
				startMovie(currentPosition);
			}
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

			}
		});
	}
	private void initSurfaceView_l() {
		surfaceView_l.getHolder().addCallback(new Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (mplayer_l != null && mplayer_l.isPlaying()) {
					currentPosition_l = mplayer_l.getCurrentPosition();
					mplayer_l.stop();
					
				}
				
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mplayer_l.setDisplay(surfaceView_l.getHolder());
				if (currentPosition > 0) {
					startMovie(currentPosition);
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
			}
		});
	}


	private float mLastMotionX;
	private float mLastMotionY;
	private int startX;
	private int startY;
	private int threshold;
	private boolean isClick = true;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionX = x;
			mLastMotionY = y;
			startX = (int) x;
			startY = (int) y;
			break;
		case MotionEvent.ACTION_MOVE:
			float deltaX = x - mLastMotionX;
			float deltaY = y - mLastMotionY;
			float absDeltaX = Math.abs(deltaX);
			float absDeltaY = Math.abs(deltaY);
			// 声音调节标识
			boolean isAdjustAudio = false;
			if (absDeltaX > threshold && absDeltaY > threshold) {
				if (absDeltaX < absDeltaY) {
					isAdjustAudio = true;
				} else {
					isAdjustAudio = false;
				}
			} else if (absDeltaX < threshold && absDeltaY > threshold) {
				isAdjustAudio = true;
			} else if (absDeltaX > threshold && absDeltaY < threshold) {
				isAdjustAudio = false;
			} else {
				return true;
			}
			if (isAdjustAudio) {
				if (x < width / 2) {
					if (deltaY > 0) {
						 lightDown(absDeltaY);
					} else if (deltaY < 0) {
						lightUp(absDeltaY);
					}
				} else {
					if (deltaY > 0) {
						volumeDown(absDeltaY);
					} else if (deltaY < 0) {
						 volumeUp(absDeltaY);
					}
				}

			} else {
				if (deltaX > 0) {
					forward(absDeltaX);
				} else if (deltaX < 0) {
					backward(absDeltaX);
				}
			}
			mLastMotionX = x;
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
			if (Math.abs(x - startX) > threshold
					|| Math.abs(y - startY) > threshold) {
				isClick = false;
			}
			mLastMotionX = 0;
			mLastMotionY = 0;
			startX = (int) 0;
			if (isClick) {
				showOrHide();
			}
			isClick = true;
			break;

		default:
			break;
		}
		return true;
	}

	private void lightDown(float delatY) {
		int down = (int) (delatY / height * 255 * 3);
		int transformatLight =lightController.getLightness(this) - down;
		lightController.setBrightness(this, transformatLight);
		lightController.showLight(transformatLight);
	}

	private void lightUp(float delatY) {
		int up = (int) (delatY / height * 255 * 3);
		int transformatLight = lightController.getLightness(this) + up;
		lightController.setBrightness(this, transformatLight);
		lightController.showLight(transformatLight);
	}

	private void volumeDown(float delatY) {
		int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int down = (int) (delatY / height * max * 3);
		int volume = Math.max(current - down, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
		int transformatVolume = volume * 100 / max;
		volumeController.setVolumeProgress(transformatVolume);
	}

	private void volumeUp(float delatY) {
		int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int up = (int) ((delatY / height) * max * 3);
		int volume = Math.min(current + up, max);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
		int transformatVolume = volume * 100 / max;
		volumeController.setVolumeProgress(transformatVolume);
	}

	private void startMovie(final int msec) {
		String path = filePath;
		//mDurationTime.setText(formatTime(mplayer_r.getDuration()));
		try {
			mplayer_l.reset();
			mplayer_r.reset();
			mplayer_r.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mplayer_l.setDataSource(path);
			mplayer_r.setDataSource(path);	
			mplayer_r.prepare();
			mplayer_l.prepare();
			mplayer_l.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mplayer_l.start();
					mplayer_l.seekTo(msec);
				}
			});
			mplayer_r.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					mplayer_r.start();
					mplayer_r.seekTo(msec);
					seekBar.setMax(mplayer_r.getDuration());
					mHandler.removeCallbacks(hideRunnable);
					mHandler.postDelayed(hideRunnable, HIDE_TIME);
					mDurationTime.setText(formatTime(mplayer_r.getDuration()));
			     mTimer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						// TODO 自动生成的方法存根
						mHandler.sendEmptyMessage(0);
					
					}
				}, 0, 1000);
     			}
			});
			new Thread() {
				@Override
				public void run() {
					try {
						isPlaying = true;
						while (isPlaying) {
							int current = mplayer_r.getCurrentPosition();
							seekBar.setProgress(current);

							sleep(500);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Runnable hideRunnable = new Runnable() {

		@Override
		public void run() {
			showOrHide();
		}
	};
    
	private String formatTime(long time) {
		DateFormat formatter = new SimpleDateFormat("mm:ss");
		return formatter.format(new Date(time));
	}
    //快退
	private void backward(float delataX) {
		int current = mplayer_r.getCurrentPosition();
		int backwardTime = (int) (delataX / width * mplayer_r.getDuration());
		int currentTime = current - backwardTime;
		mplayer_r.seekTo(currentTime);
		mplayer_l.seekTo(currentTime);
		//mHandler.sendEmptyMessage(2);
	   // seekBar.setProgress(seekBar.getProgress());
		seekBar.setProgress(seekBar.getProgress()+backwardTime);
		mPlayTime.setText(formatTime(currentTime));
		
	}
    //快进
	private void forward(float delataX) {
		int current = mplayer_r.getCurrentPosition();
		int forwardTime = (int) (delataX / width * mplayer_r.getDuration());
		int currentTime = current + forwardTime;
		mplayer_r.seekTo(currentTime);
		mplayer_l.seekTo(currentTime);
	    seekBar.setProgress(seekBar.getProgress());
		seekBar.setProgress(seekBar.getProgress()+forwardTime);
		mPlayTime.setText(formatTime(currentTime));
	}

	protected void showOrHide() {
		// TODO Auto-generated method stub
		if (mBottomView.getVisibility()==View.VISIBLE) {
			mBottomView.setVisibility(View.GONE);
			title.setVisibility(View.GONE);
		}else {
			mBottomView.setVisibility(View.VISIBLE);
			title.setVisibility(View.VISIBLE);
			mHandler.removeCallbacks(hideRunnable);
			mHandler.postDelayed(hideRunnable, HIDE_TIME);
		}
	}

	private void screenSizeInit() {
		// TODO Auto-generated method stub
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		srceenHeight = dm.heightPixels;
		srceenWidth = dm.widthPixels;
	}

	private void fullScreen(SurfaceView v) {
		// TODO Auto-generated method stub
		LayoutParams lp = v.getLayoutParams();
		lp.height = srceenHeight;
		lp.width = srceenWidth;
		v.setLayoutParams(lp);
	}

	private void halfScreen(SurfaceView v) {
		// TODO Auto-generated method stub
		LayoutParams lp = v.getLayoutParams();
		lp.height = 9*srceenHeight/16;
		lp.width = srceenWidth / 2 - 2;
		v.setLayoutParams(lp);
	}
     private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (mplayer_r.getCurrentPosition()>0) {
				   	mPlayTime.setText(formatTime(mplayer_r.getCurrentPosition()));
				   	Log.d("aaaa", "qwwewr-------");
			    }
				break;

			default:
				break;
			}
		
		};
	 };
  

	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mTimer!=null) {
			mTimer.cancel();
			mTimer=null;
		}
		if (mplayer_l.isPlaying()) mplayer_l.stop();mplayer_l.release();
		if (mplayer_r.isPlaying()) mplayer_r.stop();mplayer_r.release();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		mHandler.removeMessages(0);
		mHandler.removeCallbacksAndMessages(null);
		
       
	}

	@Override
	protected void onPause() {
		super.onPause();
		currentPosition = mplayer_r.getCurrentPosition();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		//startMovie(currentPosition);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
}
