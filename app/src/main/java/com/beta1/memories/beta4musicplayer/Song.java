package com.beta1.memories.beta4musicplayer;

import android.graphics.Bitmap;

public class Song {

    private long id;
    private String title;
    private String artist;
    private Bitmap albumB;

    public Song(long id, String title, String artist, Bitmap albumB) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumB = albumB;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Bitmap getAlbumB() {
        return albumB;
    }
}
