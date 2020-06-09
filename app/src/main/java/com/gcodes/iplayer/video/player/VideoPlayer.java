package com.gcodes.iplayer.video.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.series.SeriesPlayerFragment;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.Arrays;

public class VideoPlayer {

    private final PlayerManager playerManager = PlayerManager.getInstance();
    private ConcatenatingMediaSource mediaSource;
    private Video currentVideos[];
    private Series currentSeries;

    public MediaType getCurrentType() {
        return currentType;
    }

    private MediaType currentType;

    private int beginAt;

    public static final int REQUEST_PLAYER = 1000;
    public static final int RESULT_PLAYING = 1001;
    public static final int RESULT_DONE = 1002;

    private static VideoPlayer player;
    private long currentPosition;
    private int currentIndex;

    public enum MediaType{ VIDEOS, SERIES, URL }

    public static VideoPlayer getInstance()
    {
        if ( player == null )
            player = new VideoPlayer();
        return player;
    }

    @NonNull
    public Pair<ConcatenatingMediaSource, MediaSource> buildNewMergedSource(SingleSampleMediaSource subtitleSource, int position )
    {
        MediaSource oldSource = null;
        MediaSource[] sources = new MediaSource[ mediaSource.getSize() ];
        for ( int i = 0; i < mediaSource.getSize(); ++i )
        {
            MediaSource mediaSource = this.mediaSource.getMediaSource(i);
            if ( i == position )
            {
                oldSource = mediaSource;
                mediaSource = new MergingMediaSource( mediaSource, subtitleSource );
            }
            sources[ i ] = mediaSource;
        }

        ConcatenatingMediaSource newSource = new ConcatenatingMediaSource(sources);
        Pair<ConcatenatingMediaSource, MediaSource> sourcePair = new Pair<>(newSource, oldSource);
        return sourcePair;
    }

    @NonNull
    public Pair<ConcatenatingMediaSource, MediaSource> buildNewSource(MediaSource source, int position)
    {
        MediaSource oldSource = null;
        MediaSource[] sources = new MediaSource[ mediaSource.getSize() ];
        for ( int i = 0; i < mediaSource.getSize(); ++i )
        {
            MediaSource mediaSource = this.mediaSource.getMediaSource(i);
            if ( i == position )
            {
                oldSource = mediaSource;
                mediaSource = source;
            }
            sources[ i ] = mediaSource;
        }

        ConcatenatingMediaSource newSource = new ConcatenatingMediaSource(sources);
        Pair<ConcatenatingMediaSource, MediaSource> sourcePair = new Pair<>(newSource, oldSource);
        return sourcePair;
    }

//    private ConcatenatingMediaSource BuildNewSourceOnSubtitle2( SingleSampleMediaSource subtitleSource, int position  )
//    {
//        Gson gson = new Gson();
//        ConcatenatingMediaSource clone = gson.fromJson(gson.toJson(mediaSource), ConcatenatingMediaSource.class);
//        MediaSource targetSource = clone.getMediaSource(position);
//        MergingMediaSource newSource = new MergingMediaSource( targetSource, subtitleSource );
//        clone.removeMediaSource( position );
//        clone.addMediaSource( position, newSource );
//        playerManager.prepare( clone, false, false, PlayerManager.MediaType.VIDEO );
//        return clone;
//    }

    public void switchSources(ConcatenatingMediaSource source)
    {
        Log.d( "Video_Player", "The old source " + mediaSource );
        Log.d( "Video_Player", "The old source " + mediaSource.getSize() );
        Log.d( "Video_Player", "The old source " + source );
        Log.d( "Video_Player", "The new source " + source.getSize() );

        playerManager.prepare( mediaSource, false, false, PlayerManager.MediaType.VIDEO );
        int currentWindowIndex = playerManager.getCurrentWindow();
        long currentPosition = playerManager.getCurrentPosition();

        playerManager.prepare( source, true, true, PlayerManager.MediaType.VIDEO );
        playerManager.playTrackAt( currentWindowIndex, currentPosition );
        mediaSource = source;
    }

    public void renderVideoPlayer()
    {
        playerManager.renderVideoPlayer();
    }

    public void tryRenderVideoPlayer() {
        playerManager.tryRenderVideoPlayer();
    }

    public void tryRenderVideoPlayer( int result ) {
        playerManager.tryRenderVideoPlayer();
    }

    public void tryHideVideoPlayer() {
        playerManager.tryHideVideoPlayer();
    }

    public ProgressiveMediaSource getMediaSource(String url )
    {
        Uri media = Uri.parse( url );
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory( playerManager.getOfflineFactory() ).createMediaSource(media);
        return source;
    }

    public void saveState()
    {
        currentPosition = playerManager.getCurrentPosition();
        currentIndex = playerManager.getCurrentWindow();
    }

    public void clearState()
    {
        currentPosition = -1;
        currentIndex = -1;
    }

    public void clearSource()
    {
        currentSeries = null;
        currentVideos = null;
        mediaSource = null;
        currentType = null;
    }

    public boolean hasState()
    {
        return currentIndex != -1 && currentPosition != -1;
    }

    public void continuePlay()
    {
        playerManager.playAgain();
        restoreState();
    }

    public void restoreState()
    {
        playerManager.seekTo( currentIndex, currentPosition );
    }

    public void initVideoSources(String[] vids, int start, PlayerView player)
    {
        MediaSource sources[] = new MediaSource[ vids.length ];
        Video videos[] = new Video[vids.length];
        for ( int i = 0; i < vids.length; ++i )
        {
            Video video = Video.fromGson(vids[i]);
            videos[ i ] = video;
            sources[ i ] = getSource(video, player);
        }
        this.currentVideos = videos;
        currentType = MediaType.VIDEOS;
        mediaSource = new ConcatenatingMediaSource( sources );
        playerManager.prepare( mediaSource, PlayerManager.MediaType.VIDEO );
        beginAt = start;
    }

    public void initVideoSources(Series series, PlayerView player)
    {
        MediaSource sources[] = new MediaSource[ series.getCount() ];
        for ( int i = 0; i < series.getCount(); ++i )
        {
            sources[ i ] = getSource(series.getVideo( i ), player);
        }
        this.currentVideos = series.getVideos();
        this.currentSeries = series;
        currentType = MediaType.SERIES;
        mediaSource = new ConcatenatingMediaSource( sources );
        playerManager.prepare( mediaSource, PlayerManager.MediaType.VIDEO );
        beginAt = 0;
    }

    public void initOnlineSources(String[] vids, int start) {
        MediaSource sources[] = new MediaSource[ vids.length ];
        for ( int i = 0; i < vids.length; ++i )
        {
            sources[ i ] = getMediaSource( vids[ i ] );
        }
        currentType = MediaType.URL;
        mediaSource = new ConcatenatingMediaSource( sources );
        playerManager.prepare( mediaSource, PlayerManager.MediaType.VIDEO );
        beginAt = start;
    }


    public void initSavedSource()
    {
        restoreState();
        beginAt = currentIndex;
    }

    public static void play(Fragment fragment, Video ...videos) {
        Intent intent = new Intent( fragment.getContext(), VideoPlayerActivity.class );
        String gsonVideos[] = new String[ videos.length ];
        for ( int i = 0; i < videos.length; ++i )
            gsonVideos[ i ] = videos[ i ].toGson();
        intent.putExtra( "medias", gsonVideos );
        intent.putExtra( "data_type", "video" );
//        activity.startActivityForResult(intent, MainActivity.REQUEST_VIDEO);
        fragment.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
    }

    public static void play(Fragment fragment)
    {
        Intent intent = new Intent( fragment.getContext(), VideoPlayerActivity.class );
        intent.putExtra( "data_type", "controller" );
//        activity.startActivity(intent);
        fragment.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
    }

//    public static void play(Activity activity)
//    {
//        Intent intent = new Intent( activity, VideoPlayerActivity.class );
//        intent.putExtra( "data_type", "controller" );
////        activity.startActivity(intent);
//        activity.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
//    }

    public void showSeriesFragment() {
        SeriesPlayerFragment.navigate( currentSeries );
    }

    public static void play(Activity activity, String url )
    {
        Intent intent = new Intent( activity, VideoPlayerActivity.class );
        intent.putExtra( "medias", new String[]{ url } );
        intent.putExtra( "data_type", "url" );
//        activity.startActivityForResult(intent, MainActivity.REQUEST_VIDEO);
        activity.startActivity(intent);
    }

    public static void play(Fragment fragment, int pos, Video ...videos) {
        Intent intent = new Intent( fragment.getContext(), VideoPlayerActivity.class );
        String gsonVideos[] = new String[ videos.length ];
        for ( int i = 0; i < videos.length; ++i )
            gsonVideos[ i ] = videos[ i ].toGson();
        intent.putExtra( "medias", gsonVideos );
        intent.putExtra( "data_type", "video" );
        intent.putExtra( "begin", pos );
//        activity.startActivityForResult(intent, MainActivity.REQUEST_VIDEO);
//        activity.startActivity(intent);
        fragment.startActivityForResult( intent, VideoPlayer.REQUEST_PLAYER );
    }

    public void playNow()
    {
        playerManager.playVideo();
        if  ( player.getBeginAt() >= 0 )
            playerManager.playAt( player.getBeginAt() );
        clearState();
    }

    public boolean isPlaying()
    {
        return playerManager.isPlaying();
    }

    public boolean isInPlayingState()
    {
        return playerManager.isInPlayingState();
    }

    public void play()
    {
        playerManager.play();
    }

    public void pause()
    {
        playerManager.pause();
    }

    public void stop()
    {
        playerManager.stop();
        clearState();
        clearSource();
    }

    public Video getVideo(int position )
    {
        return currentVideos[ position ];
    }

    public Video getCurrentVideo()
    {
        return currentVideos[ playerManager.getCurrentIndex() ];
    }

    public int getCurrentIndex()
    {
        return playerManager.getCurrentIndex();
    }

    public int findIndex(Video video )
    {
        return Arrays.binarySearch( currentVideos, video );
    }

    private MediaSource getSource(Video video, PlayerView player)
    {
        return getVideoSource(video.getId());
    }

//    private MediaSource getSource(Video video, boolean displaySubtitle, PlayerView player)
//    {
//        MediaSource mediaSource = getVideoSource(video.getId());
//        if ( displaySubtitle )
//        {
//            SingleSampleMediaSource subtitleSource = getSubtitle( video, player );
//            if ( subtitleSource != null )
//                mediaSource = new MergingMediaSource( mediaSource, subtitleSource );
//        }
//        return mediaSource;
//    }

//    public void retrieveSubtitle(Video video, PlayerView player)
//    {
//        OpenSubtitleService.getInstance(player).beginSubtitling(video, player.getContext(), null);
//    }

//    public SingleSampleMediaSource retrieveSubtitle(Video video, PlayerView player)
//    {
//        File subtitle = OpenSubtitleService.getInstance(player).retrieveSubtitle(video, player.getContext(), null);
//        if ( subtitle == null )
//            Log.w( "Subtitle_Activities", "Unable to find subtitle" );
//        return getSubtitle( subtitle );
//    }

//    private SingleSampleMediaSource getSubtitle(Video video, PlayerView player)
//    {
////        File file = OpenSubtitleService.getInstance(player).getSavedSubtitle( video, player.getContext() );
//        File file = null;
//        if ( file == null )
//            return null;
//        return getSubtitle( file );
//    }

    public SingleSampleMediaSource getSubtitle(File file)
    {
        SingleSampleMediaSource.Factory subFact = new SingleSampleMediaSource.Factory( playerManager.getFactory() );
        Uri subUri = Uri.fromFile(file);
        Format subFormat = Format.createTextSampleFormat( null, MimeTypes.APPLICATION_SUBRIP, C.SELECTION_FLAG_DEFAULT, "en");
        SingleSampleMediaSource subtitle = subFact.createMediaSource(subUri, subFormat, C.TIME_UNSET);
        return subtitle;
    }

    private ProgressiveMediaSource getVideoSource(long id )
    {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );
        Log.d( "Video_Player", "Video " + id );
        Log.d( "Video_Player", "Video " + media.getPath() );
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(playerManager.getFactory()).createMediaSource(media);
        return source;
    }

    private ProgressiveMediaSource getVideoSource(String id, Context context)
    {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );
        Log.d( "Video_Player", "Video " + id );
        Log.d( "Video_Player", "Video " + media.getPath() );
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getResources().getString(R.string.app_name)));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory).createMediaSource(media);
        return source;
    }

    public int getBeginAt() {
        return beginAt;
    }

    public void setBeginAt(int beginAt) {
        this.beginAt = beginAt;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}
