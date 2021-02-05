package com.gcodes.iplayer.player;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gcodes.iplayer.music.player.MusicPlayerActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.video.model.Video;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import androidx.annotation.Nullable;

public class PlayerService extends Service {
    private static final String SERVICE_KEY = "player_service";
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
        Log.d("Player_Model", "Destroying Player Service" );
//        notificationManager.setPlayer( null );
        removeNotification();
        unregisterReceiver(checkPlayer);
//        clearServiceDetails();
//        MusicPlayer.destroy();
        super.onDestroy();
    }

    private void removeNotification()
    {
        if ( notificationManager != null )
        {
//            stopForeground(true);
            notificationManager.setPlayer( null );
            notificationManager = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (manager == null)
            manager = new PlayerManager(getBaseContext());
//        initPreferences();
        processRequest( intent );
//        if ( manager.isMusicPlaying() && notificationManager == null )
//            initNotification();
        if (!isCheckRegistered())
            registerChecks();

        return super.onStartCommand(intent, flags, startId);
    }

    private void initPreferences() {
        manager.addListener(new Player.EventListener() {
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                if ( manager.isVideoPlaying() )
                {
                    saveState();
                }
            }

            private void saveState() {
                SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(getString(R.string.preference_recent_play), manager.getVideoManager().getCurrentVideo().toGson());
                edit.apply();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if ( manager.isVideoPlaying() && playbackState == ExoPlayer.STATE_READY && !playWhenReady )
                {
                    saveState();
                }
            }
        });
    }

    private void registerChecks() {
        registerCheckReceiver();
//        saveServiceDetails();
    }

//    private void saveServiceDetails() {
//        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_prefs), MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean(SERVICE_KEY, true);
//        editor.apply();
//    }
//
//    private void clearServiceDetails() {
//        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_prefs), MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean(SERVICE_KEY, false);
//        editor.apply();
//    }
//
//    public static boolean isServiceRunning(Context context) {
//        Log.d("music_info", "checking is service is playing from preference");
//        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_prefs), MODE_PRIVATE);
//        Log.d("music_info", "got preference");
////        SharedPreferences.Editor editor = sharedPreferences.edit();
////        editor.putBoolean(SERVICE_KEY, false);
////        editor.apply();
//        if ( !sharedPreferences.contains(SERVICE_KEY) )
//            return false;
//        return sharedPreferences.getBoolean(SERVICE_KEY, false);
//    }

    private void registerCheckReceiver() {
        Log.d("Player_Model", "Registering BroadCast for Player Model" );
        checkPlayer = new PlayerBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_CHECK_PLAYER_MANAGER);
        registerReceiver(checkPlayer, filter);
//        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);
    }

    private boolean isCheckRegistered() {
        return checkPlayer != null;
    }

    private void processRequest(Intent intent) {
        switch (intent.getStringExtra("type")) {
            case "music":
            case "music_uri":
                if (notificationManager == null)
                    initNotification();
                break;

                case "url":
                removeNotification();
                processUrl(intent);
                break;

            default:
                removeNotification();
        }

        if (intent.hasExtra("broadcast") && intent.getBooleanExtra("broadcast", false ))
        {
            Log.d("Player_Model", "BroadCast Sent for Player Model" );
            broadcast();
        }
    }

//    private void processRequest(Intent intent) {
//        if (intent.getStringExtra("type").equals("music"))
//            if ( notificationManager == null )
//                initNotification();
////            processMusic(intent);
////        else if (intent.hasExtra("video"))
////            processVideo(intent);
////        else if (intent.hasExtra("series"))
////            processSeries(intent);
//        else if (intent.hasExtra("url"))
//            processUrl(intent);
//
//        if (intent.hasExtra("broadcast") && intent.getBooleanExtra("broadcast", false ))
//        {
//            Log.d("Player_Model", "BroadCast Sent for Player Model" );
//            broadcast();
//        }
//    }

    private void processUrl(Intent intent) {
        String url = intent.getStringExtra("url");
        PlayerManager.VideoManager videoManager = manager.getVideoManager();
        videoManager.initOnlineSources(url);
    }

    private void processMusicUri(Intent intent) {
        Uri data = intent.getData();
        PlayerManager.MusicManager musicManager = manager.getMusicManager();
        musicManager.play(data);
    }

//    private void processVideo(Intent intent) {
//        String[] gsonVideos = intent.getStringArrayExtra("video");
//        Video[] videos = Video.fromGson(gsonVideos);
//        PlayerManager.VideoManager videoManager = manager.getVideoManager();
//        if  ( intent.hasExtra("begin") )
//        {
//            int begin = intent.getIntExtra("begin", -1);
//            videoManager.initVideoSources(videos, begin);
//        }
//        else
//            videoManager.initVideoSources(videos);
//    }

//    private void processSeries(Intent intent) {
//        String gsonSeries = intent.getStringExtra("series");
//        Series series = Series.fromGson(gsonSeries);
//        PlayerManager.VideoManager videoManager = manager.getVideoManager();
//        videoManager.initVideoSources(series);
//    }

//    private void processMusic(Intent intent) {
//        String[] musics = intent.getStringArrayExtra("music");
//        ArrayList<Music> music = Music.fromGson(musics);
//        PlayerManager.MusicManager musicManager = manager.getMusicManager();
//        Log.d("Player_Manager", "playing " + music);
//        musicManager.playAll(music);
//    }

    private void broadcast() {
        Intent broadcast = new Intent();
        broadcast.setAction(ON_START_PLAYER_MANAGER);
        broadcast.putExtra("has_player", true );
        sendBroadcast(broadcast);
    }

//    private void broadcast(boolean external) {
//        Intent broadcast = new Intent();
//        broadcast.setAction(ON_START_PLAYER_MANAGER);
//        broadcast.putExtra("has_player", true );
//        broadcast.putExtra("from_store", external );
//        sendBroadcast(broadcast);
//    }

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
                        if (manager.isMusicPlaying())
                            return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).getName();
                        return "";
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        if (manager.isMusicPlaying())
                        {
                            Intent intent = new Intent( PlayerService.this, MusicPlayerActivity.class );
                            return PendingIntent.getActivities( PlayerService.this, 0, new Intent[]{intent},
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                        }
                        return null;
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        if (manager.isMusicPlaying())
                            return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).getArtist();
                        return "";
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        if (manager.isMusicPlaying())
                            return manager.getMusicManager().getMusic( player.getCurrentWindowIndex() ).
                                getThumbnail( PlayerService.this );
                        return null;
                    }
                }, new PlayerNotificationManager.NotificationListener() {

                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopForeground(true);
                    }

                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
//                        startForeground( notificationId, notification);
                        if  (manager.isMusicPlaying())
                        {
                            startForeground( notificationId, notification);
                            Log.d( "Music_Player", "Starting Service" );
                        }
                    }
                });

        notificationManager.setUseStopAction(true);
        notificationManager.setRewindIncrementMs(0);
        notificationManager.setFastForwardIncrementMs(0);
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

        public void stopService()
        {
            stopSelf();
        }
    }
}
