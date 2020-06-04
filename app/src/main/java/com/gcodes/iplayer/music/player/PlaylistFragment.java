package com.gcodes.iplayer.music.player;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.Duration;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class PlaylistFragment extends Fragment
{
    private Music current;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private int currentPos;

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
        adapter = new CustomAdapter();
        listView = view.findViewById( R.id.player_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
//        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1, 0, 0 ));
        listView.addItemDecoration(UIConstance.AppItemDecorator.AppItemDecoratorToolBarOffset(getContext()));
        updateList( current );
        return view;
    }

    public void updateMusic( Music music )
    {
        current = music;
        if ( isAdded() )
        {
            updateList( music );
        }
    }

    private void updateList(Music music) {
        int oldPos = currentPos;
        currentPos = MusicPlayer.getInstance().getPosition(music);
        if ( oldPos != currentPos )
            adapter.notifyItemChanged( oldPos );
        adapter.notifyItemChanged( currentPos );
        listView.scrollToPosition( currentPos );
//        listView.scrollBy( 0, (int) UIConstance.getToolBarOffsetPixel(getContext()));
    }

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
            initSelection(holder, position);

            holder.itemView.setOnClickListener(v -> {
                MusicPlayer.play( position );
            });
        }

        private void initSelection(TrackItemHolder holder, int position) {
            holder.select( position == currentPos, getContext() );
        }

        @Override
        public int getItemCount() {
            return MusicPlayer.getInstance().getMusicsCount();
        }
    }
}
