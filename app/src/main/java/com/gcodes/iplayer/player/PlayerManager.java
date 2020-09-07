package com.gcodes.iplayer.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.services.ACRService;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.player.VideoController;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class PlayerManager
{
//    private static PlayerManager PlayerManager;

    private final String userAgent;
    private final MusicManager musicManager;
    private final VideoManager videoManager;
    private SimpleExoPlayer player;
    private DefaultDataSourceFactory factory;
    private Context context;

    private MediaSource mediaSource;
//    private Intent service;

    private Cache downloadCache;
    private File downloadDirectory;
    private DatabaseProvider databaseProvider;
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

//    private MusicController musicController;
//    private VideoController fragmentVideo;
//    private static final String MUSIC_FRAGMENT_TAG = "music_controller_view";
//    private static final String VIDEO_FRAGMENT_TAG = "video_controller_view";

    private MediaType playing;
    ArrayList< Player.EventListener > listeners = new ArrayList<>();

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public VideoManager getVideoManager() {
        return videoManager;
    }

    public enum MediaType{ MUSIC, VIDEO }
    public enum VideoSourceType{ VIDEOS, SERIES, URL }

    public static final int REQUEST_VIDEO_PLAYER = 1000;
    public static final int VIDEO_RESULT_PLAYING = 1001;
    public static final int VIDEO_RESULT_DONE = 1002;

    public PlayerManager(Context context)
    {
        userAgent = Util.getUserAgent(context, context.getResources().getString(R.string.app_name));
        factory = new DefaultDataSourceFactory( context, userAgent);
        player = ExoPlayerFactory.newSimpleInstance( context, new DefaultTrackSelector() );
        this.context = context;

        PlayerDatabase.initialize( context );
        ACRService.initialize(context);

        musicManager = new MusicManager();
        videoManager = new VideoManager();
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isMusicPlaying()
    {
        return playing == MediaType.MUSIC;
    }

    public boolean isVideoPlaying()
    {
        return playing == MediaType.MUSIC;
    }


//    private void consumeVideoState(boolean playWhenReady, int playbackState) {
//        boolean isPlaying = playWhenReady && playbackState == Player.STATE_READY;
//        NavController navController = Navigation.findNavController(getActivity(), R.id.video_host);
//        try {
//            NavBackStackEntry backStackEntry = navController.getBackStackEntry(R.id.videoFragment);
//            backStackEntry.getSavedStateHandle().set( "is_playing", isPlaying );
//        } catch (IllegalArgumentException ignored){}
//    }

//    private boolean isMusicPlayer()
//    {
//        return MusicPlayer.getInstance().isMusicPlaying();
//    }
//
//    private void consumeMedia()
//    {
//        switch ( playing )
//        {
//            case MUSIC:
////                if  ( isMusicPlayer() )
//                consumeTrack();
//                break;
//
//            case VIDEO:
//                consumeVideo();
//                break;
//
//            default:
//        }
//    }

//    private void consumeTrack()
//    {
//        int index = player.getCurrentPeriodIndex();
//        Music music = MusicPlayer.getInstance().getMusic(index);
//        if  ( musicController != null )
//        {
//            musicController.consumeTrack( music );
//            if ( player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_READY )
//                musicController.startAnimation();
//            else
//                musicController.pauseAnimation();
//        }
//    }

//    private void consumeVideo()
//    {
//        VideoPlayer videoPlayer = VideoPlayer.getInstance();
//        if (videoPlayer.getCurrentType().equals(VideoSourceType.URL))
//            return;
//
//        int index = player.getCurrentPeriodIndex();
//        Video video = videoPlayer.getVideo(index);
//        if ( fragmentVideo != null && fragmentVideo.isAdded() )
//        {
//            fragmentVideo.consumeVideo( video );
//        }
////        SeriesPlayerFragment.tryUpdateControllerButton( isPlaying() );
//    }

//    public void getState()
//    {
//        player.getCurrentPeriodIndex();
//    }

//    private void renderMusicPlayer()
//    {
//        FragmentManager manager = context.getSupportFragmentManager();
//        Fragment fragment = manager.findFragmentByTag(MUSIC_FRAGMENT_TAG);
//        if ( fragment != null )
//        {
////            Log.d( "Music_Controller", "Fragment Exists" );
//            musicController = (MusicController) fragment;
//            consumeTrack();
//        }
//        else
//        {
////            Log.d( "Music_Controller", "A new Fragment" );
//            FragmentTransaction transaction = manager.beginTransaction();
//            musicController = MusicController.newInstance();
//            View musicControl = context.findViewById(R.id.player_control);
//            musicControl.setVisibility( View.VISIBLE );
//            transaction.replace( R.id.player_control, musicController, MUSIC_FRAGMENT_TAG );
//            transaction.runOnCommit(this::consumeTrack);
//            transaction.commit();
//        }
//
////        consumeTrack();
//    }

//    public void tryRenderVideoPlayer() {
//        Log.w("Video_Player", String.format("Video Controller state %b ready %b", Player.STATE_READY, player.getPlayWhenReady() ) );
//        Log.w("Video_Player", String.format("Video Controller state %s ready %s", playing, player.getPlaybackState() ) );
//        if ( playing == MediaType.VIDEO && player.getPlaybackState() == Player.STATE_READY )
//        {
//            Log.w("Video_Player", "Showing Video Controller");
//            renderVideoPlayer();
//        }
//    }

//    public void tryHideVideoPlayer()
//    {
//        FragmentManager manager = context.getSupportFragmentManager();
//        Fragment fragment = manager.findFragmentByTag(VIDEO_FRAGMENT_TAG);
//        if ( fragment != null )
//        {
//            FragmentTransaction transaction = manager.beginTransaction();
//            View musicControl = context.findViewById(R.id.player_control);
//            musicControl.setVisibility( View.GONE );
//            transaction.remove( fragment );
//            transaction.commit();
//        }
//    }
//
//    public void renderVideoPlayer()
//    {
//        FragmentManager manager = context.getSupportFragmentManager();
//        Fragment fragment = manager.findFragmentByTag(VIDEO_FRAGMENT_TAG);
//
//        if ( fragment != null )
//        {
//            fragmentVideo = (VideoController) fragment;
//            consumeVideo();
//        }
//        else
//        {
//            FragmentTransaction transaction = manager.beginTransaction();
//            fragmentVideo = VideoController.newInstance();
//            View musicControl = context.findViewById(R.id.player_control);
//            musicControl.setVisibility( View.VISIBLE );
//            transaction.replace( R.id.player_control, fragmentVideo, VIDEO_FRAGMENT_TAG );
//            transaction.runOnCommit(this::consumeVideo);
//            transaction.commit();
//        }
//    }

//    private boolean readyToRender()
//    {
////        Log.d( "Music_Controller", "Ready to render " + ( host != null ) );
////        Log.d( "Music_Controller", "Ready to render " + ( fragmentMusic == null ) );
//        return host != null && fragmentMusic == null;
//    }

//    private boolean isRendered()
//    {
//        return musicController != null;
//    }

//    private synchronized boolean readyToChangeTrack()
//    {
//        return host != null && fragmentMusic != null;
//    }

//    public void onDestroyActivity()
//    {
//        context = null;
//        musicController = null;
//        destroy();
//    }

//    public static PlayerManager getInstance()
//    {
//        return PlayerManager;
//    }

//    public static void init( AppCompatActivity context )
//    {
//        if  ( PlayerManager == null )
//        {
//            PlayerManager = new PlayerManager( context );
//            PlayerManager.initPlayerControl();
//        }
//    }

//    public Handler getHandler()
//    {
//        return new Handler( context.getMainLooper() );
//    }

//    public void destroy()
//    {
//        player.release();
//        player = null;
//        SimpleExoPlayer player = getInstance().getPlayer();
//        if ( player != null )
//        {
//            for ( Player.EventListener listener : listeners ) {
//                player.removeListener(listener);
//            }
//        }
//        listeners = null;
//    }

//    public static PlayerManager getInstance(AppCompatActivity context )
//    {
//        if ( PlayerManager == null )
//            PlayerManager = new PlayerManager( context );
//        return PlayerManager;
//    }

    public void prepare(MediaSource source, boolean position, boolean state, MediaType playing) {
        this.playing = playing;
        player.prepare( source, position, state);
        mediaSource = source;
    }

    public void prepare(MediaSource source, MediaType playing )
    {
        this.playing = playing;
        player.prepare( source );
        mediaSource = source;
    }

    public void playVideo()
    {
        play();
    }

//    public void play()
//    {
////        if  ( !player.getPlayWhenReady() )
//        player.setPlayWhenReady( true );
//        if  ( isServiceRunning() )
//            stopService();
//    }

    public void play()
    {
//        if  ( !player.getPlayWhenReady() )
        player.setPlayWhenReady( true );
//        if  ( isServiceRunning() )
//            stopService();
    }

    public void stop()
    {
//        int index = player.getCurrentPeriodIndex();
//        Video video = VideoPlayer.getInstance().getVideo(index);
//        Log.w("Video_Player", String.format("Stoping Video Controller for %s %s", video, mediaSource ) );
        player.stop(false);
        clearSource();
//        player.setPlayWhenReady( false );
    }

    public void clearSource()
    {
        mediaSource = null;
    }

    public void playOnly()
    {
        player.setPlayWhenReady( true );
    }

    public void playAgain()
    {
//        int index = player.getCurrentPeriodIndex();
//        Video video = VideoPlayer.getInstance().getVideo(index);
//        Log.w("Video_Player", String.format("Showing Video Controller for %s %s", video, mediaSource ) );
        player.prepare(mediaSource);
        player.setPlayWhenReady( true );
    }

    public void playAt( int begin )
    {
        player.seekToDefaultPosition( begin );
    }

    public void playMusic()
    {
        playOnForeground();
    }

    public void playOnForeground()
    {
        player.setPlayWhenReady( true );
//        beginService();
    }

//    public void playOnForeground()
//    {
//        player.setPlayWhenReady( true );
//        beginService();
//    }

    public void pause()
    {
        player.setPlayWhenReady( false );
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public DefaultDataSourceFactory getFactory() {
        return factory;
    }

    /** Returns a {@link DataSource.Factory}. */
    public DataSource.Factory getOfflineFactory() {
        DefaultDataSourceFactory upstreamFactory = new DefaultDataSourceFactory(context, buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    public Context getContext() {
        return context;
    }

//    public FragmentActivity getActivity() {
//        return context;
//    }

    public void shuffle()
    {
        player.setShuffleModeEnabled( true );
    }

    public void repeatAll()
    {
        player.setRepeatMode( com.google.android.exoplayer2.Player.REPEAT_MODE_ALL );
    }

    public void playTrack( int position )
    {
        player.seekTo( position, 0 );
    }

    public void seekTo(long position )
    {
        player.seekTo( position );
    }

    public void seekTo(int window, long position )
    {
        player.seekTo( window, position );
    }

    public boolean isReady()
    {
        return player.getPlaybackState() == com.google.android.exoplayer2.Player.STATE_READY;
    }

    public boolean isPlaying()
    {
        return player.getPlaybackState() == com.google.android.exoplayer2.Player.STATE_READY && player.getPlayWhenReady();
    }

    public boolean isInPlayingState()
    {
        return player.getPlayWhenReady();
    }

    public void playTrackAt( int track, long position )
    {
        player.seekTo( track, position );
    }

//    private void beginService()
//    {
//        service = new Intent( context, MusicPlayerService.class );
//        Util.startForegroundService( context, service);
//    }
//
//    private void stopService()
//    {
//        context.stopService( service );
//        service = null;
//    }
//
//    private boolean isServiceRunning()
//    {
//        return service != null;
//    }

    public int getCurrentWindow() {
        return player.getCurrentWindowIndex();
    }

    public int getCurrentIndex() {
        return player.getCurrentPeriodIndex();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public void setView(PlayerView playerView) {
        playerView.setPlayer( player );
    }

    public void setVideoScalingMode(int mode ) {
        player.setVideoScalingMode( mode );
    }

    public void addListener(com.google.android.exoplayer2.Player.EventListener listener) {
        listeners.add( listener );
        player.addListener( listener );
    }

    private void removeListener(com.google.android.exoplayer2.Player.EventListener listener ) {
        listeners.remove(listener);
        player.removeListener(listener);
    }

//    public void saveState() {
//        PlayerState.saveState();
//    }
//
//    public boolean hasState()
//    {
//        return PlayerState.hasState();
//    }
//
//    public void restoreState()
//    {
//        PlayerState state = PlayerState.getState();
//        prepare( state.getSource(), true, true );
//        playTrackAt( state.getTrack(), state.getDuration() );
//        player.setPlayWhenReady( state.isReady() );
//        if ( state.isRunningService() )
//            beginService();
//    }

    public File getDownloadDirectory() {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = context.getFilesDir();
            }
        }
        return downloadDirectory;
    }

    public synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider());
        }
        return downloadCache;
    }

    public DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider( context );
        }
        return databaseProvider;
    }

    protected static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(cache, upstreamFactory, new FileDataSourceFactory(),
                /* cacheWriteDataSinkFactory= */ null, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                /* eventListener= */ null);
    }

    /** Returns a {@link HttpDataSource.Factory}. */
    public HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(getUserAgent());
    }

    public MediaSource getMediaSource() {
        return mediaSource;
    }

//    public static interface PlayerListener extends Player.EventListener {
//        public void initialize(PlayerManager manager);
//    }

    public class MusicManager
    {
        private ArrayList<Music> musics;
        private ConcatenatingMediaSource source;
        private long currentPosition;
        private int currentIndex;

        public void saveCurrentState()
        {
            currentPosition = getCurrentPosition();
            currentIndex = getCurrentIndex();
        }

        public void restoreCurrentState()
        {
            prepare( source, PlayerManager.MediaType.MUSIC );
            seekTo( currentIndex, currentPosition );
            play();
        }

        public ArrayList<Music> getMusics() {
            return musics;
        }

        public int getMusicsCount() {
            return musics.size();
        }

        public Music getMusic(int index ) {
            return musics.get( index );
        }

        public int getIndex(Music music) {
            return musics.indexOf(music);
        }

        public int getCurrentTrack()
        {
            return getPlayerManager().getCurrentPeriodIndex();
        }

        public void play( int position )
        {
            playTrack( position );
        }

        public boolean inPlayList(Music music) {
            return musics.contains(music);
        }

        public boolean isMusicPlaying() {
            return musics != null;
        }

        public int getPosition(Music music) {
            return musics.indexOf( music );
        }

        public SimpleExoPlayer getPlayerManager()
        {
            return getPlayer();
        }

        public PlayerManager getMainPlayerManager()
        {
            return PlayerManager.this;
        }

        private void initSource(ArrayList<Music> musics )
        {
            Music music;
            ArrayList<ProgressiveMediaSource> musicSource = new ArrayList<>();
            for ( int i = 0; i < musics.size(); ++i )
            {
                music = musics.get(i);
                if ( music != null )
                    musicSource.add( getMusicSource(music) );
                else
                    Log.d("Music_Player", i + " is null" );
            }
            source = new ConcatenatingMediaSource(musicSource.toArray( new ProgressiveMediaSource[]{}));
            this.musics = musics;
            prepare( source, true, true, PlayerManager.MediaType.MUSIC );
            shuffle();
            repeatAll();
            initError();
        }

        private void initSource(ArrayList<Music> musics, MediaSource source )
        {
            this.musics = musics;
            prepare( source, true, true, PlayerManager.MediaType.MUSIC );
            shuffle();
            repeatAll();
            initError();
        }

        @NonNull
        public Pair<ConcatenatingMediaSource, MediaSource> buildNewSource(MediaSource childSource, int position)
        {
            MediaSource oldSource = null;
            MediaSource[] sources = new MediaSource[ source.getSize() ];
            for ( int i = 0; i < source.getSize(); ++i )
            {
                MediaSource mediaSource = source.getMediaSource(i);
                if ( i == position )
                {
                    oldSource = mediaSource;
                    mediaSource = childSource;
                }
                sources[ i ] = mediaSource;
            }

            ConcatenatingMediaSource newSource = new ConcatenatingMediaSource(sources);
            Pair<ConcatenatingMediaSource, MediaSource> sourcePair = new Pair<>(newSource, oldSource);
            return sourcePair;
        }

        public void addToPlaylist(Music music) {
            ProgressiveMediaSource newMusic = getMusicSource(music);
            musics.add(music);
            source.addMediaSource(newMusic);
        }

        public void switchSources(ConcatenatingMediaSource source)
        {
            int currentWindowIndex = getCurrentWindow();
            long currentPosition = getCurrentPosition();
//        playerManager.prepare( source, false, false, PlayerManager.MediaType.MUSIC );
            prepare( source, true, true, PlayerManager.MediaType.MUSIC );
            playTrackAt( currentWindowIndex, currentPosition );
            this.source = source;
        }

        private void initError() {
            SimpleExoPlayer player = getPlayer();
            player.addListener(new com.google.android.exoplayer2.Player.EventListener() {
                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    int trackNo = player.getCurrentPeriodIndex();
                    Music music = musics.get(trackNo);
//                source.removeMediaSource( trackNo );
                    String message = String.format("The track %s does not exist or %s", music.getName(), error.getMessage());
                    Toast.makeText( getContext(), message, Toast.LENGTH_SHORT).show();
                    prepare( source, PlayerManager.MediaType.MUSIC );
                    player.setPlayWhenReady( true );
                }
            });
        }

        public void playAll( ArrayList<Music> musics )
        {
            initSource( musics );
            Log.d( "Music_Player", "Id is " + musics);
//        musicPlayer.beginService();
            play();
        }

        public void playAll( ArrayList<Music> musics, MediaSource source )
        {
            initSource( musics, source );
            Log.d( "Music_Player", "Id is " + musics);
//        musicPlayer.beginService();
            play();
        }

        public void play()
        {
            playMusic();
        }

        public ProgressiveMediaSource getMusicSource( Music music )
        {
            Uri media = music.toUri();
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(getFactory()).createMediaSource(media);
//        ExtractorMediaSource source = new ExtractorMediaSource.Factory( playerManager.getFactory() ).createMediaSource(media);
            return source;
        }

        public ProgressiveMediaSource getMusicSource(File file)
        {
            Uri media = Uri.fromFile(file);
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(getFactory()).createMediaSource(media);
            return source;
        }

        public ConcatenatingMediaSource getMediaSource()
        {
            return source;
        }

        public ProgressiveMediaSource getMediaSource( String url )
        {
            Uri media = Uri.parse( url );
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(getFactory()).createMediaSource(media);
//        ExtractorMediaSource source = new ExtractorMediaSource.Factory( playerManager.getFactory() ).createMediaSource(media);
            return source;
        }

        public void addListener(Player.EventListener listener) {
            PlayerManager.this.addListener(listener);
        }

        public void removeListener(Player.EventListener listener) {
            PlayerManager.this.removeListener(listener);
        }
    }

    public class VideoManager {

        private ConcatenatingMediaSource mediaSource;
        private Video currentVideos[];

        public Series getCurrentSeries() {
            return currentSeries;
        }

        private Series currentSeries;

        public VideoSourceType getCurrentType() {
            return currentType;
        }

        private VideoSourceType currentType;

        private int beginAt = -1;

//        private static com.gcodes.iplayer.video.player.VideoPlayer player;
        private long currentPosition;
        private int currentIndex;

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

        public void switchSources(ConcatenatingMediaSource source)
        {
            Log.d( "Video_Player", "The old source " + mediaSource );
            Log.d( "Video_Player", "The old source " + mediaSource.getSize() );
            Log.d( "Video_Player", "The old source " + source );
            Log.d( "Video_Player", "The new source " + source.getSize() );

//        playerManager.prepare( mediaSource, false, false, PlayerManager.MediaType.VIDEO );
            int currentWindowIndex = getCurrentWindow();
            long currentPosition = getCurrentPosition();

            prepare( source, true, true, PlayerManager.MediaType.VIDEO ); // think of changing this playerManager.prepare( mediaSource, false, false, PlayerManager.MediaType.VIDEO );
            playTrackAt( currentWindowIndex, currentPosition );
            mediaSource = source;
        }

//        public void renderVideoPlayer()
//        {
//            PlayerManager.this.renderVideoPlayer();
//        }
//
//        public void tryRenderVideoPlayer() {
//            PlayerManager.this.tryRenderVideoPlayer();
//        }
//
//        public void tryRenderVideoPlayer( int result ) {
//            tryRenderVideoPlayer();
//        }
//
//        public void tryHideVideoPlayer() {
//            PlayerManager.this.tryHideVideoPlayer();
//        }

        public ProgressiveMediaSource getMediaSource(String url )
        {
            Uri media = Uri.parse( url );
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory( getOfflineFactory() ).createMediaSource(media);
            return source;
        }

        public void saveState()
        {
            currentPosition = getCurrentPosition();
            currentIndex = getCurrentWindow();
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
            playAgain();
            restoreState();
        }

        public void restoreState()
        {
            seekTo( currentIndex, currentPosition );
        }

        public void initVideoSources(Video[] videos, int start)
        {
            MediaSource sources[] = new MediaSource[ videos.length ];
            for ( int i = 0; i < videos.length; ++i )
                sources[ i ] = getSource(videos[i]);
            this.currentVideos = videos;
            currentType = VideoSourceType.VIDEOS;
            mediaSource = new ConcatenatingMediaSource( sources );
            prepare( mediaSource, PlayerManager.MediaType.VIDEO );
            beginAt = start;
        }

        public void initVideoSources(Video[] videos)
        {
            MediaSource sources[] = new MediaSource[ videos.length ];
            for ( int i = 0; i < videos.length; ++i )
                sources[ i ] = getSource(videos[i]);
            this.currentVideos = videos;
            currentType = VideoSourceType.VIDEOS;
            mediaSource = new ConcatenatingMediaSource( sources );
            prepare( mediaSource, PlayerManager.MediaType.VIDEO );
        }

        public void initVideoSources(Series series)
        {
            MediaSource sources[] = new MediaSource[ series.getCount() ];
            for ( int i = 0; i < series.getCount(); ++i )
            {
                sources[ i ] = getSource(series.getVideo( i ));
            }
            this.currentVideos = series.getVideos();
            this.currentSeries = series;
            currentType = VideoSourceType.SERIES;
            mediaSource = new ConcatenatingMediaSource( sources );
            prepare( mediaSource, PlayerManager.MediaType.VIDEO );
            beginAt = 0;
        }

        public void initOnlineSources(String[] vids, int start) {
            MediaSource sources[] = new MediaSource[ vids.length ];
            for ( int i = 0; i < vids.length; ++i )
            {
                sources[ i ] = getMediaSource( vids[ i ] );
            }
            currentType = VideoSourceType.URL;
            mediaSource = new ConcatenatingMediaSource( sources );
            prepare( mediaSource, PlayerManager.MediaType.VIDEO );
            beginAt = start;
        }


        public void initOnlineSources(String url) {
            this.mediaSource = new ConcatenatingMediaSource( mediaSource );
            currentType = VideoSourceType.URL;
            prepare(this.mediaSource, PlayerManager.MediaType.VIDEO );
        }


        public void initSavedSource()
        {
            restoreState();
            beginAt = currentIndex;
        }

//        public void showSeriesFragment() {
//            SeriesPlayerFragment.navigate( currentSeries );
//
//            Bundle bundle = new Bundle();
//            bundle.putString( "series", aSeries.toGson() );
//            navController.navigate( R.id.action_videoFragment_to_seriesPlayerFragment, bundle );
//        }

        public void playNow()
        {
            playVideo();
            if  ( getBeginAt() >= 0 )
                playAt( getBeginAt() );
            clearState();
        }

        public boolean isPlaying()
        {
            return PlayerManager.this.isPlaying();
        }

        public boolean isInPlayingState()
        {
            return PlayerManager.this.isInPlayingState();
        }

        public void play()
        {
            PlayerManager.this.play();
        }

        public void pause()
        {
            PlayerManager.this.pause();
        }

        public void stop()
        {
            PlayerManager.this.stop();
            clearState();
            clearSource();
        }

        public Video getVideo(int position )
        {
            return currentVideos[ position ];
        }

        public Video getCurrentVideo()
        {
            return currentVideos[ getCurrentIndex() ];
        }

        public int getCurrentIndex()
        {
            return PlayerManager.this.getCurrentIndex();
        }

        public int findIndex(Video video )
        {
            return Arrays.binarySearch( currentVideos, video );
        }

        private MediaSource getSource(Video video)
        {
            return getVideoSource(video.getId());
        }

        public SingleSampleMediaSource getSubtitle(File file)
        {
            SingleSampleMediaSource.Factory subFact = new SingleSampleMediaSource.Factory( getFactory() );
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
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(getFactory()).createMediaSource(media);
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
            return PlayerManager.this;
        }
    }


//    private static class PlayerState
//    {
//        private static PlayerState state = null;
//
//        private final MediaSource source;
//        private final int track;
//        private final long duration;
//        private final boolean ready;
//        private final boolean runningService;
//
//        public PlayerState(PlayerManager playerManager) {
//            this( playerManager.getMediaSource(), playerManager.getPlayer().getCurrentWindowIndex(),
//                    playerManager.getPlayer().getCurrentPosition(), playerManager.getPlayer().getPlayWhenReady(),
//                    playerManager.isServiceRunning() );
//        }
//
////        private static void saveState()
////        {
////            PlayerManager playerManager = PlayerManager.getInstance();
////            state = new PlayerState(playerManager);
////        }
//
//
//        public static PlayerState getState() {
//            return state;
//        }
//
//        public static boolean hasState() {
//            return state != null;
//        }
//
//        public PlayerState(MediaSource source, int track, long duration, boolean ready, boolean runningService ) {
//            this.source = source;
//            this.track = track;
//            this.duration = duration;
//            this.ready = ready;
//            this.runningService = runningService;
//        }
//
//        public boolean isRunningService() {
//            return runningService;
//        }
//
//        public boolean isReady() {
//            return ready;
//        }
//
//        public MediaSource getSource() {
//            return source;
//        }
//
//        public int getTrack() {
//            return track;
//        }
//
//        public long getDuration() {
//            return duration;
//        }
//    }
}
