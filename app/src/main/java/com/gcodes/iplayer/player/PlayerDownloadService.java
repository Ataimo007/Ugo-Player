package com.gcodes.iplayer.player;

import android.app.Notification;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.offline.ActionFileUpgradeUtil;
import com.google.android.exoplayer2.offline.DefaultDownloadIndex;
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.scheduler.PlatformScheduler;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.ui.DownloadNotificationHelper;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.api.services.youtube.model.Video;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PlayerDownloadService extends DownloadService {

    public final static int JOB_ID = 1;
    public final static int DEFAULT_NOTIFICATION_ID = 1;
    public final static String CHANNEL_ID = "Player_Download_Service";

    private static int nextNotification = DEFAULT_NOTIFICATION_ID + 1;
    private DownloadNotificationHelper notificationHelper;

    @DrawableRes
    private final static int progress = R.drawable.exo_controls_play;
    @DrawableRes
    private final static int done = R.drawable.exo_controls_play;
    @DrawableRes
    private final static int fail = R.drawable.exo_controls_play;

    private static final String TAG = "DemoApplication";
    private static final String DOWNLOAD_ACTION_FILE = "actions";
    private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";

    private DownloadManager downloadManager;
    private PlayerManager manager;

    private static HashMap< String, PlayerDatabase.MusicVideo > downloadInfo = new HashMap<>();
    private HashMap< String, Download > downloads = new HashMap<>();
    private static HashMap< String, Consumer<Download> > onDownload = new HashMap<>();

//    private DownloadTracker downloadTracker;

//    private final PlayerBinder binder = new PlayerBinder();

    public PlayerDownloadService()
    {
//        super( DEFAULT_NOTIFICATION_ID, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, CHANNEL_ID, R.string.channel_name );
        super( DEFAULT_NOTIFICATION_ID, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, CHANNEL_ID, R.string.download_channel);
        nextNotification = DEFAULT_NOTIFICATION_ID + 1;
    }

    public static void download(Video video, String uri, Music music, Consumer<Download> onStart )
    {
        download( video, uri, music );
        onDownload.put( video.getId(), onStart );
    }

    public static void download(Video video, String uri, Music music)
    {
        Uri media = Uri.parse( uri );
        DownloadRequest request = new DownloadRequest(video.getId(), DownloadRequest.TYPE_PROGRESSIVE, media, Collections.emptyList(),
                null, music.getName().getBytes() );
        DownloadService.sendAddDownload( PlayerManager.getInstance().getContext(), PlayerDownloadService.class, request, true );
        PlayerDatabase.MusicVideo musicVideo = new PlayerDatabase.MusicVideo(video, uri, music);
        downloadInfo.put( video.getId(), musicVideo );
    }

    @Override
    protected DownloadManager getDownloadManager() {
        initDownloadManager();
        return downloadManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new DownloadNotificationHelper( this, CHANNEL_ID );
        manager = PlayerManager.getInstance();
    }

    @Nullable
    @Override
    protected Scheduler getScheduler() {
        return new PlatformScheduler( getApplicationContext(), JOB_ID);
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads) {
        return notificationHelper.buildProgressNotification( progress, null, "Downloading Music Videos", downloads );
    }

    @Override
    protected void onDownloadChanged(Download download) {
        Notification notification;
        switch ( download.state )
        {
            case Download.STATE_COMPLETED:
                notification = notificationHelper.buildDownloadCompletedNotification( done, null, Util.fromUtf8Bytes( download.request.data ) );
                NotificationUtil.setNotification( this, DEFAULT_NOTIFICATION_ID, notification );
                PlayerDatabase.MusicVideo musicVideo = downloadInfo.get(download.request.id);
                if ( musicVideo != null )
                    PlayerDatabase.getInstance().playerDao().addMusicVideo( musicVideo );
                break;

            case Download.STATE_FAILED:
                notification = notificationHelper.buildDownloadFailedNotification( fail, null, Util.fromUtf8Bytes( download.request.data ) );
                NotificationUtil.setNotification( this, DEFAULT_NOTIFICATION_ID, notification );
                break;

            case Download.STATE_DOWNLOADING:
                Consumer<Download> begin = onDownload.get(download.request.id);
                if ( begin != null )
                    begin.accept( download );
        }

//        NotificationUtil.setNotification( this, nextNotification++, notification );

    }

    @Override
    protected void onDownloadRemoved(Download download) {
        super.onDownloadRemoved(download);
    }

    private synchronized void initDownloadManager() {
        if (downloadManager == null) {
            DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(manager.getDatabaseProvider());
            upgradeActionFile( DOWNLOAD_ACTION_FILE, downloadIndex, /* addNewDownloadsAsCompleted= */ false);
            upgradeActionFile( DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, /* addNewDownloadsAsCompleted= */ true);
            DownloaderConstructorHelper downloaderConstructorHelper = new DownloaderConstructorHelper(manager.getDownloadCache(), manager.buildHttpDataSourceFactory());
            downloadManager = new DownloadManager(
                            this, downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
//            downloadTracker = new DownloadTracker(/* context= */ this, getOfflineFactory(), downloadManager);
            prepareManager();
        }
    }

    private void prepareManager() {
        downloadManager.addListener(new DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(DownloadManager downloadManager, Download download) {

            }

            @Override
            public void onDownloadRemoved(DownloadManager downloadManager, Download download) {

            }
        });
    }


    private void upgradeActionFile(String fileName, DefaultDownloadIndex downloadIndex, boolean addNewDownloadsAsCompleted) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    new File(manager.getDownloadDirectory(), fileName),
                    /* downloadIdProvider= */ null,
                    downloadIndex,
                    /* deleteOnFailure= */ true,
                    addNewDownloadsAsCompleted);
        } catch (IOException e) {
            Log.e(TAG, "Failed to upgrade action file: " + fileName, e);
        }
    }

    /** Returns whether extension renderers should be used. */
//    public boolean useExtensionRenderers() {
//        return "withExtensions".equals(BuildConfig.FLAVOR);
//    }
//
//
//    public RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
//        @DefaultRenderersFactory.ExtensionRendererMode
//        int extensionRendererMode =
//                useExtensionRenderers()
//                        ? (preferExtensionRenderer
//                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
//                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
//                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
//        return new DefaultRenderersFactory(/* context= */ this)
//                .setExtensionRendererMode(extensionRendererMode);
//    }

//    public DownloadTracker getDownloadTracker() {
//        return downloadTracker;
//    }

//    private class DownloadTracker {
//        public DownloadTracker(PlayerDownloadService playerDownloadService, DataSource.Factory factory, DownloadManager downloadManager) {
//
//        }
//    }
}
