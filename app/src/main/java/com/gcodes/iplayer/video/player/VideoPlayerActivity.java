package com.gcodes.iplayer.video.player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.core.util.Supplier;
import androidx.lifecycle.Lifecycle;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.music.player.MusicPlayerActivity;
import com.gcodes.iplayer.player.PlayerService;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CustomVideoGesture;
import com.gcodes.iplayer.services.OpenSubtitleService;
import com.gcodes.iplayer.video.model.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.jarvanmo.exoplayerview.gesture.OnVideoGestureChangeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class VideoPlayerActivity extends AppCompatActivity {

//    private static SimpleExoPlayer player;

    private PopupWindow seekToPopup;
    private PlayerView playerView;
    private PlayerManager playerManager;

    private final Handler videoHandler = new Handler();
    private PopupWindow brightnessPopup;
    private PopupWindow volumePopup;
    private PlayerManager.VideoManager player;
    private OpenSubtitleService openSubtitleService;
    private ArrayList<Supplier<Boolean>> touchables;
    private PlayerService.PlayerBinder serviceBinder;
//    private DefaultDataSourceFactory factory;

    private class PlayerConnection implements ServiceConnection
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            preparePlayer(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            releasePlayer();
        }
    }

    public class VideoBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("player_info", "Player Service as Started" );
            if (intent.getBooleanExtra("has_player", false))
            {
                Log.d("player_info", "Attaching to Player Service" );
                attachPlayer();
            }
            unregisterReceiver(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent();

        setContentView(R.layout.video_player);

        initPlayer();
        initPopup();
    }

    private void processIntent()
    {
        Intent current = getIntent();
        if ( current.getAction() != null && current.getAction().equals(Intent.ACTION_VIEW) )
        {
            registerReceivers();
            externalPlayRequest();
        }
        else
            attachPlayer();
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_START_PLAYER_MANAGER);
        registerReceiver(new VideoBroadCastReceiver(), filter);
    }

    private void externalPlayRequest()
    {
        Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
        intent.putExtra( "type", "video" );
        intent.putExtra("broadcast", true );
        getApplication().getBaseContext().startService( intent );
    }

    @Override
    public void onBackPressed() {
        Log.d("Video_Controller", "Setting Result Code " + RESULT_OK);
        if ( isUrlSource() )
        {
            player.stop();
            playerView.setPlayer(null);
        }
        else
        {
            if (isTaskRoot())
            {
                Log.d("Video_Controller", "Root Activity initiating App Result Code " + RESULT_OK);
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("controller_check", true );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            else
            {
                Log.d("Video_Controller", "Initiating normal back press Result Code " + RESULT_OK);
                Intent intent = new Intent();
                if ( getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW) )
                {
                    Log.d("Video_Controller", "Controller Checker for Action View");
                    intent.putExtra("controller_check", true );
                }
                player.saveTo(intent);
                setResult(RESULT_OK, intent);
                finish();
            }
//            Log.w("Video_Player", String.format("Video Controller state setting back result" ) );
//            setResult( VideoPlayer.RESULT_PLAYING );
        }
        super.onBackPressed();
    }

    private boolean isUrlSource() {
        return getIntent().hasExtra("data_type") && getIntent().getStringExtra("data_type").equals("url");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        Log.d("Picture_In_Picture", "Picture in Picture Change State PIP Mode " + isInPictureInPictureMode);
        Log.d("Picture_In_Picture", "Picture in Picture Change Window View " + getWindow().getDecorView().getRootView().isShown());

        if ( !isInPictureInPictureMode && !getWindow().getDecorView().getRootView().isShown())
        {
            Log.d("Picture_In_Picture", "Activity Dismiss");
            player.stop();
            serviceBinder.stopService();
        }

        if  ( isInPictureInPictureMode )
        {
            findViewById(R.id.video_player_controller).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.video_player_controller).setVisibility(View.VISIBLE);
        }
    }

    private void initPopup()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        View seekTo = inflater.inflate(R.layout.popup_seekto, null);
        View brightness = inflater.inflate(R.layout.popup_brightness, null);
        View volume = inflater.inflate(R.layout.popup_volume, null);
        seekToPopup = new PopupWindow(seekTo, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);
        brightnessPopup = new PopupWindow(brightness, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);
        volumePopup = new PopupWindow(volume, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);
    }

    private void hideStatus()
    {
        View mContentView = findViewById(R.id.video_player);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mVisible = false;
    }

    private void showStatus()
    {
        Log.d( "Video_Player", "Showing Status " );
        View mContentView = findViewById(R.id.video_player);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
////        player.setPlayWhenReady(true);
//    }

    private void begin()
    {
        if ( !player.playFrom(getIntent()) )
        {
            player.play(getIntent());
//            player.continuePlay();
        }
    }

//    private boolean isFromController()
//    {
//        String dataType = getIntent().getStringExtra("data_type");
//        return dataType.equals("controller");
//    }

    //    private final Handler testHandler = new Handler();
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null)
            player.play();
//        begin();
    }

//    private void continuePlaying(Intent intent)
//    {
//        player.playFrom(intent);
//    }

    private Video getVideoAt( int position )
    {
        return player.getVideo( position );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if ( isInPictureInPictureMode() )
                    return;
            }
            player.pause();
        }
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        Log.d("Picture_In_Picture", "When Activity on Stop" );
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
////        Log.d("Picture_In_Picture", "When Activity on Stop " +  isInPictureInPictureMode() );
////            if (isInPictureInPictureMode())
////            {
////                player.stop();
////                serviceBinder.stopService();
////            }
////        }
//    }

    private void initPlayer()
    {
        initControls();
    }

    private void preparePlayer(IBinder service)
    {
        serviceBinder = (PlayerService.PlayerBinder) service;
        player = ((PlayerService.PlayerBinder) service).getVideoManager();
        processIntent(getIntent());
        playerManager = player.getPlayerManager();
        initView();
        initSubtitle();
        begin();
    }

    private void processIntent(Intent intent) {
        Log.d("player_info", "Processing the Intent" );
        if (intent.hasExtra("video"))
        {
            String[] gsonVideos = intent.getStringArrayExtra("video");
            Video[] videos = Video.fromGson(gsonVideos);
            if  ( intent.hasExtra("begin") )
            {
                int begin = intent.getIntExtra("begin", -1);
                player.initVideoSources(videos, begin);
            }
            else
                player.initVideoSources(videos);
        }
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW))
            player.initVideoSource(intent.getData());
    }

    private void releasePlayer()
    {
        player = null;
        playerManager = null;
    }

    private void attachPlayer() {
        Intent intent = new Intent(this, PlayerService.class);
        PlayerConnection connection = new PlayerConnection();
        bindService(intent, connection, BIND_IMPORTANT);
    }

    private void initSubtitle() {
        openSubtitleService = new OpenSubtitleService(playerView);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        openSubtitleService.destroy();
        openSubtitleService = null;

//        if ( playerManager.hasState() )
//            playerManager.restoreState();
    }

    private void initControls() {
        ImageView subtitleButton = findViewById(R.id.exo_custom_subtitle);
        Button hideButton = findViewById(R.id.subtitle_hide);
        Button changeButton = findViewById(R.id.subtitle_change);
        subtitleButton.setOnClickListener( v -> {
            showSubtitle();
        });
        hideButton.setOnClickListener(v -> {
            hideSubtitle();
            hideSubtitle();
        });
        changeButton.setOnClickListener(v -> {
            hideSubtitle();
            changeSubtitle();
        });
    }

    private void showSubtitle()
    {
        int position = playerManager.getCurrentWindow();
        Video video = getVideoAt(position);
        openSubtitleService.subtitle(video);
    }

    private void hideSubtitle()
    {
        int position = playerManager.getCurrentWindow();
        Video video = getVideoAt(position);
        openSubtitleService.hideSubtitle(video);
    }

    private void changeSubtitle()
    {
        int position = playerManager.getCurrentWindow();
        Video video = getVideoAt(position);
        openSubtitleService.changeSubtitle(video);
    }

    public void showSubtitleOptions()
    {
        findViewById(R.id.subtitle_options).setVisibility(View.VISIBLE);
        addTouchable(() -> {
            hideSubtitleOptions();
            return true;
        });
    }

    public boolean isSubtitleOptionsShown()
    {
        return findViewById(R.id.subtitle_options).getVisibility() == View.VISIBLE;
    }

    public void hideSubtitleOptions()
    {
        findViewById(R.id.subtitle_options).setVisibility(View.GONE);
    }

    public Pair<ConcatenatingMediaSource, MediaSource> generateSource(File subtitleFile, Video video) {
        int videoIndex = player.getCurrentVideo() == video ? player.getCurrentIndex() : player.findIndex( video );
        SingleSampleMediaSource subtitleSource = player.getSubtitle(subtitleFile);
        return player.buildNewMergedSource(subtitleSource, videoIndex);
    }

    public Pair<ConcatenatingMediaSource, MediaSource> generateSource(MediaSource source, Video video) {
        int videoIndex = player.getCurrentVideo() == video ? player.getCurrentIndex() : player.findIndex( video );
        return player.buildNewSource(source, videoIndex);
    }

    public void applySource(ConcatenatingMediaSource source)
    {
        player.switchSources( source );
    }

    private Consumer< String > messageCallback()
    {
        Toast toast = new Toast( this );
        return s -> {
            toast.setText( s );
            toast.show();
        };
    }

    private void initView()
    {
        playerView = findViewById(R.id.video_player);
        playerManager.setView( playerView );
        Log.d("Player_View", "current state " + playerManager.getPlayer().getPlaybackState());

        playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT )
        {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT );
            playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        }
        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE )
        {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL );
            playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        }

        playerManager.addListener( new KeepScreenOn(playerView) );
        playerView.setBackgroundColor(Color.BLACK);

        setCustomGestureListener();

    }

//    private void initSources()
//    {
//        Bundle vid = getIntent().getBundleExtra("medias");
//        Video video = Video.getIntance(vid);
//        getSource( video );
//    }

//    private void initSource()
//    {
//        String dataType = getIntent().getStringExtra("data_type");
//        String[] medias = getIntent().getStringArrayExtra("medias");
//        int begin = getIntent().getIntExtra("begin", -1);
//        switch ( dataType )
//        {
//            case "video" :
////                boolean displaySubtitle = isDisplaySubtitle();
//                player.initVideoSources(medias, begin, playerView);
//                break;
//
//            case "url" :
//                player.initOnlineSources( medias, begin );
//                break;
//
//            case "controller" :
//                player.initSavedSource();
//        }
//    }

//    private boolean isDisplaySubtitle()
//    {
//        return true;
//    }

//    private void downloadSubtitle(Video video)
//    {
//        OpenSubtitleService.getIntance().downloadSubtitle( video, this);
//    }

    private void showSubtitle2(Video video, int position )
    {
        Log.d( "Subtitle_Activities", "Initializing Video audio conversion in VideoPlayerActivity" );

//        Subtitle.showSubtitle( video, position, this, this::BuildNewSourceOnSubtitle,
//                this::switchSources, messageCallback() );
    }

    private class KeepScreenOn implements Player.EventListener
    {
        private final PlayerView playerView;

        private KeepScreenOn(PlayerView playerView) {
            this.playerView = playerView;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if ( playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady )
                playerView.setKeepScreenOn( false );
            else
                playerView.setKeepScreenOn( true );
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(SHOW_ANIMATION_DELAY);
    }
//
    private void toggle() {
        dispatchTouch();
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void dispatchTouch() {
        if ( touchables != null )
        {
            for ( Supplier<Boolean> action : touchables )
            {
                Boolean removed = action.get();
                if (removed)
                    touchables.remove(action);
            }
        }
    }

    public void addTouchable(Supplier<Boolean> action)
    {
        if (touchables == null)
            touchables = new ArrayList<>();
        touchables.add(action);
    }

    public void removeTouchable(Supplier<Boolean> action)
    {
        if (touchables != null)
            touchables.remove(action);
    }

    //
    private void hide() {
        // Hide UI first
//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
//        mControlsView.setVisibility(View.GONE);
//        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        videoHandler.removeCallbacks(mShowPart2Runnable);
        videoHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }
//
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        showStatus();

        // Schedule a runnable to display UI elements after a delay
        videoHandler.removeCallbacks(mHidePart2Runnable);
//        videoHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        videoHandler.postDelayed(mHidePart2Runnable, SHOW_ANIMATION_DELAY);
    }
//
    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        videoHandler.removeCallbacks(mHideRunnable);
        videoHandler.postDelayed(mHideRunnable, delayMillis);
    }
//
//    private static final boolean AUTO_HIDE = true;
//    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int SHOW_ANIMATION_DELAY = 4000;

//    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {
            hideStatus();
        }
    };

    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
//            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private void seekToOnPopup( int dur )
    {
        String seek = dur > 0 ? "+" + getDuration( dur ) : "-" + getDuration( Math.abs( dur ) );
        long duration = playerManager.getCurrentPosition() + dur;
        String seekTo = getDuration( duration < 0 ? 0 : duration );
        String result = String.format( "%s ( %s )", seek, seekTo );
        if ( !seekToPopup.isShowing() )
            seekToPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//        TextView seekInfo = findViewById(R.id.popup_seek_to);
        TextView seekInfo = seekToPopup.getContentView().findViewById(R.id.popup_seek_to);
        seekInfo.setText( result );

        seekUpdateLater( dur );
    }

    private int seek;
    private final Runnable seekUpdate = () -> seekToOnPopup( seek );
    private final Runnable seekDismiss = () -> seekToPopup.dismiss();
    private final Runnable brightnessDismiss = () -> brightnessPopup.dismiss();
    private final Runnable volumeDismiss = () -> volumePopup.dismiss();


    private void seekUpdateLater( int seek )
    {
        this.seek = seek;
        videoHandler.removeCallbacks( seekUpdate );
        videoHandler.postDelayed( seekUpdate, 1000 );
    }

    private void dismissSeekPopup()
    {
        videoHandler.postDelayed( seekDismiss, 1000 );
    }

    private void retainSeekPopup()
    {
        videoHandler.removeCallbacks( seekDismiss );
    }

    private void dismissBrightnessPopup()
    {
        videoHandler.postDelayed( brightnessDismiss, 3000 );
    }

    private void retainBrightnessPopup()
    {
        videoHandler.removeCallbacks( brightnessDismiss );
    }


    private void dismissVolumePopup()
    {
        videoHandler.postDelayed( volumeDismiss, 3000 );
    }

    private void retainVolumePopup()
    {
        videoHandler.removeCallbacks( volumeDismiss );
    }

    public String getDuration( long dur )
    {
        long h = TimeUnit.MILLISECONDS.toHours(dur);
        long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
        long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );
        String hs = h > 0 ? String.format( "%02d:", h ) : "";
        String ms = m > 0 || h > 0 ? String.format( "%02d:", m ) : "";
//        String ss = h == 0 && m == 0 ? String.format( "%02d secs", s ) : String.format( "%02d", s );
        String ss = String.format( "%02d", s );
        return hs + ms + ss;
    }
//    /**
//     * Touch listener to use for in-layout UI controls to delay hiding the
//     * system UI. This is to prevent the jarring behavior of controls going away
//     * while interacting with activity UI.
//     */
//    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            if (AUTO_HIDE) {
//                delayedHide(AUTO_HIDE_DELAY_MILLIS);
//            }
//            return false;
//        }
//    };

    private void setCustomGestureListener()
    {
        CustomVideoGesture gesture = new CustomVideoGesture( VideoPlayerActivity.this, new VideoGestureChangeListener(), () ->
                playerManager.getPlayer(), new CustomVideoGesture.GestureAction(){
            @Override
            public void onClick() {
                toggle();
            }
        }
        );
        playerView.setOnTouchListener( gesture );
    }

    public class VideoGestureChangeListener implements OnVideoGestureChangeListener
    {

        @Override
        public void onVolumeChanged(int range, int type) {
            retainVolumePopup();

            ProgressBar progress = volumePopup.getContentView().findViewById(R.id.volume_progress);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                progress.setProgress( range, true );
            else
                progress.setProgress( range );

            TextView value = volumePopup.getContentView().findViewById(R.id.volume_value);
            value.setText( range + "%" );

            if ( !seekToPopup.isShowing() )
                volumePopup.showAtLocation( playerView, Gravity.LEFT, 0, 0 );

            dismissVolumePopup();
        }

        @Override
        public void onBrightnessChanged(int brightnessPercent) {
            retainBrightnessPopup();

            ProgressBar progress = brightnessPopup.getContentView().findViewById(R.id.bright_progress);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                progress.setProgress( brightnessPercent, true );
            else
                progress.setProgress( brightnessPercent);

            TextView value = brightnessPopup.getContentView().findViewById(R.id.bright_value);
            value.setText(brightnessPercent + "%");


            if ( !seekToPopup.isShowing() )
                brightnessPopup.showAtLocation( playerView, Gravity.RIGHT, 0, 0 );

            dismissBrightnessPopup();
        }

        @Override
        public void onNoGesture() {

        }

        public void onShowSeekSize(long seekSize, long jump, boolean fastForward) {
            Log.d( "Custom_Player", "Player is seeking " + seekSize );

            retainSeekPopup();

            String seek = fastForward ? "+" + getDuration( jump ) : "-" + getDuration( Math.abs( jump ) );
//            String seekTo = getDuration( duration < 0 ? 0 : duration );
            String seekTo = getDuration( seekSize );

            String result = String.format( "%s ( %s )", seek, seekTo );

            TextView seekInfo = seekToPopup.getContentView().findViewById(R.id.popup_seek_to);
            seekInfo.setText( result );

            if ( !seekToPopup.isShowing() )
                seekToPopup.showAtLocation( playerView, Gravity.TOP, 0, 0 );
//        TextView seekInfo = findViewById(R.id.popup_seek_to);

            playerManager.seekTo( seekSize );
            dismissSeekPopup();

        }

        @Override
        public void onShowSeekSize(long seekSize, boolean fastForward) {
        }
    }

}
