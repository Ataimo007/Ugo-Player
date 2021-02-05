package com.gcodes.iplayer.music.models;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.gcodes.iplayer.MainActivity;
import com.google.common.base.Objects;

public class Genre {

    private final long id;
    private final String name;
    private Integer count;

    public Genre(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Genre getInstance(Cursor cursor) {
        return new Genre(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)));
    }

    public static final String sort = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER + " asc";

    public static final String[] projection = {
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    private static final String[] countProjection = {
            MediaStore.Audio.Genres.Members._ID,
    };

    public Integer getCount(int pos, LoaderManager loader, Context context, Consumer<Integer> consumer)
    {
        if (count == null)
        {
            loader.restartLoader(pos, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                @NonNull
                @Override
                public Loader<Cursor> onCreateLoader(int pos, @Nullable Bundle args) {
                    return new CursorLoader( context,
                            MediaStore.Audio.Genres.Members.getContentUri("external", id ), countProjection,
                            null, null, null );
                }

                @Override
                public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                    count = data.getCount();
                    consumer.accept(count);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<Cursor> loader) {}
            });
        }
        return count;
    }


    public Integer getCount(Context context)
    {
        if (count == null)
        {
            CursorLoader loader = new CursorLoader( context,
                    MediaStore.Audio.Genres.Members.getContentUri( "external", id ), countProjection,
                    null, null, null );
            Cursor cursor = loader.loadInBackground();
            count = cursor.getCount();
        }
        return count;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre)) return false;
        Genre genre = (Genre) o;
        return getId() == genre.getId() &&
                Objects.equal(getName(), genre.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getName());
    }
}
