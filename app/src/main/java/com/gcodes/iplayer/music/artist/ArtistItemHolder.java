package com.gcodes.iplayer.music.artist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;

public class ArtistItemHolder extends RecyclerView.ViewHolder
{

    private TextView title;
    private TextView subtitle;
    private ImageView image;

    public ArtistItemHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.item_title);
        subtitle = itemView.findViewById(R.id.item_subtitle);
        image = itemView.findViewById(R.id.item_image);
    }

    public String getTitle() {
        return title.getText().toString();
    }

    public void setTitle(String name) {
        this.title.setText( name );
    }

    public String getSubtitle() {
        return subtitle.getText().toString();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle.setText(subtitle);
    }

    public Bitmap getImage() {
        return image.getDrawingCache();
    }

    public void setImage(Fragment fragment, String id )
    {
        GlideApp.with( fragment.getContext() ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( fragment.getContext(), id, MediaStore.Audio.Media.ARTIST_ID ) )
                .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
    }

    public String getPath( Fragment fragment, int id )
    {
        CursorLoader loader = new CursorLoader( fragment.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{ String.valueOf(id)},
                null);
        Cursor cursor = loader.loadInBackground();
        String path = null;

        if ( cursor != null )
        {
            if ( cursor.moveToFirst()) {
                path = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART) );
                cursor.close();
            }
        }

        return path;
    }
}
