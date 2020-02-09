package com.gcodes.iplayer.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.player.PlayerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import java.io.IOException;

import static com.gcodes.iplayer.helpers.ProcessModelLoaderFactory.*;

public final class ProcessModelLoaderFactory implements ModelLoaderFactory< ProcessFetcher, Bitmap>
{
    @NonNull
    @Override
    public ModelLoader<ProcessFetcher, Bitmap> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new ProcessModelLoader();
    }

    @Override
    public void teardown() {

    }

    public static class Test{}

    public final class ProcessModelLoader implements ModelLoader< ProcessFetcher, Bitmap> {

        @Nullable
        @Override
        public LoadData<Bitmap> buildLoadData(@NonNull ProcessFetcher process, int width, int height, @NonNull Options options) {
            return new LoadData<>(new ObjectKey(process.getKey()), new ProcessDataFetcher(process));
        }

        @Override
        public boolean handles(@NonNull ProcessFetcher supplier) {
            return true;
        }
    }

    public class ProcessDataFetcher implements DataFetcher< Bitmap >
    {

        private final ProcessFetcher fetcher;

        public ProcessDataFetcher( ProcessFetcher fetcher )
        {
            this.fetcher = fetcher;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
            Bitmap image = fetcher.load();
            callback.onDataReady( image );
        }

        @Override
        public void cleanup() {
            fetcher.cleanup();
        }

        @Override
        public void cancel() {
            fetcher.cancel();
        }

        @NonNull
        @Override
        public Class<Bitmap> getDataClass() {
            return Bitmap.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    public static interface ProcessFetcher
    {
        public default void cancel(){}
        public default void cleanup(){}
        public default Object getKey(){ return this; }
        public abstract Bitmap load();
    }

    public static class MusicProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {

//        private final static MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        private final Context context;
        private final Music music;

        public MusicProcessFetcher(Context context, Music music )
        {
            this.music = music;
            this.context = context;
        }

        public MusicProcessFetcher(Fragment fragment, Music music )
        {
            this.music = music;
            this.context = fragment.getContext();
        }

        @Override
        public Object getKey() {
            return music.toUri();
        }

        @Override
        public Bitmap load()
        {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource( context, music.toUri());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    Log.d("Content_Resolver", "load: content resolver thumbnail");
                    return context.getContentResolver().loadThumbnail( music.toUri(), new Size( 640, 480), null );
                } catch (IOException e) {
                    return null;
                }
            }
            else
            {
                Log.d("Content_Resolver", "load: old content resolver thumbnail");
                byte[] picture = retriever.getEmbeddedPicture();
                if ( picture != null )
                    return BitmapFactory.decodeByteArray(picture, 0, picture.length);
                return null;
            }
        }
    }

    public static class AlbumProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
//        private final static MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        private final Context context;
        private final String id;
        private Uri uri;

        public AlbumProcessFetcher(Context context, String id )
        {
            this.id = id;
            this.context = context;
            getAMusic();
        }

        public AlbumProcessFetcher(Fragment fragment, String id )
        {
            this(fragment.getContext(), id);
        }

        @Override
        public Object getKey() {
            return "album " + id;
        }

        private void getAMusic()
        {
            CursorLoader loader = new CursorLoader( context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.ALBUM_ID ),
                    new String[]{ "0", String.valueOf(id) }, MediaStore.Audio.Media._ID + " asc limit 1" );
            Cursor cursor = loader.loadInBackground();
            cursor.moveToFirst();
            uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID) )));
        }

        @Override
        public Bitmap load()
        {
            Log.d("Glide_Load", "loading " + uri );
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource( context, uri);
            byte[] picture = retriever.getEmbeddedPicture();
            if ( picture != null )
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            return null;
        }
    }
}
