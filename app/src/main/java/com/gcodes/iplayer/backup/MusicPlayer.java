package com.gcodes.iplayer.backup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.player.MusicPlayerService;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

public class MusicPlayer
{
    private static MusicPlayer musicPlayer;
    private SimpleExoPlayer player;
    private DefaultDataSourceFactory factory;
    private Music[] musics;
    private Context context;

    private MusicPlayer(SimpleExoPlayer player, DefaultDataSourceFactory factory, Context context )
    {
        Log.d( "Music_Player", "Creating Player" );
        this.player = player;
        this.factory = factory;
        this.context = context;
    }

    public static void play( Music music, Fragment fragment )
    {
        getInstance().play( music );
//        FragmentMusic.render( fragment );
    }

    private void beginService()
    {
        Intent intent = new Intent( context, MusicPlayerService.class );
        Util.startForegroundService( context, intent );
    }

    public void play( Music music )
    {
        MusicPlayer musicPlayer = getInstance();
        musicPlayer.initSource( music );
        Log.d( "Music_Player", "Id is " + music.getMediaId() );
        musicPlayer.beginService();
//        musicPlayer.play();
    }

    public void play()
    {
        player.setPlayWhenReady(true);
    }

    public static MusicPlayer getInstance()
    {
        return musicPlayer;
    }

    public static MusicPlayer getInstance(SimpleExoPlayer player, DefaultDataSourceFactory factory, Context context)
    {
        if ( musicPlayer == null )
            musicPlayer = new MusicPlayer( player, factory, context );
        return musicPlayer;
    }

    public static void consumeTrack(Consumer< Music > playing)
    {
        SimpleExoPlayer player = musicPlayer.getPlayer();
        int index = player.getCurrentWindowIndex();
        Music music = musicPlayer.getMusic( index );
        Log.d( "Music_Player", "playing new song " + music );
        playing.accept( music );
    }

    public static void destroy()
    {
        musicPlayer.getPlayer().release();
        musicPlayer.setPlayer( null );
        musicPlayer = null;
    }

    public static void registerOnTrackChange(Consumer< Music > playing)
    {
        MusicPlayer musicPlayer = getInstance();
        SimpleExoPlayer player = musicPlayer.getPlayer();
        player.addListener(new Player.EventListener() {
            @Override
            public void onPositionDiscontinuity(int reason) {
                consumeTrack( playing );
            }
        });
    }

    public Music[] getMusics() {
        return musics;
    }

    public int getMusicsCount() {
        return musics.length;
    }

    public Music getMusic(int index ) {
        return musics[ index ];
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
    }

    private void initSource(Music ...musics )
    {
//        String gsonMusic = getArguments().getString("music");
//        Music music = Music.fromGson(gsonMusic);
        ExtractorMediaSource musicSource[] = new ExtractorMediaSource[ musics.length ];
        for ( int i = 0; i < musics.length; ++i )
            musicSource[ i ] = getMusicSource( musics[ i ] );
        ConcatenatingMediaSource source = new ConcatenatingMediaSource(musicSource);
        player.prepare( source, true, true );
        this.musics = musics;
    }

    private ExtractorMediaSource getMusicSource( Music music )
    {
        Uri media = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf( music.getMediaId() ) );
        ExtractorMediaSource source = new ExtractorMediaSource.Factory(factory).createMediaSource(media);
        return source;
    }
}
