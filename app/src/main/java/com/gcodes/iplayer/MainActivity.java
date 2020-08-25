package com.gcodes.iplayer;

import android.Manifest;
import android.app.Application;
import android.app.SearchManager;
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
import android.view.Menu;
import android.view.MenuItem;

import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.player.MusicPlayerService;
import com.gcodes.iplayer.player.PlayerManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity
{
    private int requestCode = 0;
//    private static AppCompatActivity application;
    Supplier<Boolean> action;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = this::doSelection;
    private Menu menu;
    private NavigationPager pager;
    private AppPagerAdapter adapter;
    private int currentPage;

    private BackAction[] backActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        begin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        processIntent();
    }

    private void processIntent() {
        Intent intent = getIntent();
        if  (intent.hasExtra("playing") && intent.getBooleanExtra("has_player", false ))
            obtainPlayerManager();
    }

    private void obtainPlayerManager() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        MusicConnection musicConnection = new MusicConnection();
        bindService(intent, musicConnection, BIND_IMPORTANT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        PlayerManager.getInstance().onDestroyActivity();
        Log.w("Destroy_Main", "Destroying Main Activity" );
    }

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
        if ( !popped )
        {
            super.onBackPressed();
        }
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
    }

    private void registerReceivers() {
        MusicPlayerService.MusicBroadCastReceiver musicReceiver = new MusicPlayerService.MusicBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlayerService.ON_START_PLAYER_MANAGER);
        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);
    }

    private void unRegisterReceiver() {
        MusicPlayerService.MusicBroadCastReceiver musicReceiver = new MusicPlayerService.MusicBroadCastReceiver();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
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

    private void prepareSearchMenu() {
        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
    }

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
            PlayerModel playerModel = new ViewModelProvider(MainActivity.this).get(PlayerModel.class);
            MusicPlayerService.PlayerBinder binder = (MusicPlayerService.PlayerBinder) service;
            PlayerManager playerManager = binder.getPlayerManager();
            if (playerManager != null)
                playerModel.setPlayerManager(playerManager);
            unRegisterReceiver();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            PlayerModel playerModel = new ViewModelProvider(MainActivity.this).get(PlayerModel.class);
            playerModel.setPlayerManager(null);
        }
    }

    public static class PlayerModel extends AndroidViewModel
    {
        public PlayerManager getPlayerManager() {
            return playerManager.getValue();
        }

        public PlayerManager getLivePlayerManager() {
            return playerManager.getValue();
        }

        public void setPlayerManager(PlayerManager playerManager) {
            this.playerManager.setValue(playerManager);
        }

        public MutableLiveData<PlayerManager> playerManager;

        private PlayerModel(@NonNull Application application) {
            super(application);
            playerManager = new MutableLiveData<>();
        }

        public void play(ArrayList<Music> musics)
        {
            PlayerManager manager = playerManager.getValue();
            if (manager == null)
                manager.getMusicManager().playAll(musics);
            else
            {
                String[] gMusics = Music.toGson(musics);
                Intent intent = new Intent(getApplication().getBaseContext(), MusicPlayerService.class);
                intent.putExtra("music", gMusics );
                intent.putExtra("broadcast", true );
                getApplication().getBaseContext().startService(intent);
            }
        }

        public void play(Music music) {
            play( new ArrayList< Music >(Collections.singletonList(music)) );
        }
    }
}
