package com.gcodes.iplayer.music.player;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MusicPlayerService extends Service {
    private PlayerNotificationManager notificationManager;
    private String CHANNEL_ID;
    private int NOTIFICATION_ID;

    private PlayerManager manager;

    public final static String ON_START_PLAYER_MANAGER = "com.gcodes.broadcast.player_manager_start";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
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
//        MusicPlayer.destroy();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        manager = new PlayerManager(getBaseContext());
        processRequest( intent );
        if ( notificationManager == null )
            initNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    private void processRequest(Intent intent) {
        if (intent.hasExtra("music"))
            processMusic(intent);
        if (intent.hasExtra("broadcast") && intent.getBooleanExtra("broadcast", false ))
            broadcast();
    }

    private void processMusic(Intent intent) {
        String[] musics = intent.getStringArrayExtra("music");
        ArrayList<Music> music = Music.fromGson(musics);
        PlayerManager.MusicManager musicManager = manager.getMusicManager();
        musicManager.playAll(music);
    }

    private void broadcast() {
        Intent broadcast = new Intent();
        broadcast.setAction("com.gcodes.iplayer.music.player.MusicBroadCastReceiver");
        broadcast.putExtra("playing", "music" );
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
                        Intent intent = new Intent( MusicPlayerService.this, MusicPlayerActivity.class );
                        return PendingIntent.getActivities( MusicPlayerService.this, 0, new Intent[]{intent},
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
                                getArtBitmap( MusicPlayerService.this );
//                        return null;
                    }
                });

        notificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification) {
                startForeground( notificationId, notification);
                Log.d( "Music_Player", "Starting Service" );
            }

            @Override
            public void onNotificationCancelled(int notificationId) {
                stopSelf();
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

        public PlayerManager getPlayerManager()
        {
            return manager;
        }
    }

    public static class MusicBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
}
