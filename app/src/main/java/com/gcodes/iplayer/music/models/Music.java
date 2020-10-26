package com.gcodes.iplayer.music.models;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.google.common.base.Objects;

import com.gcodes.iplayer.R;

import java.io.Serializable;
import java.util.ArrayList;

import androidx.annotation.Nullable;

import org.joda.time.Period;

import static com.gcodes.iplayer.helpers.Helper.gson;

public class Music implements Comparable<Music>, Serializable
{
    public static final String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";
    public static final String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
    };

    private final String name;
    private final String data;
    private final long mediaId;
    private final long albumId;
    private final String artist;
    private final String album;

//    private Duration duration;

    public Music(String name, long mediaId, String data, long albumId, String artist, String album) {
        this.name = name;
        this.mediaId = mediaId;
        this.data = data;
        this.albumId = albumId;
        this.artist = artist;
        this.album = album;
    }

    public static Music getInstance(Bundle bundle )
    {
        return new Music( bundle.getString("name"), bundle.getLong("mediaId"), bundle.getString("data"), bundle.getLong( "albumId" ),
                bundle.getString("artist"), bundle.getString("album"));
    }

    public static Music getInstance(Cursor cursor)
    {
        return new Music( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
    }

    public static Music getGenreInstance(Cursor cursor)
    {
        return new Music( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members._ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.DATA)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
    }

    public Bitmap getThumbnail(Context context)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource( context, toUri());
        byte[] picture = retriever.getEmbeddedPicture();
        if ( picture != null )
            return BitmapFactory.decodeByteArray(picture, 0, picture.length);
        return BitmapFactory.decodeResource( context.getResources(), R.drawable.u_song_art_padded );
    }

    public Period getDuration()
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getData());
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Period.millis( Integer.parseInt(duration) );
    }

    public String getName() {
        return name;
    }

    public long getMediaId() {
        return mediaId;
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    @Override
    public int compareTo(Music o) {
        return getName().compareTo( o.getName() );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Music)) return false;
        Music music = (Music) o;
        return mediaId == music.mediaId &&
                albumId == music.albumId &&
                Objects.equal(name, music.name) &&
                Objects.equal(data, music.data) &&
                Objects.equal(artist, music.artist) &&
                Objects.equal(album, music.album);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, data, mediaId, albumId, artist, album);
    }

    public int uniqueCode(String purpose) {
        return Objects.hashCode(purpose, name, data, mediaId, albumId, artist, album);
    }

    @Override
    public String toString() {
        return "Music{" +
                "name='" + name + '\'' +
                '}';
    }

    public Bundle toBundle()
    {
        Bundle music = new Bundle();
        music.putString( "name", name );
        music.putLong( "mediaId", mediaId );
        music.putString( "data", data );
        music.putLong( "albumId", albumId );
        music.putString( "artist", artist );
        music.putString( "album", album );
        return music;
    }

    public String toGson()
    {
        return gson.toJson(this );
    }

    public Uri toUri()
    {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf( getMediaId() ) );
    }

    public String getData()
    {
        return data;
    }

    public static Music fromGson(String json )
    {
        return gson.fromJson( json, Music.class );
    }

    public static String[] toGson(ArrayList<Music> musics)
    {
        String[] gMusics = new String[musics.size()];
        for ( int i = 0; i < gMusics.length; ++i )
            gMusics[i] = musics.get(i).toGson();
        return gMusics;
    }

    public static ArrayList<Music> fromGson(String[] gMusics)
    {
        ArrayList<Music> musics = new ArrayList<>();
        for (String gMusic : gMusics )
            musics.add(Music.fromGson(gMusic));
        return musics;
    }

}
