//package com.gcodes.iplayer;
//
//import android.content.Context;
//
//import com.gcodes.iplayer.database.PlayerDatabaseManager;
//import com.gcodes.iplayer.music.models.Music;
//import com.gcodes.iplayer.services.ACRService;
//import com.google.gson.JsonObject;
//
//public class PlayerInfoManager
//{
//    private static PlayerInfoManager infoManager;
//    private static PlayerDatabaseManager playerDatabaseManager;
//    private final Context context;
//
//    public static void initialize( Context context )
//    {
//        infoManager = new PlayerInfoManager( context );
//        playerDatabaseManager = PlayerDatabaseManager.getIntance(context);
//    }
//
//    public static PlayerInfoManager getInstance()
//    {
//        return infoManager;
//    }
//
//    public static PlayerInfoManager getInstance( Context context )
//    {
//        if ( infoManager == null )
//            infoManager = new PlayerInfoManager( context );
//        return infoManager;
//    }
//
//    public PlayerInfoManager( Context context )
//    {
//        this.context = context;
//    }
//
//    public void getMusicInfo( Music music )
//    {
//        playerDatabaseManager.ifMusicInfoExist( music.getMediaId() );
//        JsonObject info = ACRService.getInstance().recognizeMusic(music, context);
//    }
//}
