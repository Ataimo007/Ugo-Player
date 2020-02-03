package com.gcodes.iplayer.player;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.player.MusicPlayerService;
import com.gcodes.iplayer.services.ACRService;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.MediaSource;
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
import com.google.android.exoplayer2.util.Util;
import com.google.api.services.youtube.model.Video;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PlayerManager
{
    private final String userAgent;
    private SimpleExoPlayer player;
    private DefaultDataSourceFactory factory;
    private AppCompatActivity context;

    private static PlayerManager PlayerManager;
    private MediaSource mediaSource;
    private Intent service;

    private Cache downloadCache;
    private File downloadDirectory;
    private DatabaseProvider databaseProvider;
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

    private PlayerManager(AppCompatActivity context )
    {
        userAgent = Util.getUserAgent(context, context.getResources().getString(R.string.app_name));
        factory = new DefaultDataSourceFactory( context, userAgent);
        player = ExoPlayerFactory.newSimpleInstance( context, new DefaultTrackSelector() );
        this.context = context;

        PlayerDatabase.initialize( context );
        ACRService.getInstance( context );
    }

    public String getUserAgent() {
        return userAgent;
    }

    public static PlayerManager getInstance()
    {
        return PlayerManager;
    }

    public static void init( AppCompatActivity context )
    {
        PlayerManager = new PlayerManager( context );
    }

    public Handler getHandler()
    {
        return new Handler( context.getMainLooper() );
    }

    public static void destroy()
    {
        getInstance().getPlayer().release();
        getInstance().setPlayer( null );
    }

    public static PlayerManager getInstance(AppCompatActivity context )
    {
        if ( PlayerManager == null )
            PlayerManager = new PlayerManager( context );
        return PlayerManager;
    }

    public void prepare(MediaSource source)
    {
        player.prepare( source );
        mediaSource = source;
    }

    public void play()
    {
        player.setPlayWhenReady( true );
        if  ( isServiceRunning() )
            stopService();
    }

    public void playAt( int begin )
    {
        player.seekToDefaultPosition( begin );
    }

    public void playOnForeground()
    {
        player.setPlayWhenReady( true );
        beginService();
    }

    public void pause()
    {
        player.setPlayWhenReady( false );
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
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

    public AppCompatActivity getActivity() {
        return context;
    }

    public void prepare(MediaSource source, boolean position, boolean state) {
        player.prepare( source, position, state);
        mediaSource = source;
    }

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

    public boolean isReady()
    {
        return player.getPlaybackState() == com.google.android.exoplayer2.Player.STATE_READY;
    }

    public void playTrackAt( int track, long position )
    {
        player.seekTo( track, position );
    }

    private void beginService()
    {
        service = new Intent( context, MusicPlayerService.class );
        Util.startForegroundService( context, service);
    }

    private void stopService()
    {
        context.stopService( service );
        service = null;
    }

    private boolean isServiceRunning()
    {
        return service != null;
    }

    public int getCurrentTrack() {
        return player.getCurrentWindowIndex();
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

    public void addListener(com.google.android.exoplayer2.Player.EventListener listener ) {
        player.addListener( listener );
    }

    public void saveState() {
        PlayerState.saveState();
    }

    public boolean hasState()
    {
        return PlayerState.hasState();
    }

    public void restoreState()
    {
        PlayerState state = PlayerState.getState();
        prepare( state.getSource(), true, true );
        playTrackAt( state.getTrack(), state.getDuration() );
        player.setPlayWhenReady( state.isReady() );
        if ( state.isRunningService() )
            beginService();
    }

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

    private static class PlayerState
    {
        private static PlayerState state = null;

        private final MediaSource source;
        private final int track;
        private final long duration;
        private final boolean ready;
        private final boolean runningService;

        public PlayerState(PlayerManager playerManager) {
            this( playerManager.getMediaSource(), playerManager.getPlayer().getCurrentWindowIndex(),
                    playerManager.getPlayer().getCurrentPosition(), playerManager.getPlayer().getPlayWhenReady(),
                    playerManager.isServiceRunning() );
        }

        private static void saveState()
        {
            PlayerManager playerManager = PlayerManager.getInstance();
            state = new PlayerState(playerManager);
        }


        public static PlayerState getState() {
            return state;
        }

        public static boolean hasState() {
            return state != null;
        }

        public PlayerState(MediaSource source, int track, long duration, boolean ready, boolean runningService ) {
            this.source = source;
            this.track = track;
            this.duration = duration;
            this.ready = ready;
            this.runningService = runningService;
        }

        public boolean isRunningService() {
            return runningService;
        }

        public boolean isReady() {
            return ready;
        }

        public MediaSource getSource() {
            return source;
        }

        public int getTrack() {
            return track;
        }

        public long getDuration() {
            return duration;
        }
    }
}
