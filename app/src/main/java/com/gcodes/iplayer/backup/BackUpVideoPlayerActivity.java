//package com.gcodes.iplayer.player;
//
//import android.annotation.SuppressLint;
//import android.app.ActionBar;
//import android.content.Context;
//import android.content.pm.ActivityInfo;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.GestureDetector;
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.PopupWindow;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.gcodes.iplayer.R;
//import com.google.android.exoplayer2.C;
//import com.google.android.exoplayer2.ExoPlayerFactory;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.source.ExtractorMediaSource;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
//import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
//import com.google.android.exoplayer2.ui.PlayerView;
//import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
//import com.google.android.exoplayer2.util.Util;
//import com.jarvanmo.exoplayerview.gesture.OnVideoGestureChangeListener;
//
//import java.util.concurrent.TimeUnit;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.view.GestureDetectorCompat;
//
//public class CustomVideoPlayerActivity extends AppCompatActivity {
//
//    private static SimpleExoPlayer player;
//    private MediaSource mediaSource;
//    private PopupWindow seekToPopup;
//    private PlayerView playerView;
//    private final Handler videoHandler = new Handler();
//    private PopupWindow brightnessPopup;
//    private PopupWindow volumePopup;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.video_player);
//
//        initPlayer();
//
////        initFullScreen();
//        initPopup();
//    }
//
//    private void initPopup()
//    {
//        LayoutInflater inflater = LayoutInflater.from(this);
//        View seekTo = inflater.inflate(R.layout.popup_seekto, null);
//        View brightness = inflater.inflate(R.layout.popup_brightness, null);
//        View volume = inflater.inflate(R.layout.popup_volume, null);
//        seekToPopup = new PopupWindow(seekTo, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                true);
//        brightnessPopup = new PopupWindow(brightness, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                true);
//        volumePopup = new PopupWindow(volume, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                true);
//    }
//
//    private void hideStatus()
//    {
//        View mContentView = findViewById(R.id.video_player);
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        mVisible = false;
//    }
//
//    private void showStatus()
//    {
//        Log.d( "Video_Player", "Showing Status " );
//        View mContentView = findViewById(R.id.video_player);
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//        mVisible = true;
//    }
//
////    private void initFullScreen()
////    {
////        mVisible = true;
////        mControlsView = findViewById(R.id.control_view);
////        mContentView = findViewById(R.id.video_player);
////
////
////        // Set up the user interaction to manually show or hide the system UI.
////        mContentView.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                toggle();
////            }
////        });
////
////        // Upon interacting with UI controls, delay any scheduled hide()
////        // operations to prevent the jarring behavior of controls going away
////        // while interacting with the UI.
////        findViewById(R.id.control_view).setOnTouchListener(mDelayHideTouchListener);
////    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        player.setPlayWhenReady(true);
//        Log.d( "Custom_Player", "Just Started" );
//    }
//
//    private void initPlayer()
//    {
////        player = ExoPlayerFactory.newSimpleInstance( this, new DefaultTrackSelector() );
//        player = ExoPlayerFactory.newSimpleInstance( this, new DefaultTrackSelector() );
//        initView();
//        initSource();
//    }
//
//    private void initView()
//    {
//        playerView = findViewById(R.id.video_player);
//        playerView.setPlayer( player );
//        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT )
//        {
//            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT );
//            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
//        }
//        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE )
//        {
//            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL );
//            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
//        }
//        player.addListener( new KeepScreenOn(playerView) );
//        playerView.setBackgroundColor(Color.BLACK);
//
////        setOnGestureListeners();
//        setCustomGestureListener();
//
////        WindowManager.LayoutParams lp = getWindow().getAttributes();
////        lp.screenBrightness = 0.5f;
////        getWindow().setAttributes(lp);
//
//    }
//
//    private void initSource()
//    {
//        String id = getIntent().getStringExtra("media");
//        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
//        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getResources().getString(R.string.app_name)));
//        mediaSource = new ExtractorMediaSource.Factory(factory).createMediaSource( media );
//        player.prepare( mediaSource );
//    }
//
//    private void release()
//    {
//        player.release();
//    }
//
//    private class KeepScreenOn implements Player.EventListener
//    {
//        private final PlayerView playerView;
//
//        private KeepScreenOn(PlayerView playerView) {
//            this.playerView = playerView;
//        }
//
//        @Override
//        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            if ( playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady )
//                playerView.setKeepScreenOn( false );
//            else
//                playerView.setKeepScreenOn( true );
//        }
//    }
//
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//
//        // Trigger the initial hide() shortly after the activity has been
//        // created, to briefly hint to the user that UI controls
//        // are available.
//        delayedHide(SHOW_ANIMATION_DELAY);
//    }
////
//    private void toggle() {
//        if (mVisible) {
//            hide();
//        } else {
//            show();
//        }
//    }
////
//    private void hide() {
//        // Hide UI first
////        ActionBar actionBar = getActionBar();
////        if (actionBar != null) {
////            actionBar.hide();
////        }
////        mControlsView.setVisibility(View.GONE);
////        mVisible = false;
//
//        // Schedule a runnable to remove the status and navigation bar after a delay
//        videoHandler.removeCallbacks(mShowPart2Runnable);
//        videoHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
//    }
////
//    @SuppressLint("InlinedApi")
//    private void show() {
//        // Show the system bar
//        showStatus();
//
//        // Schedule a runnable to display UI elements after a delay
//        videoHandler.removeCallbacks(mHidePart2Runnable);
////        videoHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
//        videoHandler.postDelayed(mHidePart2Runnable, SHOW_ANIMATION_DELAY);
//    }
////
//    /**
//     * Schedules a call to hide() in delay milliseconds, canceling any
//     * previously scheduled calls.
//     */
//    private void delayedHide(int delayMillis) {
//        videoHandler.removeCallbacks(mHideRunnable);
//        videoHandler.postDelayed(mHideRunnable, delayMillis);
//    }
////
////    private static final boolean AUTO_HIDE = true;
////    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
//    private static final int UI_ANIMATION_DELAY = 300;
//    private static final int SHOW_ANIMATION_DELAY = 4000;
//
////    private View mContentView;
//    private final Runnable mHidePart2Runnable = new Runnable() {
//        @SuppressLint("InlinedApi")
//        @Override
//        public void run()
//        {
//            hideStatus();
//        }
//    };
//
//    private View mControlsView;
//    private final Runnable mShowPart2Runnable = new Runnable() {
//        @Override
//        public void run() {
//            // Delayed display of UI elements
//            ActionBar actionBar = getActionBar();
//            if (actionBar != null) {
//                actionBar.show();
//            }
////            mControlsView.setVisibility(View.VISIBLE);
//        }
//    };
//
//    private boolean mVisible;
//    private final Runnable mHideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            hide();
//        }
//    };
//
//    private void seekToOnPopup( int dur )
//    {
//        String seek = dur > 0 ? "+" + getDuration( dur ) : "-" + getDuration( Math.abs( dur ) );
//        long duration = player.getCurrentPosition() + dur;
//        String seekTo = getDuration( duration < 0 ? 0 : duration );
//        String result = String.format( "%s ( %s )", seek, seekTo );
//        if ( !seekToPopup.isShowing() )
//            seekToPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
////        TextView seekInfo = findViewById(R.id.popup_seek_to);
//        TextView seekInfo = seekToPopup.getContentView().findViewById(R.id.popup_seek_to);
//        seekInfo.setText( result );
//
//        seekUpdateLater( dur );
//    }
//
//    private void brightnessOnPopup( float increment )
//    {
//        retainBrightnessPopup();
//
//        ProgressBar progress = brightnessPopup.getContentView().findViewById(R.id.bright_progress);
//        float brightness = getCurrentBrightness();
//        float result = brightness + increment;
//
//        result = result < 0f ? 0f : result;
//        result = result > 100f ? 100f : result;
//
//        Log.d( "Video_Player", "current " + brightness );
//        Log.d( "Video_Player", "increment " + increment );
//        Log.d( "Video_Player", "result " + result );
//
//        setCurrentBrightness( result );
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            progress.setProgress((int) result, true );
//        else
//            progress.setProgress((int) result);
//
//        TextView value = brightnessPopup.getContentView().findViewById(R.id.bright_value);
////        value.setText( result + "%" );
//        value.setText(String.format("%d%%", ( int ) result ));
//
//
//        if ( !seekToPopup.isShowing() )
//            brightnessPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//
//        dismissBrightnessPopup();
//    }
//
//    private void setCurrentBrightness( float result )
//    {
//        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        float newB = result / 100;
//        lp.screenBrightness = newB;
//        getWindow().setAttributes(lp);
//        Log.d( "Video_Player", "new brightness " + newB );
//    }
//
//    private float getCurrentBrightness()
//    {
//        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        Log.d( "Video_Player", "brightness " + lp.screenBrightness );
//        return lp.screenBrightness * 100;
//    }
//
//    private void volumeOnPopup( int increment )
//    {
//        retainVolumePopup();
//
//        ProgressBar progress = volumePopup.getContentView().findViewById(R.id.volume_progress);
//        float volume = player.getVolume();
//        int result = (int) (volume + increment);
//        player.setVolume( result );
//
////        result = result < 0 ? 0 : result;
////        result = result > 100 ? 100 : result;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            progress.setProgress( result, true );
//        else
//            progress.setProgress( result );
//
//        TextView value = volumePopup.getContentView().findViewById(R.id.volume_value);
//        value.setText( result + "%" );
//
//        setCurrentVolume( result );
//
//        if ( !seekToPopup.isShowing() )
//            volumePopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//
//        dismissVolumePopup();
//    }
//
//    private void setCurrentVolume( int volume )
//    {
//        player.setVolume( volume / 100 );
//    }
//
//    private int getCurrentVolume()
//    {
//        return (int) player.getVolume() * 100;
//    }
//
//    private int seek;
//    private final Runnable seekUpdate = () -> seekToOnPopup( seek );
//    private final Runnable brightnessDismiss = () -> brightnessPopup.dismiss();
//    private final Runnable volumeDismiss = () -> volumePopup.dismiss();
//
//    private void seekFinish()
//    {
//        videoHandler.removeCallbacks( seekUpdate );
//    }
//
//    private void seekUpdateLater( int seek )
//    {
//        this.seek = seek;
//        videoHandler.removeCallbacks( seekUpdate );
//        videoHandler.postDelayed( seekUpdate, 1000 );
//    }
//
//    private void dismissBrightnessPopup()
//    {
//        videoHandler.postDelayed( brightnessDismiss, 3000 );
//    }
//
//    private void retainBrightnessPopup()
//    {
//        videoHandler.removeCallbacks( brightnessDismiss );
//    }
//
//
//    private void dismissVolumePopup()
//    {
//        videoHandler.postDelayed( volumeDismiss, 3000 );
//    }
//
//    private void retainVolumePopup()
//    {
//        videoHandler.removeCallbacks( volumeDismiss );
//    }
//
//    public String getDuration( long dur )
//    {
//        long h = TimeUnit.MILLISECONDS.toHours(dur);
//        long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
//        long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );
//        String hs = h > 0 ? String.format( "%02d:", h ) : "";
//        String ms = m > 0 || h > 0 ? String.format( "%02d:", m ) : "";
////        String ss = h == 0 && m == 0 ? String.format( "%02d secs", s ) : String.format( "%02d", s );
//        String ss = String.format( "%02d", s );
//        return hs + ms + ss;
//    }
////    /**
////     * Touch listener to use for in-layout UI controls to delay hiding the
////     * system UI. This is to prevent the jarring behavior of controls going away
////     * while interacting with activity UI.
////     */
////    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
////        @Override
////        public boolean onTouch(View view, MotionEvent motionEvent) {
////            if (AUTO_HIDE) {
////                delayedHide(AUTO_HIDE_DELAY_MILLIS);
////            }
////            return false;
////        }
////    };
//
//    private void setCustomGestureListener()
//    {
//        CustomVideoGesture gesture = new CustomVideoGesture( CustomVideoPlayerActivity.this, new VideoGestureChangeListener(), () ->
//                player, new CustomVideoGesture.GestureAction(){
//            @Override
//            public void onClick() {
//                toggle();
//            }
//        }
//        );
//        Log.d( "Custom_Player", "Setting the gesture listener" );
//        playerView.setOnTouchListener( gesture );
//    }
//
//    private class VideoGestureChangeListener implements OnVideoGestureChangeListener
//    {
//
//        @Override
//        public void onVolumeChanged(int range, int type) {
//            retainVolumePopup();
//
//            ProgressBar progress = volumePopup.getContentView().findViewById(R.id.volume_progress);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//                progress.setProgress( range, true );
//            else
//                progress.setProgress( range );
//
//            TextView value = volumePopup.getContentView().findViewById(R.id.volume_value);
//            value.setText( range + "%" );
//
//            if ( !seekToPopup.isShowing() )
//                volumePopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//
//            dismissVolumePopup();
//        }
//
//        @Override
//        public void onBrightnessChanged(int brightnessPercent) {
//            Log.d( "Custom_Player", "You are brightness " + brightnessPercent );
//            retainBrightnessPopup();
//
//            ProgressBar progress = brightnessPopup.getContentView().findViewById(R.id.bright_progress);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//                progress.setProgress( brightnessPercent, true );
//            else
//                progress.setProgress( brightnessPercent);
//
//            TextView value = brightnessPopup.getContentView().findViewById(R.id.bright_value);
//            value.setText(brightnessPercent + "%");
//
//
//            if ( !seekToPopup.isShowing() )
//                brightnessPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//
//            dismissBrightnessPopup();
//        }
//
//        @Override
//        public void onNoGesture() {
//
//        }
//
//        @Override
//        public void onShowSeekSize(long seekSize, boolean fastForward) {
//            Log.d( "Custom_Player", "You are seeking " + seekSize );
//
////            String seek = fastForward ? "+" + getDuration( seek ) : "-" + getDuration( Math.abs( dur ) );
////            long duration = player.getCurrentPosition() + dur;
////            String seekTo = getDuration( duration < 0 ? 0 : duration );
////            String result = String.format( "%s ( %s )", seek, seekTo );
////            if ( !seekToPopup.isShowing() )
////                seekToPopup.showAtLocation( playerView, Gravity.CENTER, 0, 0 );
//////        TextView seekInfo = findViewById(R.id.popup_seek_to);
////            TextView seekInfo = seekToPopup.getContentView().findViewById(R.id.popup_seek_to);
////            seekInfo.setText( result );
////
////            seekUpdateLater( dur );
//        }
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private void setOnGestureListeners()
//    {
//        playerView.setOnTouchListener(new OnSwipeTouchListener( CustomVideoPlayerActivity.this )
//        {
//            private static final int SEEK_PER_SCROLL_UNIT = 180;
//            private static final float BRIGHTNESS_PER_SCROLL_UNIT = 0.003f;
//            private static final float VOLUME_PER_SCROLL_UNIT = 0.003f;
//            private boolean seeking;
//            private int seekingTime;
//
//            @Override
//            public void onScrollRight( float distance )
//            {
//                super.onScrollRight( distance );
//                // Scroll to the right
//                seekingTime = (int) (distance * SEEK_PER_SCROLL_UNIT);
//                seekToOnPopup( seekingTime );
//                seeking = true;
//            }
//
//            @Override
//            public void onScrollLeft( float distance ) {
//                super.onScrollLeft( distance );
//                // Scroll to the left
//                seekingTime = (int) (distance * SEEK_PER_SCROLL_UNIT);
//                seekingTime = -seekingTime;
//                seekToOnPopup( seekingTime );
//                seeking = true;
//            }
//
//            @Override
//            public void onUp() {
//                super.onUp();
//                if ( seeking )
//                {
//                    player.seekTo( player.getCurrentPosition() + seekingTime );
//                    seeking = false;
//                    seekFinish();
//                    seekToPopup.dismiss();
//                }
//            }
//
//            @Override
//            public void onClick( MotionEvent e ) {
//                super.onClick( e );
//                // User tapped once (This is what you want)
////                playerView.onTouchEvent( e );
//                toggle();
//            }
//
//            @Override
//            public void onScrollRightTop( float distance )
//            {
//                super.onScrollRightTop( distance );
//
//                float bright = distance * BRIGHTNESS_PER_SCROLL_UNIT;
//                brightnessOnPopup( bright );
//            }
//
//            @Override
//            public void onScrollRightBottom( float distance )
//            {
//                super.onScrollRightBottom( distance );
//
//                float bright = distance * BRIGHTNESS_PER_SCROLL_UNIT;
//                bright = -bright;
//                brightnessOnPopup( bright );
//            }
//
//            @Override
//            public void onScrollLeftTop( float distance )
//            {
//                super.onScrollLeftTop( distance );
//
//                int volume = (int) (distance * VOLUME_PER_SCROLL_UNIT);
//                volumeOnPopup( volume );
//            }
//
//            @Override
//            public void onScrollLeftBottom( float distance )
//            {
//                super.onScrollLeftBottom( distance );
//
//                int volume = (int) (distance * VOLUME_PER_SCROLL_UNIT);
//                volume = -volume;
//                volumeOnPopup( volume );
//            }
//
//        });
//    }
//
//    public class OnSwipeTouchListener implements View.OnTouchListener {
//
//        private static final String TAG = "OnSwipeTouchListener";
//
//        private final GestureDetectorCompat mDetector;
//
//        public OnSwipeTouchListener(Context context) {
//            mDetector = new GestureDetectorCompat(context, new GestureListener());
//        }
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event)
//        {
//            if ( event.getAction() == MotionEvent.ACTION_UP )
//            {
//                onUp();
//            }
//            playerView.onTouchEvent( event );
//            return mDetector.onTouchEvent(event);
//        }
//
//        public void onUp()
//        {
//
//        }
//
//        public void onSwipeRight() {
//            Log.i(TAG, "onSwipeRight: Swiped to the RIGHT");
//        }
//
//        public void onSwipeLeft() {
//            Log.i(TAG, "onSwipeLeft: Swiped to the LEFT");
//        }
//
//        public void onSwipeTop() {
//            Log.i(TAG, "onSwipeTop: Swiped to the TOP");
//        }
//
//        public void onSwipeBottom() {
//            Log.i(TAG, "onSwipeBottom: Swiped to the BOTTOM");
//        }
//
//        public void onScrollRight( float distance ) {
//            Log.i(TAG, "onSwipeRight: Scroll to the RIGHT");
//        }
//
//        public void onScrollLeft(  float distance ) {
//            Log.i(TAG, "onSwipeLeft: Scroll to the LEFT");
//        }
//
//        public void onScrollTop( float distance ) {
//            Log.i(TAG, "onScrollTop: Scroll to the TOP");
//        }
//
//        public void onScrollRightTop( float distance ) {
//            Log.i(TAG, "onScrollRightTop: Scroll to the TOP");
//        }
//
//        public void onScrollLeftTop( float distance ) {
//            Log.i(TAG, "onScrollRightTop: Scroll to the TOP");
//        }
//
//        public void onScrollBottom( float distance ) {
//            Log.i(TAG, "onScrollBottom: Scroll to the BOTTOM");
//        }
//
//        public void onScrollRightBottom(float abs) {
//            Log.i(TAG, "onScrollRightBottom: Scroll to the BOTTOM");
//        }
//
//        public void onScrollLeftBottom(float abs) {
//            Log.i(TAG, "onScrollLeftBottom: Scroll to the BOTTOM");
//        }
//
//        public void onClick( MotionEvent e )
//        {
//        }
//
//        public void onClickConfirm( MotionEvent e )
//        { }
//
//        public void onDoubleClick() {
//            Log.i(TAG, "onClick: Clicking TWO TIMES in the screen");
//        }
//
//        public void onLongClick() {
//            Log.i(TAG, "onLongClick: LONG click in the screen");
//        }
//
//        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
//
//            private static final int SWIPE_THRESHOLD = 100;
//            private static final int SWIPE_VELOCITY = 100;
//            private MotionEvent current;
//
//            private float currentY;
//            private boolean yScrolled;
//            private float yThresh = 100f;
//
//            @Override
//            public boolean onDown(MotionEvent e) {
//                return true;
//            }
//
//            @Override
//            public boolean onSingleTapUp(MotionEvent e) {
//                onClick( e );
//                return super.onSingleTapUp(e);
//            }
//
//            @Override
//            public boolean onDoubleTap(MotionEvent e) {
//                onDoubleClick();
//                return super.onDoubleTap(e);
//            }
//
//            @Override
//            public void onLongPress(MotionEvent e) {
//                onLongClick();
//                super.onLongPress(e);
//            }
//
//            @Override
//            public boolean onSingleTapConfirmed(MotionEvent e)
//            {
//                onClickConfirm( e );
//                return super.onSingleTapConfirmed(e);
//            }
//
//            @Override
//            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
//            {
//                float widthHalf = playerView.getWidth() / 2;
//                float point = e1.getX() * e1.getXPrecision();
//                boolean result = super.onScroll( e1, e2, distanceX, distanceY );
//                if ( result )
//                    Log.d( "Video_Player", "the onscroll has finish" );
//                try {
////                    float diffY = e2.getY() - e1.getY();
//                    float diffX = e2.getX() - e1.getX();
//                    float diffY = e2.getY() - e1.getY();
//
//                    if (Math.abs(diffX) > Math.abs(diffY))
//                    {
//                        if (diffX > 0) {
//                            onScrollRight( Math.abs( diffX ) );
//                        } else {
//                            onScrollLeft(  Math.abs( diffX ) );
//                        }
//                        result = true;
//                    }
//                    else
//                    {
//                        if (diffY > 0)
//                        {
//                            onScrollBottom( Math.abs( diffY ) );
////                            if ( point > widthHalf )
////                                onScrollRightBottom( Math.abs( diffY ) );
////                            else
////                                onScrollLeftBottom( Math.abs( diffY ) );
//
//                        } else {
//                            onScrollTop( Math.abs( diffY ) );
////                            if ( point > widthHalf )
////                                onScrollRightTop( Math.abs( diffY ) );
////                            else
////                                onScrollLeftTop( Math.abs( diffY ) );
//
//                        }
//                        result = true;
//                    }
//
//                    float cdiffY = currentY - e2.getY();
//                    if  ( Math.abs( cdiffY ) > yThresh )
//                    {
//                        if ( point > widthHalf )
//                            onScrollRightBottom( Math.abs( diffY ) );
//                        else
//                            onScrollLeftBottom( Math.abs( diffY ) );
//                    }
//                    else
//                    {
//                        if ( point > widthHalf )
//                            onScrollRightTop( Math.abs( diffY ) );
//                        else
//                            onScrollLeftTop( Math.abs( diffY ) );
//                    }
//
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return result;
//            }
//
//            @Override
//            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                boolean result = false;
//                try {
//                    float diffY = e2.getY() - e1.getY();
//                    float diffX = e2.getX() - e1.getX();
//                    if (Math.abs(diffX) > Math.abs(diffY)) {
//                        if (diffX > 0) {
//                            onSwipeRight();
//                        } else {
//                            onSwipeLeft();
//                        }
//                        result = true;
//                    } else {
//                        if (diffY > 0) {
//                            onSwipeBottom();
//                        } else {
//                            onSwipeTop();
//                        }
//                        result = true;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return result;
//            }
//
////            @Override
////            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
////                boolean result = false;
////                try {
////                    float diffY = e2.getY() - e1.getY();
////                    float diffX = e2.getX() - e1.getX();
////                    if (Math.abs(diffX) > Math.abs(diffY)) {
////                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY) {
////                            if (diffX > 0) {
////                                onSwipeRight();
////                            } else {
////                                onSwipeLeft();
////                            }
////                            result = true;
////                        }
////                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY) {
////                        if (diffY > 0) {
////                            onSwipeBottom();
////                        } else {
////                            onSwipeTop();
////                        }
////                        result = true;
////                    }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////                return result;
////            }
//        }
//    }
//
//}
