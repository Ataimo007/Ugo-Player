package com.gcodes.iplayer.music.player;

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

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.models.Music;
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

    private final PlayerManager.MusicManager manager;
    private ControlListener controlListener;

    public MusicController(PlayerManager.MusicManager manager) {
        super();
        this.manager = manager;
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

    private void initView() {
        PlayerControlView control = controlView.findViewById(R.id.music_control_view);
        control.setShowTimeoutMs( -1 );
        control.setPlayer( manager.getPlayerManager() );
        initRotateAnimation();

        if (  manager.getPlayerManager().getPlayWhenReady() &&  manager.getPlayerManager().getPlaybackState() == Player.STATE_READY )
            startAnimation();
        else
            pauseAnimation();

        controlView.setOnClickListener( v -> {
            showMusicPlayer();
        });
        prepareStop();

        controlListener = new ControlListener(manager);
        manager.addListener(controlListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        manager.removeListener(controlListener);
    }

    private void prepareStop() {
        ImageView stop = controlView.findViewById(R.id.exo_stop);
        stop.setOnClickListener( v -> {
            stop();
        });
    }

    private void stop() {
        manager.stop();
        new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).removeMusicController(requireActivity().getSupportFragmentManager());
        manager.removeListener(controlListener);
        Log.d("Video_Controller", "Removed Listener" );
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
        if  ( MusicController.this.isAdded() )
            GlideApp.with( MusicController.this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( MusicController.this.getContext(), music ) )
                    .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView = inflater.inflate(R.layout.fragment_music, container, false);
        initView();
        return controlView;
    }

    private class ControlListener implements Player.EventListener {

        private PlayerManager.MusicManager manager;

        public ControlListener(PlayerManager.MusicManager manager) {
            this.manager = manager;
            consume();
            sync();
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            consume();
        }

        private void consume()
        {
            int index = manager.getPlayerManager().getCurrentPeriodIndex();
            Music music = manager.getMusic(index);
            consumeTrack(music);
        }

        private void sync()
        {
            if ( manager.getPlayerManager().getPlayWhenReady() && manager.getPlayerManager().getPlaybackState() == Player.STATE_READY )
                startAnimation();
            else
                pauseAnimation();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if ( playWhenReady && playbackState == Player.STATE_READY )
                startAnimation();
            else
                pauseAnimation();
        }
    }

}
