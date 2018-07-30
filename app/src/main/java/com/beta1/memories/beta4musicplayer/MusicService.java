package com.beta1.memories.beta4musicplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mMedia;
    private List<Song> songsList;
    private int position = -1;

    private NotificationManager mManager;
    private String songTitle;
    private final int NOTIFY_ID = 1;

    private EventMediaControls eventMediaControls;

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
    }

    public void setOnEventControl(EventMediaControls e) {
        eventMediaControls = e;
    }

    public void notifManagerCancel() {
        mManager.cancel(NOTIFY_ID);
    }

    public void setList(List<Song> arr) {
        this.songsList = arr;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void play() {
        if (eventMediaControls != null) {
            eventMediaControls.pause();
        }

        mMedia.reset();

        final Song arSong = songsList.get(position);
        final long id = arSong.getId();

        songTitle = arSong.getTitle();

        final Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            mMedia.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMedia.prepareAsync();
    }

    public Song getList() {
        return songsList.get(position);
    }

    public long getCurrentPosition() {
        return mMedia.getCurrentPosition();
    }

    public int position() {
        return position;
    }

    public long duration() {
        return mMedia.getDuration();
    }

    public boolean isPlaying() {
        return mMedia.isPlaying();
    }

    public void pause() {
        mMedia.pause();
        if (eventMediaControls != null) {
            eventMediaControls.pause();
        }
    }

    public void start() {
        mMedia.start();
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

    public long seek(final long whereto) {
        mMedia.seekTo((int) whereto);
        return whereto;
    }

    public void prev() {
        if (position > 0) {
            position--;
        } else {
            position = songsList.size() - 1;
        }
        play();
    }

    public void next() {
        int sizeSong = songsList.size() - 1;
        if (sizeSong == 0) return;
        if (position < sizeSong) {
            position++;
        } else {
            position = 0;
        }
        play();
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        Intent intent = new Intent(this, ContentMusic.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent PI = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap iconSong = getList().getAlbumB();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "music_notif")
                        .setContentIntent(PI)
                        .setSmallIcon(R.drawable.ic_music_icon)
                        .setLargeIcon(iconSong)
                        .setTicker(songTitle)
                        .setOngoing(true)
                        .setContentTitle("")
                        .setContentText(songTitle);

        mManager.notify(NOTIFY_ID, builder.build());

        if (eventMediaControls != null) {
            eventMediaControls.play();
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
