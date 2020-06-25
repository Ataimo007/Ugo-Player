package com.gcodes.iplayer.music.player;

import android.net.Uri;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.player.PlayerManager;

import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.ExoPlaybackException;
//import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

public class MusicPlayer
{
    private final PlayerManager playerManager = PlayerManager.getInstance();
    private static MusicPlayer musicPlayer;
    private ArrayList<Music> musics;
    private ConcatenatingMediaSource source;
    private long currentPosition;
    private int currentIndex;
//    private

//    private SimpleExoPlayer player;
//    private DefaultDataSourceFactory factory;
//    private Context context;

//    private MusicPlayer( SimpleExoPlayer player, DefaultDataSourceFactory factory, Context context )
//    {
//        Log.d( "Music_Player", "Creating Player" );
//        this.player = player;
//        this.factory = factory;
//        this.context = context;
//    }


    public static void play(ArrayList<Music> music )
    {
        getInstance().playAll( music );
//        FragmentMusic.render();
    }

    public void saveCurrentState()
    {
        currentPosition = playerManager.getCurrentPosition();
        currentIndex = playerManager.getCurrentIndex();
    }

    public void restoreCurrentState()
    {
        playerManager.prepare( source, PlayerManager.MediaType.MUSIC );
        playerManager.seekTo( currentIndex, currentPosition );
        play();
    }

    public static void play(Music music) {
        play( new ArrayList< Music >(Collections.singletonList(music)) );
    }

    public static void play(ArrayList<Music> music, FragmentActivity activity )
    {
        getInstance().playAll( music );
//        FragmentMusic.render( activity );
    }

    public static void play(Music music, FragmentActivity activity ) {
        play(new ArrayList<>(Collections.singletonList(music)), activity  );
    }

//    private void beginService()
//    {
//        Intent intent = new Intent( context, MusicPlayerService.class );
//        Util.startForegroundService( context, intent );
//    }

    public void playAll( ArrayList<Music> musics )
    {
        MusicPlayer musicPlayer = getInstance();
        musicPlayer.initSource( musics );
        Log.d( "Music_Player", "Id is " + musics);
//        musicPlayer.beginService();
        musicPlayer.play();
    }

    public static void play( int position )
    {
        MusicPlayer musicPlayer = getInstance();
        PlayerManager playerManager = musicPlayer.getMainPlayerManager();
        playerManager.playTrack( position );
    }

    public void play()
    {
        playerManager.playMusic();
    }

    public static MusicPlayer getInstance()
    {
        if ( musicPlayer == null )
            musicPlayer = new MusicPlayer();
        return musicPlayer;
    }

    public static void consumeTrack(Consumer< Music > playing)
    {
        SimpleExoPlayer player = musicPlayer.getPlayerManager();
        int index = player.getCurrentPeriodIndex();
//        Duration duration = Duration.millis(player.getDuration());
//        int index = player.getCurrentWindowIndex();
        Music music = musicPlayer.getMusic( index );
//        Log.d( "Music_Player", "playing new song " + music + "duration " + duration );
//        music.setDuration( duration );
        playing.accept( music );
    }

    public static void destroy()
    {
//        musicPlayer.getPlayer().release();
//        musicPlayer.setPlayer( null );
        musicPlayer = null;
    }

    public static com.google.android.exoplayer2.Player.EventListener registerOnTrackChange(Consumer< Music > playing)
    {
        MusicPlayer musicPlayer = getInstance();
        SimpleExoPlayer player = musicPlayer.getPlayerManager();
        com.google.android.exoplayer2.Player.EventListener trackListenr = new com.google.android.exoplayer2.Player.EventListener() {
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                Log.d("Music_Player_Track", "track has changed");
                consumeTrack(playing);
            }
        };
        player.addListener( trackListenr );
        return trackListenr;
    }

    public static void removeStateChange(Player.EventListener listener)
    {
        musicPlayer.getPlayerManager().removeListener( listener );
    }

    public static Player.EventListener onStateChange(CardView view )
    {
        MusicPlayer musicPlayer = getInstance();
        Animation rotate = AnimationUtils.loadAnimation(musicPlayer.getMainPlayerManager().getContext(), R.anim.u_rotate);
        rotate.setFillAfter( true );
        Player.EventListener eventListener = new Player.EventListener() {

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    Log.d("Animation_View", "playing music");
                    view.startAnimation(rotate);
                } else {
                    Log.d("Animation_View", "music paused");
                    view.clearAnimation();
                }
            }
        };

        musicPlayer.getPlayerManager().addListener( eventListener );

        if ( musicPlayer.getPlayerManager().getPlayWhenReady() && musicPlayer.getPlayerManager().getPlaybackState() == Player.STATE_READY )
        {
            Log.d("Animation_View", "playing music" );
            view.startAnimation( rotate );
        }
        else
        {
            Log.d("Animation_View", "music paused" );
            view.clearAnimation();
        }

        return eventListener;
    }

    public static void unRegisterOnTrackChange(com.google.android.exoplayer2.Player.EventListener trackListener)
    {
        SimpleExoPlayer player = musicPlayer.getPlayerManager();
        player.removeListener( trackListener );
    }

    public static int getCurrentTrack()
    {
        return musicPlayer.getPlayerManager().getCurrentPeriodIndex();
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

    public boolean isMusicPlaying() {
        return musics != null;
    }

    public int getPosition(Music music) {
        return musics.indexOf( music );
    }

    public SimpleExoPlayer getPlayerManager()
    {
        return playerManager.getPlayer();
    }

    public PlayerManager getMainPlayerManager()
    {
        return playerManager;
    }

//    public void setPlayer(SimpleExoPlayer player)
//    {
//        this.player = player;
//    }

    private void initSource(ArrayList<Music> musics )
    {
        Music music;
        ArrayList< ProgressiveMediaSource > musicSource = new ArrayList<>();
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
        playerManager.prepare( source, true, true, PlayerManager.MediaType.MUSIC );
        playerManager.shuffle();
        playerManager.repeatAll();
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

    public void switchSources(ConcatenatingMediaSource source)
    {
//        int currentWindowIndex = playerManager.getCurrentWindow();
//        long currentPosition = playerManager.getCurrentPosition();
        playerManager.prepare( source, false, false, PlayerManager.MediaType.VIDEO );
//        playerManager.playTrackAt( currentWindowIndex, currentPosition );
        this.source = source;
    }

//    public void playVideo( String url, Music musicVideo )
//    {
//        ProgressiveMediaSource mediaSource = getMediaSource(url);
//        musics.add( 0, musicVideo );
////        source.add
//        source.addMediaSource( 0, mediaSource, playerManager.getHandler(), () -> {
//            playerManager.playTrack( 0 );
//            MusicPlayer.registerOnTrackChange( music -> {
//                musics.remove( 0 );
//                source.removeMediaSource( 0 );
//            });
//        } );
//    }

    private void initError() {
        SimpleExoPlayer player = this.playerManager.getPlayer();
        player.addListener(new com.google.android.exoplayer2.Player.EventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                int trackNo = player.getCurrentPeriodIndex();
                Music music = musics.get(trackNo);
//                source.removeMediaSource( trackNo );
                String message = String.format("The track %s does not exist or %s", music.getName(), error.getMessage());
                Toast.makeText( MusicPlayer.this.playerManager.getContext(), message, Toast.LENGTH_SHORT).show();
                playerManager.prepare( source, PlayerManager.MediaType.MUSIC );
                player.setPlayWhenReady( true );
            }
        });
    }

    public ProgressiveMediaSource getMusicSource( Music music )
    {
        Uri media = music.toUri();
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(playerManager.getFactory()).createMediaSource(media);
//        ExtractorMediaSource source = new ExtractorMediaSource.Factory( playerManager.getFactory() ).createMediaSource(media);
        return source;
    }

    public ProgressiveMediaSource getMusicSource(File file)
    {
        Uri media = Uri.fromFile(file);
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(playerManager.getFactory()).createMediaSource(media);
        return source;
    }

    public ConcatenatingMediaSource getMediaSource()
    {
        return source;
    }

    public ProgressiveMediaSource getMediaSource( String url )
    {
        Uri media = Uri.parse( url );
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(playerManager.getFactory()).createMediaSource(media);
//        ExtractorMediaSource source = new ExtractorMediaSource.Factory( playerManager.getFactory() ).createMediaSource(media);
        return source;
    }
}
