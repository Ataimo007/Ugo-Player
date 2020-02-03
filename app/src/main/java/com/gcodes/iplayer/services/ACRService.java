package com.gcodes.iplayer.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.acrcloud.utils.ACRCloudRecognizer;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.Music;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ACRService
{
    private static ACRService acrService;
    private final Context context;

    private final ACRCloudRecognizer acr;

    public static ACRService getInstance()
    {
        return acrService;
    }

    public static ACRService getInstance(Context context)
    {
        if ( acrService == null )
            acrService = new ACRService( context );
        return acrService;
    }

    public static void initialize(Context context)
    {
        acrService = new ACRService( context );
    }

    private ACRService(Context context)
    {
        Map<String, Object> config = new HashMap<String, Object>();
        // Replace "xxxxxxxx" below with your project's host, access_key and access_secret.
        config.put("access_key", "11a944736130d325297e21591541d556");
        config.put("access_secret", "D4NI0m1kMhwfhtzppMlV9ByJJDJhcIcip4UtnFmv");
        config.put("host", "identify-eu-west-1.acrcloud.com");
        config.put("debug", false);
        config.put("timeout", 5);

        acr = new ACRCloudRecognizer(config);
        this.context = context;
    }

    public PlayerDatabase.MusicInfo recognizeMusic(Music music) {
        JsonObject info = null;
        try {
            String result = recognizeMusicOnline(music, context);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            info = gson.fromJson(result, JsonObject.class);
            Log.d( "ACR_Service", "The info " + info );
            String msg = info.getAsJsonObject( "status" ).getAsJsonPrimitive( "msg" ).getAsString();
            if ( msg.equalsIgnoreCase("success") )
            {
                PlayerDatabase.MusicInfo musicInfo = new PlayerDatabase.MusicInfo(info, music);
                Log.d( "ACR_Service", "The music info " + musicInfo );
                return musicInfo;
            }
            else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String recognizeMusicOnline(Music music, Context context ) throws IOException {
        Uri uri = music.toUri();
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        byte[] bufferMusic = Helper.copyToBuffer( inputStream );
        Log.d( "ACR_Service", "The music buffer length " + bufferMusic.length );
        String result = acr.recognizeByFileBuffer(bufferMusic, bufferMusic.length, 10);
        Log.d( "ACR_Service", "ACR result " + result );
        return result;
    }
}
