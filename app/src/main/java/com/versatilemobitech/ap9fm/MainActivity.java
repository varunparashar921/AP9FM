package com.versatilemobitech.ap9fm;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, ServiceConnection, OnRadioActionChangeListener {

    private static final String RADIOJOCKY_CALL_NUMBER = "+918688166331";
    private static final String SKYPE_CALLID = "AP9FM";//Ap9fm
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int READ_PHONE_STATE_REQUESTCODE = 124;
    private AppCompatSeekBar mVolumeSeekBar;
    private ImageView playStopRadioImageView;
    private ImageView skypeCallImageView;
    private ImageView pausePlayImageView;
    private ImageView callImageView;
    private boolean isBindServiceEnabled;
    private AudioManager audioManger;
    private RadioCallBacks mService;
    private ContentObserver mVolumeChangeObserver;
    private Toolbar toolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolBar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolBar);
        initializeUIElements();
//        startFMService();
    }

//    private void initBannerAd(boolean enableAdMob) {
//        if (enableAdMob) {
//            mAdView = (AdView) findViewById(R.id.adView);
//            AdRequest.Builder builder = new AdRequest.Builder();
//            if (BuildConfig.DEBUG) {
//                builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
//            }
//            AdRequest adRequest = builder.build();
//            mAdView.loadAd(adRequest);
//            mAdView.setAdListener(new AdListener() {
//                @Override
//                public void onAdFailedToLoad(int errorCode) {
//                    Log.d(TAG, "onAdFailedToLoad...." + errorCode);
//                    mAdView.setVisibility(View.GONE);
//                }
//                @Override
//                public void onAdLoaded() {
//                    Log.d(TAG, "onAdLoadeed....");
//                    mAdView.setVisibility(View.VISIBLE);
//                }
//            });
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();
        int progress = audioManger.getStreamVolume(AudioManager.STREAM_MUSIC) * mVolumeSeekBar.getMax() /
                audioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekBar.setProgress(progress);
        if (mService != null) {
            playStopRadioImageView.setSelected(mService.isRadioPlaying());
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    READ_PHONE_STATE_REQUESTCODE);
        } else {
            Intent intent = new Intent(this, TG9FMRadioService.class);
            startService(intent);
            bindService(intent, this, BIND_AUTO_CREATE);
            isBindServiceEnabled = true;
        }
    }

    private void requestPhoneStateDialog() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    READ_PHONE_STATE_REQUESTCODE);
        }
    }

    private void startFMService() {
        Intent intent = new Intent(this, TG9FMRadioService.class);
        startService(intent);
        bindService(intent, this, BIND_AUTO_CREATE);
        isBindServiceEnabled = true;
    }

    private void initializeUIElements() {
        callImageView = (ImageView) findViewById(R.id.callImageView);
        skypeCallImageView = (ImageView) findViewById(R.id.skypeCallImageView);
        pausePlayImageView = (ImageView) findViewById(R.id.pause_playImageView);
        mVolumeSeekBar = (AppCompatSeekBar) findViewById(R.id.volumeSeekBar);
        playStopRadioImageView = (ImageView) findViewById(R.id.pause_playImageView);
        audioManger = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mVolumeSeekBar.setOnSeekBarChangeListener(this);
        playStopRadioImageView.setOnClickListener(this);
        skypeCallImageView.setOnClickListener(this);
        pausePlayImageView.setOnClickListener(this);
        callImageView.setOnClickListener(this);
        mVolumeChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mVolumeSeekBar.setOnSeekBarChangeListener(null);
                int progress = audioManger.getStreamVolume(AudioManager.STREAM_MUSIC) *
                        mVolumeSeekBar.getMax() /
                        audioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mVolumeSeekBar.setProgress(progress);
                mVolumeSeekBar.setOnSeekBarChangeListener(MainActivity.this);
            }
        };
        getApplicationContext().getContentResolver().
                registerContentObserver(android.provider.Settings.System.CONTENT_URI, true,
                        mVolumeChangeObserver);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.callImageView) {
            callRadioJockey(RADIOJOCKY_CALL_NUMBER);
        } else if (v.getId() == R.id.skypeCallImageView) {
            //TODO call skype call
            makeSkypeCall(SKYPE_CALLID);
            if (mService != null && mService.isRadioPlaying()) {
                mService.stopRadio();
            }
        } else if (v.getId() == R.id.pause_playImageView) {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (!playStopRadioImageView.isSelected()) {
                    mService.startRadio();
                    playStopRadioImageView.setSelected(true);
                } else {
                    mService.stopRadio();
                    playStopRadioImageView.setSelected(false);
                }
                Log.d(TAG, "RADIO CALLBACKS  onClick...." + playStopRadioImageView.isSelected());
            } else {
                Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRadioStopped() {
        Log.d(TAG, "RADIO CALLBACKS  onRadioStopped....");
        playStopRadioImageView.setSelected(false);
    }

    public void makeSkypeCall(String number) {
        try {
            Intent sky = new Intent("android.intent.action.VIEW");
            sky.setData(Uri.parse("skype:" + number));
            Log.d("UTILS", "tel:" + number);
            startActivity(sky);
        } catch (ActivityNotFoundException e) {
            Log.e("SKYPE CALL", "Skype failed", e);
            Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("market://details?id=com.skype.raider"));
            startActivity(promptInstall);
        }
    }

    /**
     * calling radio progress changed
     *
     * @param radiojockyCallNumber
     */
    private void callRadioJockey(String radiojockyCallNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + radiojockyCallNumber));
        startActivity(intent);
    }

    /**
     * on volume progress changed
     * @param seekBar
     * @param progress
     * @param fromUser
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        getApplicationContext().getContentResolver().
                unregisterContentObserver(mVolumeChangeObserver);

        int maxVolme = audioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxProgress = mVolumeSeekBar.getMax();

        int audioProgress = progress * maxVolme / maxProgress;
        audioManger.setStreamVolume(
                AudioManager.STREAM_MUSIC, audioProgress,
                0);
        getApplicationContext().getContentResolver().
                registerContentObserver(android.provider.Settings.System.CONTENT_URI, true,
                        mVolumeChangeObserver);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_PHONE_STATE_REQUESTCODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, TG9FMRadioService.class);
                startService(intent);
                bindService(intent, this, BIND_AUTO_CREATE);
                isBindServiceEnabled = true;
            } else {
                finish();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        TG9FMRadioService.LocalBinder binder = (TG9FMRadioService.LocalBinder) service;
        mService = binder.getService();
        mService.addOnRadioActionChange(this);
        Log.d(TAG, "RADIO CALLBACKS.....onServiceConnected");
        if (mService != null) {
            playStopRadioImageView.setSelected(mService.isRadioPlaying());
        }
        requestPhoneStateDialog();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().
                unregisterContentObserver(mVolumeChangeObserver);
        if (mService != null) {
            mService.removeRadioActionChange(this);
        }
        if (isBindServiceEnabled) {
            unbindService(this);
        }
        mService = null;
    }

    @Override
    public void onRadioStarted() {
        Log.d(TAG, "RADIO CALLBACKS  onRadioStarted....");
        playStopRadioImageView.setSelected(true);
    }

    @Override
    public void onRadioBuffering() {

    }

    /**
     *
     * @param errorMsg
     */
    @Override
    public void onRadioError(String errorMsg) {
        Log.d(TAG, "RADIO CALLBACKS  onRadioError...." + errorMsg);
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        if (playStopRadioImageView.isSelected()) {
            playStopRadioImageView.setSelected(false);
        }
    }

}
