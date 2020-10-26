package com.gcodes.iplayer.services.karaoke;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpegExecution;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.models.Music;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.gcodes.iplayer.music.player.MusicPlayerActivity;
import com.gcodes.iplayer.player.PlayerManager;
import com.google.common.collect.Lists;


import org.joda.time.Period;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.gcodes.iplayer.music.player.MusicPlayerActivity.PLAY_KARAOKE;

public class KaraokeService extends Service
{

    public static final String CANCEL_KARAOKE = "CANCEL_KARAOKE";
    public static final String CREATE_KARAOKE = "CREATE_KARAOKE";
    private PlayerManager.MusicManager manager;
//    public static final String ON_START_KARAOKE = "com.gcodes.iplayer.services.karaoke.ON_START_KARAOKE";
    public static final String STOP_KARAOKE = "com.gcodes.iplayer.services.karaoke.STOP_KARAOKE";

    public static class KaraokeMaker extends BroadcastReceiver
    {
        private static KaraokeMaker maker;
        private Music producing;

        private Context context;
        private KaraokeHandler handler;
        private boolean making = false;
        private PlayerManager.MusicManager manager;
        private KaraokeConnection connection;


        public static KaraokeMaker getInstance()
        {
            if (maker == null)
                maker = new KaraokeMaker();
            return maker;
        }

        public void createKaraoke(Music music, PlayerManager.MusicManager manager)
        {
            Log.d("FFmpag_Service", "The Service attach state " + making);
            if ( !making)
            {
                karaoke(music, manager);
                registerCanceler();
            }
        }

        private void registerCanceler() {
            KaraokeCancelReceiver canceler = new KaraokeCancelReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(STOP_KARAOKE);
            context.registerReceiver(canceler, filter);
        }

        public void cancelCreation()
        {
            Log.d("Player_Karaoke", "Handling Cancel Karaoke in Player Fragment");
            finish();
        }

//        private synchronized void attach(Fragment fragment, KaraokeHandler handler, PlayerManager.MusicManager manager)
//        {
//            if ( !fragment.isDetached() )
//            {
//                this.context = fragment.getContext();
//                this.handler = handler;
//                this.manager = manager;
//                attached = true;
//            }
//        }

        private synchronized void init(PlayerManager.MusicManager manager)
        {
            this.manager = manager;
            making = true;
        }

        private synchronized void release()
        {
            this.manager = null;
            making = false;
        }

        public synchronized void attachView(Fragment fragment, KaraokeHandler handler)
        {
            this.context = fragment.getContext();
            this.handler = handler;
            if ( isMaking() )
                bindToService();
//            if (connection != null)
//                connection.registerHandler(this.handler);
        }

//        public synchronized void dettach()
//        {
//            context = null;
//            handler = null;
//            manager = null;
//            attached = false;
//        }

        public synchronized void detachView()
        {
            handler = null;
            if ( making )
                unbindToService();
//            if (connection != null)
//                connection.unregisterHandler();
        }

        private synchronized boolean isMaking()
        {
            return making;
        }

        public void karaoke(Music music, PlayerManager.MusicManager manager)
        {
            Log.d("FFmpag_Service", String.format("Creating karaoke for %s", music));
//            Context context = fragment.getContext();
            String karaokePath = getKaraokePath(music, context);
            execute(music, karaokePath, context);
            Log.d("FFmpag_Service", String.format("Attaching session for %s path %s", music, karaokePath));
            init(manager);
//            attach(fragment, handler, manager);
//            return karaokePath;
        }

        private void execute(Music music, String output, Context context)
        {
            Intent karaokeService = new Intent(context, KaraokeService.class);
            karaokeService.putExtra("music", music.toGson());
            karaokeService.putExtra("output", output);
            karaokeService.setAction(CREATE_KARAOKE);
            Log.d("FFmpag_Service", String.format("Executing session for %s path %s", music, output));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService( karaokeService );
            }
            else
                context.startService(karaokeService);
        }

        public File getKaraoke(Music music, Context context) {
            String karaokePath = getKaraokePath(music, context);
            return new File(karaokePath);
        }

        public String getKaraokePath(Music music, Context context)
        {
            String name = String.valueOf(music.getMediaId());
            File parent = new File( context.getExternalFilesDir( null ), "karaoke" );
            File file = new File( context.getExternalFilesDir( null ), "karaoke/" + name + ".mp3" );
            if ( !parent.exists() )
                parent.mkdirs();
            Log.w( "FFmpag_Service", "karaoke file path " + file );
            return file.getPath();
        }

        public Music producing() {
            return producing;
        }

        public Context getContext() {
            return context;
        }

        public KaraokeHandler getHandler() {
            return handler;
        }

        public synchronized void bindToService() {
            if (making)
            {
                Intent intent = new Intent(getContext(), KaraokeService.class);
                connection = new KaraokeConnection(handler, manager);
                context.bindService(intent, connection, BIND_IMPORTANT);
            }
        }

        public void unbindToService() {
            if (making)
            {
                if (connection != null)
                    context.unbindService(connection);
            }
        }

        public synchronized void finish() {
            if (making)
            {
                if (connection != null)
                    context.unbindService(connection);

                release();
                Intent intent = new Intent(getContext(), KaraokeService.class);
                context.stopService(intent);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }

        private class KaraokeCancelReceiver extends BroadcastReceiver
        {
            @Override
            public void onReceive(Context context, Intent intent) {
                cancelCreation();
            }
        }
    }

    private static class KaraokeConnection implements ServiceConnection
    {
        private final PlayerManager.MusicManager manager;
        private KaraokeService.KaraokeBinder binder;
        private KaraokeHandler karaokePlayerHandler;

        private KaraokeConnection(KaraokeHandler karaokePlayerHandler, PlayerManager.MusicManager manager) {
            this.karaokePlayerHandler = karaokePlayerHandler;
            this.manager = manager;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof KaraokeService.KaraokeBinder)
            {
                binder = (KaraokeService.KaraokeBinder) service;
                binder.prepare(karaokePlayerHandler, manager);
            }

        }

//        public void registerHandler(KaraokeHandler handler)
//        {
//            karaokePlayerHandler = handler;
//            binder.register(handler);
//        }
//
//        public void unregisterHandler()
//        {
//            karaokePlayerHandler = null;
//            binder.release();
//        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder.release();
        }
    }

    private class KaraokeNotification implements KaraokeHandler
    {
        private File karaokeFile;
        private Music music;

        @Override
        public void success(File file, Music music) {
            karaokeFile = file;
            this.music = music;
            processOutput(file, music);
        }

        @Override
        public void failure(Music music) {

        }

        @Override
        public void update(int percentage) {
            updateNotificationProgress(percentage);
        }

        @Override
        public void cancel() {
            removeNotification();
        }
    }

//    private class KaraokeCheckReceiver extends BroadcastReceiver
//    {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Intent message = intent.getParcelableExtra("message");
//            sendBroadcast(message);
//        }
//    }

    private final String karaokeCmd = "-i \"%s\" -af pan=stereo|c0=c0|c1=-1*c1 -ac 1 \"%s\"";
    private final String[] karaokeCmds = { "-i", "", "-af", "pan=stereo|c0=c0|c1=-1*c1", "-ac", "1", "" };
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;

    private FFmpeg ffmpeg;

    private State state = State.LOADING;

    private Music music;
    private String output;

    private int NOTIFICATION_ID;
    private final int MAX = 100;
    private final int MIN = 0;

    public class KaraokeBinder extends Binder {
        private KaraokeHandler playerHandler;

        public void prepare(KaraokeHandler handler, PlayerManager.MusicManager manager)
        {
            if (handler != null)
            {
                playerHandler = handler;
                addKaraokeHandler(handler);
            }
            setManager(manager);
        }

        public synchronized void release()
        {
            removeKaraokeHandler(playerHandler);
            playerHandler = null;
        }

//        public void register(KaraokeHandler handler) {
//            playerHandler = handler;
//            addKaraokeHandler(handler);
//        }
    }

    private void setManager(PlayerManager.MusicManager manager) {
        this.manager = manager;
    }

    private PlayerManager.MusicManager getManager() {
        return manager;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new KaraokeBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        processAction(intent);
        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        releaseCreator();
        super.onDestroy();
    }

//    private void processAction(Intent intent) {
//        Log.d("Player_Karaoke", "Handling intent for " + intent.getAction());
//        switch (intent.getAction())
//        {
//            case CREATE_KARAOKE:
//                createKaraoke(intent);
//                registerCheckReceiver();
//                break;
//
//            case CANCEL_KARAOKE:
//                cancelKaraoke();
//        }
//    }

    private void processAction(Intent intent) {
        Log.d("Player_Karaoke", "Handling intent for " + intent.getAction());
        createKaraoke(intent);
//        registerCheckReceiver();
    }

//    private void registerCheckReceiver() {
//        KaraokeCheckReceiver receiver = new KaraokeCheckReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ON_START_KARAOKE);
//        registerReceiver(receiver, filter);
//    }

    private void cancelKaraoke() {
        Log.d("Player_Karaoke", "Handling Cancel Karaoke in Player Fragment");
        handleCancel();
        stopForeground();
    }

    private void createKaraoke(Intent intent)
    {
        initialize(intent);
        prepareNotification();
        initNotification();
        initNotificationHandler();
        beginForeground();
        execute( music, output );
        requestBinder();
    }

    public void initNotificationHandler()
    {
        KaraokeNotification notification = new KaraokeNotification();
        addKaraokeHandler(notification);
    }

    public void updateNotificationProgress(int progress)
    {
        builder.setProgress(MAX, progress, false).setContentText(progress + "%");
        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void beginForeground()
    {
        builder.setProgress(MAX, MIN, false);
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
        builder.setChannelId(getString(R.string.karaoke_id)).setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private void requestBinder() {
        KaraokeMaker maker = KaraokeMaker.getInstance();
        maker.bindToService();
    }

    private void initialize(Intent intent)
    {
        music = Music.fromGson(intent.getStringExtra("music"));
        output = intent.getStringExtra("output");
    }

    private enum State{LOADING,READY,UNUSABLE}

    private ArrayList<KaraokeHandler> karaokeHandlers;

    public synchronized void addKaraokeHandler(KaraokeHandler karaokeHandler) {
        if (karaokeHandlers == null)
            karaokeHandlers = new ArrayList<>();
        karaokeHandlers.add(karaokeHandler);
    }

    public synchronized void removeKaraokeHandler(KaraokeHandler karaokeHandler) {
        karaokeHandlers.remove(karaokeHandler);
    }

    public synchronized void handleSuccess(File file, Music music) {
        stopForeground();
        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
        {
            for (KaraokeHandler handler : karaokeHandlers )
                handler.success(file, music);
        }
        karaokeHandlers.clear();
//        stopSelf();
    }

    public void stopForeground()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_DETACH);
        else
            stopForeground( false );
    }

    public synchronized void handleFailure(Music music) {
        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
        {
            for (KaraokeHandler handler : karaokeHandlers )
                handler.failure(music);
        }
    }

    public synchronized void updateHandlerProgress(int progress) {
        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
        {
            for (KaraokeHandler handler : karaokeHandlers )
                handler.update(progress);
        }
    }

    public synchronized void handleCancel() {
        int i = 1;
        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
        {
            for (KaraokeHandler handler : karaokeHandlers )
            {
                Log.d("Player_Karaoke", "Handling Cancel Karaoke handler " + i++ );
                handler.cancel();
            }
            karaokeHandlers.clear();
        }
    }

    @Override
    public void onCreate() {
        createChannel();
//        prepareNotification();
//        loadLibrary();
    }

    private void createChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence headsupName = getString(R.string.headsup_karaoke_channel);
            String headsupId = getString(R.string.headsup_karaoke_id);
            String headsupDescription = getString(R.string.headsup_karaoke_description);
            int headsupImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel headsupChannel = new NotificationChannel(headsupId, headsupName, headsupImportance);
            headsupChannel.setDescription(headsupDescription);

            CharSequence name = getString(R.string.karaoke_channel);
            String id = getString(R.string.karaoke_id);
            String description = getString(R.string.karaoke_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            headsupChannel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannels(Lists.newArrayList(headsupChannel, channel));

            String groupId = getString(R.string.headsup_karaoke_id);
            String groupName = getString(R.string.karaoke_group);
            NotificationChannelGroup karaokeGroup = new NotificationChannelGroup(groupId, groupName);
            notificationManager.createNotificationChannelGroup(karaokeGroup);

            headsupChannel.setGroup(groupId);
            channel.setGroup(groupId);
        }
    }

    private void prepareNotification()
    {
        NOTIFICATION_ID = getResources().getInteger( R.integer.karaoke_id );
        builder = new NotificationCompat.Builder(this, getString(R.string.headsup_karaoke_id) );
        notificationManager = NotificationManagerCompat.from(this);

        Intent cancel = new Intent();
        cancel.setAction(STOP_KARAOKE);
        PendingIntent cancelKaraoke = PendingIntent.getBroadcast(this, music.uniqueCode(STOP_KARAOKE), cancel, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setChannelId(getString(R.string.headsup_karaoke_id))
                .addAction( 0, "Cancel", cancelKaraoke);
    }

//    private void prepareNotification()
//    {
//        NOTIFICATION_ID = getResources().getInteger( R.integer.karaoke_id );
//        builder = new NotificationCompat.Builder(this, getString(R.string.headsup_karaoke_id) );
//        notificationManager = NotificationManagerCompat.from(this);
//
//        Intent cancel = new Intent(this, KaraokeService.class);
//        cancel.setAction(CANCEL_KARAOKE);
//        PendingIntent cancelKaraoke = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
//            cancelKaraoke = PendingIntent.getForegroundService(this, 0, cancel, 0);
//        else
//            cancelKaraoke = PendingIntent.getService(this, 0, cancel, 0);
//
//        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setChannelId(getString(R.string.headsup_karaoke_id))
//                .addAction( 0, "Cancel", cancelKaraoke);
//    }

    private void initNotification()
    {
        builder.setContentTitle("Karaoke : " + music.getName())
                .setContentText("0%");
    }

    private void processOutput(File file, Music music) {
        PlayerManager.MusicManager manager = getManager();
        if (!manager.isMusicPlaying(music))
            finishNotification();
        else
            removeNotification();
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void finishNotification()
    {
//        Intent karaoke = new Intent(this, MusicPlayerActivity.class);
        Intent karaoke = new Intent(this, MusicPlayerActivity.class);
        Log.d("Handling_Intent", "Saving Karaoke for " + music);
        Log.d("Handling_Intent", "Saving Karaoke for " + music.toGson());
        Log.d("Handling_Intent", "Saving Karaoke for " + output);
        Log.d("Handling_Intent", "Saving Karaoke for " + music.hashCode());

        karaoke.putExtra("music", music.toGson());
        karaoke.putExtra("output", output);
        karaoke.setAction(PLAY_KARAOKE);
//        karaoke.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        karaoke.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent playKaraoke = PendingIntent.getActivity(this, music.hashCode(), karaoke, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder finishBuilder = new NotificationCompat.Builder(this, getString(R.string.headsup_karaoke_id));
        finishBuilder.setContentTitle("Finish Creating Karaoke")
                .setContentText(this.music.getName())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(playKaraoke)
                .setAutoCancel(true);

        Notification notification = finishBuilder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void releaseCreator()
    {
        List<FFmpegExecution> executions = FFmpeg.listExecutions();
        if (!executions.isEmpty())
        {
            cancelKaraoke();
            FFmpeg.cancel();
            File file = new File(output);
            if ( file.exists() )
                file.delete();
        }
    }

    private void execute(Music music, String output)
    {
        String karaokePath = output;
        String musicPath = music.getData();
        String[] command = getCommand(musicPath, karaokePath);
        KaraokeExecutor karaokeExecutor = new KaraokeExecutor(music, output);
        FFmpeg.executeAsync(command, karaokeExecutor);
        Config.enableStatisticsCallback(karaokeExecutor);
    }

    public String[] getCommand( String input, String output )
    {
        karaokeCmds[ 1 ] = input;
        karaokeCmds[ 6 ] = output;
        return karaokeCmds;
    }

    public static interface KaraokeHandler
    {
        void success(File file, Music music);
        void failure(Music music);
        void update( int percentage );
        void cancel();
    }

    private class KaraokeExecutor implements ExecuteCallback, StatisticsCallback
    {
        private final Music music;
        private final String output;

        private KaraokeExecutor(Music music, String output) {
            super();
            this.music = music;
            this.output = output;
        }

        @Override
        public void apply(long executionId, int returnCode) {
            if (returnCode == RETURN_CODE_SUCCESS) {
                Log.d("FFmpag_Service", "Async command execution completed successfully.");
                handleSuccess(new File(output), music);
                KaraokeMaker maker = KaraokeMaker.getInstance();
                maker.finish();
            } else if (returnCode == RETURN_CODE_CANCEL) {
                Log.d("FFmpag_Service", "Async command execution cancelled by user.");
            } else {
                Log.d("FFmpag_Service", String.format("Async command execution failed with rc=%d.", returnCode));
                handleFailure(music);
            }
        }

        @Override
        public void apply(Statistics statistics) {
            Period duration = music.getDuration();
            double progress = (double) statistics.getTime() / duration.toStandardDuration().getMillis() * 100;
            Log.w( "FFmpag_Service", "String Processing progress percentage " + progress);
            updateHandlerProgress((int) progress);
        }
    };
}
