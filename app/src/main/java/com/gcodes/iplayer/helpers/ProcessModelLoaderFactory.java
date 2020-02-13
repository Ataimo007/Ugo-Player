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
            return "music " + music.toUri();
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

    public static class MusicCategoryProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
//        private final static MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        private final Context context;
        private final String id;
        private final String cat;
        private byte[] picture;

        public MusicCategoryProcessFetcher(Context context, String id, String cat )
        {
            this.id = id;
            this.context = context;
            this.cat = cat;
            getAMusic();
        }

        public boolean hasPicture()
        {
            return picture != null;
        }

        public MusicCategoryProcessFetcher(Fragment fragment, String id, String cat )
        {
            this(fragment.getContext(), id, cat);
        }

        @Override
        public Object getKey() {
            return "album " + id;
        }

        private void getAMusic()
        {
            CursorLoader loader = new CursorLoader( context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC, cat ),
                    new String[]{ "0", String.valueOf(id) }, MediaStore.Audio.Media._ID + " asc" );
            Cursor cursor = loader.loadInBackground();
            cursor.moveToFirst();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            do{
                Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID) )));
                try {
                    retriever.setDataSource( context, uri);
                    picture = retriever.getEmbeddedPicture();
                    Log.d("Glide_Load", "loading " + uri );
                    if ( picture != null )
                        break;
                }
                catch (Exception ignored){}
            } while ( cursor.moveToNext() );
        }

        @Override
        public Bitmap load()
        {
            if ( picture != null )
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            return null;
        }
    }

}
