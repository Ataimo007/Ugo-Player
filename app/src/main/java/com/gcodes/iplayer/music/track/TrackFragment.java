package com.gcodes.iplayer.music.track;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class TrackFragment extends Fragment
{
    private String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";
//    private String sort = MediaStore.Audio.Media.ALBUM_ID + " asc";

    private String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
    };

    private static Cursor cursor;
    private static ArrayList< Music > musics;
    private CursorLoader artLoader;

    public TrackFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
//        initFloatingAction();
    }

    @Override
    public void onStart() {
        super.onStart();
        initFloatingAction();
    }

    private void initFloatingAction()
    {
        FloatingActionButton floating = getActivity().findViewById(R.id.action_floating);
        floating.setOnClickListener( v -> {
            FragmentActivity owner = requireActivity();
            Log.w("Player_Model", "Owner of Player Model " + owner );
            MainActivity.PlayerModel playerModel = new ViewModelProvider(owner).get(MainActivity.PlayerModel.class);
            Log.d("Player_Model", "Player Model Music " + playerModel );
            playerModel.play(musics);
        });
    }

    public void load()
    {
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, null, sort );
            cursor = loader.loadInBackground();
            artLoader = new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + "=?", null,
                    null);
            loadMusic();
        }
    }

    private void loadMusic()
    {
        musics = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                musics.add( Music.getInstance(cursor, artLoader) );
            } while ( cursor.moveToNext() );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));

        return view;
    }


    public class CustomAdapter extends RecyclerView.Adapter<TrackItemHolder>
    {
        @Override
        public TrackItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new TrackItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrackItemHolder holder, int position) {
            Music music = musics.get( position );
            holder.setTitle( music.getName() );
            holder.setSubtitle( music.getArtist() );
            holder.setImage( TrackFragment.this, music );
            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );

            holder.itemView.setOnClickListener(v -> {
                Log.d("Player_Manager", "playing " + music);
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
            });
        }

        @Override
        public int getItemCount() {
            return musics.size();
        }
    }



}