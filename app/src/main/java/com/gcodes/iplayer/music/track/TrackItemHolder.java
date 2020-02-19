package com.gcodes.iplayer.music.track;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class TrackItemHolder extends RecyclerView.ViewHolder
{

    private TextView title;
    private TextView subtitle;
    private ImageView image;

    public TrackItemHolder(@NonNull View itemView) {
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

    public void setImage(Fragment fragment, Music music)
    {
        setImage( fragment.getContext(), music );
    }

    public void setImage(Context context, Music music)
    {
        GlideApp.with( context ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( context, music ) )
                .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
    }
}
