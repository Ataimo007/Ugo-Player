package com.gcodes.iplayer.music.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.GlideRequests;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.folder.FolderFragment;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerControlView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment
 */
public class FragmentMusic extends Fragment {

    private View controlView;
    private GlideRequests request;

    private Animation rotate;
    private Music currentMusic;
//    private SimpleExoPlayer player;
//    private DefaultDataSourceFactory factory;

    public FragmentMusic() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static FragmentMusic newInstance() {
        FragmentMusic fragment = new FragmentMusic();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        request = GlideApp.with(FragmentMusic.this);
        initRotateAnimation();
    }

    private void initRotateAnimation() {
        rotate = AnimationUtils.loadAnimation(getContext(), R.anim.u_rotate);
        rotate.setFillAfter( true );
    }

    public void startAnimation()
    {
        if ( controlView != null )
        {
            CardView art = controlView.findViewById(R.id.exo_album_art);
            art.startAnimation( rotate );
        }
    }

    public void pauseAnimation()
    {
        if ( controlView != null )
        {
            CardView art = controlView.findViewById(R.id.exo_album_art);
            art.clearAnimation();
        }
    }

    private void initView()
    {
//        PlayerView view;
        PlayerControlView control = controlView.findViewById(R.id.music_control_view);
        control.setShowTimeoutMs( -1 );
        control.setPlayer( MusicPlayer.getInstance().getPlayerManager() );
//        MusicPlayer.registerOnTrackChange(this::consumeTrack);
//        MusicPlayer.consumeTrack( this::consumeTrack );
        controlView.setOnClickListener( v -> {
            showMusicPlayer();
        });
        initRotateAnim();
    }

//    private void initRotateAnim() {
//        CardView art = controlView.findViewById(R.id.exo_album_art);
//        MusicPlayer.onStateChange( art );
//    }

    private void initRotateAnim() {
        MusicPlayer musicPlayer = MusicPlayer.getInstance();
        if ( musicPlayer.getPlayerManager().getPlayWhenReady() && musicPlayer.getPlayerManager().getPlaybackState() == Player.STATE_READY )
            startAnimation();
        else
            pauseAnimation();
    }

    private void showMusicPlayer()
    {
        Intent intent = new Intent( getContext(), MusicPlayerActivity.class );
        getContext().startActivity( intent );
    }

    public void consumeTrack( Music music )
    {
        if ( currentMusic != music )
        {
            TextView trackName = controlView.findViewById(R.id.exo_track);
            trackName.setText( music.getName() );
    //        TextView artistName = controlView.findViewById(R.id.exo_artist);
    //        artistName.setText( music.getArtist() );
            ImageView art = controlView.findViewById(R.id.exo_album);
            setImage( music, art);
            currentMusic = music;
        }
    }

    public void setImage(Music music, ImageView image)
    {
        if  ( FragmentMusic.this.isAdded() )
            GlideApp.with( FragmentMusic.this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( FragmentMusic.this.getContext(), music ) )
                    .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        controlView = inflater.inflate(R.layout.fragment_music, container, false);
        initView();
        return controlView;
    }

}
