package com.gcodes.iplayer.music.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment
 */
public class MusicController extends Fragment {

    private View controlView;

    private Animation rotate;
    private Music currentMusic;

    // TODO: Rename and change types and number of parameters
    public static MusicController newInstance() {
        MusicController fragment = new MusicController();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).playerManager.observe(requireActivity(), manager -> {
            if ( manager != null )
            {
                ControlListener controlListener = new ControlListener(manager);
                manager.addListener(controlListener);
            }
        });
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
        controlView.setOnClickListener( v -> {
            showMusicPlayer();
        });
    }

    private void initView(PlayerManager manager) {
        PlayerControlView control = controlView.findViewById(R.id.music_control_view);
        control.setShowTimeoutMs( -1 );
        control.setPlayer( manager.getPlayer() );
        if ( manager.getPlayer().getPlayWhenReady() && manager.getPlayer().getPlaybackState() == Player.STATE_READY )
            startAnimation();
        else
            pauseAnimation();
    }

    private void showMusicPlayer()
    {
        Intent intent = new Intent( getContext(), MusicPlayerActivity.class );
        getContext().startActivity( intent );
    }

    private void showMusicController()
    {
        controlView.findViewById(R.id.controller_host).setVisibility(View.VISIBLE);
    }

    private void hideMusicController()
    {
        controlView.findViewById(R.id.controller_host).setVisibility(View.GONE);
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
        if  ( MusicController.this.isAdded() )
            GlideApp.with( MusicController.this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( MusicController.this.getContext(), music ) )
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

    private class ControlListener implements Player.EventListener {

        private PlayerManager manager;

        public ControlListener(PlayerManager manager) {
            this.manager = manager;
            initView(manager);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            int index = manager.getPlayer().getCurrentPeriodIndex();
            Music music = manager.getMusicManager().getMusic(index);
            consumeTrack(music);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                if ( playing == MediaType.MUSIC && isMusicPlayer() )
            if ( manager.isMusicPlaying() )
            {
                if ( playWhenReady )
                {
                    showMusicController();
                    if ( playbackState == Player.STATE_READY )
                        startAnimation();
                }
                else
                {
                    pauseAnimation();
                    hideMusicController();
                }
            }
        }
    }

}
