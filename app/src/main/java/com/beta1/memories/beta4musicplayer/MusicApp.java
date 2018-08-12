package com.beta1.memories.beta4musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;

import com.beta1.memories.beta4musicplayer.adapter.BaseSongAdapter;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MusicApp extends AppCompatActivity {


    private final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 200;
    private String PREFERENCES_MUSIC_ID = "music_id";
    private String PREFERENCES_MUSIC_POSITION = "music_duration";

    private boolean permission = false;
    private final String[] arPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
    };

    private final List<Song> arListSongs = new ArrayList<>();
    private ListView songView;
    private BaseSongAdapter songAdapter;

    public static MusicService mService;
    private Intent musicIntent;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = findViewById(R.id.lvContainer);
        sharedPref = getPreferences(Context.MODE_PRIVATE);

        if (hasPermissions()) {
            permission = true;
        } else {
            ActivityCompat.requestPermissions(this, arPermissions, REQUEST_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (permission) {
            startMusicServiceIntent();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeColorTitleSong();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mService != null) {

            if (sharedPref != null && mService.position() > -1) {
                final SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                sharedPrefEditor.putLong(PREFERENCES_MUSIC_ID, mService.position());
                sharedPrefEditor.putLong(PREFERENCES_MUSIC_POSITION, mService.getCurrentPosition());
                sharedPrefEditor.commit();
            }

            mService.notifManagerCancel();
            stopService(musicIntent);
            mService = null;
        }
    }

    private boolean hasPermissions() {
        boolean isPermission = true;
        for (String perms : arPermissions) {
            int res = checkCallingOrSelfPermission(perms);
            if (res != PackageManager.PERMISSION_GRANTED) {
                isPermission = false;
                break;
            }
        }
        return isPermission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean permissionToStorageAccepted = true;
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE_PERMISSION:
                for (int res : grantResults) {
                    if (res != PackageManager.PERMISSION_GRANTED) {
                        permissionToStorageAccepted = false;
                        break;
                    }
                }
                break;
            default:
                permissionToStorageAccepted = false;
                break;
        }
        if (permissionToStorageAccepted) {
            permission = true;
            startMusicServiceIntent();
        } else {
            startActivity(new Intent(this, ErrorPermissionActivity.class));
            finish();
        }
    }

    private void startMusicServiceIntent() {
        if (musicIntent == null) {
            musicIntent = new Intent(this, MusicService.class);
            bindService(musicIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(musicIntent);
        }
    }

    public Bitmap getAlbumart(Long album_id) {

        Bitmap bm = null;
        try {

            final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            final Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

            final ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");

            if (pfd != null) {
                final FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bm;
    }

    private void getSongList() {
        final ContentResolver musicResolver = getContentResolver();
        final Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final Cursor cursor = musicResolver.query(musicUri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // get columns
            final int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            final int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            // add song to list
            do {
                final long musicId = cursor.getLong(idColumn);
                Log.d("myLog", "getSongList: musicId = " + musicId);
                final String musicTitle = cursor.getString(titleColumn);
                final String musicArtist = cursor.getString(artistColumn);
                final long album_id = cursor.getLong(albumColumn);

                arListSongs.add(new Song(musicId, musicTitle, musicArtist, getAlbumart(album_id)));
            } while (cursor.moveToNext());
        }
    }

    private void setSongAdapter() {
        songAdapter = new BaseSongAdapter(this, arListSongs);
        songView.setAdapter(songAdapter);
        songView.setOnItemClickListener(new EventListMusic());
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // get song list
            getSongList();
            // sort
            Collections.sort(arListSongs, new Comparator<Song>() {
                @Override
                public int compare(Song o1, Song o2) {
                    return o1.getTitle().compareTo(o2.getTitle());
                }
            });

            final MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();
            mService.setList(arListSongs);

            if (sharedPref.contains(PREFERENCES_MUSIC_ID)) {
                final long songPosition = sharedPref.getLong(PREFERENCES_MUSIC_ID, 0);
                final long songCurrentPosition = sharedPref.getLong(PREFERENCES_MUSIC_POSITION, 0);
                mService.setPosition(songPosition);
                mService.setCurrentPosition(songCurrentPosition);
                mService.loadSong(true);
            }

            setSongAdapter();

            Log.d("myLog", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                songAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    private void changeColorTitleSong() {
        if (mService == null) return;
        if (mService.position() < 0) return;

        Log.d("myLog", "changeColorTitleSong");

        for (int i = 0; i < songView.getCount(); i++) {
            final View listView = songView.getChildAt(i);
            final TextView tvSong = listView.findViewById(R.id.tvTitle);
            tvSong.setTextColor(Color.BLACK);
        }

        final TextView tvSong = songView.findViewWithTag(mService.position()).findViewById(R.id.tvTitle);
        tvSong.setTextColor(Color.parseColor("#E91E63"));
    }

    private class EventListMusic implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mService == null) return;

            final long positionId = (long) view.getTag();
            final Intent intent = new Intent(MusicApp.this, ContentMusic.class);

            if (mService.position() != positionId) {
                mService.setPosition(positionId);
                mService.play();
                intent.putExtra("play", 1);
                changeColorTitleSong();
            }

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }
}
