package com.beta1.memories.beta4musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    public static final String NEXT_ACTION = "com.beta1.memories.beta4musicplayer.next";
    public static final String PREVIOUS_ACTION = "com.beta1.memories.beta4musicplayer.prev";
    public static final String PLAY_OR_PAUSE_ACTION = "com.beta1.memories.beta4musicplayer.play_or_pause";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mMedia;
    private List<Song> songsList;
    private boolean notPlaying = true;
    private long positionId = -1;
    private long currentPosition = 0;

    private NotificationManager mManager;
    private final int NOTIFY_ID = 1;

    private EventMediaControls eventMediaControls;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            handleCommandIntent(intent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mMedia = new MediaPlayer();
        mMedia.setOnCompletionListener(this);
        mMedia.setOnErrorListener(this);
        mMedia.setOnPreparedListener(this);
        mMedia.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMedia.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(PLAY_OR_PAUSE_ACTION);
        filter.addAction(PREVIOUS_ACTION);
        filter.addAction(NEXT_ACTION);
        /*filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Intent.ACTION_SCREEN_ON);*/
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mMedia.isPlaying()) {
            mMedia.stop();
        }
        mMedia.release();
        mMedia = null;
        return false;
    }

    public void onDestroy() {
        if (mMedia != null) {
            mMedia.release();
        }
        unregisterReceiver(mIntentReceiver);
    }

    private void handleCommandIntent(Intent intent) {
        final String action = intent.getAction();

        if (NEXT_ACTION.equals(action)) {
            next();
        } else if (PREVIOUS_ACTION.equals(action)) {
            prev();
        } else if (PLAY_OR_PAUSE_ACTION.equals(action)) {
            playOrPause();
        }
    }

    public void setOnEventControl(EventMediaControls e) {
        eventMediaControls = e;
    }

    public void notifManagerCancel() {
        mManager.cancel(NOTIFY_ID);
    }

    public void setList(List<Song> arr) {
        songsList = arr;
    }

    public void setPosition(long positionId) {
        this.positionId = positionId;
    }

    public void loadSong(final boolean notPlaying) {
        if (mMedia == null) return;
        this.notPlaying = notPlaying;
        mMedia.reset();
        final Song arSong = getList();
        final long id = arSong.getId();

        try {
            final Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            mMedia.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMedia.prepareAsync();
    }

    public void play() {
        if (eventMediaControls != null) {
            eventMediaControls.pause();
        }
        loadSong(false);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (notPlaying) {
            seek(currentPosition);
            currentPosition = 0;
            return;
        }

        mp.start();

        buildNotification();
        if (eventMediaControls != null) {
            eventMediaControls.play();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp == mMedia) next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d("myLog", "Error: what = " + what + ", extra = " + extra);
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                mMedia.release();
                mMedia = new MediaPlayer();
                mMedia.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mMedia.setAudioStreamType(AudioManager.STREAM_MUSIC);
                Log.d("myLog", "Error: MEDIA_ERROR_SERVER_DIED");
                return true;
            default:
                break;
        }
        return false;
    }

    public Song getList() {
        Song selectedSong = null;
        for (final Song song : songsList) {
            if (song.getId() == positionId) {
                selectedSong = song;
                break;
            }
        }
        return selectedSong;
    }

    public long getCurrentPosition() {
        return mMedia.getCurrentPosition();
    }

    public long position() {
        return positionId;
    }

    public long duration() {
        return mMedia.getDuration();
    }

    public boolean isPlaying() {
        return mMedia.isPlaying();
    }

    public void setCurrentPosition(final long position) {
        this.currentPosition = position;
    }

    public void pause() {
        mMedia.pause();
        buildNotification();
        if (eventMediaControls != null) {
            eventMediaControls.pause();
        }
    }

    public void start() {
        mMedia.start();
        buildNotification();
        if (eventMediaControls != null) {
            eventMediaControls.start();
        }
    }

    public void playOrPause() {
        if (mMedia != null) {
            if (isPlaying()) {
                pause();
            } else {
                start();
            }
        }
    }

    public void seek(final long whereto) {
        mMedia.seekTo((int) whereto);
    }

    public void prev() {
        final int sizeSong = songsList.size();

        for (int i = 0; i < sizeSong; i++) {
            if (songsList.get(i).getId() == positionId) {
                if (i > 0) {
                    positionId = songsList.get(--i).getId();
                } else {
                    positionId = songsList.get(sizeSong-1).getId();
                }
                break;
            }
        }

        play();
    }

    public void next() {
        final int sizeSong = songsList.size() - 1;
        if (sizeSong < 1) return;

        for (int i = 0; i <= sizeSong; i++) {
            if (songsList.get(i).getId() == positionId) {
                if (i < sizeSong) {
                    positionId = songsList.get(++i).getId();
                } else {
                    positionId = songsList.get(0).getId();
                }
                break;
            }
        }
        play();
    }

    private final PendingIntent retrievePlaybackAction(final String action) {
        final Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(this, 1, intent, 0);
    }

    private void buildNotification() {
        final Song arSong = getList();
        final String titleName = arSong.getTitle();
        final String artistName = arSong.getArtist();
        final Bitmap albumB = arSong.getAlbumB();
        final boolean isPlaying = isPlaying();

        int playButtonResId = isPlaying
                ? R.drawable.ic_action_pause_min : R.drawable.ic_action_play_min;

        Intent intent = new Intent(this, ContentMusic.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent PI = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action_prev = new NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_min, "", retrievePlaybackAction(PREVIOUS_ACTION)).build();
        NotificationCompat.Action action_next = new NotificationCompat.Action.Builder(R.drawable.ic_skip_next_min, "", retrievePlaybackAction(NEXT_ACTION)).build();
        NotificationCompat.Action action_play_or_pause = new NotificationCompat.Action.Builder(playButtonResId, "", retrievePlaybackAction(PLAY_OR_PAUSE_ACTION)).build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "music_notif")
                .setSmallIcon(R.drawable.ic_music_icon)
                .setLargeIcon(albumB)
                .setContentIntent(PI)
                .setContentTitle(titleName)
                .setContentText(artistName)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(action_prev)
                .addAction(action_play_or_pause)
                .addAction(action_next);

        android.support.v4.media.app.NotificationCompat.MediaStyle style = new android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2, 3);
        builder.setStyle(style);

        if (albumB != null) {
            builder.setColor(Palette.from(albumB).generate().getVibrantColor(Color.parseColor("#403f4d")));
        }

        Notification n = builder.build();

        mManager.notify(NOTIFY_ID, n);
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
