package com.gcodes.iplayer.music.album;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;

public class MainAlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
//    private String selection;
    private final String selection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ALBUM );
    private final String artistSelection = String.format( "%s != ? and %s = ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Artists.ARTIST_KEY );
    private final String trackSelection = String.format( "%s = ?", MediaStore.Audio.Genres.Members.ALBUM_KEY );

//    private String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";
    private final String sort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";
    private final String genreSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";

    private String albumKey;
//    private CursorLoader artLoader;

    private final String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_KEY,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private final String[] genreProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.DATA,
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ARTIST_KEY,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private ArrayList<Music> musics;
//    private Cursor cursor;
//    private Toolbar toolbar;
    private String albumName;
    private String from;
    private long genreId;
    private String artistKey;
    private CustomAdapter adapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_album_full, container, false);
//        toolbar = view.findViewById(R.id.album_toolbar);
//        getActivity().setActionBar(toolbar);

        initView( view );
        initPlay(view);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
//        LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
    }

    private void initPlay(View view) {
        FloatingActionButton play = view.findViewById(R.id.fragment_play);
        play.setOnClickListener(v -> {
            new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(musics);
        });
        FloatingActionButton appButton = requireActivity().findViewById(R.id.action_floating);
        appButton.hide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FloatingActionButton appButton = requireActivity().findViewById(R.id.action_floating);
        appButton.show();
    }

    private void init() {
        initArgs();
        initTracks();
    }

    private void initArgs()
    {
        from = getArguments().getString( "from" );
        switch ( from )
        {
            case "album":
                albumKey = getArguments().getString( "album_key" );
                albumName = getArguments().getString("album_name");
                break;

            case "genre":
                albumKey = getArguments().getString( "album_key" );
                albumName = getArguments().getString("album_name");
                genreId = getArguments().getLong("genre_id", 0);
                break;

            case "artist":
                albumKey = getArguments().getString( "album_key" );
                artistKey = getArguments().getString( "artist_key" );
                albumName = getArguments().getString("album_name");
                break;
        }

    }

//    public void initTracks()
//    {
//        switch ( from )
//        {
//            case "album":
//                LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.ALBUM.getId(), null, this);
////                initAlbumTracks();
//                break;
//
//            case "genre":
//                LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.GENRE.getId(), null, this);
////                initGenreTracks();
//                break;
//
//            case "artist":
//                LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.ARTIST.getId(), null, this);
////                initArtistTracks();
//                break;
//        }
//    }

    public void initTracks()
    {
        if (from.equalsIgnoreCase("genre"))
            LoaderManager.getInstance(requireActivity()).restartLoader(MainActivity.AppLoader.GENRE_MEMBERS.getId(), null, this);
        else
            LoaderManager.getInstance(requireActivity()).restartLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Log.d("Album_Fragment", "id " + id);
        Log.d("Album_Fragment", "from " + from);
        if (id == MainActivity.AppLoader.TRACK.getId())
        {
            if (from.equalsIgnoreCase("album"))
                return new CursorLoader( this.getContext(),
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                        selection, new String[]{ "0", String.valueOf(albumName)}, sort );
            else if (from.equalsIgnoreCase("artist"))
                return new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                        artistSelection, new String[]{ "0", String.valueOf(albumKey), String.valueOf(artistKey)}, sort );
        }
        else if (id == MainActivity.AppLoader.GENRE_MEMBERS.getId())
            return new CursorLoader( this.getContext(),
                    MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
                    trackSelection, new String[]{ String.valueOf(albumKey) }, genreSort );
        return new CursorLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (musics == null)
            musics = new ArrayList<>();
        cursor.moveToFirst();
        do
        {
            musics.add( Music.getInstance(cursor) );
        } while ( cursor.moveToNext() );
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        musics = null;
        adapter.notifyDataSetChanged();
    }

//    public void initAlbumTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this.getContext(),
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
//                selection, new String[]{ "0", String.valueOf(albumKey)}, sort );
//        cursor = loader.loadInBackground();
//        loadMusic();
//    }
//
//    public void initArtistTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
//                artistSelection, new String[]{ "0", String.valueOf(albumKey), String.valueOf(artistKey)}, sort );
//        cursor = loader.loadInBackground();
//        loadMusic();
//    }
//
//    public void initGenreTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this.getContext(),
//                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
//                trackSelection, new String[]{ String.valueOf(albumKey) }, genreSort );
//        cursor = loader.loadInBackground();
//        loadGenreMusic();
//    }
//
//    private void loadMusic() {
//        cursor.moveToFirst();
//        do
//        {
//            musics.add( Music.getInstance(cursor) );
//        } while ( cursor.moveToNext() );
//    }
//
//    private void loadGenreMusic() {
//        cursor.moveToFirst();
//        do
//        {
//            musics.add( Music.getGenreInstance(cursor) );
//        } while ( cursor.moveToNext() );
//    }

    public void initView(View view) {
        initRecycleView( view );
        initToolbar( view );
    }

    private void initToolbar(View view) {
        setToolbarImage(view);
        setTitles(view);
    }

    private void setTitles(View view) {
        Toolbar toolbar = view.findViewById(R.id.album_toolbar);
        toolbar.setTitle( albumName );
    }

    public void initRecycleView(View view) {
        RecyclerView listView = view.findViewById(R.id.list_track);
        adapter = new CustomAdapter();
        listView.setLayoutManager( new LinearLayoutManager( this.getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
    }

    public void setToolbarImage(View view)
    {
        ImageView image = view.findViewById(R.id.album_art);
        switch ( from )
        {
            case "album":
                GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( this, String.valueOf(albumKey), MediaStore.Audio.Media.ALBUM_KEY ) )
                        .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
                break;

            case "genre":
                GlideApp.with( this ).load( new ProcessModelLoaderFactory.CustomGenreProcessFetcher( this, genreId, MediaStore.Audio.Genres.Members.ALBUM_KEY, albumKey ) )
                        .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
                break;

            case "artist":
                GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicDualCategoryProcessFetcher( this, artistKey, MediaStore.Audio.Media.ARTIST_KEY, albumKey, MediaStore.Audio.Media.ALBUM_KEY ) )
                        .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
                break;
        }
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
            holder.setImage( MainAlbumFragment.this, music );
//            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );


            holder.itemView.setOnClickListener(v -> {
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
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
