package com.gcodes.iplayer.helpers;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import androidx.annotation.NonNull;

@GlideModule
public final class GlideModules extends AppGlideModule
{
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend( ProcessModelLoaderFactory.ProcessFetcher.class, Bitmap.class, new ProcessModelLoaderFactory() );
    }
}
