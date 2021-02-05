package com.gcodes.iplayer.video.player;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavHost;
import androidx.navigation.NavHostController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;

import com.gcodes.iplayer.helpers.CustomVideoGesture;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.model.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;

import static android.app.Activity.RESULT_OK;

public class VideoController extends Fragment {

    private View controlView;
    private Video currentVideo;
//    private PlayerManager playerManager;
//    private PlayerManager.VideoManager player;
    private boolean playing = false;
    private boolean expanded = false;
    private PlayerView control;

    private final PlayerManager.VideoManager videoManager;
    private ControlListener controlListener;
    private ActivityResultLauncher<Intent> player;

    public VideoController(PlayerManager.VideoManager videoManager) {
        this.videoManager = videoManager;
    }

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
//        playerManager = player.getPlayerManager();
        videoManager.getPlayerManager().setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        videoManager.getPlayerManager().setView( control );

        Log.w("Video_Controller", "Continue Playing" );
        videoManager.continuePlay();
        control.showController();

        prepareTouch( control, videoManager );
        prepareStop(videoManager);

        controlListener = new ControlListener(videoManager);
        videoManager.addListener(controlListener);
        Log.d("Video_Controller", "Added Listener" );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        videoManager.removeListener(controlListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoManager != null)
            videoManager.play();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoManager != null && videoManager.getPlayerManager().isVideoPlaying())
            videoManager.pause();
    }

    private void prepareStop(PlayerManager.VideoManager player) {
        ImageView stop = controlView.findViewById(R.id.exo_stop);
        stop.setOnClickListener( v -> {
            stop();
        });
    }

    private void stop() {
        videoManager.stop();
        new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).removeVideoController(requireActivity().getSupportFragmentManager());
        videoManager.removeListener(controlListener);
        Log.d("Video_Controller", "Removed Listener" );
    }

    private void prepareTouch(PlayerView control, PlayerManager.VideoManager player ) {
        CustomVideoGesture gesture = new CustomVideoGesture( getContext(), new Helper.VideoGestureChangeListener(), () ->
                player.getPlayerManager().getPlayer(), new CustomVideoGesture.GestureAction(){
                    @Override
                    public void onClick() {
                        expanded = true;
                        if  ( player.getCurrentType() == PlayerManager.VideoSourceType.VIDEOS )
                            showVideoPlayer();
                        if  ( player.getCurrentType() == PlayerManager.VideoSourceType.SERIES )
                            showSeriesFragment();
//                        detach();
                    }
                }
        );
        control.setOnTouchListener( gesture );
    }

    private void detach() {
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.remove(this);
        fragmentTransaction.commitNow();
    }

    private void showSeriesFragment() {
//        NavController navController = Navigation.findNavController(requireActivity(), R.id.video_session);
        NavController navController = Navigation.findNavController(requireActivity(), R.id.video_host);
        if (navController.getCurrentDestination().getId() == R.id.videoFragment)
            navController.navigate( R.id.action_videoFragment_to_seriesPlayerFragment );
        if (navController.getCurrentDestination().getId() == R.id.videoSearchFragment)
            navController.navigate( R.id.action_videoSearchFragment_to_seriesPlayerFragment );
        detach();
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
        videoManager.saveTo(intent);
//        intent.putExtra( "data_type", "controller" );
//        activity.startActivity(intent);
//        fragment.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
//        startActivityForResult( intent, PlayerManager.REQUEST_VIDEO_PLAYER );
        player.launch(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        player = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Log.d("Video_Controller", "Rendering Video Controller");
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).showVideoController(requireActivity().getSupportFragmentManager());
            }
        });
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if ( requestCode == PlayerManager.REQUEST_VIDEO_PLAYER && resultCode == RESULT_OK )
//        {
//            Log.d("Video_Controller", "Rendering Video Controller");
//            new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).showVideoController(requireActivity().getSupportFragmentManager());
//        }
//    }

    public void consumeVideo(Video video) {
        if ( currentVideo != video )
        {
            TextView trackName = controlView.findViewById(R.id.exo_track);
            trackName.setText( video.getName() );
            currentVideo = video;
        }
    }

    private class ControlListener implements Player.EventListener {

        private PlayerManager.VideoManager manager;

        public ControlListener(PlayerManager.VideoManager manager) {
            this.manager = manager;
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            int index = manager.getPlayerManager().getCurrentIndex();
            Video video = manager.getVideo(index);
            consumeVideo(video);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            if (!playWhenReady && manager.getPlayerManager().getPlayer().getDuration() == manager.getPlayerManager().getPlayer().getCurrentPosition())
//                stop();
            Log.d("Video_Controller", String.format("State changed playWhenReady %b, playbackState %d", playWhenReady, playbackState));
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED)
                stop();
        }
    }
}
