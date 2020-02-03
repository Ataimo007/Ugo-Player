package com.gcodes.iplayer.backup;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class YouTubeDataApi
{
    private final int REQUEST_ACCOUNT_PICKER = 1000;
    private final int REQUEST_AUTHORIZATION = 1001;
    private final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    public static final String YOUTUBE_TAG = "YouTube_API";

    private final AppCompatActivity activity;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private GoogleAccountCredential mCredential;

    public YouTubeDataApi(AppCompatActivity activity )
    {
        this.activity = activity;
        initializeCredentials();
    }

    private void initializeCredentials()
    {
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                activity.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    public void getResultsFromApi()
    {
        Log.d( YOUTUBE_TAG, "Getting result from Youtube API" );
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Log.d( YOUTUBE_TAG, "No network connection available." );
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    public void chooseAccount() {
        Log.d( YOUTUBE_TAG, "Choosing Account" );
        if (EasyPermissions.hasPermissions(activity, Manifest.permission.GET_ACCOUNTS))
        {
            String accountName = activity.getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                Log.d( YOUTUBE_TAG, "Using Account " + accountName );
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
//                 Start a dialog from which the user can choose an account
                Log.d( YOUTUBE_TAG, "Choosing a new Account" );
                activity.startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            Log.d( YOUTUBE_TAG, "Requesting account permission" );
            EasyPermissions.requestPermissions( activity, "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline()
    {
        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable( activity );
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(activity );
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    public void handleResult( int requestCode, int resultCode, Intent data )
    {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != activity.RESULT_OK) {
                    Log.d( YOUTUBE_TAG, "This app requires Google Play Services. Please install Google Play " +
                            "Services on your device and relaunch this app." );
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == activity.RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = activity.getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == activity.RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(activity, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the YouTube Data API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private YouTube mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.youtube.YouTube.Builder(transport, jsonFactory,
            mService = new YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("YouTube Data API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call YouTube Data API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         * @return List of Strings containing information about the channel.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> channelInfo = new ArrayList<String>();
            ChannelListResponse result = mService.channels().list("snippet,contentDetails,statistics")
                    .setForUsername("GoogleDevelopers")
                    .execute();
            List<Channel> channels = result.getItems();
            if (channels != null) {
                Channel channel = channels.get(0);
                channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                        "Its title is '" + channel.getSnippet().getTitle() + ", " +
                        "and it has " + channel.getStatistics().getViewCount() + " views.");
            }
            return channelInfo;
        }


        @Override
        protected void onPreExecute() {
            Log.d( YOUTUBE_TAG, "Executing Request" );
        }

        @Override
        protected void onPostExecute(List<String> output) {
            Log.d( YOUTUBE_TAG, "Executed Request" );
            if (output == null || output.size() == 0) {
                Log.d( YOUTUBE_TAG, "No results returned." );
            } else {
                output.add(0, "Data retrieved using the YouTube Data API:");
                Log.d( YOUTUBE_TAG, "Result " + TextUtils.join("\n", output) );
            }
        }

        @Override
        protected void onCancelled() {
            Log.d( YOUTUBE_TAG, "Cancel Request" );
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    Log.d( YOUTUBE_TAG, "User Recoverable Auth IO Exception" );
                    activity.startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Log.d( YOUTUBE_TAG, "The following error occurred: " + mLastError.getMessage() );
                }
            } else {
                Log.d( YOUTUBE_TAG, "Request cancelled." );
            }
        }
    }

    public static class YoutubeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private YouTube mService = null;
        private Exception mLastError = null;

        public YoutubeRequestTask() {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            String apiKey = "AIzaSyDpZNHV4XP-JMyKvPhz-vZbw_qEOCQjodw";
            YouTubeRequestInitializer initializer = new YouTubeRequestInitializer( apiKey, null );
            mService = new YouTube.Builder(transport, jsonFactory, request -> {} )
                    .setApplicationName("Ultimate Player").setYouTubeRequestInitializer( initializer ).build();
        }

        /**
         * Background task to call YouTube Data API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         * @return List of Strings containing information about the channel.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> channelInfo = new ArrayList<String>();
            ChannelListResponse result = mService.channels().list("snippet,contentDetails,statistics")
                    .setForUsername("GoogleDevelopers")
                    .execute();
            List<Channel> channels = result.getItems();
            if (channels != null) {
                Channel channel = channels.get(0);
                channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                        "Its title is '" + channel.getSnippet().getTitle() + ", " +
                        "and it has " + channel.getStatistics().getViewCount() + " views.");
            }
            return channelInfo;
        }


        @Override
        protected void onPreExecute() {
            Log.d( YOUTUBE_TAG, "Executing Request" );
        }

        @Override
        protected void onPostExecute(List<String> output) {
            Log.d( YOUTUBE_TAG, "Executed Request" );
            if (output == null || output.size() == 0) {
                Log.d( YOUTUBE_TAG, "No results returned." );
            } else {
                output.add(0, "Data retrieved using the YouTube Data API:");
                Log.d( YOUTUBE_TAG, "Result " + TextUtils.join("\n", output) );
            }
        }

        @Override
        protected void onCancelled() {
            Log.d( YOUTUBE_TAG, "Cancel Request" );
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    Log.d( YOUTUBE_TAG, "Require Google Play Service" + mLastError.getMessage() );
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    Log.d( YOUTUBE_TAG, "User Recoverable Auth IO Exception" );
                } else {
                    Log.d( YOUTUBE_TAG, "The following error occurred: " + mLastError.getMessage() );
                }
            } else {
                Log.d( YOUTUBE_TAG, "Request cancelled." );
            }
        }
    }
}
