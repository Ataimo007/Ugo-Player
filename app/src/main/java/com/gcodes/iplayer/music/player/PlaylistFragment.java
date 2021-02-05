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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class PlaylistFragment extends Fragment
{
    private final PlayerManager.MusicManager manager;

    private Music currentSelection;
    private Music previousSelection;

    private String filter;
    private LinearLayoutManager layout;
    private CustomAdapter adapter;

    public final DiffUtil.ItemCallback<Music> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Music>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Music oldUser,  @NonNull Music newUser) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldUser.getMediaId() == newUser.getMediaId();
                }
                @Override
                public boolean areContentsTheSame(
                        @NonNull Music oldUser,  @NonNull Music newUser) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
//                    Log.d("Current_Playlist", String.format("Are Contents The Same %s %s %b", oldUser, newUser, oldUser.equalsInstance(newUser)));
//                    return oldUser.equalsInstance(newUser);

                    Log.d("Current_Playlist", "Are Contents The Same " + (newUser != previousSelection && newUser != currentSelection) );
                    return newUser != previousSelection && newUser != currentSelection;

//                    return false;
                }
            };

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
        adapter = new CustomAdapter(DIFF_CALLBACK);
        RecyclerView listView = view.findViewById(R.id.player_list);
        layout = new LinearLayoutManager(getContext());
        listView.setLayoutManager(layout);
        listView.setAdapter(adapter);
//        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1, 0, 0 ));
        listView.addItemDecoration(UIConstance.AppItemDecorator.AppItemDecoratorToolBarOffset(getContext()));

        adapter.submitList(manager.getMusics());

        updateMusic();
//        reload();
        return view;
    }

    public void updateMusic()
    {
        updateMusic(manager.getMusic(manager.getCurrentTrack()));
    }

    public void updateMusic( Music music )
    {
        if (currentSelection == null || currentSelection != music)
        {
            previousSelection = currentSelection;
            currentSelection = music;
        }
        reload();
        scrollToMusic(currentSelection);
    }

    private void scrollToMusic(Music music)
    {
        if (adapter != null)
        {
            List<Music> currentList = adapter.getCurrentList();
            if  ( currentList.contains(music) )
            {
                int pos = currentList.indexOf(music);
                layout.scrollToPosition(pos);
            }
        }
    }

    private void reload() {
        if (adapter == null)
            return;

        if (filter == null)
            adapter.submitList(new ArrayList<>(manager.getMusics()));
        else
        {
            ArrayList<Music> newMusics = new ArrayList<>();
            for ( Music music : manager.getMusics() )
            {
                if (music.getName().toLowerCase().contains(filter.toLowerCase()) || music.getArtist().toLowerCase().contains(filter.toLowerCase())
                        || music.getAlbum().toLowerCase().contains(filter.toLowerCase()))
                    newMusics.add(music);
            }
            adapter.submitList(newMusics);
        }
    }

//    public void updateMusic( Music music )
//    {
//        previousSelection = currentSelection;
//        currentSelection = music;
//        if ( isAdded() )
//        {
//            updateList();
//        }
//    }
//
//    public void updateMusic()
//    {
//        previousSelection = currentSelection;
//        currentSelection = manager.getMusic(manager.getCurrentTrack());
//        if ( isAdded() )
//        {
//            updateList();
//        }
//    }

    public void search(String query)
    {
        filter = query;
        reload();
    }

//    private void updateList(Music music) {
//        Log.d("Player_List", "updating playlist");
//        int oldPos = currentPos;
//        currentPos = manager.getPosition(music);
//        Log.d("Player_List", String.format("updating playlist oldPos:%d, newPos:%d", oldPos, currentPos));
//        if ( oldPos != currentPos )
//            adapter.notifyItemChanged( oldPos );
//        adapter.notifyItemChanged( currentPos );
//        listView.scrollToPosition( currentPos );
////        listView.scrollBy( 0, (int) UIConstance.getToolBarOffsetPixel(getContext()));
//    }

    public class CustomAdapter extends ListAdapter<Music, TrackItemHolder>
    {
        protected CustomAdapter(@NonNull DiffUtil.ItemCallback<Music> diffCallback) {
            super(diffCallback);
        }

        @NonNull
        @Override
        public TrackItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new TrackItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrackItemHolder holder, int position) {
            Music music = getItem(position);
            holder.setTitle( music.getName() );
            holder.setSubtitle( music.getArtist() );
            holder.setImage( PlaylistFragment.this, music );
            initSelection(holder, music);

            holder.itemView.setOnClickListener(v -> {
                manager.play( manager.getIndex(music) );
            });
        }

        private void initSelection(TrackItemHolder holder, Music music) {
            holder.select( music == currentSelection, getContext() );
        }

//        @Override
//        public int getItemCount() {
//            return manager.getMusicsCount();
//        }
    }
}
