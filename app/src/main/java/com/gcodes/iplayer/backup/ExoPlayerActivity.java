package com.gcodes.iplayer.backup;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jarvanmo.exoplayerview.ui.ExoVideoView;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public class ExoPlayerActivity extends AppCompatActivity {

    private static SimpleExoPlayer player;
    private MediaSource mediaSource;
    private ExoVideoView playerView;
    private final Handler videoHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.exo_video_player);

        initPlayer();

//        initFullScreen();
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

//    private void initFullScreen()
//    {
//        mVisible = true;
//        mControlsView = findViewById(R.id.control_view);
//        mContentView = findViewById(R.id.video_player);
//
//
//        // Set up the user interaction to manually show or hide the system UI.
//        mContentView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggle();
//            }
//        });
//
//        // Upon interacting with UI controls, delay any scheduled hide()
//        // operations to prevent the jarring behavior of controls going away
//        // while interacting with the UI.
//        findViewById(R.id.control_view).setOnTouchListener(mDelayHideTouchListener);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        player.setPlayWhenReady(true);

    }

    private void initPlayer()
    {
//        player = ExoPlayerFactory.newSimpleInstance( this, new DefaultTrackSelector() );
        player = ExoPlayerFactory.newSimpleInstance( this, new DefaultTrackSelector() );
        initView();
        initSource();
    }

    private void initView()
    {
        playerView = findViewById(R.id.video_player);
        playerView.setPlayer( player );
//        playerView.setControllerDisplayMode( ExoVideoView.);

        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT )
        {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT );
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        }
        if  ( getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE )
        {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL );
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        }
//        player.addListener( new KeepScreenOn(playerView) );
        playerView.setBackgroundColor(Color.BLACK);

        setOnGestureListeners();
    }

    private void initSource()
    {
        String id = getIntent().getStringExtra("media");
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getResources().getString(R.string.app_name)));
        mediaSource = new ExtractorMediaSource.Factory(factory).createMediaSource( media );
        player.prepare( mediaSource );
    }

    private void release()
    {
        player.release();
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
    private void toggle()
    {
        Log.d( "Exo_Player", "Toggling " + mVisible );
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
        videoHandler.removeCallbacks(mHidePart2Runnable);
        videoHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }
//
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        showStatus();

        // Schedule a runnable to display UI elements after a delay
        videoHandler.removeCallbacks(mHidePart2Runnable);
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

    @SuppressLint("ClickableViewAccessibility")
    private void setOnGestureListeners()
    {
//        playerView.setOnTouchListener(new OnSwipeTouchListener( ExoPlayerActivity.this )
//        {
//
//            @Override
//            public void onClick( MotionEvent e ) {
//                toggle();
//                super.onClick( e );
//            }
//
//        });
//        CustomVideoGesture gesture = new CustomVideoGesture( ExoPlayerActivity.this, )
    }

    public class OnSwipeTouchListener implements View.OnTouchListener {

        private static final String TAG = "OnSwipeTouchListener";

        private final GestureDetectorCompat mDetector;

        public OnSwipeTouchListener(Context context) {
            mDetector = new GestureDetectorCompat(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            mDetector.onTouchEvent(event);
            return playerView.onTouchEvent( event );
        }

        public void onClick( MotionEvent e )
        {
        }

        public void onDoubleClick() {
            Log.i(TAG, "onClick: Clicking TWO TIMES in the screen");
        }

        public void onLongClick() {
            Log.i(TAG, "onLongClick: LONG click in the screen");
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onClick( e );
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                onDoubleClick();
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                onLongClick();
                super.onLongPress(e);
            }
        }
    }

}
