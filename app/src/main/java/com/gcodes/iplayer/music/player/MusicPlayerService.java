package com.gcodes.iplayer.music.player;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;

import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import androidx.annotation.Nullable;

public class MusicPlayerService extends Service {
    private PlayerNotificationManager manager;
    private String CHANNEL_ID;
    private int NOTIFICATION_ID;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        manager.setPlayer( null );
//        MusicPlayer.destroy();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d( "Music_Player", "Creating Service" );
        CHANNEL_ID = this.getString( R.string.app_name );
        NOTIFICATION_ID = getResources().getInteger( R.integer.player_id );
        initNotification();
//        MusicPlayer.getInstance().play();
    }

    @SuppressLint("WrongConstant")
    private void initNotification()
    {
        manager = PlayerNotificationManager.createWithNotificationChannel(this, CHANNEL_ID, R.string.app_name,
                NOTIFICATION_ID, new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return MusicPlayer.getInstance().getMusic( player.getCurrentWindowIndex() ).getName();
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
                        return MusicPlayer.getInstance().getMusic( player.getCurrentWindowIndex() ).getArtist();
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return MusicPlayer.getInstance().getMusic( player.getCurrentWindowIndex() ).
                                getArtBitmap( MusicPlayerService.this );
//                        return null;
                    }
                });

        manager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
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

        manager.setPlayer(PlayerManager.getInstance().getPlayer());
    }
}
