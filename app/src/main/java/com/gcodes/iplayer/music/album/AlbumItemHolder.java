package com.gcodes.iplayer.music.album;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class AlbumItemHolder extends RecyclerView.ViewHolder
{
    private TextView title;
    private TextView subtitle;
    private ImageView image;

    public AlbumItemHolder(@NonNull View itemView) {
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
        setImage( fragment.getContext(), id );
    }

    public void setImage(Context context, String id )
    {
        GlideApp.with( context ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( context, id, MediaStore.Audio.Media.ALBUM_ID ) )
                .placeholder( R.drawable.u_song_solid ).apply( circleCropTransform() ).into( image );
    }
}
