package com.gcodes.iplayer.music.player;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.tabs.TabLayout;
import com.google.api.services.youtube.model.Video;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicPlayerActivity extends AppCompatActivity{

    private Music currentMusic;
    private Player.EventListener trackListener;
    private PlayerDatabase.MusicInfo musicInfo;
    private PlayerControlView control;
    private TextView musicName;
    private TextView artistName;
    private SectionsPagerAdapter pagerAdapter;
    private PlayerDatabase manager;

    private ImageView background;
    private final Handler handler = new Handler();
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music_player);
        manager = PlayerDatabase.getInstance();

        pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        initView();
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
        TabLayout tabs = getTabs();
        ViewPager pager = findViewById(R.id.player_pager);
        pager.setAdapter( pagerAdapter );
        pager.addOnPageChangeListener( new TabLayout.TabLayoutOnPageChangeListener( tabs ) );
        tabs.addOnTabSelectedListener( new TabLayout.ViewPagerOnTabSelectedListener( pager ) );

        tabs.getTabAt( pagerAdapter.getDefaultTabPos() ).select();
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

    @Override
    protected void onStart() {
        super.onStart();
        manageMusic();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ( trackListener != null )
        {
            MusicPlayer.unRegisterOnTrackChange( trackListener );
            trackListener = null;
        }
    }

    private void manageMusic() {
        trackListener = MusicPlayer.registerOnTrackChange(this::consumeTrack);
        MusicPlayer.consumeTrack( this::consumeTrack );
    }

    private final int NETWORKWATCHER = 1000;
    private boolean watching = false;
    private final Runnable runnable = new Runnable() {
        int count = 0;
        boolean flag = true;
        @Override
        public void run() {

            boolean deviceOnline = Helper.isDeviceOnline( MusicPlayerActivity.this );
            Log.d("Music_Player_Service", "is the device online " + deviceOnline );
            if (  deviceOnline )
            {
                retrieveInfoOnline( currentMusic );
            }
            else
            {
                handler.postDelayed( runnable, NETWORKWATCHER );
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks( runnable );
    }

    private void retrieveInfo(Music music)
    {
        boolean deviceOnline = Helper.isDeviceOnline(this);
        Log.d("Music_Player_Service", "is the device online " + deviceOnline );
        if (  !deviceOnline && !watching )
        {
            handler.postDelayed( runnable, NETWORKWATCHER );
            watching = true;
        }
        retrieveInfoOnline( music );
    }

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

    private void retrieveInfoOnline(Music music)
    {
        Helper.Worker.executeTask(() -> {
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

            final PlayerDatabase.MusicLyrics finalLyrics = lyrics;
            final List<Video> finalMusicVideo = musicVideo;
            return () -> {
                if ( info != null )
                {
                    pagerAdapter.updatePlayer( info, finalLyrics );
                }
                else
                {
                    pagerAdapter.updatePlayer(finalLyrics);
                }
                pagerAdapter.updateMusicVideos( finalMusicVideo );
            };
        });
    }

    private void consumeTrack( Music music )
    {
        updatePlayer( music );
        pagerAdapter.updatePlayer( music );
        pagerAdapter.updatePlaylist( music );
        pagerAdapter.updateMusicVideos( music );
        pagerAdapter.onLoading();

        retrieveInfo( music );
        currentMusic = music;
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

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private MusicVideoFragment musicVideo;
        private MusicPlayerFragment player;
        private PlaylistFragment playlist;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            init();
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
            musicVideo = new MusicVideoFragment();
            player = new MusicPlayerFragment();
            playlist = new PlaylistFragment();
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
