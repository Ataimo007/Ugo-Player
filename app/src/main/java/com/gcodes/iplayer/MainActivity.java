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

import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.MusicFragment;
import com.gcodes.iplayer.music.player.FragmentMusic;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.services.YouTubeService;
import com.gcodes.iplayer.video.VideoFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.services.youtube.model.Video;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.subtitle.Subtitle;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity
{
    private int requestCode = 0;
    private static AppCompatActivity application;
    Supplier<Boolean> action;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = this::doSelection;
    private Menu menu;
    private BackManager backAction;

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

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        init();
        application = this;

        navigation.setSelectedItemId( R.id.navigation_music );
//        doSelection(item);

//        testYoutube();
//        getLyrics();
//        test();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPermission();

        PlayerManager.getInstance().onNewActivity( this );
    }

    private void start()
    {

        //        navigation.setSelectedItemId( R.id.navigation_music );
//        navigation.setSelectedItemId( R.id.navigation_music );
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        this.menu = menu;
//        getMenuInflater().inflate(R.menu.main_menu, menu);
//        prepareSearchMenu();
////        return super.onCreateOptionsMenu(menu);
//        return true;
//    }

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
                Log.d("Selecting_music", "Selected Music" );
                MusicFragment.InitFragments( getSupportFragmentManager() );
                return true;
            case R.id.navigation_video:
                VideoFragment.InitFragments( getSupportFragmentManager() );
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
//        initACR();
    }

    private void initPlayer() {
        PlayerManager.init( this );
    }

    private void initACR()
    {
//        ACRFileService.initialize( this );

//        ACRService.initialize( this );

//        try {
//            ACRFileService.getInstance().recognizeMusic( Uri.parse( "content://media/external/audio/media/12377" ) );
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String path = Environment.getExternalStorageDirectory().toString()
//                + "/acrcloud/model";
//
//        File file = new File(path);
//        if(!file.exists()){
//            file.mkdirs();
//        }
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

    public static void renderSession( Fragment fragment )
    {
        FragmentManager manager = application.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
//        Fragment layout = manager.findFragmentById(R.id.layout_main);
//        transaction.remove( layout );
//        transaction.add( R.id.layout_main, fragment);
        transaction.replace( R.id.layout_main, fragment);
        transaction.commitNowAllowingStateLoss();
    }

    public static void setToolbar(Toolbar toolbar)
    {
//        ActionBar supportActionBar = application.getSupportActionBar();
        application.setSupportActionBar( toolbar );
    }

//    public static void renderSession( Fragment fragment )
//    {
//        FragmentManager manager = application.getSupportFragmentManager();
//        FragmentTransaction transaction = manager.beginTransaction();
//        View musicControl = application.findViewById(R.id.application_session);
//        transaction.add( R.id.application_session, fragment);
//        transaction.commit();
//    }

    public static void renderPlayer()
    {
        PlayerManager playerManager = PlayerManager.getInstance();
        FragmentManager manager = application.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FragmentMusic fragmentMusic = FragmentMusic.newInstance();
        View musicControl = playerManager.getActivity().findViewById(R.id.music_control);
        musicControl.setVisibility( View.VISIBLE );
        transaction.replace( R.id.music_control, fragmentMusic);
        transaction.commit();
    }

    public void testYoutube()
    {
        YouTubeService youtube = YouTubeService.getIntance();
        Helper.Worker.executeTask(() -> {
            try {
                List<Video> videos = youtube.getVideos("Under The Canopy Frank Edwards");
                Log.d( "YouTube_API", "Videos " + videos );
            } catch (IOException e) {
                Log.d( "YouTube_API", "Error in getting videos" );
                e.printStackTrace();
            }
            return () -> {};
        });
    }

    private void getLyrics()
    {
        Helper.Worker.executeTask(() -> {
            Log.d( "Music_Player", "getting lyrics" );
            try {
                String apiKey = "7d07b4f39f7722d3cd1f52cd45da73a2";
                MusixMatch musixMatch = new MusixMatch(apiKey);
                Track track = musixMatch.getTrack(124734511);
                Subtitle subtitle = musixMatch.getSubtitle(124734511);
                String subtitleBody = subtitle.getSubtitleBody();
                Lyrics lyrics = musixMatch.getLyrics(124734511);
//                Snippet snippet = musixMatch.getSnippet(124734511);
//                snippet.setUpdatedTime( "20000" );
//                String snippetBody = snippet.getSnippetBody();
                String lyricsBody = lyrics.getLyricsBody();
                Log.d( "Music_Player", "lyrics body " + lyricsBody );
//                Log.d( "Music_Player", "snippet body - " + snippetBody );
                Log.d( "Music_Player", "snippet body - " + subtitleBody );
            } catch (MusixMatchException e) {
                Log.d( "Music_Player", "lyrics error cause by " + e.getMessage() );
                e.printStackTrace();
            }
            return () -> {};
        });
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


}
