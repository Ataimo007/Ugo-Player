package com.gcodes.iplayer.music;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MusicTabFragment extends Fragment
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        int layout = getArguments().getInt("layout");
        TabLayout tab = ( TabLayout ) inflater.inflate( layout, container, false);
        return tab;
    }

    public static MusicTabFragment InitTab(FragmentManager manager, int layout)
    {
        FragmentTransaction transaction = manager.beginTransaction();
        MusicTabFragment tab = new MusicTabFragment();
        Bundle args = new Bundle();
        args.putInt( "layout", layout );
        tab.setArguments( args );
        transaction.replace( R.id.main_tablayout, tab);
        transaction.commitNow();
        return tab;
    }

    public static MusicTabFragment InitTab(FragmentManager manager, int layout, Runnable runnable)
    {
        FragmentTransaction transaction = manager.beginTransaction();
        MusicTabFragment tab = new MusicTabFragment();
        Bundle args = new Bundle();
        args.putInt( "layout", layout );
        tab.setArguments( args );
        transaction.replace( R.id.main_tablayout, tab);
        transaction.runOnCommit(runnable);
        transaction.commit();
        return tab;
    }

    public TabLayout getTab() {
        return (TabLayout) getView();
    }
}
