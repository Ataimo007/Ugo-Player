package com.gcodes.iplayer.video;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.video.folder.FolderFragment;
import com.gcodes.iplayer.video.series.SeriesFragment;
import com.gcodes.iplayer.video.videos.AllFragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;


public class VideoFragment extends Fragment
{

    public static VideoFragment utility;

    private AppBarLayout appBar;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    private MainActivity.BackAction backAction = null;
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
        View content = inflater.inflate(R.layout.video_fragment, container, false);
        mViewPager = content.findViewById( R.id.main_content );
        tabLayout = content.findViewById( R.id.video_tabs );
        Toolbar appbar = content.findViewById(R.id.app_toolbar);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        tabLayout.getTabAt( mSectionsPagerAdapter.getDefaultTabPos() ).select();

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

        NavHostFragment.findNavController( this ).getBackStackEntry(R.id.videoFragment).getSavedStateHandle().getLiveData("Page_Title_Changed").observe( getViewLifecycleOwner(),
                hasChanged -> {
                    appbar.setTitle( mSectionsPagerAdapter.getPageTitle( mViewPager.getCurrentItem() ) );
                });

        // current Title
        appbar.setTitle( mSectionsPagerAdapter.getPageTitle( mViewPager.getCurrentItem() ) );

        return content;
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final Fragment[] views;
//        private final String[] fragmentTitles;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            views = new Fragment[ 3 ];
            views[ 0 ] = new AllFragment();
//            views[ 0 ] = new Fragment();
            views[ 1 ] = new SeriesFragment();
//            views[ 2 ] = new Fragment();
            views[ 2 ] = new FolderFragment();
//            fragmentTitles = new String[]{ "Videos", "Series", "Folders"  };
        }

        @Override
        public Fragment getItem(int position)
        {
            return views[ position ];
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return (( PageTitle ) views[ position ]).getTitle();
        }

        @Override
        public int getCount() {
            return views.length;
        }

        public int getDefaultTabPos() {
            return 1;
        }

        public static interface PageTitle
        {
            String getTitle();
        }
    }

//    public class VideoViewModel extends ViewModel
//    {
//        private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
//
//        public LiveData<Boolean> isPlaying()
//        {
//            return this.isPlaying;
//        }
//
//        public void setIsPlaying(Boolean isPlaying )
//        {
//            this.isPlaying.setValue( isPlaying );
//        }
//    }
}
