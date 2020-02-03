package com.gcodes.iplayer.video.player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.backup.CustomVideoGesture;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.services.OpenSubtitleService;
import com.gcodes.iplayer.video.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.Gson;
import com.jarvanmo.exoplayerview.gesture.OnVideoGestureChangeListener;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class VideoPlayerActivity extends AppCompatActivity {

//    private static SimpleExoPlayer player;
    private final PlayerManager playerManager = PlayerManager.getInstance();
    private ConcatenatingMediaSource mediaSource;
    private PopupWindow seekToPopup;
    private PlayerView playerView;
    private final Handler videoHandler = new Handler();
    private PopupWindow brightnessPopup;
    private PopupWindow volumePopup;
    private int begin;
//    private DefaultDataSourceFactory factory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_player);

        initPlayer();

        initPopup();
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

    public static void play(Context context, Video ...videos)
    {
        Intent intent = new Intent( context, VideoPlayerActivity.class );
        String gsonVideos[] = new String[ videos.length ];
        for ( int i = 0; i < videos.length; ++i )
            gsonVideos[ i ] = videos[ i ].toGson();
        intent.putExtra( "medias", gsonVideos );
        intent.putExtra( "data_type", "video" );
        context.startActivity( intent );
    }

    public static void play(Context context, String url )
    {
        Intent intent = new Intent( context, VideoPlayerActivity.class );
        intent.putExtra( "medias", new String[]{ url } );
        intent.putExtra( "data_type", "url" );
        context.startActivity( intent );
    }

    public static void play(Context context, int pos, Video ...videos)
    {
        Intent intent = new Intent( context, VideoPlayerActivity.class );
        String gsonVideos[] = new String[ videos.length ];
        for ( int i = 0; i < videos.length; ++i )
            gsonVideos[ i ] = videos[ i ].toGson();
        intent.putExtra( "medias", gsonVideos );
        intent.putExtra( "data_type", "video" );
        intent.putExtra( "begin", pos );
        context.startActivity( intent );
    }

    private void showStatus()
    {
        Log.d( "Video_Player", "Showing Status " );
        View mContentView = findViewById(R.id.video_player);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
//        player.setPlayWhenReady(true);
    }

//    private final Handler testHandler = new Handler();
    @Override
    protected void onResume() {
        super.onResume();
        play();
    }

    private void playNow()
    {
        playerManager.play();
        if  ( begin >= 0 )
            playerManager.playAt( begin );
    }

    private void play()
    {
        playNow();
    }

    private Video getVideoAt( int position )
    {
        String[] medias = getIntent().getStringArrayExtra("medias");
        return Video.fromGson( medias[ position ] );
    }

    @Override
    protected void onPause() {
        super.onPause();
//        player.stop();
        playerManager.pause();
    }

    private void initPlayer()
    {
        initView();

        if ( playerManager.isReady() )
            playerManager.saveState();

        initSource();
//        initAutoSubtitle();
        initControls();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if ( playerManager.hasState() )
            playerManager.restoreState();
    }

    private void initControls() {
        ImageView subtitleButton = findViewById(R.id.exo_custom_subtitle);
        subtitleButton.setOnClickListener( v -> {
            Log.d( "Subtitle_Activities", "The current window is " + playerManager.getCurrentTrack() );
            showSubtitle();
        });
    }

    private void showSubtitle()
    {
        int position = playerManager.getCurrentTrack();
        Video video = getVideoAt(position);
        showSubtitle( video, position );
    }

    private void autoSubtitle()
    {
        playerManager.addListener(new Player.EventListener() {
            @Override
            public void onPositionDiscontinuity(int reason) {
                showSubtitle();
                Log.d( "Video_Player", "The current window is " + playerManager.getCurrentTrack() );
            }
        });
    }

    private void initAutoSubtitle() {
        if ( isDisplaySubtitle() )
        {
           autoSubtitle();
        }
    }

    private Consumer< String > messageCallback()
    {
        Toast toast = new Toast( this );
        return s -> {
            toast.setText( s );
            toast.show();
        };
    }

    @NonNull
    public ConcatenatingMediaSource BuildNewSourceOnSubtitle(File file, int position )
    {
        SingleSampleMediaSource subtitleSource = getSubtitle( file );
        MediaSource[] sources = new MediaSource[ mediaSource.getSize() ];
        for ( int i = 0; i < mediaSource.getSize(); ++i )
        {
            MediaSource mediaSource = this.mediaSource.getMediaSource(i);
            if ( i == position )
            {
                mediaSource = new MergingMediaSource( mediaSource, subtitleSource );
            }
            sources[ i ] = mediaSource;
        }

//        player.setPlayWhenReady( false );
//        mediaSource.clear( () -> {
//            mediaSource.addMediaSources(Arrays.asList( sources ), () ->
//            {
////                player.prepare( mediaSource, false, false );
////                mediaSource.pre
//                player.setPlayWhenReady( true );
//
//            });
//        });

        ConcatenatingMediaSource newSource = new ConcatenatingMediaSource(sources);
//        player.prepare( newSource, false, false );

        return newSource;
    }

    private ConcatenatingMediaSource BuildNewSourceOnSubtitle2( File file, int position  )
    {
        Gson gson = new Gson();
        ConcatenatingMediaSource clone = gson.fromJson(gson.toJson(mediaSource), ConcatenatingMediaSource.class);
        MediaSource targetSource = clone.getMediaSource(position);
        SingleSampleMediaSource subtitleSource = getSubtitle( file );
        MergingMediaSource newSource = new MergingMediaSource( targetSource, subtitleSource );
        clone.removeMediaSource( position );
        clone.addMediaSource( position, newSource );
        playerManager.prepare( clone, false, false );
        return clone;
    }

    private void switchSources(ConcatenatingMediaSource source)
    {
        Log.d( "Video_Player", "The old source " + mediaSource );
        Log.d( "Video_Player", "The old source " + mediaSource.getSize() );
        Log.d( "Video_Player", "The old source " + source );
        Log.d( "Video_Player", "The new source " + source.getSize() );

        playerManager.prepare( mediaSource, false, false );
        int currentWindowIndex = playerManager.getCurrentTrack();
        long currentPosition = playerManager.getCurrentPosition();

        playerManager.prepare( source, true, true );
        playerManager.playTrackAt( currentWindowIndex, currentPosition );
        mediaSource = source;

//        source.prepareSourceInternal( player, false, null );
//        player.prepare( source, false, false );
//        mediaSource = source;
    }

    private void initView()
    {
        playerView = findViewById(R.id.video_player);
        playerManager.setView( playerView );
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

    private void initSource()
    {
        String dataType = getIntent().getStringExtra("data_type");
        switch ( dataType )
        {
            case "video" :
                initVideoSources();
                break;

            case "url" :
                initOnlineSources();
                break;
        }
    }

    private void initOnlineSources() {
        String[] vids = getIntent().getStringArrayExtra("medias");
        MediaSource sources[] = new MediaSource[ vids.length ];
        for ( int i = 0; i < vids.length; ++i )
        {
            sources[ i ] = getMediaSource( vids[ i ] );
        }
        mediaSource = new ConcatenatingMediaSource( sources );
        playerManager.prepare( mediaSource );
        begin = getIntent().getIntExtra("begin", -1 );
    }

    public ProgressiveMediaSource getMediaSource( String url )
    {
        Uri media = Uri.parse( url );
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory( playerManager.getOfflineFactory() ).createMediaSource(media);
        return source;
    }

    private void initVideoSources()
    {
        String[] vids = getIntent().getStringArrayExtra("medias");
        MediaSource sources[] = new MediaSource[ vids.length ];
        for ( int i = 0; i < vids.length; ++i )
        {
            Video video = Video.fromGson(vids[i]);
            sources[ i ] = getSource(video);
        }
        mediaSource = new ConcatenatingMediaSource( sources );
        playerManager.prepare( mediaSource );
        begin = getIntent().getIntExtra("begin", -1 );
    }

//    private void getSource( Video video )
//    {
//        ProgressiveMediaSource videoSource = getVideoSource(video.getId());
////        mediaSource = new MergingMediaSource( videoSource, subtitleSource );
//        mediaSource = videoSource;
////        getSubtitleSource( video );
//        player.prepare( mediaSource );
//    }

    private MediaSource getSource(Video video )
    {
        MediaSource mediaSource = getVideoSource(video.getId());
        if ( isDisplaySubtitle() )
        {
            SingleSampleMediaSource subtitleSource = getSubtitle( video );
            if ( subtitleSource != null )
                mediaSource = new MergingMediaSource( mediaSource, subtitleSource );
        }
        return mediaSource;
    }

    private boolean isDisplaySubtitle()
    {
        return true;
    }

    private SingleSampleMediaSource getSubtitle(File file)
    {
        SingleSampleMediaSource.Factory subFact = new SingleSampleMediaSource.Factory( playerManager.getFactory() );
        Uri subUri = Uri.fromFile(file);
        Format subFormat = Format.createTextSampleFormat( null, MimeTypes.APPLICATION_SUBRIP, C.SELECTION_FLAG_DEFAULT, "en");
        SingleSampleMediaSource subtitle = subFact.createMediaSource(subUri, subFormat, C.TIME_UNSET);
        return subtitle;
    }

    private SingleSampleMediaSource getSubtitle(Video video)
    {
        File file = OpenSubtitleService.getIntance().getSavedSubtitle( video, this );
        if ( file == null )
            return null;
        return getSubtitle( file );
    }

    private void downloadSubtitle(Video video)
    {
        OpenSubtitleService.getIntance().downloadSubtitle( video, this);
    }

    private void showSubtitle2(Video video, int position )
    {
        Log.d( "Subtitle_Activities", "Initializing Video audio conversion in VideoPlayerActivity" );

//        Subtitle.showSubtitle( video, position, this, this::BuildNewSourceOnSubtitle,
//                this::switchSources, messageCallback() );
    }

    private void showSubtitle(Video video, int position )
    {
        Log.d( "Subtitle_Activities", "Initializing Video audio conversion in VideoPlayerActivity" );

        Helper.Worker.executeTask( () -> {
//            File subtitle = OpenSubtitleService.getIntance().retrieveSubtitle(video, this, messageCallback());
            File subtitle = OpenSubtitleService.getIntance().retrieveSubtitle(video, this, null);
            if ( subtitle == null )
                Log.d( "Subtitle_Activities", "Unable to find subtitle" );
            ConcatenatingMediaSource newSource = BuildNewSourceOnSubtitle(subtitle, position);
            return () -> {
                switchSources( newSource );
            };
        });
    }



    private void noSubtitle()
    {

    }

    private void displaySubtitle()
    {
        OpenSubtitleService.getIntance().displaySubtitle();
    }

    private ProgressiveMediaSource getVideoSource(String id )
    {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );
        Log.d( "Video_Player", "Video " + id );
        Log.d( "Video_Player", "Video " + media.getPath() );
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getResources().getString(R.string.app_name)));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory).createMediaSource(media);
        return source;
    }

    private ProgressiveMediaSource getVideoSource(long id )
    {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );
        Log.d( "Video_Player", "Video " + id );
        Log.d( "Video_Player", "Video " + media.getPath() );
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(playerManager.getFactory()).createMediaSource(media);
        return source;
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
        if (mVisible) {
            hide();
        } else {
            show();
        }
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
                volumePopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );

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
                brightnessPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );

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
                seekToPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//        TextView seekInfo = findViewById(R.id.popup_seek_to);

            playerManager.seekTo( seekSize );
            dismissSeekPopup();

        }

        @Override
        public void onShowSeekSize(long seekSize, boolean fastForward) {
        }
    }

}
