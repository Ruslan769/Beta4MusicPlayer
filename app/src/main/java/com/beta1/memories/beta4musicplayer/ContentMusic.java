package com.beta1.memories.beta4musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContentMusic extends AppCompatActivity {

    private ImageButton btnPlay, btnPrev, btnNext;
    private TextView tvTimePassed, tvTimeLeft, tvSongNameContent, tvArtistNameContent;
    private SeekBar seekBar;
    private ImageView imgContentAlbum;
    private CircleImageView imgContentAlbumMin;
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
        tvSongNameContent = findViewById(R.id.tvSongNameContent);
        tvArtistNameContent = findViewById(R.id.tvArtistNameContent);
        seekBar = findViewById(R.id.seekBarSong);
        imgContentAlbum = findViewById(R.id.imgContentAlbum);
        imgContentAlbumMin = findViewById(R.id.imgContentAlbumMin);

        tvSongNameContent.setSelected(true);

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
            MusicApp.mService.setOnEventControl(new ControlEvent());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        seekBarStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        activityPaused = false;
        if (MusicApp.mService != null) {
            if (!getIntent().hasExtra("play") || MusicApp.mService.isPlaying()) {
                setContentSong();
            }
        }
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

    private class ControlEvent implements EventMediaControls {

        @Override
        public void play() {
            if (!activityPaused) {
                setContentSong();
            }
        }

        @Override
        public void start() {
            if (!activityPaused) {
                buttonLogoPause();
                seekBarStop();
                seekBarStart();
            }
        }

        @Override
        public void pause() {
            if (!activityPaused) {
                buttonLogoPlay();
                seekBarStop();
            }
        }
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
            //Log.d("myLog", "mUpdateProgress");
            setSeekBarText();
            final int delay = 250;
            seekBar.postDelayed(mUpdateProgress, delay); //delay
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
            seekBarStop();
            seekBarStart();
        } else {
            buttonLogoPlay();
            setSeekBarText();
        }
    }

    private void seekBarStart() {
        if (seekBar != null && mUpdateProgress != null) {
            seekBar.postDelayed(mUpdateProgress, 10);
        }
    }

    private void seekBarStop() {
        if (seekBar != null && mUpdateProgress != null) {
            seekBar.removeCallbacks(mUpdateProgress);
        }
    }

    private void setContentSong() {
        Log.d("myLog", "setContentSong");
        if (MusicApp.mService == null) return;

        if (seekBar != null) {
            seekBar.setMax((int) MusicApp.mService.duration());
            btnAndSeekBarChange();
        }

        final Song arSong = MusicApp.mService.getList();
        final Bitmap albumB = arSong.getAlbumB();
        final String artist = arSong.getArtist();
        final String title = arSong.getTitle();

        imgContentAlbum.setImageBitmap(albumB);
        imgContentAlbum.setColorFilter(R.color.filterContentImage);
        imgContentAlbumMin.setImageBitmap(albumB);
        tvSongNameContent.setText(title);
        tvArtistNameContent.setText(artist);
    }
}
