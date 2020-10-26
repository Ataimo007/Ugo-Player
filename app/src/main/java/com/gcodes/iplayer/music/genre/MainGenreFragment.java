package com.gcodes.iplayer.music.genre;

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
import com.gcodes.iplayer.music.models.Album;
import com.gcodes.iplayer.music.models.Artist;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.album.AlbumItemHolder;
import com.gcodes.iplayer.music.artist.ArtistItemHolder;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.content.CursorLoader;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;

public class MainGenreFragment extends Fragment
{
    private SectionsPagerAdapter pagerAdapter;
    private ViewPager pager;
    private TabLayout tabs;

//    private String selection;
    private final String[] trackProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.DATA,
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private final String[] artistProjection = {
            MediaStore.Audio.Genres.Members.ARTIST_KEY,
            MediaStore.Audio.Genres.Members.ARTIST_ID,
            MediaStore.Audio.Genres.Members.DATA,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ALBUM_ID
    };

//    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
//            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";

//    private String albumSort = MediaStore.Audio.Artists.Albums.ALBUM_KEY + " asc";

//    private static CursorLoader artLoader;

//    private String artistKey;
    private long genreId;
    private String genre;
    private String albumArt;


    private Toolbar toolbar;
    private ArrayList<Music> tracks;
    private Album[] albums;
    private Artist[] artist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_genre_full, container, false);
//        toolbar = view.findViewById(R.id.album_toolbar);
//        getActivity().setActionBar(toolbar);

        init();
        initView(view);

        return view;
    }

    private void initPager(View view)
    {
        pager = view.findViewById(R.id.genre_viewpager);
        tabs = view.findViewById(R.id.genre_tabs);
        pagerAdapter = new SectionsPagerAdapter( getChildFragmentManager(), tracks, albums, artist, genreId, this );
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));
    }

    private void init() {
        initArgs();
//        initArtLoader();
        tracks = getTracks();
        albums = getAlbums();
        artist = getArtist();
    }

    private void initArgs() {
//        artistKey = getIntent().getStringExtra( "artist_key" );
        genre = getArguments().getString("genre");
        genreId = getArguments().getLong("genre_id" );
        albumArt = getArguments().getString("album_art");
    }

//    private void initArtLoader()
//    {
//        artLoader = new CursorLoader( this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
//                MediaStore.Audio.Albums._ID + "=?", null,
//                null);
//    }

    public ArrayList<Music> getTracks()
    {
        ArrayList<Music> musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this.getContext(),
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), trackProjection,
                null, null, trackSort );
        Cursor cursor = loader.loadInBackground();

//        Log.d("Genre_Activity", "Genre Track View" );
        if  ( cursor.moveToFirst() )
        {
            do
            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
                musics.add( Music.getInstance(cursor) );
            } while ( cursor.moveToNext() );
        }
        return musics;
    }

    public Artist[] getArtist()
    {
//        TreeSet<Album> albums = new TreeSet<>();
        HashSet<Artist> albums = new HashSet<>();
        CursorLoader loader = new CursorLoader( this.getContext(),
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ),  artistProjection,
                null, null, MediaStore.Audio.Genres.Members.ARTIST_KEY );
        Cursor cursor = loader.loadInBackground();

        if  ( cursor.moveToFirst() )
        {
            do
            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
                albums.add( Artist.getGenreInstance( cursor ) );
            } while ( cursor.moveToNext() );
        }

        Log.d("Genre_Activity", "Albums " + albums );
        return albums.toArray( new Artist[]{} );
    }

    public Album[] getAlbums()
    {
//        TreeSet<Album> albums = new TreeSet<>();
        HashSet<Album> albums = new HashSet<>();
        CursorLoader loader = new CursorLoader( this.getContext(),
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), Album.genreProjection,
                null, null, MediaStore.Audio.Genres.Members.ALBUM_KEY );
        Cursor cursor = loader.loadInBackground();

        if  ( cursor.moveToFirst() )
        {
            do
            {
                albums.add( Album.getGenreInstance( cursor ) );
            } while ( cursor.moveToNext() );
        }

        Log.d("Genre_Activity", "Albums " + albums );
        return albums.toArray( new Album[]{} );
    }

    public void initView(View view) {
        initToolbar(view);
        initPager(view);
    }

    private void initToolbar(View view) {
        setToolbarImage(view);
        setTitles(view);
    }

    private void setTitles(View view) {
        Toolbar appBar = view.findViewById(R.id.genre_toolbar);
        appBar.setTitle( genre );
    }

    public void setToolbarImage(View view)
    {
        ImageView image = view.findViewById(R.id.album_art);
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.GenreProcessFetcher( this, genreId ) ).placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final long genreId;
        private final ArrayList<Music> tracks;
        private final Album[] albums;
        private final Artist[] artist;
        private final FragmentActivity activity;

        public SectionsPagerAdapter(FragmentManager fm, ArrayList<Music> tracks, Album[] albums, Artist[] artist, long genreId, MainGenreFragment genreActivity)
        {
            super(fm);
            this.tracks = tracks;
            this.albums = albums;
            this.genreId = genreId;
            this.artist = artist;
            activity = genreActivity.getActivity();
        }

        @Override
        public Fragment getItem(int position)
        {
            switch ( position )
            {
                case 0:
                    GenreTrackFragment genreTracks = new GenreTrackFragment();
                    genreTracks.setMusics( tracks );
                    genreTracks.setActivity( activity );
                    return genreTracks;

                case 1:
                    GenreArtistFragment genreArtist = new GenreArtistFragment();
                    genreArtist.setArtist( artist );
                    genreArtist.setGenreId( genreId );
                    return genreArtist;

                case 2:
                    GenreAlbumFragment genreAlbum = new GenreAlbumFragment();
                    genreAlbum.setAlbums( albums );
                    genreAlbum.setGenreId( genreId );
                    return genreAlbum;

                default:
                    return new Fragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    public static class GenreTrackFragment extends Fragment
    {
        private ArrayList<Music> musics;
        private FragmentActivity activity;

        public void setMusics(ArrayList<Music> musics) {
            this.musics = musics;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            RecyclerView listView = new RecyclerView( getContext() );
            CustomAdapter adapter = new CustomAdapter();
            listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
            listView.setAdapter(adapter);
            listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
            return listView;
        }

        public void setActivity(FragmentActivity activity) {
            this.activity = activity;
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
                holder.setImage( getContext(), music );
//                Log.d( "Track_Fragment", "the art path " + music.getArtPath() );

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

    public static class GenreAlbumFragment extends Fragment
    {
        private Album[] albums;
        private long genreId;

        public void setAlbums(Album[] albums) {
            this.albums = albums;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            RecyclerView listView = new RecyclerView( getContext() );
            CustomAdapter adapter = new CustomAdapter();
            listView.setLayoutManager( new GridLayoutManager(getContext(), getSpan()) );
            listView.setAdapter(adapter);
            listView.addItemDecoration(new UIConstance.AppItemDecorator( getSpan()));
            return listView;
        }

        private int getSpan() {
            return 2;
        }

        public void setGenreId(long genreId) {
            this.genreId = genreId;
        }

        public class CustomAdapter extends RecyclerView.Adapter<AlbumItemHolder>
        {

            @Override
            public AlbumItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card, parent, false);
                return new AlbumItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull AlbumItemHolder holder, int position) {
                Album album = albums[position];
                holder.setSubtitle( album.getArtist() );
//                String albumArt = Music.getArtPath(album.getAlbumId(), artLoader);

                holder.setTitle(album.getAlbum());
                holder.setImage(GenreAlbumFragment.this, String.valueOf(album.getAlbumId()));
                holder.setImageFromGenre( GenreAlbumFragment.this, genreId, MediaStore.Audio.Genres.Members.ALBUM_KEY, album.getAlbumKey() );

                holder.itemView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString( "from", "genre" );
                    args.putLong( "genre_id", genreId );
                    args.putString( "album_key", album.getAlbumKey() );
                    args.putString( "album_name", album.getAlbum() );
//                    args.putString( "album_art", albumArt );
                    NavHostFragment.findNavController(GenreAlbumFragment.this.getParentFragment()).navigate( R.id.action_mainGenreFragment_to_mainAlbumFragment, args);
                });
            }

            @Override
            public int getItemCount() {
                return albums.length;
            }
        }
    }

    public static class GenreArtistFragment extends Fragment
    {
        private Artist[] artists;
        private long genreId;

        public void setArtist(Artist[] artists) {
            this.artists = artists;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            RecyclerView listView = new RecyclerView( getContext() );
            CustomAdapter adapter = new CustomAdapter();
            listView.setLayoutManager( new GridLayoutManager(getContext(), getSpan()) );
            listView.setAdapter(adapter);
            listView.addItemDecoration(new UIConstance.AppItemDecorator( getSpan()));
            return listView;
        }

        private int getSpan() {
            return 2;
        }

        public void setGenreId(long genreId) {
            this.genreId = genreId;
        }

        public class CustomAdapter extends RecyclerView.Adapter<ArtistItemHolder>
        {
            @Override
            public ArtistItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card_full, parent, false);
                return new ArtistItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ArtistItemHolder holder, int position) {
                Artist artist = artists[position];
                holder.setSubtitle( artist.getAlbum() );
//                String albumArt = Music.getArtPath(artist.getAlbumId(), artLoader);

                holder.setTitle(artist.getArtist());
                holder.setImageFromGenre( GenreArtistFragment.this, genreId, MediaStore.Audio.Genres.Members.ARTIST_KEY, artist.getArtistKey() );

                holder.itemView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString( "from", "genre" );
                    args.putLong( "genre_id", genreId );
                    args.putString( "artist", artist.getArtist() );
                    args.putLong( "artist_id", artist.getArtistId() );
                    args.putString( "artist_key", artist.getArtistKey() );
//                    args.putString( "album_art", albumArt );
                    NavHostFragment.findNavController(GenreArtistFragment.this.getParentFragment()).navigate(R.id.action_mainGenreFragment_to_artistOnlyFragment, args);
                });
            }

            @Override
            public int getItemCount() {
                return artists.length;
            }
        }

    }

}
