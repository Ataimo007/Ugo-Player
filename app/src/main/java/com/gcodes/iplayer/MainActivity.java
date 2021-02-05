package com.gcodes.iplayer;

import android.Manifest;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.gcodes.iplayer.music.MusicFragment;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.player.MusicController;
import com.gcodes.iplayer.player.PlayerService;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.model.Series;
import com.gcodes.iplayer.video.player.VideoController;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;

import static com.gcodes.iplayer.player.PlayerService.ON_CHECK_PLAYER_MANAGER;

//import okhttp3.Cookie;
//import okhttp3.CookieJar;
//import okhttp3.HttpUrl;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    private final int requestCode = 0;
//    private static AppCompatActivity application;
    Supplier<Boolean> action;

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = this::doSelection;
    private NavigationPager pager;
    private AppPagerAdapter adapter;
    private int currentPage;

    private BackAction[] backActions;
    private PlayerBroadCastReceiver musicReceiver;
    private ControllerListener controllerListener;
    private PlayerConnection playerConnection;
    private PlayerService.PlayerBinder binder;
    private BottomNavigationView navigation;
//    private PlayerManager playerManager;

    public enum AppLoader
    {
        TRACK(0), ARTIST(1), ALBUM(2), GENRE(3), MUSIC_FOLDER(4), VIDEO(5), SERIES(6), VIDEO_FOLDER(7), GENRE_MEMBERS(8);

        int id;
        AppLoader(int id)
        {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static AppLoader getInstance(int id)
        {
            for ( AppLoader loader : values() )
                if (loader.getId() == id)
                    return loader;
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Handling_Intent", "On Create " + (getIntent().hasExtra("music") ? getIntent().getStringExtra("music") : "no music") );
        super.onCreate(savedInstanceState);
        Log.d("Player_Model", "Creating a new Activity " + savedInstanceState );
//        Log.d("Handling_Karaoke", "Handling Karaoke Event");
        begin();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PlayerManager playerManager = getPlayerManager();
        if (playerManager == null)
            unRegisterReceiver();
        else
            releasePlayerManager(playerManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlayerManager playerManager = getPlayerManager();
        if (playerManager == null)
        {
            registerReceivers();
            checkPlayer();
        }
        else
            prepareController(playerManager);
    }

    @Override
    protected void onDestroy() {
        Log.d("Handling_Intent", "On Destroy" );
//        releasePlayerManager();
        super.onDestroy();
    }

//    private void processIntent() {
//        Intent intent = getIntent();
//        processIntent(intent);
//    }

    private void processIntent(Intent intent) {
        Log.d("Player_Model", "Processing Broadcast from Player Service" );
        PlayerManager playerManager = getPlayerManager();
        if (playerManager != null)
        {
            new ViewModelProvider(MainActivity.this).get(PlayerModel.class).playPendings(playerManager);
            return;
        }
        if  (intent.getBooleanExtra("has_player", false ))
            obtainPlayerManager();
    }

    private void obtainPlayerManager() {
        Log.d("Player_Model", "Obtaining Player Manager from Player Service" );
        Intent intent = new Intent(this, PlayerService.class);
        playerConnection = new PlayerConnection();
        bindService(intent, playerConnection, BIND_IMPORTANT);
    }

    private void releasePlayerManager(PlayerManager playerManager) {
        Log.d("Player_Model", "release Player Manager from Player Service" );
//        PlayerManager playerManager = getPlayerManager();
        playerManager.removeListener(controllerListener);
        controllerListener = null;
//        if (playerManager != null)
//        {
//        }
//        if (playerConnection != null)
//            unbindService(playerConnection);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        PlayerManager.getInstance().onDestroyActivity();
//        Log.w("Destroy_Main", "Destroying Main Activity" );
//    }

    @Override
    public void onBackPressed() {
        BackAction action = backActions[pager.getCurrentItem()];
        if ( action != null )
        {
            boolean back = action.goBack();
            if ( !back )
                back();
        }
        else
            back();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private void back()
    {
        boolean popped = NavHostFragment.findNavController(adapter.getItem(currentPage)).popBackStack();
        new ViewModelProvider(this).get(BackStackModel.class).onStackPopped.setValue(popped);
        if ( !popped )
        {
            if (!moveToDefaultPos())
                super.onBackPressed();
        }
        new ViewModelProvider(this).get(BackStackModel.class).onStackPopped.setValue(false);
    }

    private boolean moveToDefaultPos() {
        if (isInDefaultNav())
        {
            if (isInDefaultTab())
                return false;
            else
                goToDefaultTab();
        }
        else
        {
            goToDefaultNav();
            goToDefaultTab();
        }
        return true;
    }

    private void begin()
    {
        setContentView(R.layout.activity_main_default4);

        Toolbar toolbar = findViewById(R.id.video_toolbar);
        setSupportActionBar( toolbar );

        if ( checkPermissions() )
            beginApp();
        else
            getPermission();

//        registerReceivers();
//        checkPlayer();
//        obtainPlayerManager();
    }

    private void checkPlayer() {
        Intent broadcast = new Intent();
        broadcast.setAction(ON_CHECK_PLAYER_MANAGER);
        sendBroadcast(broadcast);
    }

    private void broadcast() {

    }

    private void registerReceivers() {
        Log.d("Player_Model", "Registering BroadCast for Player Model" );
        musicReceiver = new PlayerBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_START_PLAYER_MANAGER);
        registerReceiver(musicReceiver, filter);
    }

    private void unRegisterReceiver() {
        if (musicReceiver != null)
        {
            unregisterReceiver(musicReceiver);
            musicReceiver = null;
        }
    }

    private void beginApp()
    {
        initContent();
        navigation = findViewById(R.id.navigation);
        new ViewModelProvider(this).get(NavigationModel.class).showNavigation.observe(this, aBoolean -> {
            Log.d("Nav_Visibility", "the visibility is " + aBoolean);
            if (aBoolean)
                navigation.setVisibility(View.VISIBLE);
            else
                navigation.setVisibility(View.GONE);
        });

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        pager.setCurrentItem(AppPagerAdapter.defaultPage);
    }

    private void initContent() {
        adapter = new AppPagerAdapter(getSupportFragmentManager());
        pager = findViewById( R.id.layout_main );
        pager.setAdapter( adapter );
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;

//                FloatingActionButton floating = findViewById(R.id.action_floating);
//                floating.hide();
            }
        });
        backActions = new BackAction[adapter.getCount()];

    }

    private boolean isInDefaultNav()
    {
        return pager.getCurrentItem() == adapter.getDefaultTabPos();
    }

    private boolean isInDefaultTab()
    {
        if (isInDefaultNav())
        {
            ViewPager mViewPager = findViewById(R.id.main_content);
            return mViewPager.getCurrentItem() == MusicFragment.DEFAULT_TAB;
        }
        return false;
    }

    private boolean goToDefaultTab()
    {
        if (isInDefaultNav())
        {
            ViewPager mViewPager = findViewById(R.id.main_content);
            mViewPager.setCurrentItem(MusicFragment.DEFAULT_TAB, true);
            return true;
        }
        return false;
    }

    private boolean goToDefaultNav()
    {
        if (!isInDefaultNav())
        {
            navigation.setSelectedItemId(R.id.musicFragment);
//            pager.setCurrentItem(adapter.getDefaultTabPos(), true);
            return true;
        }
        return false;
    }

    private boolean doSelection(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.musicFragment:
                pager.setCurrentItem( 0 );
                return true;
            case R.id.videoFragment:
                pager.setCurrentItem( 1 );
                return true;
            default:
                return false;
        }
    }

//    private void prepareSearchMenu() {
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//
//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//
//        SearchView searchView = null;
//        if (searchItem != null) {
//            searchView = (SearchView) searchItem.getActionView();
//        }
//        if (searchView != null) {
//            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        }
//    }

    private boolean checkPermissions()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission()
    {
        Log.d("Selecting_music", "Getting Permissions is " + checkPermissions() );
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( requestCode == this.requestCode )
        {
            if ( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED
                    && grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED  )
                beginApp();
        }
    }

    public void registerBack(BackAction action) {
        backActions[pager.getCurrentItem()] = action;
    }

    public void unRegisterBack() {
        backActions[pager.getCurrentItem()] = null;
    }

    public interface BackAction {
        boolean goBack();
    }

    public static class AppPagerAdapter extends FragmentPagerAdapter {
        private static final int defaultPage = 0;
        private final Fragment[] host = {new Fragment(R.layout.music_host), new Fragment(R.layout.video_host)};
//        private BackActionManager[] managers = new BackActionManager[  ];

        public AppPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return host[position];
        }

        @Override
        public int getCount() {
            return 2;
        }

        public int getDefaultTabPos() {
            return 0;
        }
    }

    private class PlayerConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Player_Model", "Connected to player manager, manager " + service );
            Log.d("Player_Model", "Connected to player manager, Initializing PlayerModel" );
            binder = (PlayerService.PlayerBinder) service;
            PlayerManager playerManager = binder.getPlayerManager();
            if (playerManager != null && playerManager != getPlayerManager())
            {
                consumePlayer(playerManager);
                prepareController(playerManager);
//                if  ( playerManager.isMusicPlaying() )
//                    prepareController(playerManager);
//                if  (playerManager.isVideoPlaying() && playerManager.getVideoManager().isPlayingExternalSource() )
//                    new ViewModelProvider(MainActivity.this).get(PlayerModel.class).showVideoController(getSupportFragmentManager());
            }
            unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Player_Model", "Disconnected to Player Manager" );
        }
    }

    private void consumePlayer(PlayerManager playerManager) {
//        this.playerManager = playerManager;
        PlayerModel playerModel = new ViewModelProvider(MainActivity.this).get(PlayerModel.class);
        playerModel.setPlayerManager(playerManager);
    }

    private PlayerManager getPlayerManager()
    {
        PlayerModel playerModel = new ViewModelProvider(MainActivity.this).get(PlayerModel.class);
        return playerModel.getPlayerManager();
    }

    private void prepareController(PlayerManager playerManager) {
        controllerListener = new ControllerListener(playerManager);
        playerManager.addListener(controllerListener);
    }

    public class PlayerBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Player_Model", "Broadcast for Player Model as been received " + intent );
//            PlayerManager playerManager = binder.getPlayerManager();
//            consumePlayer(playerManager);
            processIntent(intent);
            unRegisterReceiver();
        }
    }

    private class ControllerListener implements Player.EventListener {

        public ControllerListener(PlayerManager playerManager) {
            Log.d("Player_Controllers", "Constructing Controller Listener");
//            if  (playerManager.isVideoPlaying() && (playerManager.getVideoManager().isPlayingExternalSource() ||
//                    (getIntent().hasExtra("controller_check") && getIntent().getBooleanExtra("controller_check", false))))
//                new ViewModelProvider(MainActivity.this).get(PlayerModel.class).showVideoController(getSupportFragmentManager());
//            if  (playerManager.isVideoPlaying() && getIntent().hasExtra("controller_check") && getIntent().getBooleanExtra("controller_check", false))

            new ViewModelProvider(MainActivity.this).get(PlayerModel.class).renderController(getSupportFragmentManager(), getIntent());
            if  (playerManager.isVideoPlaying() && (playerManager.getVideoManager().isPlayingExternalSource() ||
                    (getIntent().hasExtra("controller_check") && getIntent().getBooleanExtra("controller_check", false))))
                new ViewModelProvider(MainActivity.this).get(PlayerModel.class).showVideoController(getSupportFragmentManager());
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                if ( playing == MediaType.MUSIC && isMusicPlayer() )
//            Log.d("Player_Controllers2", String.format("player state whenReady %b and state %d", playWhenReady, playbackState));
            if ( playWhenReady )
                new ViewModelProvider(MainActivity.this).get(PlayerModel.class).renderMusicController(getSupportFragmentManager());
            if (playbackState == ExoPlayer.STATE_IDLE)
                new ViewModelProvider(MainActivity.this).get(PlayerModel.class).removeController(getSupportFragmentManager());
        }
    }

    public static class BackStackModel extends ViewModel
    {
        public MutableLiveData<Boolean> onStackPopped = new MutableLiveData<>(false);
    }

    public static class NavigationModel extends ViewModel
    {
        public MutableLiveData<Boolean> showNavigation = new MutableLiveData<>(true);
    }

//    public static class ConcurrentModel extends ViewModel
//    {
//        private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
//        private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
//
//        public <T> ListenableFuture<T> runInBackground(Callable<T> task)
//        {
//            return executorService.submit(task);
//        }
//
//        public void runInUI(Runnable task)
//        {
//            mainThreadHandler.post(task);
//        }
//
//        public ListeningExecutorService getExecutorService()
//        {
//            return executorService;
//        }
//    }

    public static class PlayerModel extends AndroidViewModel
    {
        private Fragment controller;
        private final static String MUSIC_TAG = "MUSIC_CONTROLLER";
        private final static String VIDEO_TAG = "VIDEO_CONTROLLER";
        private ArrayList<Music> pendingPlaylist;
        private Series pendingSeries;

        private void renderController(FragmentManager fragmentManager, Intent intent) {
            PlayerManager manager = playerManager.getValue();
            Log.d("Player_Controllers", "Rendering Controller Listener");
            Log.d("Player_Controllers", String.format("Render Music Controller playing %b rendered %b", manager.isMusicPlaying(), isMusicRender() ));
            if ( manager.isMusicPlaying() && !isMusicRender() ) {
                showMusicController(fragmentManager);
            }
//            Log.d("Player_Controllers", String.format("Render Video Controller playing %b rendered %b %b %b", manager.isVideoPlaying(), intent != null,
//                    intent.hasExtra("controller_check"), intent.getBooleanExtra("controller_check", false) ));
//            if  (manager.isVideoPlaying() && intent != null && intent.hasExtra("controller_check") && intent.getBooleanExtra("controller_check", false))
//            {
//                showVideoController(fragmentManager);
//            }
//            if ( manager.isVideoPlaying() && !manager.isInPlayingState() && !isVideoRender() ) {
//                showVideoController(fragmentManager);
//            }
        }

        private void renderMusicController(FragmentManager fragmentManager) {
            PlayerManager manager = playerManager.getValue();
            Log.d("Player_Controllers", "Rendering Music Controller Listener");
            Log.d("Player_Controllers", String.format("Render Music Controller playing %b rendered %b", manager.isMusicPlaying(), isMusicRender() ));
            if ( manager.isMusicPlaying() && !isMusicRender() ) {
                showMusicController(fragmentManager);
            }
        }

        public void showVideoController(FragmentManager fragmentManager) {
            PlayerManager manager = playerManager.getValue();
            Log.d("Player_Controllers", "Rendering Video Controller Listener");
            Log.d("Player_Controllers", String.format("Render Video Controller playing %b rendered %b", manager.isVideoPlaying(), isVideoRender() ));
            VideoController videoController = new VideoController(playerManager.getValue().getVideoManager());
            render(videoController, fragmentManager, VIDEO_TAG);
        }

        public void removeVideoController(FragmentManager fragmentManager) {
            Log.d("Player_Controllers", "Remove Video Controller" );
            Fragment videoController = fragmentManager.findFragmentByTag(VIDEO_TAG);
            detach(videoController, fragmentManager);
        }

        public void removeMusicController(FragmentManager fragmentManager) {
            Log.d("Player_Controllers", "Remove Music Controller" );
            Fragment videoController = fragmentManager.findFragmentByTag(MUSIC_TAG);
            detach(videoController, fragmentManager);
        }

        public void removeController(FragmentManager fragmentManager) {
            Log.d("Player_Controllers", "Remove Controller" );
            if ( controller != null)
                detach(controller, fragmentManager);
        }

        public boolean isMusicRender()
        {
            return controller != null && controller instanceof MusicController;
        }

        public boolean isVideoRender()
        {
            return controller != null && controller instanceof VideoController;
        }

        private void detach( Fragment fragment, FragmentManager fragmentManager )
        {
            fragmentManager.beginTransaction().remove(fragment).commit();
            controller = null;
        }

        private void render(Fragment fragment, FragmentManager fragmentManager, String tag)
        {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.controller_host, fragment, tag );
            fragmentTransaction.commit();
            controller = fragment;
        }

        private void detach(FragmentManager fragmentManager)
        {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(controller);
            fragmentTransaction.commit();
            controller = null;
        }

        public void showMusicController(FragmentManager fragmentManager) {
            Log.d("Player_Controllers", "Show Music Controller");
            MusicController musicController = new MusicController(playerManager.getValue().getMusicManager());
            render(musicController, fragmentManager, MUSIC_TAG);
        }

        public synchronized PlayerManager getPlayerManager() {
            return getLivePlayerManager().getValue();
        }

        public synchronized PlayerManager.MusicManager getMusicManager() {
            return getLivePlayerManager().getValue().getMusicManager();
        }

        public synchronized PlayerManager.VideoManager getVideoManager() {
            return getLivePlayerManager().getValue().getVideoManager();
        }

        public synchronized MutableLiveData<PlayerManager> getLivePlayerManager() {
            if (playerManager == null)
                playerManager = new MutableLiveData<>();
            return playerManager;
        }

        public synchronized void setPlayerManager(PlayerManager playerManager) {
            if (this.playerManager == null)
                this.playerManager = new MutableLiveData<>();
            playPendings(playerManager);
            this.playerManager.setValue(playerManager);
        }

        public synchronized PlayerManager.VideoManager getOrListenForVideoManager(LifecycleOwner owner, @NonNull Observer<PlayerManager> observer) {
            PlayerManager playerManager = getLivePlayerManager().getValue();
            if (playerManager != null && playerManager.getVideoManager() != null)
                return playerManager.getVideoManager();
            getLivePlayerManager().observe(owner, observer);
            return null;
        }

        private MutableLiveData<PlayerManager> playerManager;

        public PlayerModel(@NonNull Application application) {
            super(application);
        }

        public void play(ArrayList<Music> musics)
        {
            PlayerManager manager = getPlayerManager();
            Log.d("Player_Model", "The current manager " + manager );
            if (manager != null){
                manager.getMusicManager().playAll(musics);
                Log.d("Player_Model", "The manager " + manager );

                Intent intent = new Intent(getApplication().getBaseContext(), PlayerService.class);
                setPendingPlaylist(musics);
                intent.putExtra("type", "music" );
                getApplication().getBaseContext().startService(intent);
            }
            else
            {
                Log.d("Player_Model", "Manager don't exists" );
//                String[] gMusics = Music.toGson(musics);
                Intent intent = new Intent(getApplication().getBaseContext(), PlayerService.class);
//                intent.putExtra("music", gMusics );
                setPendingPlaylist(musics);
                intent.putExtra("broadcast", true );
                intent.putExtra("type", "music" );
                getApplication().getBaseContext().startService(intent);
            }
        }

        public void initSource(Series aSeries) {
            PlayerManager manager = getPlayerManager();
            if (manager != null)
            {
                manager.getVideoManager().initVideoSources(aSeries);

                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                intent.putExtra("type", "series" );
                getApplication().getBaseContext().startService( intent );
            }
            else
            {
                setPendingSeries(aSeries);
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                intent.putExtra("broadcast", true );
                intent.putExtra("type", "series" );
                getApplication().getBaseContext().startService( intent );
            }
        }

        private void setPendingSeries(Series aSeries) {
            pendingSeries = aSeries;
        }

        public Series getPendingSeries() {
            return pendingSeries;
        }

        public void clearPendingSeries() {
            pendingSeries = null;
        }

        private void setPendingPlaylist(ArrayList<Music> musics) {
            pendingPlaylist = musics;
        }

        public ArrayList<Music> getPendingPlaylist() {
            return pendingPlaylist;
        }

        public void clearPendingPlaylist() {
            pendingPlaylist = null;
        }

        public boolean pendingPlaylistExist() {
            return pendingPlaylist != null;
        }

        public boolean pendingSeriesExist() {
            return pendingSeries != null;
        }

        public void playPendings(PlayerManager playerManager)
        {
            if (pendingPlaylistExist()){
                playerManager.getMusicManager().playAll(getPendingPlaylist());
                clearPendingPlaylist();
            }
            if (pendingSeriesExist()){
                playerManager.getVideoManager().initVideoSources(getPendingSeries());
                clearPendingSeries();
            }
        }

        public void initSource() {
            if (getPlayerManager() == null)
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
    //                String[] gsonVideos = Video.toGson(videos);
    //                intent.putExtra( "video", gsonVideos );
                intent.putExtra( "type", "video" );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService( intent );
            }
            else
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                intent.putExtra( "type", "video" );
                getApplication().getBaseContext().startService( intent );
            }
        }

//        public void initSource(Video...videos) {
//            PlayerManager manager = getPlayerManager();
//            if (manager != null)
//                manager.getVideoManager().initVideoSources(videos);
//            else
//            {
//                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
//                String[] gsonVideos = Video.toGson(videos);
//                intent.putExtra( "video", gsonVideos );
//                intent.putExtra( "media_type", "video" );
//                intent.putExtra("broadcast", true );
//                getApplication().getBaseContext().startService( intent );
//            }
//        }

//        public void initSource(Video[] videos, int begin) {
//            PlayerManager manager = getPlayerManager();
//            if (manager != null)
//                manager.getVideoManager().initVideoSources(videos);
//            else
//            {
//                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
//                String[] gsonVideos = Video.toGson(videos);
//                intent.putExtra( "video", gsonVideos );
//                intent.putExtra( "begin", begin );
//                intent.putExtra("broadcast", true );
//                getApplication().getBaseContext().startService( intent );
//            }
//        }

//        public void initSource(String url)
//        {
//            PlayerManager manager = getPlayerManager();
//            if (manager != null)
//                manager.getVideoManager().initOnlineSources(url);
//            else
//            {
//                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
//                intent.putExtra( "url", url );
//                getApplication().getBaseContext().startService( intent );
//            }
//        }

        public void play(Music music) {
            play( new ArrayList< Music >(Collections.singletonList(music)) );
        }
    }
}
