package com.gcodes.iplayer.video;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.google.android.material.appbar.AppBarLayout;
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
public class VideoFragment extends Fragment
{

    public static VideoFragment utility;

    private AppBarLayout appBar;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

//    public UtilityFragment() {
//        // Required empty public constructor
//    }

    public TabLayout getTabLayout() {
        return tabLayout;
    }

    public void setTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout;
    }

    public static void InitFragments(FragmentManager manager )
    {
        VideoTabFragment tabFragment = VideoTabFragment.InitTab(manager, R.layout.video_tabs);
        FragmentTransaction transaction = manager.beginTransaction();
        VideoFragment fragment = new VideoFragment();
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
        View content = inflater.inflate(R.layout.video_view_pager, container, false);
        mSectionsPagerAdapter = new SectionsPagerAdapter( getChildFragmentManager() );
        mViewPager = (ViewPager) content;
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        getTabLayout().addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        getTabLayout().getTabAt( mSectionsPagerAdapter.getDefaultTabPos() ).select();
        return content;
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private Fragment[] views;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            init();
        }

        public void init()
        {
            views = new Fragment[ 4 ];
            views[ 0 ] = new Fragment();
            views[ 1 ] = new FolderFragment();
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
