package com.gcodes.iplayer.music;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.album.AlbumFragment;
import com.gcodes.iplayer.music.artist.ArtistFragment;
import com.gcodes.iplayer.music.folder.FolderFragment;
import com.gcodes.iplayer.music.genre.GenreFragment;
import com.gcodes.iplayer.music.track.TrackFragment;
import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;


///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link UtilityFragment.OnFragmentInteractionListener} interface
// * to handle interaction events.
// * Use the {@link UtilityFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class MusicFragment extends Fragment
{

//    private AppBarLayout appBar;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    public TabLayout getTabLayout() {
        return tabLayout;
    }

    public void setTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout;
    }

    public static void InitFragments(FragmentManager manager )
    {
        MusicTabFragment tabFragment = MusicTabFragment.InitTab(manager, R.layout.music_tabs);
        FragmentTransaction transaction = manager.beginTransaction();
        MusicFragment fragment = new MusicFragment();
        fragment.setTabLayout( tabFragment.getTab() );
        transaction.replace( R.id.main_content, fragment);
        transaction.commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.music_view_pager, container, false);
        mSectionsPagerAdapter = new SectionsPagerAdapter( getChildFragmentManager() );
//        mSectionsPagerAdapter = new SectionsPagerAdapter( getFragmentManager() );
        mViewPager = content.findViewById( R.id.music_viewpager );
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        getTabLayout().addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        getTabLayout().getTabAt( mSectionsPagerAdapter.getDefaultTabPos() ).select();
        return content;
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private Fragment[] views;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
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

}
