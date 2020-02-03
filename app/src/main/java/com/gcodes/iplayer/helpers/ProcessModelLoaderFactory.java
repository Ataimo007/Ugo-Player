package com.gcodes.iplayer.helpers;

import android.graphics.Bitmap;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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


}
