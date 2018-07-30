package com.beta1.memories.beta4musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContentMusic extends AppCompatActivity implements EventMediaControls {

    private ImageButton btnPlay, btnPrev, btnNext;
    private TextView tvTimePassed, tvTimeLeft;
    private SeekBar seekBar;
    private boolean musicPaused = true;
    private boolean activityPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_music);

        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvTimePassed = findViewById(R.id.tvTimePassed);
        tvTimeLeft = findViewById(R.id.tvTimeLeft);
        seekBar = findViewById(R.id.seekBarSong);

        btnPlay.setOnClickListener(new eventButton());
        btnPrev.setOnClickListener(new eventButton());
        btnNext.setOnClickListener(new eventButton());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b && MusicApp.mService != null) {
                    MusicApp.mService.seek((long) i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        if (MusicApp.mService != null) {
            MusicApp.mService.setOnEventControl(this);
            if (!getIntent().hasExtra("play") || MusicApp.mService.isPlaying()) {
                setContentSong();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        musicPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        activityPaused = false;
        btnAndSeekBarChange();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void play() {
        if (!activityPaused) {
            setContentSong();
            btnAndSeekBarChange();
        }
    }

    @Override
    public void start() {
        buttonLogoPause();
        seekBarStart();
    }

    @Override
    public void pause() {
        musicPaused = true;
        buttonLogoPlay();
    }

    private class eventButton implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (MusicApp.mService == null) return;
            switch (v.getId()) {
                case R.id.btnPlay:
                    MusicApp.mService.playOrPause();
                    break;
                case R.id.btnPrev:
                    MusicApp.mService.prev();
                    break;
                case R.id.btnNext:
                    MusicApp.mService.next();
                    break;
            }
        }
    }

    private void setSeekBarText() {
        if (seekBar == null || MusicApp.mService == null) return;
        final long position = MusicApp.mService.getCurrentPosition();
        final long positionSec = position / 1000;
        final long timeLeft = MusicApp.mService.duration() / 1000 - positionSec;
        seekBar.setProgress((int) position);
        if (tvTimePassed != null && tvTimeLeft != null) {
            tvTimePassed.setText(makeShortTimeString(getBaseContext(), positionSec));
            tvTimeLeft.setText("-" + makeShortTimeString(getBaseContext(), timeLeft));
        }
    }

    private String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    //seekbar
    private final Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {
            if (!musicPaused) {
                setSeekBarText();
                final int delay = 250;
                seekBar.postDelayed(mUpdateProgress, delay); //delay
            }
        }
    };

    private void buttonLogoPause() {
        btnPlay.setImageResource(R.drawable.ic_action_pause);
    }

    private void buttonLogoPlay() {
        btnPlay.setImageResource(R.drawable.ic_action_play);
    }

    private void btnAndSeekBarChange() {
        if (MusicApp.mService == null) return;
        if (MusicApp.mService.isPlaying()) {
            buttonLogoPause();
            seekBarStart();
        } else {
            buttonLogoPlay();
            setSeekBarText();
        }
    }

    private void seekBarStart() {
        if (musicPaused) {
            musicPaused = false;
            if (seekBar != null) {
                seekBar.postDelayed(mUpdateProgress, 10);
            }
        }
    }

    private void setContentSong() {
        if (MusicApp.mService == null) return;

        final Song arSong = MusicApp.mService.getList();
        if (arSong == null) return;

        if (seekBar != null) {
            seekBar.setMax((int) MusicApp.mService.duration());
        }

        final Bitmap albumB = arSong.getAlbumB();
        final String artist = arSong.getArtist();
        final String title = arSong.getTitle();

        final ImageView imgContentAlbum = findViewById(R.id.imgContentAlbum);
        imgContentAlbum.setImageBitmap(albumB);
        imgContentAlbum.setColorFilter(R.color.filterContentImage);

        final CircleImageView imgContentAlbumMin = findViewById(R.id.imgContentAlbumMin);
        imgContentAlbumMin.setImageBitmap(albumB);

        final TextView tvArtistNameContent = findViewById(R.id.tvArtistNameContent);
        tvArtistNameContent.setText(artist);

        final TextView tvSongNameContent = findViewById(R.id.tvSongNameContent);
        tvSongNameContent.setText(title);
    }
}
