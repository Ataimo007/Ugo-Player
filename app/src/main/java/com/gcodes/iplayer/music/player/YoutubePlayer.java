package com.gcodes.iplayer.music.player;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.services.YouTubeService;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class YoutubePlayer extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlayerStateChangeListener {
    private static final int RECOVERY_DIALOG_REQUEST = 1;
    public static final String DEVELOPER_KEY = "AIzaSyBFboHVZz-Q-WIFSKF6Clb8qZn3BsxsFoM";
    private JacksonFactory jsonFactory;

    private YouTubePlayer player = null;
    private Video video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        jsonFactory = YouTubeService.getInstance().jsonFactory;

        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(DEVELOPER_KEY, this);
    }

    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        if (!wasRestored) {
            youTubePlayer.setPlayerStateChangeListener(this);
            player = youTubePlayer;
            playVideo();
        }
    }

    private void playVideo() {
        String jsonVideo = getIntent().getStringExtra("video");
        try {
            video = jsonFactory.fromString(jsonVideo, Video.class);
            player.loadVideo(video.getId());
//            youTubePlayer.cueVideo(video.getId());
//                youTubePlayer.play();
        } catch (IOException e) {
            Helper.toast(this, "Can't play this Video");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            // Retry initialization if user performed a recovery action
            getYouTubePlayerProvider().initialize(DEVELOPER_KEY, this);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
            if (errorReason.isUserRecoverableError()) {
                errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
            } else {
                String errorMessage = String.format(getString(R.string.error_player), errorReason.toString());
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onLoading() {

    }

    @Override
    public void onLoaded(String s) {
        player.play();
    }

    @Override
    public void onAdStarted() {

    }

    @Override
    public void onVideoStarted() {

    }

    @Override
    public void onVideoEnded() {
        finish();
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        if (errorReason == YouTubePlayer.ErrorReason.NOT_PLAYABLE) {
            Intent intent = new Intent();
            intent.putExtra("state", "restricted" );
            setResult(RESULT_FIRST_USER, intent);
        }
    }

}