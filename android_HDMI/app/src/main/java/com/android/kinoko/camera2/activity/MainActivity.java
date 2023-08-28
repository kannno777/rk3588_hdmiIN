package com.android.kinoko.camera2.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.kinoko.camera2.R;
import com.android.kinoko.camera2.util.DataUtils;
import com.android.kinoko.camera2.util.SystemPropertiesProxy;

public class MainActivity extends Activity implements
        View.OnAttachStateChangeListener, View.OnClickListener {

    private static final String TAG = "MainActivity";

    private final String HDMI_OUT_ACTION = "android.intent.action.HDMI_PLUGGED";
    private final String DP_OUT_ACTION = "android.intent.action.DP_PLUGGED";

    private final int MSG_START_TV = 0;
    private final int MSG_ENABLE_SETTINGS = 1;

    private RelativeLayout rootView;
    private TvView tvView;

    private Object mLock = new Object();
    private Uri mChannelUri;
    private MyBroadCastReceiver mBroadCastReceiver;
    private boolean mPopSettingsPrepared;
    private boolean mResumePrepared;
    private boolean mTvSurfacePrepared;
    private boolean mAlreadyTvTune;
    private boolean mIsDestory;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mIsDestory) {
                return;
            }
            synchronized (mLock) {
                if (mIsDestory) {
                    return;
                }
                Log.v(TAG, "deal message " + msg.what);
                if (MSG_START_TV == msg.what) {
                    if (mResumePrepared && mTvSurfacePrepared
                            && !mAlreadyTvTune) {
                        mAlreadyTvTune = true;
                        tvView.tune(DataUtils.INPUT_ID, mChannelUri);
                        startHdmiAudioService();
                    }
                } else if (MSG_ENABLE_SETTINGS == msg.what) {
                    mPopSettingsPrepared = true;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fullScreen();
        rootView = (RelativeLayout) findViewById(R.id.root_view);
        rootView.setOnClickListener(this);

        mChannelUri = TvContract.buildChannelUriForPassthroughInput(DataUtils.INPUT_ID);
        registerReceiver();

    }

    private void fullScreen() {
        getWindow().getDecorView().getRootView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    private void initSideband() {
        mResumePrepared = false;
        mTvSurfacePrepared = false;

        if (null != tvView) {
            rootView.removeView(tvView);
        }
        tvView = new TvView(this);
        tvView.addOnAttachStateChangeListener(this);
        tvView.setOnClickListener(this);
        rootView.addView(tvView);
    }

    private void resumeSideband() {
        initSideband();
        mResumePrepared = true;
        mHandler.removeMessages(MSG_START_TV);
        mHandler.sendEmptyMessageDelayed(MSG_START_TV, DataUtils.START_TV_REVIEW_DELAY);
        Log.v(TAG, "resumeSideband");
    }

    private void pauseSideband() {
        stopHdmiAudioService();
        mResumePrepared = false;
        mAlreadyTvTune = false;
        if (null != tvView) {
            tvView.reset();
            rootView.removeView(tvView);
        }
        Log.v(TAG, "pauseSideband");
    }

    private void registerReceiver() {
        mBroadCastReceiver = new MyBroadCastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intentFilter.addAction(HDMI_OUT_ACTION);
        intentFilter.addAction(DP_OUT_ACTION);
        registerReceiver(mBroadCastReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        if (null != mBroadCastReceiver) {
            unregisterReceiver(mBroadCastReceiver);
        }
    }

    private void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void startHdmiAudioService() {
        Log.v(TAG, "startHdmiAudioService");
        SystemPropertiesProxy.set("vendor.hdmiin.audiorate", "48KHZ");
        Intent intent = new Intent();
        ComponentName cn = new ComponentName(DataUtils.HDMIIN_AUDIO_PACKAGE_NAME, DataUtils.HDMIIN_AUDIO_CLS_NAME);
        intent.setComponent(cn);
        startForegroundService(intent);
    }

    private void stopHdmiAudioService() {
        Log.v(TAG, "stopHdmiAudioService");
        Intent intent = new Intent();
        ComponentName cn = new ComponentName(DataUtils.HDMIIN_AUDIO_PACKAGE_NAME, DataUtils.HDMIIN_AUDIO_CLS_NAME);
        intent.setComponent(cn);
        stopService(intent);
    }

    private void exitApp() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopHdmiAudioService();
        finish();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                showToast(R.string.back_key_warn);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        Log.v(TAG, "onViewAttachedToWindow");
        mTvSurfacePrepared = true;
        mHandler.removeMessages(MSG_START_TV);
        mHandler.sendEmptyMessageDelayed(MSG_START_TV, DataUtils.START_TV_REVIEW_DELAY);
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        Log.v(TAG, "onViewDetachedFromWindow");
        mTvSurfacePrepared = false;
        if (null != tvView) {
            tvView.reset();//?
        }
    }

    @Override
    public void onClick(View v) {
        Log.v(TAG, "onClick " + v);
     }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        resumeSideband();
        if (!mPopSettingsPrepared) {
            mHandler.sendEmptyMessageDelayed(MSG_ENABLE_SETTINGS, DataUtils.MAIN_ENABLE_SETTINGS_DEALY);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        pauseSideband();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        mIsDestory = true;
        mHandler.removeMessages(MSG_START_TV);
        mHandler.removeMessages(MSG_ENABLE_SETTINGS);
        unregisterReceiver();
    }

    private final class MyBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, action);
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                exitApp();
            } else if (HDMI_OUT_ACTION.equals(action) || DP_OUT_ACTION.equals(action)) {
                mAlreadyTvTune = false;
                resumeSideband();
            }
        }
    }
}
