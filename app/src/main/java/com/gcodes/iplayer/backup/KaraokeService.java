//package com.gcodes.iplayer.backup;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Binder;
//import android.os.Build;
//import android.os.IBinder;
//import android.util.Log;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//import androidx.fragment.app.Fragment;
//
//import com.gcodes.iplayer.R;
//import com.gcodes.iplayer.music.models.Music;
//import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
//import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
//import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
//import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
//
//import org.joda.time.Period;
//import org.joda.time.format.PeriodFormatter;
//import org.joda.time.format.PeriodFormatterBuilder;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class KaraokeService extends Service
//{
//
//    public static class KaraokeMaker
//    {
//        private static KaraokeMaker maker;
//        private Music producing;
//
//        private Context context;
//        private KaraokeHandler handler;
//        private boolean attached = false;
//
//
//        public static KaraokeMaker getInstance()
//        {
//            if (maker == null)
//                maker = new KaraokeMaker();
//            return maker;
//        }
//
//        public void createKaraoke(Music music, KaraokeHandler handler, Fragment fragment)
//        {
//            Log.d("FFmpag_Service", "The Service attach state " + attached);
//            if ( !attached)
//                karaoke(music, handler, fragment);
//        }
//
//        private synchronized void attach(Fragment fragment, KaraokeHandler handler)
//        {
//            if ( !fragment.isDetached() )
//            {
//                this.context = fragment.getContext();
//                this.handler = handler;
//                attached = true;
//            }
//        }
//
//        public synchronized void dettach()
//        {
//            context = null;
//            handler = null;
//            attached = false;
//        }
//
//        private synchronized boolean isAttached()
//        {
//            return attached;
//        }
//
//        public void karaoke(Music music, KaraokeHandler handler, Fragment fragment)
//        {
//            Log.d("FFmpag_Service", String.format("Creating karaoke for %s", music));
//            Context context = fragment.getContext();
//            String karaokePath = getKaraokePath(music, context);
//            execute(music, karaokePath, context);
//            Log.d("FFmpag_Service", String.format("Attaching session for %s path %s", music, karaokePath));
//            attach(fragment, handler);
////            return karaokePath;
//        }
//
//        private void execute(Music music, String output, Context context)
//        {
//            Intent karaokeService = new Intent(context, KaraokeService.class);
//            karaokeService.putExtra("music", music.toGson());
//            karaokeService.putExtra("output", output);
//            Log.d("FFmpag_Service", String.format("Executing session for %s path %s", music, output));
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService( karaokeService );
//            }
//            else
//                context.startService(karaokeService);
//        }
//
//        public File getKaraoke(Music music, Context context) {
//            String karaokePath = getKaraokePath(music, context);
//            File file = new File(karaokePath);
//            return file;
//        }
//
//        public String getKaraokePath(Music music, Context context)
//        {
//            String name = String.valueOf(music.getMediaId());
//            File parent = new File( context.getExternalFilesDir( null ), "karaoke" );
//            File file = new File( context.getExternalFilesDir( null ), "karaoke/" + name + ".mp3" );
//            if ( !parent.exists() )
//                parent.mkdirs();
//            Log.w( "FFmpag_Service", "karaoke file path " + file );
//            return file.getPath();
//        }
//
//        public Music producing() {
//            return producing;
//        }
//
//        public Context getContext() {
//            return context;
//        }
//
//        public KaraokeHandler getHandler() {
//            return handler;
//        }
//
//        public void bindToService() {
//            if (attached)
//            {
//                Intent intent = new Intent(getContext(), KaraokeService.class);
//                KaraokeConnection connection = new KaraokeConnection(handler);
//                context.bindService(intent, connection, BIND_IMPORTANT);
//            }
//        }
//
//        public void finish() {
//            if (attached)
//            {
//                Intent intent = new Intent(getContext(), KaraokeService.class);
//                context.stopService(intent);
//                dettach();
//            }
//        }
//    }
//
//    private static class KaraokeConnection implements ServiceConnection
//    {
//        private KaraokeService.KaraokeBinder binder;
//        private final KaraokeHandler karaokePlayerHandler;
//
//        private KaraokeConnection(KaraokeHandler karaokePlayerHandler) {
//            this.karaokePlayerHandler = karaokePlayerHandler;
//        }
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            if (service instanceof KaraokeService.KaraokeBinder)
//            {
//                binder = (KaraokeService.KaraokeBinder) service;
//                binder.prepare(karaokePlayerHandler);
//            }
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            binder.release();
//        }
//    }
//
//    private class KaraokeNotification implements KaraokeHandler
//    {
//
//        private File karaokeFile;
//        private Music music;
//
//        @Override
//        public void success(File file, Music music) {
//            karaokeFile = file;
//            this.music = music;
//        }
//
//        @Override
//        public void failure(Music music) {
//
//        }
//
//        @Override
//        public void update(int percentage) {
//            updateNotificationProgress(percentage);
//        }
//    }
//
//    private final String karaokeCmd = "-i \"%s\" -af pan=stereo|c0=c0|c1=-1*c1 -ac 1 \"%s\"";
//    private final String[] karaokeCmds = { "-i", "", "-af", "pan=stereo|c0=c0|c1=-1*c1", "-ac", "1", "" };
//    private NotificationCompat.Builder builder;
//    private NotificationManagerCompat notificationManager;
//
//    private FFmpeg ffmpeg;
//
//    private State state = State.LOADING;
//
//    private Music music;
//    private String output;
//
//    private int NOTIFICATION_ID;
//    private final int MAX = 100;
//    private final int MIN = 0;
//
//    public class KaraokeBinder extends Binder {
//        private KaraokeHandler playerHandler;
//
//        public void prepare(KaraokeHandler handler)
//        {
//            playerHandler = handler;
//            addKaraokeHandler(handler);
//        }
//
//        public synchronized void release()
//        {
//            removeKaraokeHandler(playerHandler);
//        }
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return new KaraokeBinder();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        initialize(intent);
//        initNotification();
//        initNotificationHandler();
//        beginForeground();
//        execute( music, output );
//        requestBinder();
//        return super.onStartCommand(intent, flags, startId);
//    }
//
//    public void initNotificationHandler()
//    {
//        KaraokeNotification notification = new KaraokeNotification();
//        addKaraokeHandler(notification);
//    }
//
//    public void updateNotificationProgress(int progress)
//    {
//        builder.setProgress(MAX, progress, false);
//        Notification notification = builder.build();
//        notificationManager.notify(NOTIFICATION_ID, notification);
//    }
//
//    public void beginForeground()
//    {
//        builder.setProgress(MAX, MIN, false);
//        Notification notification = builder.build();
//        startForeground(NOTIFICATION_ID, notification);
//    }
//
//    private void requestBinder() {
////        Intent broadcastIntent = new Intent(getBaseContext(), MusicP)
////        PendingIntent intent = PendingIntent.getBroadcast(getBaseContext(), 100, )
//
//        KaraokeMaker maker = KaraokeMaker.getInstance();
//        maker.bindToService();
//    }
//
//    private void initialize(Intent intent)
//    {
//        music = Music.fromGson(intent.getStringExtra("music"));
//        output = intent.getStringExtra("output");
//    }
//
//    private enum State{LOADING,READY,UNUSABLE}
//
//    private ArrayList<KaraokeHandler> karaokeHandlers;
//
//    public synchronized void addKaraokeHandler(KaraokeHandler karaokeHandler) {
//        if (karaokeHandlers == null)
//            karaokeHandlers = new ArrayList<>();
//        karaokeHandlers.add(karaokeHandler);
//    }
//
//    public synchronized void removeKaraokeHandler(KaraokeHandler karaokeHandler) {
//        karaokeHandlers.remove(karaokeHandler);
//    }
//
//    public synchronized void handleSuccess(File file, Music music) {
//        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
//        {
//            for (KaraokeHandler handler : karaokeHandlers )
//                handler.success(file, music);
//        }
//        karaokeHandlers.clear();
//        stopSelf();
//    }
//
//    public synchronized void handleFailure(Music music) {
//        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
//        {
//            for (KaraokeHandler handler : karaokeHandlers )
//                handler.failure(music);
//        }
//    }
//
//    public synchronized void updateHandlerProgress(int progress) {
//        if (karaokeHandlers != null && !karaokeHandlers.isEmpty())
//        {
//            for (KaraokeHandler handler : karaokeHandlers )
//                handler.update(progress);
//        }
//    }
//
//    @Override
//    public void onCreate() {
//        createChannel();
//        prepareNotification();
//        loadLibrary();
//    }
//
//    private void createChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = getString(R.string.karaoke_channel);
//            String id = getString(R.string.karaoke_id);
//            String description = getString(R.string.karaoke_description);
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel(id, name, importance);
//            channel.setDescription(description);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    private void prepareNotification()
//    {
//        NOTIFICATION_ID = getResources().getInteger( R.integer.karaoke_id );
//        builder = new NotificationCompat.Builder(this, getString(R.string.karaoke_id) );
//        notificationManager = NotificationManagerCompat.from(this);
//    }
//
//    private void initNotification()
//    {
//        builder.setContentTitle("Creating Karaoke")
//                .setContentText(music.getName())
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//    }
//
//    private void loadLibrary() {
//        ffmpeg = FFmpeg.getInstance(getBaseContext());
//        try {
//            ffmpeg.loadBinary(new LoadBinaryResponseHandler(){
//                @Override
//                public void onFailure() {
//                    super.onFailure();
//                    state = State.UNUSABLE;
//                    Log.w("FFmpag_Service", "Binaries failed to load" );
//                }
//
//                @Override
//                public void onSuccess() {
//                    super.onSuccess();
//                    state = State.READY;
//                    Log.w("FFmpag_Service", "Binaries are ready" );
//                }
//            });
//        } catch (FFmpegNotSupportedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void execute(Music music, String output)
//    {
//        try {
//            String karaokePath = output;
//            String musicPath = music.getData();
//            String[] command = getCommand(musicPath, karaokePath);
//            ffmpeg.execute(command, new KaraokeExecutor(music, output));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public String[] getCommand( String input, String output )
//    {
//        karaokeCmds[ 1 ] = input;
//        karaokeCmds[ 6 ] = output;
//        return karaokeCmds;
//    }
//
//    public static interface KaraokeHandler
//    {
//        void success(File file, Music music);
//        void failure(Music music);
//        void update(int percentage);
//    }
//
//    private class KaraokeExecutor extends ExecuteBinaryResponseHandler
//    {
//        private final String periodRegex = "time=[\\d]{2}:[\\d]{2}:[\\d]{2}";
//        private final Pattern pattern = Pattern.compile(periodRegex);
//        private final Music music;
//        private final String output;
//        private final PeriodFormatter periodFormatter = new PeriodFormatterBuilder().minimumPrintedDigits(2).appendHours()
//                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendMinutes()
//                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).printZeroAlways().appendSeconds().toFormatter();
//        private Period progress = null;
//
//        private KaraokeExecutor(Music music, String output) {
//            super();
//            this.music = music;
//            this.output = output;
//        }
//
//        @Override
//        public synchronized void onSuccess(String message) {
//            super.onSuccess(message);
//            Log.w( "FFmpag_Service", "created karaoke file at " + output );
//            Log.w( "FFmpag_Service", "Karaoke for " + music );
//            Log.w( "FFmpag_Service", "Path " + output );
//            Log.w("FFmpag_Service", "Karaoke Created Successfully " + message );
//
//            handleSuccess(new File(output), music);
//
//            KaraokeMaker maker = KaraokeMaker.getInstance();
//            maker.finish();
//        }
//
//        @Override
//        public void onStart() {
//            super.onStart();
//            Log.w("FFmpag_Service", "Karaoke Have Started Creating" );
//        }
//
//        @Override
//        public void onFinish() {
//            super.onFinish();
//            Log.w("FFmpag_Service", "Karaoke Have Finish Creating" );
//        }
//
//        @Override
//        public synchronized void onProgress(String message) {
//            super.onProgress(message);
//            Log.w("FFmpag_Service", "Karaoke Creation in progress " + message );
//
//            int percentage = extractProgress(message);
//            Log.w( "FFmpag_Service", "Karaoke Update progress" + percentage);
//            updateHandlerProgress(percentage);
//        }
//
//        private int extractProgress(String message)
//        {
//            Log.w( "FFmpag_Service", "String Processing " + message);
//            Matcher matcher = pattern.matcher(message);
//            if  ( matcher.find() )
//            {
//                String time = matcher.group();
//                Log.w( "FFmpag_Service", "String Processing found" + time);
//                String[] split = time.split("=");
//                time = split[ 1 ];
//                Log.w( "FFmpag_Service", "String Processing extracted" + time);
//                progress = Period.parse(time, periodFormatter);
//            }
//            if ( progress == null )
//                return -1;
//            else
//            {
//                Period duration = music.getDuration();
//                Log.w( "FFmpag_Service", "String Processing progress" + duration);
//                double result = (double) progress.toStandardSeconds().getSeconds() / duration.toStandardSeconds().getSeconds() * 100;
//                Log.w( "FFmpag_Service", "String Processing progress percentage" + result);
//                return (int) result;
//            }
//        }
//
//        @Override
//        public void onFailure(String message) {
//            super.onFailure(message);
//            Log.w("FFmpag_Service", "Karaoke Failed to create reason " + message );
//            handleFailure(music);
//        }
//    };
//}
