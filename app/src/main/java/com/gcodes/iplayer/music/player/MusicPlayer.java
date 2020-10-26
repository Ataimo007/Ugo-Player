//package com.gcodes.iplayer.music.player;
//
//import android.util.Log;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//
//import com.gcodes.iplayer.R;
//import com.gcodes.iplayer.player.PlayerManager;
//
//import com.gcodes.iplayer.music.models.Music;
////import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.source.TrackGroupArray;
//import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
//
//import java.util.ArrayList;
//import java.util.Collections;
//
//import androidx.cardview.widget.CardView;
//import androidx.core.util.Consumer;
//import androidx.fragment.app.FragmentActivity;
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
//
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
//
//    public void removeStateChange(Player.EventListener listener)
//    {
//        playerManager.getPlayer().removeListener(listener);
//    }
//
//
////    public void unRegisterOnTrackChange(com.google.android.exoplayer2.Player.EventListener trackListener)
////    {
////        MusicPlayer musicPlayer = getInstance();
////        SimpleExoPlayer player = playerManager.getPlayer();
////        player.removeListener( trackListener );
////    }
//
//
////    public void setPlayer(SimpleExoPlayer player)
////    {
////        this.player = player;
////    }
//
//}
