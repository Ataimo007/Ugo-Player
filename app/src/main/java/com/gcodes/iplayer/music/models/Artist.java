package com.gcodes.iplayer.music.models;

import android.database.Cursor;
import android.provider.MediaStore;

import com.google.common.base.Objects;

public class Artist implements Comparable<Artist> {

    private final String artist;
    private final String album;
    private final long artistId;
    private final String artistKey;
    private final long albumId;
    private final int albumCount;
    private final int trackCount;

    public static final String sort = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + " asc";
    public static final String[] projection = {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.ARTIST_KEY,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
    };

    public static Artist getInstance(Cursor cursor) {
        return new Artist( cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                null,
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY)), -1,
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS))
                );
    }

    public Artist(String artist, String album, long artistId, String artistKey, long albumId, int tracks, int albums) {
        this.artist = artist;
        this.album = album;
        this.artistId = artistId;
        this.artistKey = artistKey;
        this.albumId = albumId;
        trackCount = tracks;
        albumCount = albums;
    }

    public static Artist getGenreInstance(Cursor cursor )
    {
        return new Artist( cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID)), -1, -1);
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public long getArtistId() {
        return artistId;
    }

    public String getArtistKey() {
        return artistKey;
    }

    public long getAlbumId() {
        return albumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist)) return false;
        Artist artist1 = (Artist) o;
        return getArtistId() == artist1.getArtistId() &&
                getAlbumId() == artist1.getAlbumId() &&
                Objects.equal(getArtist(), artist1.getArtist()) &&
                Objects.equal(getAlbum(), artist1.getAlbum()) &&
                Objects.equal(getArtistKey(), artist1.getArtistKey());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getArtist(), getAlbum(), getArtistId(), getArtistKey(), getAlbumId());
    }

    @Override
    public String toString() {
        return "Artist{" +
                "artist='" + artist + '\'' +
                ", artistId=" + artistId +
                ", artistKey='" + artistKey + '\'' +
                ", albumId='" + albumId + '\'' +
                '}';
    }

    @Override
    public int compareTo(Artist o) {
        return getArtistKey().compareTo(o.getArtistKey());
    }
}
