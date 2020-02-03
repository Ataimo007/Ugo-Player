package com.gcodes.iplayer.services;

import android.content.Context;
import android.util.Log;

import com.novoda.merlin.Merlin;

public class ConnectivityService
{
    private static Merlin connected;
    private static Merlin disconnected;

//    private static void registerDisconnectivity(Subtitle.Action action, Context context )
//    {
//        connected = new Merlin.Builder().withDisconnectableCallbacks().build( context );
//        connected.registerDisconnectable(() -> {
//            Log.d( "Subtitle_Activities", "network is diconnected performing disconnected action" );
//            action.perform();
//        });
//    }
//
//    private static void registerConnectivity(Subtitle.Action action, Context context )
//    {
//        connected = new Merlin.Builder().withConnectableCallbacks().build( context );
//        connected.registerConnectable(() -> {
//            Log.d( "Subtitle_Activities", "network is connected performing connected action" );
//            action.perform();
//        });
//    }
//
//    private static void doneWithConnectivity()
//    {
//        Log.d( "Subtitle_Activities", "done with connected action" );
//        if ( connected != null )
//        {
//            connected.unbind();
//            connected = null;
//        }
//        if ( disconnected != null )
//        {
//            disconnected.unbind();
//            disconnected = null;
//        }
//    }
}
