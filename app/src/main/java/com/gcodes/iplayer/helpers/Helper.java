package com.gcodes.iplayer.helpers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterInputStream;

/**
 * Created by Edem's Family on 2/23/2018.
 */

public class Helper
{
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024; // 1 mega byte

    public static class Worker extends AsyncTask< BackgroundAction, Void, UIAction >
    {
        @Override
        protected UIAction doInBackground(BackgroundAction... backgroundActions) {
            return backgroundActions[ 0 ].perform();
        }

        @Override
        protected void onPostExecute(UIAction done) {
            done.perform();
            super.onPostExecute(done);
        }

        public static void executeTask(BackgroundAction backAction )
        {
            new Worker().execute( backAction );
        }

        public static void executeAllTask(BackgroundAction... backgroundActions )
        {
            new Worker().execute( backgroundActions );
        }
    }

    public static void toast(Context context, String message )
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public static interface UIAction
    {
        public void perform();
    }

    public static interface BackgroundAction
    {
        public UIAction perform();
    }

    public static boolean isDeviceOnline(Activity activity)
    {
        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isGooglePlayServicesAvailable( Activity activity )
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable( activity );
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    public static void acquireGooglePlayServices( Activity activity ) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(activity );
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode, activity);
        }
    }

    public static void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode, Activity activity ) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(activity, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public static byte[] copyAllToBuffer(InputStream inputStream) {
        byte[] raw = null;
        try {
            raw = new byte[inputStream.available()];
            inputStream.read(raw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return raw;
    }

    public static byte[] copyToBuffer(InputStream inputStream)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int read = -1;
        int streamSize = 0;
        try
        {
            streamSize = inputStream.available();
            int bufferSize = streamSize < DEFAULT_BUFFER_SIZE ? streamSize : DEFAULT_BUFFER_SIZE;
            byte[] stream = new byte[ bufferSize ];
            read = inputStream.read( stream, 0, bufferSize );
            while ( read > 0 )
            {
                outputStream.write( stream, 0, read );
                streamSize = inputStream.available();
                bufferSize = streamSize < DEFAULT_BUFFER_SIZE ? streamSize : DEFAULT_BUFFER_SIZE;
                read = inputStream.read( stream, 0, bufferSize );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputStream.toByteArray();
    }
}
