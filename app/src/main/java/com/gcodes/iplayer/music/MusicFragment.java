package com.gcodes.iplayer.music;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.album.AlbumFragment;
import com.gcodes.iplayer.music.artist.ArtistFragment;
import com.gcodes.iplayer.music.folder.FolderFragment;
import com.gcodes.iplayer.music.genre.GenreFragment;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.gcodes.iplayer.music.track.TrackFragment;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MusicFragment extends Fragment
{

//    private AppBarLayout appBar;
    private SectionsPagerAdapter mSectionsPagerAdapter;
//    private MainActivity.BackAction backAction = null;
    private MainActivity app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSectionsPagerAdapter = new SectionsPagerAdapter( getChildFragmentManager() );
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if ( context instanceof MainActivity )
        {
            app = (MainActivity) context;
            Log.d("Back_Action", "The App is " + app);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.music_fragment, container, false);
        ViewPager mViewPager = content.findViewById(R.id.main_content);
        TabLayout tabLayout = content.findViewById(R.id.music_tabs);
        Toolbar appbar = content.findViewById(R.id.app_toolbar);

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.setCurrentItem( mSectionsPagerAdapter.getDefaultTabPos(), true );

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                appbar.setTitle( mSectionsPagerAdapter.getPageTitle( position ) );
                Fragment fragment = mSectionsPagerAdapter.getItem(position);
                if ( fragment instanceof MainActivity.BackAction )
                    app.registerBack((MainActivity.BackAction) fragment);
                else
                    app.unRegisterBack();
            }
        });

        // current Title
        appbar.setTitle( mSectionsPagerAdapter.getPageTitle( mViewPager.getCurrentItem() ) );

        return content;
    }

//    @Override
//    public MainActivity.BackAction getAction() {
//        return backAction;
//    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final Fragment[] views;
        private final String[] fragmentTitles;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            views = new Fragment[ 5 ];
            views[ 0 ] = new AlbumFragment();
            views[ 1 ] = new ArtistFragment();
            views[ 2 ] = new TrackFragment();
            views[ 3 ] = new GenreFragment();
            views[ 4 ] = new FolderFragment();
            fragmentTitles = new String[]{ "Albums", "Artists", "Songs", "Genres", "Folders" };
        }

        @Override
        public Fragment getItem(int position)
        {
            return views[ position ];
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitles[ position ];
        }

        @Override
        public int getCount() {
            return 5;
        }

        public int getDefaultTabPos() {
            return 2;
        }
    }

}
