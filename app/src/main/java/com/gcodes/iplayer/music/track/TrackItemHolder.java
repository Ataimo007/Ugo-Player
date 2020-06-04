package com.gcodes.iplayer.music.track;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class TrackItemHolder extends RecyclerView.ViewHolder
{

    private final CardView imageContainer;
    private final TextView title;
    private final TextView subtitle;
    private final ImageView image;
    private final View selected;

    public TrackItemHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.item_title);
        subtitle = itemView.findViewById(R.id.item_subtitle);
        image = itemView.findViewById(R.id.item_image);
        selected = itemView.findViewById(R.id.item_selected);
        imageContainer = itemView.findViewById(R.id.item_card);
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

    public void select( boolean selected, Context context )
    {
        if  ( selected )
        {
            this.selected.setVisibility( View.VISIBLE );
            Animation animation = initRotateAnimation(context);
            startAnimation( animation );
        }
        else
        {
            this.selected.setVisibility( View.GONE );
            pauseAnimation();
        }
    }

    private Animation initRotateAnimation(Context context) {
        Animation rotate = AnimationUtils.loadAnimation(context, R.anim.u_rotate);
        rotate.setFillAfter( true );
        return rotate;
    }

    public void startAnimation(Animation rotate)
    {
        imageContainer.startAnimation( rotate );
    }

    public void pauseAnimation()
    {
        imageContainer.clearAnimation();
    }

    public void setImage(Context context, Music music)
    {
        GlideApp.with( context ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( context, music ) )
                .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
    }
}
