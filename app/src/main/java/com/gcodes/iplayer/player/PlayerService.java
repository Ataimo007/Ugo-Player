package com.gcodes.iplayer.player;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.player.MusicPlayerActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class PlayerService extends Service {
    private PlayerNotificationManager notificationManager;
    private String CHANNEL_ID;
    private int NOTIFICATION_ID;

    private PlayerManager manager;

    public final static String ON_START_PLAYER_MANAGER = "com.gcodes.broadcast.player_manager_start";
    public final static String ON_CHECK_PLAYER_MANAGER = "com.gcodes.broadcast.player_manager_check";
    private PlayerBroadCastReceiver checkPlayer;

    private class PlayerBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcast();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        if (manager == null)
//            return null;
        PlayerBinder playerBinder = new PlayerBinder();
        return playerBinder;
    }

//    @Override
//    public void onDestroy()
//    {
//        manager.setPlayer( null );
//        player.release();
//        player = null;
//        super.onDestroy();
//    }

    @Override
    public void onDestroy()
    {
        notificationManager.setPlayer( null );
        unregisterReceiver(checkPlayer);
//        MusicPlayer.destroy();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        manager = new PlayerManager(getBaseContext());
        processRequest( intent );
        if ( notificationManager == null )
            initNotification();
        registerCheckReceiver();
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerCheckReceiver() {
        Log.d("Player_Model", "Registering BroadCast for Player Model" );
        checkPlayer = new PlayerBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_CHECK_PLAYER_MANAGER);
        registerReceiver(checkPlayer, filter);
//        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);
    }

    private void processRequest(Intent intent) {
        if (intent.hasExtra("music"))
            processMusic(intent);
        else if (intent.hasExtra("video"))
            processVideo(intent);
        else if (intent.hasExtra("series"))
            processSeries(intent);
        else if (intent.hasExtra("url"))
            processUrl(intent);

        if (intent.hasExtra("broadcast") && intent.getBooleanExtra("broadcast", false ))
        {
            Log.d("Player_Model", "BroadCast Sent for Player Model" );
            broadcast();
        }
    }

    private void processUrl(Intent intent) {
        String url = intent.getStringExtra("url");
        PlayerManager.VideoManager videoManager = manager.getVideoManager();
        videoManager.initOnlineSources(url);
    }

    private void processVideo(Intent intent) {
        String[] gsonVideos = intent.getStringArrayExtra("video");
        Video[] videos = Video.fromGson(gsonVideos);
        PlayerManager.VideoManager videoManager = manager.getVideoManager();
        if  ( intent.hasExtra("begin") )
        {
            int begin = intent.getIntExtra("begin", -1);
            videoManager.initVideoSources(videos, begin);
        }
        else
            videoManager.initVideoSources(videos);
    }

    private void processSeries(Intent intent) {
        String gsonSeries = intent.getStringExtra("series");
        Series series = Series.fromGson(gsonSeries);
        PlayerManager.VideoManager videoManager = manager.getVideoManager();
        videoManager.initVideoSources(series);
    }

    private void processMusic(Intent intent) {
        String[] musics = intent.getStringArrayExtra("music");
        ArrayList<Music> music = Music.fromGson(musics);
        PlayerManager.MusicManager musicManager = manager.getMusicManager();
        Log.d("Player_Manager", "playing " + music);
        musicManager.playAll(music);
    }

    private void broadcast() {
        Intent broadcast = new Intent();
        broadcast.setAction(ON_START_PLAYER_MANAGER);
        broadcast.putExtra("has_player", true );
        sendBroadcast(broadcast);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d( "Music_Player", "Creating Service" );
        CHANNEL_ID = this.getString( R.string.app_name );
        NOTIFICATION_ID = getResources().getInteger( R.integer.player_id );
//        MusicPlayer.getInstance().play();
    }

    @SuppressLint("WrongConstant")
    private void initNotification()
    {
        notificationManager = PlayerNotificationManager.createWithNotificationChannel(this, CHANNEL_ID, R.string.app_name,
                NOTIFICATION_ID, new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).getName();
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent( PlayerService.this, MusicPlayerActivity.class );
                        return PendingIntent.getActivities( PlayerService.this, 0, new Intent[]{intent},
                                PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).getArtist();
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).
                                getThumbnail( PlayerService.this );
//                        return null;
                    }
                }, new PlayerNotificationManager.NotificationListener() {

                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopSelf();
                    }

                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        if  (manager.isMusicPlaying())
                        {
                            startForeground( notificationId, notification);
                            Log.d( "Music_Player", "Starting Service" );
                        }
                    }
                });

        notificationManager.setPlayer(manager.getPlayer());
    }

    public class PlayerBinder extends Binder
    {
        public PlayerManager.MusicManager getMusicManager()
        {
            return manager.getMusicManager();
        }

        public PlayerManager.VideoManager getVideoManager()
        {
            return manager.getVideoManager();
        }

        public PlayerManager getPlayerManager()
        {
            return manager;
        }
    }
}
