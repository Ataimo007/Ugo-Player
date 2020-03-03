package com.gcodes.iplayer.music.player;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.services.ACRService;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonObject;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import jp.wasabeef.glide.transformations.BlurTransformation;

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

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private Fragment[] views;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            init();
        }

        public void init()
        {
            views = new Fragment[ 3 ];
            views[ 0 ] = new Fragment();
            views[ 1 ] = new MusicPlayerFragment();
            views[ 2 ] = new PlaylistFragment();
        }

        @Override
        public Fragment getItem(int position)
        {
            return views[ position ];
        }

        @Override
        public int getCount() {
            return 3;
        }

        public int getDefaultTabPos() {
            return 1;
        }
    }
}
