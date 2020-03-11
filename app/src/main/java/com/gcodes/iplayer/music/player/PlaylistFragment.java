package com.gcodes.iplayer.music.player;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.track.TrackFragment;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class PlaylistFragment extends Fragment
{
    public PlaylistFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = view.findViewById( R.id.player_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
        return view;
    }



//    public static void hide( FragmentManager fragmentManager ) {
//        Fragment playlist = fragmentManager.findFragmentByTag("Playlist_View");
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.remove( playlist );
//        transaction.commit();
//    }
//
//    public static void show(FragmentManager fragmentManager) {
//
//
////        VideoTabFragment tabFragment = VideoTabFragment.InitTab(manager, R.layout.video_tabs);
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        PlaylistFragment musicFragment = new PlaylistFragment();
//        transaction.add( R.id.player_playlist, musicFragment, "Playlist_View" );
//        transaction.commit();
//    }

    public class CustomAdapter extends RecyclerView.Adapter<TrackItemHolder>
    {
        @NonNull
        @Override
        public TrackItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new TrackItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrackItemHolder holder, int position) {
            Music music = MusicPlayer.getInstance().getMusic(position);
            holder.setTitle( music.getName() );
            holder.setSubtitle( music.getArtist() );
            holder.setImage( PlaylistFragment.this, music );

            holder.itemView.setOnClickListener(v -> {
                MusicPlayer.play( music );
            });
        }

        @Override
        public int getItemCount() {
            return MusicPlayer.getInstance().getMusicsCount();
        }
    }

}
