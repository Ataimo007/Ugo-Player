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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.player.MusicController;
import com.gcodes.iplayer.player.PlayerService;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.player.VideoController;
import com.google.android.exoplayer2.Player;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
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
    private int requestCode = 0;
//    private static AppCompatActivity application;
    Supplier<Boolean> action;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = this::doSelection;
    private NavigationPager pager;
    private AppPagerAdapter adapter;
    private int currentPage;

    private BackAction[] backActions;
    private MusicBroadCastReceiver musicReceiver;
    private ControllerListener controllerListener;
    private MusicConnection musicConnection;
    private PlayerService.PlayerBinder binder;
    private PlayerManager playerManager;

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
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        Log.d("Handling_Intent", "On New Intent" );
//        super.onNewIntent(intent);
//        Log.d("Handling_Karaoke", "Handling Karaoke Event " + intent.getExtras());
//        if (intent.getAction() != null && intent.getAction().equals(PLAY_KARAOKE))
//            handleKaraoke(intent);
//    }

//    private void handleKaraoke(Intent result)
//    {
//        Intent intent = new Intent( this, MusicPlayerActivity.class );
//        intent.putExtra("music", result.getStringExtra("music"));
//        intent.putExtra("output", result.getStringExtra("output"));
//        Log.d("Play_Karaoke", String.format("Play Karaoke music %s file %s", result.getStringExtra("music"), result.getStringExtra("output")));
//        intent.setAction(result.getAction());
//        startActivity( intent );
//    }

    //    private void testSubtitle()
//    {
//        new Thread(() -> {
//            URL serverUrl = null;
//            try {
//                OkHttpClient client = CFMobile.createOkHttp3Client();
////                OkHttpClient.Builder builder = new OkHttpClient.Builder()
////                        .addInterceptor(new HttpLoggingInterceptor())
////                        .cache(new Cache(cacheDir, cacheSize));
////                OkHttpClient client = CFMobile.createOkHttp3Client(builder);
//
//                Request request = new Request.Builder()
//                        .url("https://api.opensubtitles.org/xml-rpc")
//                        .build();
//
//                WebView webView = getWebView();
//                webView.loadDataWithBaseURL();
//
//                try (Response response = client.newCall(request).execute()) {
//                    String html = Base64.encodeToString(response.body().string().getBytes(), Base64.NO_PADDING);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }).start();
//    }

//    private WebView getWebView()
//    {
//        WebView webView = new WebView(this);
//        webView.getSettings().setJavaScriptEnabled(true);
//        WebViewClient client = new WebViewClient() {
//            @Override
//            public void onPageStarted(WebView view, String url, Bitmap favicon) {
//                super.onPageStarted(view, url, favicon);
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                super.onPageFinished(view, url);
//            }
//
//            @Override
//            public void onLoadResource(WebView view, String url) {
//                super.onLoadResource(view, url);
//            }
//
//            @Nullable
//            @Override
//            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                return super.shouldInterceptRequest(view, request);
//            }
//        };
//
//        webView.setWebViewClient(client);
//        return webView;
//    }

//    private void testSubtitle()
//    {
//        new Thread(() -> {
//            URL serverUrl = null;
//            try {
//                serverUrl = new URL("https", "api.opensubtitles.org", 443, "/xml-rpc");
//                OpenSubtitlesClient osClient = new OpenSubtitlesClientImpl(serverUrl);
//
//                Log.d("Player_Subtitle", "Subtitle Status " + osClient.serverInfo());
//
//                // logging in
//                Response response = osClient.login("username", "password", "en", "TemporaryUserAgent");
//
//                // checking login status
//                Log.d("Player_Subtitle", "Subtitle Login " + response.getStatus());
//                Log.d("Player_Subtitle", "Subtitle Login " + osClient.isLoggedIn());
//
//                // searching by string query + season/episode
//                ListResponse<SubtitleInfo> resp = osClient.searchSubtitles("eng", "Friends", "1", "1");
//                List<SubtitleInfo> subtitles = resp.getData();
//                Log.d("Player_Subtitle", "Subtitle Search " + subtitles);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }).start();
//    }

//    private void testSubtitle()
//    {
//        new Thread(() -> {
//            OkHttpClient client = new OkHttpClient.Builder()
//                    .cookieJar(new CookieJar() {
//                        private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();
//
//                        @Override
//                        public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
//                            cookieStore.put(url, cookies);
//                        }
//
//                        @NonNull
//                        @Override
//                        public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
//                            List<Cookie> cookies = cookieStore.get(url);
//                            return cookies != null ? cookies : new ArrayList<Cookie>();
//                        }
//                    }).followRedirects(true).followSslRedirects(true)
//                    .build();
//
////            OkHttpClient client = new OkHttpClient();
//
//            Request request = new Request.Builder()
//                    .url("https://api.opensubtitles.org/xml-rpc")
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                Log.d( "Player_Subtitle", response.body().string() );
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }

    @Override
    protected void onDestroy() {
        Log.d("Handling_Intent", "On Destroy" );
        releasePlayerManager();
        super.onDestroy();
    }

//    private void processIntent() {
//        Intent intent = getIntent();
//        processIntent(intent);
//    }

    private void processIntent(Intent intent) {
        Log.d("Player_Model", "Processing Broadcast from Player Service" );
        if  (intent.getBooleanExtra("has_player", false ))
            obtainPlayerManager();
    }

    private void obtainPlayerManager() {
        Log.d("Player_Model", "Obtaining Player Manager from Player Service" );
        Intent intent = new Intent(this, PlayerService.class);
        musicConnection = new MusicConnection();
        bindService(intent, musicConnection, BIND_IMPORTANT);
    }

    private void releasePlayerManager() {
        Log.d("Player_Model", "release Player Manager from Player Service" );
        playerManager.removeListener(controllerListener);
        unbindService(musicConnection);
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
            super.onBackPressed();
        }
        new ViewModelProvider(this).get(BackStackModel.class).onStackPopped.setValue(false);
    }

    private void begin()
    {
        setContentView(R.layout.activity_main_default4);

        Toolbar toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar( toolbar );

        if ( checkPermissions() )
            beginApp();
        else
            getPermission();

        registerReceivers();
        checkPlayer();
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
        musicReceiver = new MusicBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.ON_START_PLAYER_MANAGER);
        registerReceiver(musicReceiver, filter);
//        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);
    }

    private void unRegisterReceiver() {
        unregisterReceiver(musicReceiver);
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
    }

    private void beginApp()
    {
        initContent();

        BottomNavigationView navigation = findViewById(R.id.navigation);

//        NavController navController = Navigation.findNavController(this, R.id.app_internal_nav);
//        NavigationUI.setupWithNavController( navigation, navController );

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
            }
        });
        backActions = new BackAction[adapter.getCount()];
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

    private class MusicConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Player_Model", "Connected to player manager, manager " + service );
            Log.d("Player_Model", "Connected to player manager, Initializing PlayerModel" );
            binder = (PlayerService.PlayerBinder) service;
            PlayerManager playerManager = binder.getPlayerManager();
            if (playerManager != null)
                consumePlayer(playerManager);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Player_Model", "Disconnected to Player Manager" );
        }
    }

    private void consumePlayer(PlayerManager playerManager) {
        this.playerManager = playerManager;
        PlayerModel playerModel = new ViewModelProvider(MainActivity.this).get(PlayerModel.class);
        playerModel.setPlayerManager(playerManager);
        prepareController(playerManager);
    }

    private void prepareController(PlayerManager playerManager) {
        controllerListener = new ControllerListener();
        playerManager.addListener(controllerListener);
    }

    public class MusicBroadCastReceiver extends BroadcastReceiver
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

        public ControllerListener() {
            Log.d("Player_Controllers", "Constructing Controller Listener");
            new ViewModelProvider(MainActivity.this).get(PlayerModel.class).renderController(getSupportFragmentManager());
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                if ( playing == MediaType.MUSIC && isMusicPlayer() )
            if ( playWhenReady )
            {
                new ViewModelProvider(MainActivity.this).get(PlayerModel.class).renderMusicController(getSupportFragmentManager());
            }
        }
    }

    public static class BackStackModel extends ViewModel
    {
        public MutableLiveData<Boolean> onStackPopped = new MutableLiveData<>(false);
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

        private void renderController(FragmentManager fragmentManager) {
            PlayerManager manager = playerManager.getValue();
            Log.d("Player_Controllers", "Rendering Controller Listener");
            Log.d("Player_Controllers", String.format("Render Music Controller playing %b rendered %b", manager.isMusicPlaying(), isMusicRender() ));
            if ( manager.isMusicPlaying() && !isMusicRender() ) {
                showMusicController(fragmentManager);
            }
            if ( manager.isVideoPlaying() && !isVideoRender() ) {
                showVideoController(fragmentManager);
            }
        }

        private void renderMusicController(FragmentManager fragmentManager) {
            PlayerManager manager = playerManager.getValue();
            Log.d("Player_Controllers", "Rendering Controller Listener");
            Log.d("Player_Controllers", String.format("Render Music Controller playing %b rendered %b", manager.isMusicPlaying(), isMusicRender() ));
            if ( manager.isMusicPlaying() && !isMusicRender() ) {
                showMusicController(fragmentManager);
            }
        }

        public void showVideoController(FragmentManager fragmentManager) {
            VideoController videoController = new VideoController(playerManager.getValue().getVideoManager());
            render(videoController, fragmentManager, VIDEO_TAG);
        }

        public void removeVideoController(FragmentManager fragmentManager) {
            Fragment videoController = fragmentManager.findFragmentByTag(VIDEO_TAG);
            detach(videoController, fragmentManager);
        }

        public void removeMusicController(FragmentManager fragmentManager) {
            Fragment videoController = fragmentManager.findFragmentByTag(VIDEO_TAG);
            detach(videoController, fragmentManager);
        }

        public void removeController(FragmentManager fragmentManager) {
            if ( controller != null)
                detach(controller, fragmentManager);
        }

        private boolean isMusicRender()
        {
            return controller != null && controller instanceof MusicController;
        }

        private boolean isVideoRender()
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
            }
            else
            {
                Log.d("Player_Model", "Manager don't exists" );
                String[] gMusics = Music.toGson(musics);
                Intent intent = new Intent(getApplication().getBaseContext(), PlayerService.class);
                intent.putExtra("music", gMusics );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService(intent);
            }
        }

        public void initSource(Video...videos) {
            PlayerManager manager = getPlayerManager();
            if (manager != null)
                manager.getVideoManager().initVideoSources(videos);
            else
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                String[] gsonVideos = Video.toGson(videos);
                intent.putExtra( "video", gsonVideos );
                intent.putExtra( "media_type", "video" );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService( intent );
            }
        }

        public void initSource(Video[] videos, int begin) {
            PlayerManager manager = getPlayerManager();
            if (manager != null)
                manager.getVideoManager().initVideoSources(videos);
            else
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                String[] gsonVideos = Video.toGson(videos);
                intent.putExtra( "video", gsonVideos );
                intent.putExtra( "begin", begin );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService( intent );
            }
        }

        public void initSource(Series aSeries) {
            PlayerManager manager = getPlayerManager();
            if (manager != null)
                manager.getVideoManager().initVideoSources(aSeries);
            else
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                String series = aSeries.toGson();
                intent.putExtra( "series", series );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService( intent );
            }
        }

        public void initSource(String url)
        {
            PlayerManager manager = getPlayerManager();
            if (manager != null)
                manager.getVideoManager().initOnlineSources(url);
            else
            {
                Intent intent = new Intent( getApplication().getBaseContext(), PlayerService.class );
                intent.putExtra( "url", url );
                getApplication().getBaseContext().startService( intent );
            }
        }

        public void play(Music music) {
            play( new ArrayList< Music >(Collections.singletonList(music)) );
        }
    }
}
