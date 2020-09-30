package com.gcodes.iplayer.music.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.player.PlayerService;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.material.tabs.TabLayout;
import com.google.api.services.youtube.model.Video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.List;

import kotlin.Triple;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicPlayerActivity extends AppCompatActivity
{

    private Music currentMusic;
//    private Player.EventListener trackListener;
    private PlayerDatabase.MusicInfo musicInfo;
    private PlayerControlView control;
    private TextView musicName;
    private TextView artistName;
    private SectionsPagerAdapter pagerAdapter;
    private PlayerDatabase manager;
    private PlayerManager.MusicManager musicManager;

    private ImageView background;
    private final Handler handler = new Handler();
    private View loadingView;

    public static final String PLAY_KARAOKE = "PLAY_KARAOKE";
    private PlayerListener listener;
    private MusicConnection musicConnection;
    private Handler infoRenderer;
    private Handler infoRetriever;
    private ViewPager pager;

//    private static class LooperThread extends Thread {
//        public Handler mHandler;
//
//        public void run() {
//            Looper.prepare();
//
//            mHandler = new Handler() {
//                public void handleMessage(Message msg) {
//                    // process incoming messages here
//                }
//            };
//
//            Looper.loop();
//        }
//    }

    private class MusicConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            musicManager = binder.getMusicManager();
            if (musicManager != null)
                manageMusic();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicManager = null;
        }
    }

    private class PlayerListener implements Player.EventListener {

        private PlayerManager.MusicManager manager;

        public PlayerListener(PlayerManager.MusicManager manager) {
            this.manager = manager;
            consumeTrack();
//            initView(manager);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            consumeTrack();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            if ( manager.isMusicPlaying() && playWhenReady )
//                consumeTrack();
            pagerAdapter.processPlayerRotate(playWhenReady, playbackState);
        }

        public synchronized void consumeTrack()
        {
            int index = musicManager.getPlayerManager().getCurrentPeriodIndex();
            Music music = musicManager.getMusic( index );
            MusicPlayerActivity.this.consumeTrack(music);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music_player);
        manager = PlayerDatabase.getInstance();
        initInfoRenderer();
        initInfoRetriever(manager, this);
        obtainMusicManager();
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        processAction(intent);
//    }

    private void obtainMusicManager() {
        Intent intent = new Intent(this, PlayerService.class);
        musicConnection = new MusicConnection();
        bindService(intent, musicConnection, BIND_IMPORTANT);
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        processAction(intent);
//    }

    private void processAction(Intent intent) {
        Log.d("Play_Karaoke", "Processing New Intent");
        if (PLAY_KARAOKE.equals(intent.getAction())) {
            Log.d("Play_Karaoke", "Processing Karaoke");
            playKaraoke(intent);
        }
    }

    private void playKaraoke(Intent intent) {

        TabLayout tabs = getTabs();
        tabs.getTabAt( pagerAdapter.getDefaultTabPos() ).select();

        Music music = Music.fromGson(intent.getStringExtra("music"));
        File file = new File(intent.getStringExtra("output"));

        pagerAdapter.playKaraoke(file, music);
    }

    //    private void prepareMusicPlayer()
//    {
//        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
//        videoScreen.setVisibility( View.GONE );
//        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
//        coverArt.setVisibility(View.VISIBLE);
//    }
//
//    private void prepareVideoPlayer()
//    {
//        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
//        videoScreen.setVisibility( View.VISIBLE );
//        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
//        coverArt.setVisibility(View.GONE);
//    }

    private TabLayout getTabs()
    {
        TabLayout tabs = findViewById( R.id.player_tabs );
        TabLayout.Tab tabAt = tabs.getTabAt(0);
        tabAt.setCustomView(LayoutInflater.from(this).inflate(R.layout.player_custom_tab, null));
        return tabs;
    }

    public View getMusicVideoTab()
    {
        TabLayout tabs = findViewById( R.id.player_tabs );
        TabLayout.Tab tabAt = tabs.getTabAt(0);
        return tabAt.getCustomView();
    }

    public void updateTabView()
    {
        TabLayout tabs = findViewById( R.id.player_tabs );
        tabs.invalidate();
        tabs.requestLayout();
    }

    private void initView()
    {
        pagerAdapter = new SectionsPagerAdapter();
        TabLayout tabs = getTabs();
        pager = findViewById(R.id.player_pager);
        pager.setAdapter( pagerAdapter );
        pager.addOnPageChangeListener( new TabLayout.TabLayoutOnPageChangeListener( tabs ) );
        tabs.addOnTabSelectedListener( new TabLayout.ViewPagerOnTabSelectedListener( pager ) );

//        tabs.getTabAt( pagerAdapter.getDefaultTabPos() ).select();
        pager.setCurrentItem(pagerAdapter.getDefaultTabPos(), true);
        findViewById( R.id.player_bar).setVisibility( View.VISIBLE );

        pager.addOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                hideAll();
                showBar( position );
            }
        });

        initPlayer();
        initLoading();
    }

    private void initLoading() {
        loadingView = findViewById(R.id.player_loading);
    }

    public void loading()
    {
        loadingView.setVisibility(View.VISIBLE);
    }

    public void notLoading()
    {
        loadingView.setVisibility(View.GONE);
    }

    private void initPlayer() {
        musicName = findViewById( R.id.song_name );
        artistName = findViewById( R.id.artist_name );
        background = findViewById( R.id.player_background );
    }

    private void manageMusic() {
        initView();
        listener = new PlayerListener(musicManager);
        beginListening();
        processAction(getIntent());
    }

    private synchronized void beginListening()
    {
        if (listener != null)
        {
            Log.d("Player_List", "begin to listen");
            musicManager.addListener(listener);
        }
    }

    private synchronized void endListening()
    {
        if (listener != null)
        {
            Log.d("Player_List", "stop to listen");
            musicManager.removeListener(listener);
        }
    }

    private synchronized void releaseManager() {
        listener = null;
//        handler.removeCallbacks( runnable );
        infoRetriever.getLooper().quit();
        manager = null;
    }

    private final int NETWORKWATCHER = 1000;
    private boolean watching = false;

//    private final Runnable runnable = new Runnable() {
////        int count = 0;
////        boolean flag = true;
//        @Override
//        public void run() {
//
//            boolean deviceOnline = Helper.isDeviceOnline( MusicPlayerActivity.this );
//            Log.d("Music_Player_Service", "is the device online " + deviceOnline );
//            if (  deviceOnline )
//            {
//                retrieveInfoOnline( currentMusic );
//            }
//            else
//            {
//                handler.postDelayed( runnable, NETWORKWATCHER );
//            }
//        }
//    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseManager();
        unbindService(musicConnection);
    }

//    @Override
//    protected void onStart() {
//
//        super.onStart();
//    }

    @Override
    protected void onResume() {
        beginListening();
        super.onResume();
    }

    @Override
    protected void onPause() {
        endListening();
        super.onPause();
    }

//    @Override
//    protected void onStop() {
//
//        super.onStop();
//    }

    private void retrieveInfo(Music music)
    {
        infoRetriever.obtainMessage(0, music).sendToTarget();
    }

//    private void retrieveInfo(Music music)
//    {
//        boolean deviceOnline = Helper.isDeviceOnline(this);
//        Log.d("Music_Player_Service", "is the device online " + deviceOnline );
//        if (  !deviceOnline && !watching )
//        {
//            handler.postDelayed( runnable, NETWORKWATCHER );
//            watching = true;
//        }
//        retrieveInfoOnline( music );
//    }

//    private void retrieveInfo(Music music)
//    {
//        boolean deviceOnline = Helper.isDeviceOnline(this);
//        Log.d("Music_Player_Service", "is the device online " + deviceOnline );
//        if (  deviceOnline )
//        {
//            handler.removeCallbacks( runnable );
//            watching = false;
//        }
//        else
//        {
//            if ( !watching )
//            {
//                watching = true;
//                handler.postDelayed( runnable, NETWORKWATCHER );
//            }
//        }
//        retrieveInfoOnline( music );
//    }

//    private synchronized void retrieveInfoOnline(Music music)
//    {
//        Helper.Worker.executeTask(() -> {
//            PlayerDatabase manager = null;
//            if ( this.manager != null )
//                manager = this.manager;
//
//            PlayerDatabase.MusicInfo info = manager.getInfo(music);
//            List<Video> musicVideo = null;
//            PlayerDatabase.MusicLyrics lyrics = null;
//
//            if ( info != null )
//            {
//                Log.d("Music_Player", "The info is " + info );
//                lyrics = manager.getLyrics( info );
//                musicVideo = manager.getMusicVideo( info );
//            }
//            else
//            {
//                lyrics = manager.getLyrics(music);
//                musicVideo = manager.getMusicVideo(music);
//            }
////            manager.getKaraoke(music);
//
//            final PlayerDatabase.MusicLyrics finalLyrics = lyrics;
//            final List<Video> finalMusicVideo = musicVideo;
//            return () -> {
//                if ( info != null )
//                {
//                    pagerAdapter.updatePlayer( info, finalLyrics );
//                }
//                else
//                {
//                    pagerAdapter.updatePlayer(finalLyrics);
//                }
//                pagerAdapter.updateMusicVideos( finalMusicVideo );
//            };
//        });
//    }

//    private synchronized void retrieveInfoOnline(Music music)
//    {
//        Helper.Worker.executeTask(() -> {
//            PlayerDatabase.MusicInfo info = null;
//            try {
//                if ( manager != null )
//                {
//                    info = manager.getInfo(music);
//                    List<Video> musicVideo = null;
//                    PlayerDatabase.MusicLyrics lyrics = null;
//
//                    if ( info != null )
//                    {
//                        Log.d("Music_Player", "The info is " + info );
//                        lyrics = manager.getLyrics( info );
//                        musicVideo = manager.getMusicVideo( info );
//                    }
//                    else
//                    {
//                        lyrics = manager.getLyrics(music);
//                        musicVideo = manager.getMusicVideo(music);
//                    }
//                    final PlayerDatabase.MusicLyrics finalLyrics = lyrics;
//                    final List<Video> finalMusicVideo = musicVideo;
//                }
//            } catch (Exception ignored) {}
//
//            return () -> {
//                if ( info != null )
//                {
//                    pagerAdapter.updatePlayer( info, finalLyrics );
//                }
//                else
//                {
//                    pagerAdapter.updatePlayer(finalLyrics);
//                }
//                pagerAdapter.updateMusicVideos( finalMusicVideo );
//            };
////            manager.getKaraoke(music);
//        });
//    }

    public void consumeTrack( Music music )
    {
        updatePlayer( music );
        pagerAdapter.updatePlayer( music );
        pagerAdapter.updatePlaylist( music );
        pagerAdapter.updateMusicVideos( music );
        pagerAdapter.onLoading();

        currentMusic = music;
        retrieveInfo( music );
    }

    private void initInfoRenderer() {
        infoRenderer = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg)
            {
                Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>> result = (Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>>) msg.obj;
                PlayerDatabase.MusicInfo info = result.getFirst();
                PlayerDatabase.MusicLyrics finalLyrics = result.getSecond();
                List<Video> finalMusicVideo = result.getThird();

                if ( info != null )
                    pagerAdapter.updatePlayer( info, finalLyrics );
                else
                    pagerAdapter.updatePlayer(finalLyrics);
                if (finalMusicVideo != null )
                    pagerAdapter.updateMusicVideos( finalMusicVideo );
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() != pagerAdapter.getDefaultTabPos())
            pager.setCurrentItem(pagerAdapter.getDefaultTabPos(), true);
        else
            super.onBackPressed();
    }

    private void initInfoRetriever(PlayerDatabase manager, Context context) {
        Thread thread = new Thread(() -> {
            Looper.prepare();
            infoRetriever = new Handler(Looper.myLooper()){
                private Music music;
                private Runnable retriever = new Runnable() {
                    @Override
                    public void run() {
                        boolean deviceOnline = Helper.isDeviceOnline(context);
                        if (  deviceOnline )
                            retrieve();
                        else
                            infoRetriever.postDelayed( this, NETWORKWATCHER );
                    }
                };

                @Override
                public void handleMessage(@NonNull Message msg) {
                    music = (Music) msg.obj;
                    infoRetriever.removeCallbacks(retriever);
                    boolean deviceOnline = Helper.isDeviceOnline(context);
                    if (  deviceOnline )
                        retrieve();
                    else
                    {
                        retrieveLocally();
                        infoRetriever.postDelayed( retriever, NETWORKWATCHER );
                    }
                }

                private void retrieve()
                {
                    PlayerDatabase.MusicInfo info = manager.getInfo(music);
                    List<Video> musicVideo = null;
                    PlayerDatabase.MusicLyrics lyrics = null;

                    if ( info != null )
                    {
                        Log.d("Music_Player", "The info is " + info );
                        lyrics = manager.getLyrics( info );
                        musicVideo = manager.getMusicVideo( info );
                    }
                    else
                    {
                        lyrics = manager.getLyrics(music);
                        musicVideo = manager.getMusicVideo(music);
                    }

                    Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>> result = new Triple<>(info, lyrics, musicVideo);
                    infoRenderer.obtainMessage(0, result).sendToTarget();
                }

                private void retrieveLocally()
                {
                    PlayerDatabase.MusicInfo info = manager.getInfo(music);
                    PlayerDatabase.MusicLyrics lyrics = null;

                    if ( info != null )
                    {
                        Log.d("Music_Player", "The info is " + info );
                        lyrics = manager.getLyrics( info );
                    }
                    else
                    {
                        lyrics = manager.getLyrics(music);
                    }

                    if (info != null || lyrics != null )
                    {
                        Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>> result = new Triple<>(info, lyrics, null);
                        infoRenderer.obtainMessage(0, result).sendToTarget();
                    }
                }
            };
            Looper.loop();
        });
        thread.start();
    }


    private void updatePlayer(Music music) {
        musicName.setText( music.getName() );
        artistName.setText( music.getArtist() );
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( this, music ) )
                .placeholder( R.drawable.u_song_art_padded ).into( background );
    }

//    private void getLyrics()
//    {
//        Helper.Worker.executeTask(() -> {
//            Log.d( "Music_Player", "getting lyrics" );
//            PlayerDatabase.MusicLyrics lyric = manager.getLyrics( currentMusic );
//            if ( lyric != null )
//            {
//                String lyricsBody = lyric.getLyricsBody();
//                lyrics = lyricsBody.split("\n");
//            }
//            else
//                lyrics = new String[0];
//
//            return () -> {
////                adapter.notifyDataSetChanged();
//                Log.d( "Music_Player", "lyrics body " + Arrays.toString(lyrics));
//            };
//        });
//    }


//    public void showMusicVideo(View view)
//    {
////        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic );
//        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic, musicInfo, t -> prepareVideoPlayer(),
//                t1 -> prepareMusicPlayer() );
//    }

//    private void recognizeMusic( Music music )
//    {
//        Log.d( "ACR_Service", "Getting music info ..." );
//        Helper.Worker.executeTask(() -> {
//            return () -> {};
//        });
//    }

//    private void update() {
//
//    }

    public void hideAll()
    {
        findViewById( R.id.player_video_bar ).setVisibility( View.GONE );
        findViewById( R.id.playerlist_bar).setVisibility( View.GONE );
        findViewById( R.id.playerlist_bar ).setVisibility( View.GONE );
    }

    public void showBar( int pos )
    {
        switch ( pos )
        {
            case 0:
                findViewById( R.id.player_video_bar ).setVisibility( View.VISIBLE );
                break;

            case 1:
                findViewById( R.id.player_bar).setVisibility( View.VISIBLE );
                break;

            case 2:
                findViewById( R.id.playerlist_bar ).setVisibility( View.VISIBLE );
                break;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private MusicVideoFragment musicVideo;
        private MusicPlayerFragment player;
        private PlaylistFragment playlist;

        public SectionsPagerAdapter()
        {
            super(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            init();
        }

        private void processPlayerRotate(boolean playWhenReady, int playbackState)
        {
            player.processRotate(playWhenReady, playbackState);
        }

        private void updatePlayer(Music music)
        {
            player.updateMusic( music );
        }

        private void updatePlayer(PlayerDatabase.MusicInfo info, PlayerDatabase.MusicLyrics lyrics )
        {
            player.updateInfo( info );
            player.updateLyrics( lyrics );
        }

        public void updateMusicVideos(Music music) {
            musicVideo.updateMusic( music );
        }

        private void updatePlayer(PlayerDatabase.MusicLyrics lyrics )
        {
            player.updateLyrics( lyrics );
        }

        private void playKaraoke(File file, Music music)
        {
            Log.d("Play_Karaoke", "Applying Karaoke " + music);
            player.playKaraoke(file, music);
        }

        private void updateMusicVideos( List<Video> videos )
        {
            musicVideo.updateMusicVideos( videos );
        }

        private void updatePlaylist( Music music )
        {
            playlist.updateMusic( music );
        }

        public void init()
        {
            musicVideo = new MusicVideoFragment(musicManager);
            player = new MusicPlayerFragment(musicManager);
            playlist = new PlaylistFragment(musicManager);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch ( position )
            {
                case 0:
                    return musicVideo;

                case 1:
                    return player;

                case 2:
                    return playlist;

                default:
                    return new Fragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        public int getDefaultTabPos() {
            return 1;
        }

        public void onLoading() {
            musicVideo.onLoading();
            player.onLoading();
        }

//        public void setToolbar( int pos )
//        {
//            for ( Fragment bar : views )
//            {
//                PlayerBar playerBar = (PlayerBar) views[ pos ];
//                playerBar.HideBar();
//            }
//            PlayerBar playerBar = (PlayerBar) views[ pos ];
//            playerBar.ShowBar();
//        }
    }

//    public static abstract class SimplePlayerBar extends Fragment
//    {
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            View content = inflater.inflate(getLayout(), container, false);
//            TextView title = content.findViewById( R.id.player_title );
//            title.setText( getTitle() );
//            return content;
//        }
//
//        protected int getLayout()
//        {
//            return R.layout.player_simple_toolbar;
//        }
//
//        protected abstract String getTitle();
//    }

//    public static interface PlayerBar
//    {
//        void ShowBar();
//        void HideBar();
//    }
}
