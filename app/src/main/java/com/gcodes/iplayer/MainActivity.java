package com.gcodes.iplayer;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.video.player.VideoPlayer;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
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
    private NavigationPager pager;
    private AppPagerAdapter adapter;
    private int currentPage;

//    public static final int REQUEST_VIDEO = 1000;


//    private MusicFragment music;
//    private VideoFragment video;

//    private TabLayout.ViewPagerOnTabSelectedListener mPagerListener;
//    private TabLayout.TabLayoutOnPageChangeListener mTabListener;
//    private TabLayout musicTabs;
//    private TabLayout videoTabs;
//    private TabLayout.TabLayoutOnPageChangeListener vTabListener;
//    private TabLayout.ViewPagerOnTabSelectedListener vPagerListener;

//    private MusicAdapter musicAdapter;
//    private VideoAdapter videoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        begin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlayerManager.getInstance().onDestroyActivity();
        Log.w("Destroy_Main", "Destroying Main Activity" );
    }

    @Override
    protected void onStop() {
        super.onStop();
//        PlayerManager.getInstance().onDestroyActivity( this );
    }

//    @Override
//    public void onBackPressed() {
//        boolean popped = NavHostFragment.findNavController(adapter.getItem(currentPage)).popBackStack();
//        if ( !popped )
//        {
//            if ( backAction != null )
//            {
//                boolean back = backAction.goBack();
//                if ( !back )
//                    super.onBackPressed();
//            }
//            else
//                super.onBackPressed();
//        }
//    }

    @Override
    public void onBackPressed() {
        if ( backAction != null )
        {
            boolean back = backAction.goBack();
            if ( !back )
                back();
        }
        else
            back();
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
        //        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main_default4);

        Toolbar toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar( toolbar );

        init();
        application = this;

        if ( checkPermissions() )
            beginApp();
        else
            getPermission();

//        navigation.setSelectedItemId( R.id.navigation_music );
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
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//    }

//    private boolean doSelection(MenuItem item)
//    {
//        if ( checkPermissions() )
//            return doSelection( item.getItemId() );
//        return false;
//    }

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

//    private boolean doSelection( int id )
//    {
//
//    }

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
//        if ( !checkPermissions() ) {
//            Log.d("Selecting_music", "Getting Permissions is " + checkPermissions() );
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    requestCode );
//        }

        Log.d("Selecting_music", "Getting Permissions is " + checkPermissions() );
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode );
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
                beginApp();
        }
    }

//    @Override
//    protected void onPostResume() {
//        super.onPostResume();
//        VideoPlayer.getInstance().tryRenderVideoPlayer();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("Video_Player", "Rendering Video Controller");
//        VideoPlayer.getInstance().tryRenderVideoPlayer();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if ( requestCode == REQUEST_VIDEO)
//        {
//            if ( resultCode == VideoPlayer.RESULT_PLAYING )
//            {
////                VideoPlayer.getInstance().renderVideoPlayer();
//            }
//        }
//    }

    public void register(BackManager action) {
        backAction = action;
    }

    public void unregister() {
        backAction = null;
    }

    public interface BackManager {
        boolean goBack();
    }

    public static class AppPagerAdapter extends FragmentPagerAdapter
    {
        private static final int defaultPage = 0;
        private final Fragment[] host = {new Fragment(R.layout.music_host), new Fragment(R.layout.video_host)};

        public AppPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position)
        {
            return host[ position ];
        }

        @Override
        public int getCount() {
            return 2;
        }

        public int getDefaultTabPos() {
            return 0;
        }
    }

//    public void musicSwitch()
//    {
//        videoTabs.setVisibility(View.GONE);
//        musicTabs.setVisibility(View.VISIBLE);
//
//        pager.setAdapter(musicAdapter);
//        pager.addOnPageChangeListener( mTabListener );
//        musicTabs.addOnTabSelectedListener( mPagerListener );
//        pager.invalidate();
//    }
//
//    public void videoSwitch()
//    {
//        musicTabs.setVisibility(View.GONE);
//        videoTabs.setVisibility(View.VISIBLE);
//
//        pager.setAdapter(videoAdapter);
//        pager.addOnPageChangeListener( vTabListener );
//        musicTabs.addOnTabSelectedListener( vPagerListener );
//        pager.invalidate();
//    }
//
//    public static class MusicAdapter extends FragmentPagerAdapter
//    {
//        private Fragment[] views;
//
//        public MusicAdapter(FragmentManager fm)
//        {
//            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
//            init();
//        }
//
//        public void init()
//        {
//            views = new Fragment[ 5 ];
//            views[ 0 ] = new AlbumFragment();
//            views[ 1 ] = new ArtistFragment();
//            views[ 2 ] = new TrackFragment();
//            views[ 3 ] = new GenreFragment();
//            views[ 4 ] = new FolderFragment();
//        }
//
//        @Override
//        public Fragment getItem(int position)
//        {
//            return views[ position ];
//        }
//
//        @Override
//        public int getCount() {
//            return 5;
//        }
//
//        public int getDefaultTabPos() {
//            return 2;
//        }
//    }
//
//    public static class VideoAdapter extends FragmentPagerAdapter
//    {
//        private Fragment[] views;
//
//        public VideoAdapter(FragmentManager fm) {
//            super(fm);
//            init();
//        }
//
//        public void init()
//        {
//            views = new Fragment[ 4 ];
//            views[ 0 ] = new Fragment();
//            views[ 1 ] = new com.gcodes.iplayer.video.folder.FolderFragment();
//            views[ 2 ] = new SeriesFragment();
//            views[ 3 ] = new AllFragment();
//        }
//
//        @Override
//        public Fragment getItem(int position)
//        {
//            return views[ position ];
//        }
//
//        @Override
//        public int getCount() {
//            return 4;
//        }
//
//        public int getDefaultTabPos() {
//            return 2;
//        }
//    }

}
