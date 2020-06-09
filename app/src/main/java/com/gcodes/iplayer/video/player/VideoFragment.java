package com.gcodes.iplayer.video.player;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.gcodes.iplayer.R;

import com.gcodes.iplayer.helpers.CustomVideoGesture;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerView;

public class VideoFragment extends Fragment {

    private View controlView;
    private Video currentVideo;
    private PlayerManager playerManager;
    private VideoPlayer player;
    private boolean playing = false;
    private boolean expanded = false;
    private PlayerView control;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        controlView = inflater.inflate(R.layout.fragment_video, container, false);
        initView();
        return controlView;
    }

    private void initView()
    {
        Log.w("Video_Fragment", "Setting up player" );

        control = controlView.findViewById(R.id.video_control_view);
//        control.setControllerShowTimeoutMs( -1 );
        player = VideoPlayer.getInstance();
        playerManager = player.getPlayerManager();
        playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        playerManager.setView( control );
        player.continuePlay();
        control.showController();

        prepareTouch( control );
        prepareStop();
    }

    private void prepareStop() {
        ImageView stop = controlView.findViewById(R.id.exo_stop);
        stop.setOnClickListener( v -> {
            player.stop();
            player.tryHideVideoPlayer();
        });
    }

    private void prepareTouch(PlayerView control) {
        CustomVideoGesture gesture = new CustomVideoGesture( getContext(), new Helper.VideoGestureChangeListener(), () ->
                playerManager.getPlayer(), new CustomVideoGesture.GestureAction(){
                    @Override
                    public void onClick() {
                        expanded = true;
                        player.saveState();
                        if  ( player.getCurrentType() == VideoPlayer.MediaType.VIDEOS )
                            showVideoPlayer();
                        if  ( player.getCurrentType() == VideoPlayer.MediaType.SERIES )
                            showSeriesFragment();
                    }
                }
        );
        control.setOnTouchListener( gesture );

//        controlView.setOnClickListener( v -> {
//            showVideoPlayer();
//        });
    }

    private void showSeriesFragment() {
        player.showSeriesFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        expanded = false;
        Log.w("Video_Fragment", "Setting up player from resume" );
        if ( playing && player.hasState() )
        {
            Log.w( "Series_Player", "playing again" );
            playerManager.setView( control );
            control.showController();
        }
        else
        {
            playing = true;
        }
        player.play();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.w("Video_Fragment", "Stopping player" );
//        playerManager.stop();
        if ( !expanded )
            player.pause();
        control.setPlayer(null);
    }

    private void showVideoPlayer() {
        VideoPlayer.play(this);
    }

    public void consumeVideo(Video video) {
        if ( currentVideo != video )
        {
            TextView trackName = controlView.findViewById(R.id.exo_track);
            trackName.setText( video.getName() );
            currentVideo = video;
        }
    }

    public static VideoFragment newInstance() {
        VideoFragment fragment = new VideoFragment();
        return fragment;
    }
}
