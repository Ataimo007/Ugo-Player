package com.gcodes.iplayer.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.player.PlayerService;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.api.services.youtube.model.Video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;
import kotlin.Pair;
import kotlin.Triple;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;
import static com.gcodes.iplayer.player.PlayerService.ON_CHECK_PLAYER_MANAGER;

public class MusicPlayerActivity extends AppCompatActivity implements TextWatcher
{

//    private Music currentMusic;

//    public synchronized Music getCurrentLoadedMusic() {
//        return currentLoadedMusic;
//    }
//
//    public synchronized void setCurrentLoadedMusic(Music currentLoadedMusic) {
//        this.currentLoadedMusic = currentLoadedMusic;
//    }

    private Music loadedMusic;
    private Music loadingMusic;
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
    private TextView header;
    private ImageButton search;
    private TextInputLayout searchLayout;
    private TextInputEditText searchField;
    private boolean searching = false;
    private Message currentMessage;

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

    public class MusicBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("has_player", false))
                obtainMusicManager();
            unregisterReceiver(this);
        }
    }

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
//            consumeTrack();
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

//            music.select();

//            int previousIndex = musicManager.getPlayerManager().getPreviousWindowIndex();
//            if (previousIndex != C.INDEX_UNSET)
//                musicManager.getMusic(previousIndex).unSelect();

//            Log.d("Current_Playlist", "Current Tracks");
//            for (Music track : musicManager.getMusics())
//                Log.d("Current_Playlist", "Track " + track);

            MusicPlayerActivity.this.consumeTrack(music);
        }
    }

    private Music getCurrentMusic()
    {
        int index = musicManager.getPlayerManager().getCurrentPeriodIndex();
        return musicManager.getMusic( index );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music_player);
        manager = PlayerDatabase.getInstance(this);
        initInfoRenderer();
        initInfoRetriever(manager, this);
        processIntent();
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        processAction(intent);
//    }

    private void processIntent(Intent intent)
    {
        if ( intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW) )
        {
            musicManager.play(intent.getData());
        }
    }

    private void processIntent()
    {
        Intent current = getIntent();
        if ( current.getAction() != null && current.getAction().equals(Intent.ACTION_VIEW) )
        {
            registerReceivers();
            externalPlayRequest(current);
        }
        else
            obtainMusicManager();
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_START_PLAYER_MANAGER);
        registerReceiver(new MusicBroadCastReceiver(), filter);
    }

    private void externalPlayRequest(Intent current)
    {
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtra("broadcast", true );
        intent.putExtra("type", "music_uri" );
//        intent.setData(current.getData());
        startService(intent);
    }

    private void obtainMusicManager() {
        Intent intent = new Intent(this, PlayerService.class);
        musicConnection = new MusicConnection();
        bindService(intent, musicConnection, BIND_IMPORTANT);
    }

    private void checkPlayer() {
        Intent broadcast = new Intent();
        broadcast.setAction(ON_CHECK_PLAYER_MANAGER);
        sendBroadcast(broadcast);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
        processAction(intent);
    }

    private void processAction(Intent intent) {
        Log.d("Play_Karaoke", "Processing New Intent");
        if (PLAY_KARAOKE.equals(intent.getAction())) {
            Log.d("Play_Karaoke", "Processing Karaoke");
            playKaraoke(intent);
        }
    }

    private void playKaraoke(Intent intent) {

//        TabLayout tabs = getTabs();
//        tabs.getTabAt( pagerAdapter.getDefaultTabPos() ).select();

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
        initPlaylist();
        initLoading();
    }

    private void initPlaylist() {
        header = findViewById(R.id.playerlist_header);
        search = findViewById(R.id.playlist_search);
        searchLayout = findViewById(R.id.search_layout);
        searchField = findViewById(R.id.search_field);

        search.setOnClickListener(v -> {
            showSearch();
        });

        searchField.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        pagerAdapter.searchPlaylist(s.toString());
    }

    private void showSearch()
    {
        header.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
        searching = true;

        searchField.requestFocusFromTouch();
        searchField.requestFocus();
        InputMethodManager IManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        IManager.showSoftInput(searchField, 0);
    }

    private void hideSearch()
    {
        searchField.getEditableText().clear();
        pagerAdapter.updatePlaylist();

        searchLayout.setVisibility(View.GONE);
        header.setVisibility(View.VISIBLE);
        search.setVisibility(View.VISIBLE);
        searching = false;
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
        Intent current = getIntent();
        if ( current.getAction() != null && current.getAction().equals(Intent.ACTION_VIEW) )
            musicManager.play(current.getData());

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
            listener.consumeTrack();
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
        currentMessage = infoRetriever.obtainMessage(0, music);
        infoRetriever.removeMessages(0);
        currentMessage.sendToTarget();
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
        if (loadedMusic != music && loadingMusic != music)
        {
            loadingMusic = music;

            updatePlayer( music );
            pagerAdapter.updatePlayer( music );
            pagerAdapter.updatePlaylist( music );
            pagerAdapter.updateMusicVideos( music );
            pagerAdapter.onLoading();

            retrieveInfo( music );
        }
    }

    private void initInfoRenderer() {
        infoRenderer = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg)
            {
                Pair<Music, Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>>> fullResult = (Pair<Music, Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>>>) msg.obj;
                Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>> result = fullResult.getSecond();
                PlayerDatabase.MusicInfo info = result.getFirst();
                PlayerDatabase.MusicLyrics finalLyrics = result.getSecond();
                List<Video> finalMusicVideo = result.getThird();


                Music loaded = fullResult.getFirst();
                if (getCurrentMusic().equals(loaded))
                {
                    if ( info != null )
                        pagerAdapter.updatePlayer( info, finalLyrics );
                    else
                        pagerAdapter.updatePlayer(finalLyrics);
                    if (finalMusicVideo != null )
                        pagerAdapter.updateMusicVideos( finalMusicVideo );

                    loadedMusic = loaded;
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (searching)
            hideSearch();
        else if (pager.getCurrentItem() != pagerAdapter.getDefaultTabPos())
            pager.setCurrentItem(pagerAdapter.getDefaultTabPos(), true);
        else if (pagerAdapter.isLyricsVisible())
            pagerAdapter.hideLyrics();
        else
        {
            if (isTaskRoot())
            {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            else
                super.onBackPressed();
        }
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
                    List<Video> musicVideo;
                    PlayerDatabase.MusicLyrics lyrics;

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

                    Pair<Music, Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>>> result = new Pair<>( music, new Triple<>(info, lyrics, musicVideo) );
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
                        Pair<Music, Triple<PlayerDatabase.MusicInfo, PlayerDatabase.MusicLyrics, List<Video>>> result = new Pair<>(music, new Triple<>(info, lyrics, null));
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
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( this, music ) ).apply(RequestOptions.bitmapTransform(new BlurTransformation(7, 1)))
                .placeholder( R.drawable.music_background2).into( background );
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

        private boolean isLyricsVisible()
        {
            return player.isLyricsVisible();
        }

        private void hideLyrics()
        {
            player.hideLyrics();
        }

        private void showLyrics()
        {
            player.showLyrics();
        }

        private void playKaraoke(File file, Music music)
        {
            Log.d("Play_Karaoke", "Applying Karaoke " + music);
            player.prepareKaraoke(file, music);
        }

        private void updateMusicVideos( List<Video> videos )
        {
            musicVideo.updateMusicVideos( videos );
        }

        private void updatePlaylist( Music music )
        {
            playlist.updateMusic( music );
        }

        private void updatePlaylist()
        {
            playlist.updateMusic();
        }

        private void searchPlaylist(String query)
        {
            playlist.search(query);
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
