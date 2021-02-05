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
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;
import static com.gcodes.iplayer.music.models.Music.projection;
import static com.gcodes.iplayer.music.models.Music.sort;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class TrackFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private static ArrayList< Music > musics;
    private CustomAdapter adapter;

    public TrackFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
//        initFloatingAction();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        initFloatingAction();
    }

    @Override
    public void onPause() {
        super.onPause();
        FloatingActionButton floating = requireActivity().findViewById(R.id.action_floating);
        floating.hide();
    }

    private void initFloatingAction()
    {
        FloatingActionButton floating = requireActivity().findViewById(R.id.action_floating);
        floating.show();
        floating.setOnClickListener( v -> {
            FragmentActivity owner = requireActivity();
            Log.w("Player_Model", "Owner of Player Model " + owner );
            MainActivity.PlayerModel playerModel = new ViewModelProvider(owner).get(MainActivity.PlayerModel.class);
            Log.d("Player_Model", "Player Model Music " + playerModel );
            playerModel.play(musics);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));

        return view;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if ( musics == null )
            musics = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                musics.add( Music.getInstance(cursor));
            } while ( cursor.moveToNext() );
        }
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

//    @Override
//    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
//        MainActivity.ConcurrentModel concurrentModel = new ViewModelProvider(requireActivity()).get(MainActivity.ConcurrentModel.class);
//        ListenableFuture<ArrayList<Music>> loadMusic = concurrentModel.runInBackground(() -> {
//            ArrayList<Music> music = new ArrayList<>();
//            if (cursor.getCount() > 0) {
//                cursor.moveToFirst();
//                do {
//                    music.add(Music.getInstance(cursor));
//                } while (cursor.moveToNext());
//            }
//            return music;
//        });
//        Futures.addCallback(loadMusic, new FutureCallback<ArrayList<Music>>() {
//            @Override
//            public void onSuccess(@org.checkerframework.checker.nullness.qual.Nullable ArrayList<Music> result) {
//                musics = result;
//                concurrentModel.runInUI(() -> {
//                    adapter.notifyDataSetChanged();
//                });
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//
//            }
//        }, concurrentModel.getExecutorService());
//    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        musics = null;
        adapter.notifyDataSetChanged();
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
//            Log.d("music_info", "Data " + music.getData());

            holder.itemView.setOnClickListener(v -> {
                Log.d("Player_Manager", "playing " + music);
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
//                throw new RuntimeException("Test Crash"); // Force a crash
            });
        }

        @Override
        public int getItemCount() {
            if (musics == null)
                return 0;
            return musics.size();
        }
    }
}