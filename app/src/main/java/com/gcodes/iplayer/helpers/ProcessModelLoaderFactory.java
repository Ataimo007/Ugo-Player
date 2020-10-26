package com.gcodes.iplayer.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.gcodes.iplayer.music.models.Music;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import java.util.Locale;

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

            Log.d("Content_Resolver", "load: old content resolver thumbnail");
            byte[] picture = retriever.getEmbeddedPicture();
            if ( picture != null )
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            return null;
        }
    }

    public static class MusicDualCategoryProcessFetcher extends MusicCategoryProcessFetcher
    {
        private final String id2;
        private final String cat2;

        public MusicDualCategoryProcessFetcher(Context context, String id, String cat, String id2, String cat2 ) {
            super(context, id, cat);
            this.id2 = id2;
            this.cat2 = cat2;
        }

        public MusicDualCategoryProcessFetcher(Fragment fragment, String id, String cat, String id2, String cat2) {
            this(fragment.getContext(), id, cat, id2, cat2);
        }

        @Override
        protected CursorLoader getLoader()
        {
            return new CursorLoader( context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    String.format( "%s != ? and %s = ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC, cat, cat2 ),
                    new String[]{ "0", id, id2 }, MediaStore.Audio.Media._ID + " asc" );
        }

        public Object getKey() {
            return String.format( Locale.ENGLISH, "%s %s %s %s", cat, id, cat2, id2 );
        }
    }

    public static class MusicCategoryProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
//        private final static MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        protected final Context context;
        protected final String id;
        protected final String cat;
        protected byte[] picture;

        public MusicCategoryProcessFetcher(Context context, String id, String cat )
        {
            this.id = id;
            this.context = context;
            this.cat = cat;
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
            return cat + " " + id;
        }

        private void initMusic()
        {
            if  ( Looper.myLooper() == null || Looper.myLooper().getThread() != Thread.currentThread() )
                Looper.prepare();
            picture = getPicture( id, cat );
        }

        protected CursorLoader getLoader()
        {
            return new CursorLoader( context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC, cat ),
                    new String[]{ "0", id }, MediaStore.Audio.Media._ID + " asc" );
        }

        protected byte[] getPicture(String id, String cat )
        {
            CursorLoader loader = getLoader();
            Cursor cursor = loader.loadInBackground();
            cursor.moveToFirst();

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            byte[] picture;
            do{
                Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(cursor.getLong( cursor.getColumnIndex(MediaStore.Audio.Media._ID) )));
                try {
                    retriever.setDataSource( context, uri);
                    picture = retriever.getEmbeddedPicture();
                    Log.d("Glide_Load", "loading " + uri );
                    if ( picture != null )
                        return picture;
                }
                catch (Exception ignored){}
            } while ( cursor.moveToNext() );
            return null;
        }

        @Override
        public Bitmap load()
        {
            initMusic();
            if ( picture != null )
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            return null;
        }
    }

    public static class CustomGenreProcessFetcher extends GenreProcessFetcher
    {
        private final String cat;
        private final String id;
        private final String[] genreProjection = {
                MediaStore.Audio.Genres.Members._ID,
                MediaStore.Audio.Genres.Members.ALBUM_KEY,
                MediaStore.Audio.Genres.Members.ARTIST_KEY
        };

        public CustomGenreProcessFetcher(Fragment fragment, long genreId, String cat, String id) {
            this(fragment.getContext(), genreId, cat, id);
        }

        public CustomGenreProcessFetcher(Context context, long genreId,  String cat, String id) {
            super(context, genreId);
            this.cat = cat;
            this.id = id;
        }

        @Override
        public Object getKey() {
            return String.format("genre %s %s %s",  genreId, cat, id );
        }

        protected CursorLoader getLoader()
        {
            return new CursorLoader( context,
                    MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
                    String.format( "%s = ?", cat ), new String[]{ id }, MediaStore.Audio.Media._ID + " COLLATE LOCALIZED ASC" );
        }

    }

    public static class GenreProcessFetcher extends MusicCategoryProcessFetcher
    {

        protected final long genreId;

        public GenreProcessFetcher(Fragment fragment, long genreId) {
            super(fragment, null, null);
            this.genreId = genreId;
        }

        public GenreProcessFetcher(Context context, long genreId) {
            super(context, null, null);
            this.genreId = genreId;
        }

        @Override
        public Object getKey() {
            return "genre " + genreId;
        }

        protected CursorLoader getLoader()
        {
            return new CursorLoader( context,
                    MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), new String[]{MediaStore.Audio.Genres.Members._ID},
                    null, null, MediaStore.Audio.Media._ID + " COLLATE LOCALIZED ASC" );
        }

        @Override
        public Bitmap load() {

            if  ( Looper.myLooper() == null || Looper.myLooper().getThread() != Thread.currentThread() )
                Looper.prepare();

            CursorLoader loader = getLoader();
            Cursor cursor = loader.loadInBackground();

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            if  ( cursor.moveToFirst() )
            {
                do
                {
                    Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf( cursor.getLong( cursor.getColumnIndex( MediaStore.Audio.Genres.Members._ID ) ) ) );
                    retriever.setDataSource( context, uri );
                    byte[] picture = retriever.getEmbeddedPicture();
                    if ( picture != null )
                        return BitmapFactory.decodeByteArray(picture, 0, picture.length);
                } while ( cursor.moveToNext() );
            }

            return null;
        }
    }

}
