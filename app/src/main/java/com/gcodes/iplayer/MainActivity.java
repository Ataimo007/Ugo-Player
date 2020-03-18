package com.gcodes.iplayer;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gcodes.iplayer.music.MusicFragment;
import com.gcodes.iplayer.music.album.AlbumFragment;
import com.gcodes.iplayer.music.artist.ArtistFragment;
import com.gcodes.iplayer.music.folder.FolderFragment;
import com.gcodes.iplayer.music.genre.GenreFragment;
import com.gcodes.iplayer.music.track.TrackFragment;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.AllFragment;
import com.gcodes.iplayer.video.SeriesFragment;
import com.gcodes.iplayer.video.VideoFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

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
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity
{
    private int requestCode = 0;
    private static AppCompatActivity application;
    Supplier<Boolean> action;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = this::doSelection;
    private Menu menu;
    private BackManager backAction;

    private TabLayout.ViewPagerOnTabSelectedListener mPagerListener;
    private TabLayout.TabLayoutOnPageChangeListener mTabListener;
    private MusicAdapter musicAdapter;
    private TabLayout musicTabs;
    private ViewPager pager;
    private TabLayout videoTabs;
    private VideoAdapter videoAdapter;
    private TabLayout.TabLayoutOnPageChangeListener vTabListener;
    private TabLayout.ViewPagerOnTabSelectedListener vPagerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        begin();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PlayerManager.getInstance().onDestroyActivity( this );
    }

    @Override
    public void onBackPressed() {
        if ( backAction != null )
        {
            boolean back = backAction.goBack();
            if ( !back )
                super.onBackPressed();
        }
        super.onBackPressed();
    }

    private void begin()
    {
        //        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main_default);

        Toolbar toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar( toolbar );

        initContent();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        init();
        application = this;

        navigation.setSelectedItemId( R.id.navigation_music );

    }

    private void initContent() {
        musicTabs = findViewById(R.id.music_tabs);
        pager = findViewById( R.id.content_viewpager );
        musicAdapter = new MusicAdapter(getSupportFragmentManager());
        mTabListener = new TabLayout.TabLayoutOnPageChangeListener(musicTabs);
        mPagerListener = new TabLayout.ViewPagerOnTabSelectedListener(pager);

        videoTabs = findViewById(R.id.video_tabs);
        videoAdapter = new VideoAdapter(getSupportFragmentManager());
        vTabListener = new TabLayout.TabLayoutOnPageChangeListener(musicTabs);
        vPagerListener = new TabLayout.ViewPagerOnTabSelectedListener(pager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPermission();

        PlayerManager.getInstance().onNewActivity( this );
    }

    private boolean doSelection(MenuItem item)
    {
        if ( checkPermissions() )
            return doSelection( item.getItemId() );
        return false;
    }

    private boolean doSelection( int id )
    {
        switch (id) {
            case R.id.navigation_music:
//                Log.d("Selecting_music", "Selected Music" );
//                MusicFragment.InitFragments( getSupportFragmentManager() );
                musicSwitch();
                return true;
            case R.id.navigation_video:
//                VideoFragment.InitFragments( getSupportFragmentManager() );
                videoSwitch();
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
        // Here, thisActivity is the current activity
        if ( !checkPermissions() ) {
            Log.d("Selecting_music", "Getting Permissions is " + checkPermissions() );
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode );
        }
    }

    private void init()
    {
        initPlayer();
    }

    private void initPlayer() {
        PlayerManager.init( this );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( requestCode == this.requestCode )
        {
//            for ( String permission : permissions )
//            {
//                if ( ContextCompat.checkSelfPermission(this, permission)
//                        != PackageManager.PERMISSION_GRANTED )
//                    return;
//            }
            if ( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED
                    && grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED  )
                doSelection(R.id.navigation_music);
//            begin();

        }
    }

    public void register(BackManager action) {
        backAction = action;
    }

    public void unregister() {
        backAction = null;
    }

    public interface BackManager {
        boolean goBack();
    }

    public void musicSwitch()
    {
        videoTabs.setVisibility(View.GONE);
        musicTabs.setVisibility(View.VISIBLE);

        pager.setAdapter(musicAdapter);
        pager.addOnPageChangeListener( mTabListener );
        musicTabs.addOnTabSelectedListener( mPagerListener );
        pager.invalidate();
    }

    public void videoSwitch()
    {
        musicTabs.setVisibility(View.GONE);
        videoTabs.setVisibility(View.VISIBLE);

        pager.setAdapter(videoAdapter);
        pager.addOnPageChangeListener( vTabListener );
        musicTabs.addOnTabSelectedListener( vPagerListener );
        pager.invalidate();
    }

    public static class MusicAdapter extends FragmentPagerAdapter
    {
        private Fragment[] views;

        public MusicAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            init();
        }

        public void init()
        {
            views = new Fragment[ 5 ];
            views[ 0 ] = new AlbumFragment();
            views[ 1 ] = new ArtistFragment();
            views[ 2 ] = new TrackFragment();
            views[ 3 ] = new GenreFragment();
            views[ 4 ] = new FolderFragment();
        }

        @Override
        public Fragment getItem(int position)
        {
            return views[ position ];
        }

        @Override
        public int getCount() {
            return 5;
        }

        public int getDefaultTabPos() {
            return 2;
        }
    }

    public static class VideoAdapter extends FragmentPagerAdapter
    {
        private Fragment[] views;

        public VideoAdapter(FragmentManager fm) {
            super(fm);
            init();
        }

        public void init()
        {
            views = new Fragment[ 4 ];
            views[ 0 ] = new Fragment();
            views[ 1 ] = new com.gcodes.iplayer.video.FolderFragment();
            views[ 2 ] = new SeriesFragment();
            views[ 3 ] = new AllFragment();
        }

        @Override
        public Fragment getItem(int position)
        {
            return views[ position ];
        }

        @Override
        public int getCount() {
            return 4;
        }

        public int getDefaultTabPos() {
            return 2;
        }
    }

}
