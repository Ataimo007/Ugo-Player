package com.gcodes.iplayer.music.artist;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class ArtistOnlyFragment extends Fragment
{
//    private String selection;
    private String[] trackProjection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private String[] genreProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.DATA,
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Genres.Members.TITLE + " COLLATE LOCALIZED ASC";

//    private static CursorLoader artLoader;

    private String artistKey;
    private long artistId;
    private String artist;
//    private String albumArt;


    private Toolbar toolbar;
    private ArrayList<Music> musics;
    private String from;
    private long genreId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_artist_only_full, container, false);
//        toolbar = view.findViewById(R.id.album_toolbar);
//        getActivity().setActionBar(toolbar);

        init();
        initView(view);
        initPlay(view);

        return view;
    }

    private void initPlay(View view) {
        FloatingActionButton play = view.findViewById(R.id.fragment_play);
        play.setOnClickListener(v -> {
            new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(musics);
        });
        FloatingActionButton appButton = requireActivity().findViewById(R.id.action_floating);
        appButton.hide();
    }

    private void init() {
        initArgs();
//        initArtLoader();
        initTracks();
    }

    private void initArgs()
    {
        from = getArguments().getString( "from" );

        switch ( from )
        {
            case "track":
                artistKey = getArguments().getString( "artist_key" );
                artist = getArguments().getString("artist");
                artistId = getArguments().getLong("artist_id", 0 );
//                albumArt = getArguments().getString("album_art");
                break;

            case "genre":
                artistKey = getArguments().getString( "artist_key" );
                artist = getArguments().getString("artist");
                genreId = getArguments().getLong("genre_id", 0 );
                artistId = getArguments().getLong("artist_id", 0 );
//                albumArt = getArguments().getString("album_art");
                break;

        }
    }

//    private void initArtLoader()
//    {
//        artLoader = new CursorLoader( this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
//                MediaStore.Audio.Albums._ID + "=?", null,
//                null);
//    }

    public void initTracks()
    {
        switch ( from )
        {
            case "track":
                initMusicTracks();

            case "genre":
                initGenreTracks();
        }
    }

    public void initMusicTracks()
    {
        musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getInstance(cursor) );
        } while ( cursor.moveToNext() );
    }

    public void initGenreTracks()
    {
        musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this.getContext(),
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getInstance(cursor) );
        } while ( cursor.moveToNext() );
    }

    public void initView(View view) {
        initToolbar(view);
        initRecycleView(view);
    }

    private void initToolbar(View view) {
        setToolbarImage(view);
        setTitles(view);
    }

    public void initRecycleView(View view) {
        RecyclerView listView = view.findViewById(R.id.list_track);
        CustomAdapter adapter = new CustomAdapter();
        listView.setLayoutManager( new LinearLayoutManager( this.getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
    }

    private void setTitles(View view) {
        Toolbar appBar = view.findViewById(R.id.artist_toolbar);
        appBar.setTitle( artist );
    }

    public void setToolbarImage(View view)
    {
        ImageView image = view.findViewById(R.id.album_art);
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.CustomGenreProcessFetcher( this, genreId, MediaStore.Audio.Genres.Members.ARTIST_KEY, artistKey ) )
                .placeholder( R.drawable.u_artist_avatar ).apply( circleCropTransform() ).into( image );
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
            holder.setImage( ArtistOnlyFragment.this, music );
//            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );

            holder.itemView.setOnClickListener(v -> {
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
            });
        }

        @Override
        public int getItemCount() {
            return musics.size();
        }
    }

}
