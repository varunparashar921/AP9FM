package com.versatilemobitech.ap9fm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat.Builder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.util.Util;

public class TG9FMRadioService extends Service implements ExoPlayer.Listener, RadioCallBacks {
    //   previos url
    private static final String BASE_URL = "http://s6.voscast.com:10472";
    private static final String TAG = TG9FMRadioService.class.getSimpleName();
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final int RADIO_NOTIFICATION = 122;
    private OnRadioActionChangeListener mRadioActionChangedListener;
    private ExoPlayer exoPlayer;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage..........." + msg);
        }
    };

    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "PhoneStateListener....Ringing: ");
                        stopRadio();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "PhoneStateListener....CALL_STATE_OFFHOOK: ");
                        stopRadio();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(TAG, "PhoneStateListener....CALL_STATE_IDLE: ");
                        break;
                    default:
                }
            } catch (Exception e) {
            }
        }
    };
    private TelephonyManager teleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeExoPlayer();
        teleManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        teleManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        TG9FMRadioService getService() {
            return TG9FMRadioService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void initializeExoPlayer() {
        Uri builtUri = Uri.parse(BASE_URL).buildUpon().build();
        Log.d(TAG, "BUILT URI FOR STREAMING: " + builtUri);
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        SampleSource sampleSource = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(handler, new BandwidthMeter.EventListener() {
                @Override
                public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
                    Log.d(TAG, "RADIO CALLBACKS  onBandwidthSample()");
                }
            });
            DataSource dataSource = new DefaultUriDataSource(this, bandwidthMeter, userAgent);
            sampleSource = new ExtractorSampleSource(
                    builtUri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        } else {
            sampleSource = new FrameworkSampleSource(this, builtUri, null);
        }
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                sampleSource, MediaCodecSelector.DEFAULT);
        exoPlayer = ExoPlayer.Factory.newInstance(1);
        exoPlayer.prepare(audioRenderer);
        exoPlayer.addListener(this);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "RADIO CALLBACKS  onPlayerStateChanged....." + playWhenReady + " " + playbackState);
        if (playWhenReady == true && playbackState == ExoPlayer.STATE_READY) {
            enableNotification();
            if (mRadioActionChangedListener != null) {
                mRadioActionChangedListener.onRadioStarted();
            }
        } else if (playWhenReady == true && playbackState == ExoPlayer.STATE_ENDED) {
            if (mRadioActionChangedListener != null) {
                mRadioActionChangedListener.onRadioStopped();
            }
            stopForeground(true);
        } else if (playWhenReady == true && playbackState == ExoPlayer.STATE_BUFFERING) {
            if (mRadioActionChangedListener != null) {
                mRadioActionChangedListener.onRadioBuffering();
            }
        }
    }

    private void enableNotification() {
        Builder builder = new Builder(this)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle(getString(R.string.app_name) + " is Playing");
//                .setContentText("Hello World!");
        Notification notification = builder.build();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = pendingIntent;
        startForeground(RADIO_NOTIFICATION, notification);
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        Log.d(TAG, "onPlayWhenReadyCommitted");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error != null) {
            Log.d(TAG, "onPlayerError....." + error.getMessage());
            if (!error.caughtAtTopLevel) {
                Throwable cause = error.getCause();
                if (cause instanceof HttpDataSource.HttpDataSourceException) {
                    Throwable exp = cause.getCause();
                    if (exp instanceof java.net.SocketTimeoutException
                            ) {
                        if (mRadioActionChangedListener != null) {
                            mRadioActionChangedListener.onRadioError("Oops...Problem occuring with server!!!");
                        }
                        initializeExoPlayer();
                    }
                }
            }
        }
    }

    @Override
    public void startRadio() {
        Log.d(TAG, "startRadio....RADIO CALLBACKS  ");
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stopRadio() {
        Log.d(TAG, "stopRadio......RADIO CALLBACKS  ");
        stopForeground(true);
        exoPlayer.stop();
        exoPlayer.release();
        if (mRadioActionChangedListener != null) {
            mRadioActionChangedListener.onRadioStopped();
        }
        exoPlayer.removeListener(this);
        initializeExoPlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRadio();
        teleManager.listen(null, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean isRadioPlaying() {
        Log.d(TAG, "RADIO CALLBACKS  isRadioPlaying....." + exoPlayer.getPlayWhenReady());
        return exoPlayer.getPlayWhenReady();
    }

    @Override
    public void addOnRadioActionChange(OnRadioActionChangeListener onRadioActionChangeListener) {
        mRadioActionChangedListener = onRadioActionChangeListener;
    }

    @Override
    public void removeRadioActionChange(OnRadioActionChangeListener onRadioActionChangeListener) {
        mRadioActionChangedListener = null;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        if (mRadioActionChangedListener != null) {
            throw new UnsupportedOperationException("OnRadioActionChangeListener is not re4moved");
        }
    }

}
