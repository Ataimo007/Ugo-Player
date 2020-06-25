package com.gcodes.iplayer.music;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import com.google.common.base.Objects;

import com.gcodes.iplayer.R;

import java.io.Serializable;

import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;

import org.joda.time.Period;

import static com.gcodes.iplayer.helpers.Helper.gson;

public class Music implements Comparable<Music>, Serializable
{
    private final String name;
    private final String data;
    private final long mediaId;
    private final long albumId;
    private final String artist;
    private final String album;
    private final String artPath;
//    private Duration duration;

    public Music(String name, long mediaId, String data, long albumId, String artist, String album, String artPath) {
        this.name = name;
        this.mediaId = mediaId;
        this.data = data;
        this.albumId = albumId;
        this.artist = artist;
        this.album = album;
        this.artPath = artPath;
    }

    public static Music getInstance(Bundle bundle )
    {
        return new Music( bundle.getString("name"), bundle.getLong("mediaId"), bundle.getString("data"), bundle.getLong( "albumId" ),
                bundle.getString("artist"), bundle.getString("album"), bundle.getString("artPath"));
    }

    public static Music getInstance(Cursor cursor, CursorLoader artLoader )
    {
        return new Music( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                getArtPath( cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)), artLoader ) );
    }

    public static Music getGenreInstance(Cursor cursor, CursorLoader artLoader )
    {
        return new Music( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members._ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.DATA)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)),
                getArtPath( cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID)), artLoader ) );
    }

//    public static Music getIntance(Cursor cursor)
//    {
//        return new Music( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
//                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
//                cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
//                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
//                cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) );
//    }

//    public Bitmap getArtBitmap(Context context, int width, int height )
//    {
//        return Bitmap.createScaledBitmap(getArtBitmap(context), width, height, true);
//    }
//
//    public Bitmap getArtBitmap(Context context)
//    {
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        retriever.setDataSource( context, toUri() );
//        byte[] picture = retriever.getEmbeddedPicture();
//        if ( picture != null )
//        {
//            Bitmap albumArt = BitmapFactory.decodeByteArray(picture, 0, picture.length);
//            return albumArt;
//        }
//        return BitmapFactory.decodeResource( context.getResources(), R.drawable.ic_track_black_24dp );
//    }

    public Bitmap getArtBitmap(Context context, int width, int height )
    {
        if ( artPath != null )
        {
            Bitmap bitmap = BitmapFactory.decodeFile(artPath);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            if ( bitmap != null )
            {
                return bitmap;
            }
        }
        return BitmapFactory.decodeResource( context.getResources(), R.drawable.ic_track_black_24dp );
    }

    public Bitmap getArtBitmap(Context context)
    {
        if ( artPath != null )
        {
            Bitmap bitmap = BitmapFactory.decodeFile(artPath);
            if ( bitmap != null )
            {
                return bitmap;
            }
        }
        return BitmapFactory.decodeResource( context.getResources(), R.drawable.ic_track_black_24dp );
    }

    public Period getDuration()
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getData());
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Period.millis( Integer.parseInt(duration) );
    }

    public static String getArtPath(long id, CursorLoader artLoader)
    {
        String path = null;
        synchronized ( artLoader )
        {
            artLoader.setSelectionArgs( new String[]{ String.valueOf(id)} );
            Cursor cursor = artLoader.loadInBackground();

            if ( cursor != null )
            {
    //                Log.d( "Albums_Detail", "Albums " + Arrays.toString(cursor.getColumnNames()));
                if ( cursor.moveToFirst()) {
                    path = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART) );
                    // do whatever you need to do
                    cursor.close();
                }
            }
        }

        return path;
    }

    public String getArtPath() {
        return artPath;
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
        music.putString( "artPath", artPath );
        return music;
    }

    public String toGson()
    {
//        Gson gson = new Gson();
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
//        Gson gson = new Gson();
        return gson.fromJson( json, Music.class );
    }

    public static class Artist implements Comparable< Album >
    {
        private final String artist;
        private final String album;
        private final long artistId;
        private final String artistKey;
        private final long albumId;

        public Artist(Cursor cursor) {
            artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST));
            album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM));
            artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY));
            artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_ID));
            albumId = cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID));
        }

        public static Artist getInstance( Cursor cursor )
        {
            return new Artist( cursor );
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
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
        public int compareTo(Album o) {
            return getArtistKey().compareTo( o.getAlbumKey() );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Artist artist = (Artist) o;
            return Objects.equal(artistKey, artist.artistKey);
//            return Objects.equals(artistKey, artist.artistKey);
//            return Objects.equals(artistKey, artist.artistKey);
        }

//        public int hashCode() {
//            return Objects.hash(artistKey);
//        }
        @Override
        public int hashCode() {
            return Objects.hashCode(artistKey);
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
    }

    public static class Album implements Comparable< Album >
    {
        private final String album;
        private final String albumKey;
        private final long albumId;
        private final String artist;

        public Album(Cursor cursor) {
            album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM));
            albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_KEY));
            albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID));
            artist = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ARTIST));
        }

        public static Album getInstance( Cursor cursor )
        {
            return new Album( cursor );
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

//        @Override
//        public int compareTo(Album o) {
//            return getAlbumKey().compareTo(o.getAlbumKey());
//        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof  Album)
                return getAlbumKey().equals( ( (Album) obj ).getAlbumKey() );
            return false;
        }

        @Override
        public int hashCode() {
            return getAlbumKey().hashCode();
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

        //        @Override
//        public boolean equals(@Nullable Object obj) {
//            if (obj instanceof  Album)
//                return getAlbumKey().equals( ( (Album) obj ).getAlbumKey() );
//            return false;
//        }
    }

}
