//package com.gcodes.iplayer.backup;
//
//import android.util.Log;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//
//import androidx.cardview.widget.CardView;
//import androidx.core.util.Consumer;
//import androidx.fragment.app.FragmentActivity;
//
//import com.gcodes.iplayer.R;
//import com.gcodes.iplayer.music.models.Music;
//import com.gcodes.iplayer.player.PlayerManager;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.source.TrackGroupArray;
//import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
//
//import java.util.ArrayList;
//import java.util.Collections;
//
////import com.google.android.exoplayer2.Player;
//
//public class MusicPlayer
//{
//    public static class MusicPlayer2
//    {
//        public void play(ArrayList<Music> music )
//        {
//            getInstance().playAll( music );
//        }
//
//        public void play(Music music) {
//            play( new ArrayList< Music >(Collections.singletonList(music)) );
//        }
//
//        public void play(ArrayList<Music> music, FragmentActivity activity )
//        {
//            getInstance().playAll( music );
////        FragmentMusic.render( activity );
//        }
//
//        public void play(Music music, FragmentActivity activity ) {
//            play(new ArrayList<>(Collections.singletonList(music)), activity  );
//        }
//    }
//
//    private final PlayerManager playerManager;
//
//    public MusicPlayer(PlayerManager playerManager) {
//        this.playerManager = playerManager;
//    }
//
//    public static MusicPlayer getInstance() {
//        return null;
//    }
//
//
//
////    private void beginService()
////    {
////        Intent intent = new Intent( context, MusicPlayerService.class );
////        Util.startForegroundService( context, intent );
////    }
//
//    public void play( int position )
//    {
//        playerManager.playTrack( position );
//    }
//
//
////    public static MusicPlayer getInstance()
////    {
////        if ( musicPlayer == null )
////            musicPlayer = new MusicPlayer();
////        return musicPlayer;
////    }
//
//
//    // remove method
//    public void consumeTrack(Consumer< Music > playing)
//    {
//        SimpleExoPlayer player = playerManager.getPlayer();
//        int index = player.getCurrentPeriodIndex();
////        Duration duration = Duration.millis(player.getDuration());
////        int index = player.getCurrentWindowIndex();
//        Music music = playerManager.getMusic( index );
////        Log.d( "Music_Player", "playing new song " + music + "duration " + duration );
////        music.setDuration( duration );
//        playing.accept( music );
//    }
//
//    public static void destroy()
//    {
//        MusicPlayer musicPlayer = getInstance();
////        musicPlayer.getPlayer().release();
////        musicPlayer.setPlayer( null );
//        musicPlayer = null;
//    }
//
//
//    // remove method
//    public Player.EventListener registerOnTrackChange(Consumer< Music > playing)
//    {
//        MusicPlayer musicPlayer = getInstance();
//        SimpleExoPlayer player = playerManager.getPlayer();
//        Player.EventListener trackListenr = new Player.EventListener() {
//            @Override
//            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//                Log.d("Music_Player_Track", "track has changed");
//                consumeTrack(playing);
//            }
//        };
//        player.addListener( trackListenr );
//        return trackListenr;
//    }
//
//    public void removeStateChange(Player.EventListener listener)
//    {
//        playerManager.getPlayer().removeListener(listener);
//    }
//
//    public Player.EventListener onStateChange(CardView view)
//    {
//        MusicPlayer musicPlayer = getInstance();
//        Animation rotate = AnimationUtils.loadAnimation(playerManager.getContext(), R.anim.u_rotate);
//        rotate.setFillAfter( true );
//        Player.EventListener eventListener = new Player.EventListener() {
//
//            @Override
//            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                if (playWhenReady && playbackState == Player.STATE_READY) {
//                    Log.d("Animation_View", "playing music");
//                    view.startAnimation(rotate);
//                } else {
//                    Log.d("Animation_View", "music paused");
//                    view.clearAnimation();
//                }
//            }
//        };
//
//        playerManager.getPlayer().addListener( eventListener );
//
//        if ( playerManager.getPlayer().getPlayWhenReady() && playerManager.getPlayer().getPlaybackState() == Player.STATE_READY )
//        {
//            Log.d("Animation_View", "playing music" );
//            view.startAnimation( rotate );
//        }
//        else
//        {
//            Log.d("Animation_View", "music paused" );
//            view.clearAnimation();
//        }
//
//        return eventListener;
//    }
//
//    public void unRegisterOnTrackChange(Player.EventListener trackListener)
//    {
//        MusicPlayer musicPlayer = getInstance();
//        SimpleExoPlayer player = playerManager.getPlayer();
//        player.removeListener( trackListener );
//    }
//
//    public int getCurrentTrack()
//    {
//        MusicPlayer musicPlayer = getInstance();
//        return playerManager.getPlayer().getCurrentPeriodIndex();
//    }
//
////    public void setPlayer(SimpleExoPlayer player)
////    {
////        this.player = player;
////    }
//
//}
