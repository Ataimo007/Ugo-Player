package com.gcodes.iplayer.music.player;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
    private final PlayerManager.MusicManager manager;
    private Music current;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private int currentPos;

    // try joining audio column to media column


    public PlaylistFragment(PlayerManager.MusicManager manager) {
        this.manager = manager;
    }

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
        updateMusic();
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

    public void updateMusic()
    {
        current = manager.getMusic(manager.getCurrentTrack());
        if ( isAdded() )
        {
            updateList( current );
        }
    }

    private void updateList(Music music) {
        Log.d("Player_List", "updating playlist");
        int oldPos = currentPos;
        currentPos = manager.getPosition(music);
        Log.d("Player_List", String.format("updating playlist oldPos:%d, newPos:%d", oldPos, currentPos));
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
            Music music = manager.getMusic(position);
            holder.setTitle( music.getName() );
            holder.setSubtitle( music.getArtist() );
            holder.setImage( PlaylistFragment.this, music );
            initSelection(holder, position);

            holder.itemView.setOnClickListener(v -> {
                manager.play( position );
            });
        }

        private void initSelection(TrackItemHolder holder, int position) {
            holder.select( position == currentPos, getContext() );
        }

        @Override
        public int getItemCount() {
            return manager.getMusicsCount();
        }
    }
}
