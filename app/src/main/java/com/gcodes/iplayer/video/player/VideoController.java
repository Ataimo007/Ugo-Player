package com.gcodes.iplayer.video.player;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;

import com.gcodes.iplayer.helpers.CustomVideoGesture;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;

public class VideoController extends Fragment {

    private View controlView;
    private Video currentVideo;
//    private PlayerManager playerManager;
//    private PlayerManager.VideoManager player;
    private boolean playing = false;
    private boolean expanded = false;
    private PlayerView control;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        controlView = inflater.inflate(R.layout.fragment_video, container, false);
        return controlView;
    }

    private void init() {
        new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).getLivePlayerManager().observe(this, manager -> {
            if ( manager != null )
            {
                ControlListener controlListener = new ControlListener(manager);
                manager.addListener(controlListener);
            }
        });
    }

    private void initView(PlayerManager playerManager)
    {
        Log.w("Video_Fragment", "Setting up player" );

        control = controlView.findViewById(R.id.video_control_view);
//        control.setControllerShowTimeoutMs( -1 );
        PlayerManager.VideoManager player = playerManager.getVideoManager();
//        playerManager = player.getPlayerManager();
        playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        playerManager.setView( control );
        player.continuePlay();
        control.showController();

        prepareTouch( control, player );
        prepareStop(player);
    }

    private void prepareStop(PlayerManager.VideoManager player) {
        ImageView stop = controlView.findViewById(R.id.exo_stop);
        stop.setOnClickListener( v -> {
            player.stop();
        });
    }

    private void prepareTouch(PlayerView control, PlayerManager.VideoManager player ) {
        CustomVideoGesture gesture = new CustomVideoGesture( getContext(), new Helper.VideoGestureChangeListener(), () ->
                player.getPlayerManager().getPlayer(), new CustomVideoGesture.GestureAction(){
                    @Override
                    public void onClick() {
                        expanded = true;
                        player.saveState();
                        if  ( player.getCurrentType() == PlayerManager.VideoSourceType.VIDEOS )
                            showVideoPlayer();
                        if  ( player.getCurrentType() == PlayerManager.VideoSourceType.SERIES )
                            showSeriesFragment();
                    }
                }
        );
        control.setOnTouchListener( gesture );
    }

    private void showSeriesFragment() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.video_session);
        Bundle bundle = new Bundle();
        navController.navigate( R.id.action_videoFragment_to_seriesPlayerFragment, bundle );
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        expanded = false;
//        Log.w("Video_Fragment", "Setting up player from resume" );
//        if ( playing && player.hasState() )
//        {
//            Log.w( "Series_Player", "playing again" );
//            playerManager.setView( control );
//            control.showController();
//        }
//        else
//        {
//            playing = true;
//        }
//        player.play();
//    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        Log.w("Video_Fragment", "Stopping player" );
////        playerManager.stop();
//        if ( !expanded )
//            player.pause();
//        control.setPlayer(null);
//    }

    private void showVideoPlayer() {
//        VideoPlayer.play(this);

        Intent intent = new Intent( getContext(), VideoPlayerActivity.class );
        intent.putExtra( "data_type", "controller" );
//        activity.startActivity(intent);
//        fragment.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
        startActivity( intent );
    }

    public void consumeVideo(Video video) {
        if ( currentVideo != video )
        {
            TextView trackName = controlView.findViewById(R.id.exo_track);
            trackName.setText( video.getName() );
            currentVideo = video;
        }
    }

    private void showMusicController()
    {
        controlView.findViewById(R.id.controller_host).setVisibility(View.VISIBLE);
    }

    private void hideMusicController()
    {
        controlView.findViewById(R.id.controller_host).setVisibility(View.GONE);
    }

    private void prepare()
    {
        PlayerManager.VideoManager videoManager = new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).getVideoManager();
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
            Video video = manager.getVideoManager().getVideo(index);
            consumeVideo(video);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                if ( playing == MediaType.MUSIC && isMusicPlayer() )
            if ( manager.isVideoPlaying() )
            {
                if ( playWhenReady )
                    showMusicController();
                else
                    hideMusicController();
            }
        }
    }
}
