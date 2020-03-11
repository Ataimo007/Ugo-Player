package com.gcodes.iplayer.music.player;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicPlayerActivity extends AppCompatActivity{

    private PlayerView playerView;
    private Menu menu;
    private Toolbar toolbar;
    private int currentTrack = -1;
    private Music currentMusic;
    private Player.EventListener trackListener;
    private PlayerDatabase.MusicInfo musicInfo;
    private PlayerControlView control;
    private TextView musicName;
    private TextView artistName;
    private ImageView image;
    private SectionsPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music_player);

        pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        initView();
    }

    private void prepareMusicPlayer()
    {
        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
        videoScreen.setVisibility( View.GONE );
        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
        coverArt.setVisibility(View.VISIBLE);
    }

    private void prepareVideoPlayer()
    {
        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
        videoScreen.setVisibility( View.VISIBLE );
        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
        coverArt.setVisibility(View.GONE);
    }

    private void initView()
    {
        TabLayout tabs = findViewById( R.id.player_tabs );
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
    }


    public void showLyrics(View view)
    {
        LyricsFragment.show( getSupportFragmentManager(), currentMusic, musicInfo );
    }

    public void showMusicVideo(View view)
    {
//        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic );
        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic, musicInfo, t -> prepareVideoPlayer(),
                t1 -> prepareMusicPlayer() );
    }

    private void recognizeMusic( Music music )
    {
        Log.d( "ACR_Service", "Getting musis info ..." );
        Helper.Worker.executeTask(() -> {



            return () -> {};

        });
    }

    private void update() {

    }

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
        private Fragment[] views;
        private final FragmentManager fm;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fm = fm;
            init();
        }

        public void init()
        {
            views = new Fragment[ 3 ];
            views[ 0 ] = new MusicVideoFragment();
            views[ 1 ] = new MusicPlayerFragment();
            views[ 2 ] = new PlaylistFragment();
        }

        @Override
        public Fragment getItem(int position)
        {
//            setToolbar( position );
            return views[ position ];
        }

        @Override
        public int getCount() {
            return 3;
        }

        public int getDefaultTabPos() {
            return 1;
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
