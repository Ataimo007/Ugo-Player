package com.gcodes.iplayer.music.models;

import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.google.common.base.Objects;

public class Album implements Comparable<Album>
{
    public static final String sort = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + " asc";
    public static final String[] projection = {
            MediaStore.Audio.Albums.ALBUM_KEY,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums._ID
//            MediaStore.Audio.Albums.ALBUM_ID,
    };

    public static final String[] genreProjection = {
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private final String album;
    private final String albumKey;
    private final long albumId;
    private final String artist;

    public static Album getInstance(Cursor cursor) {
        return new Album( cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
    }

    public Album(String album, String albumKey, long albumId, String artist) {
        this.album = album;
        this.albumKey = albumKey;
        this.albumId = albumId;
        this.artist = artist;
    }

    public static Album getGenreInstance(Cursor cursor)
    {
        return new Album( cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_KEY)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ARTIST)));
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumKey() {
        return albumKey;
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public int compareTo(Album o) {
        return getAlbumKey().compareTo(o.getAlbumKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album)) return false;
        Album album1 = (Album) o;
        return getAlbumId() == album1.getAlbumId() &&
                Objects.equal(getAlbum(), album1.getAlbum()) &&
                Objects.equal(getAlbumKey(), album1.getAlbumKey()) &&
                Objects.equal(getArtist(), album1.getArtist());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getAlbum(), getAlbumKey(), getAlbumId(), getArtist());
    }

    @Override
    public String toString() {
        return "Album{" +
                "album='" + album + '\'' +
                ", albumKey='" + albumKey + '\'' +
                ", albumId=" + albumId +
                ", artist='" + artist + '\'' +
                '}';
    }
}
