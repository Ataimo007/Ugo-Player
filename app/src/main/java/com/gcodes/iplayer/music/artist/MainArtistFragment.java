package com.gcodes.iplayer.music.artist;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.album.AlbumItemHolder;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

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

public class MainArtistFragment extends Fragment
{
    private SectionsPagerAdapter pagerAdapter;
    private ViewPager pager;
    private TabLayout tabs;

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

    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";

    private String[] albumProjection;

    private String albumSort = MediaStore.Audio.Artists.Albums.ALBUM_KEY + " asc";

    private static CursorLoader artLoader;

    private String artistKey;
    private long artistId;
    private String artist;
    private String albumArt;


    private ArrayList<Music> tracks;
    private Cursor albums;

    public MainArtistFragment()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            albumProjection = new String[] {
                    MediaStore.Audio.Artists.Albums.ALBUM_KEY,
                    MediaStore.Audio.Artists.Albums.ARTIST_ID,
                    MediaStore.Audio.Artists.Albums.ALBUM,
                    MediaStore.Audio.Artists.Albums.ARTIST,
                    MediaStore.Audio.Artists.Albums.ALBUM_ART
            };
        }
        else
        {
            albumProjection = new String[] {
                    MediaStore.Audio.Artists.Albums.ALBUM_KEY,
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.Albums.ALBUM,
                    MediaStore.Audio.Artists.Albums.ARTIST,
                    MediaStore.Audio.Artists.Albums.ALBUM_ART
            };
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_artist_full, container, false);
//        toolbar = view.findViewById(R.id.album_toolbar);
//        getActivity().setActionBar(toolbar);

        init();
        initView(view);

        return view;
    }

    private void initPager(View view)
    {
        pager = view.findViewById(R.id.artist_viewpager);
        tabs = view.findViewById(R.id.artist_tabs);
        pagerAdapter = new SectionsPagerAdapter( getChildFragmentManager(), tracks, albums, artistKey, this );
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));
    }

    private void init() {
        initArgs();
        initArtLoader();
        tracks = getTracks();
        albums = getAlbums();
    }

    private void initArgs() {
        artistKey = getArguments().getString( "artist_key" );
        artist = getArguments().getString("artist");
        artistId = getArguments().getLong("artist_id");
        albumArt = getArguments().getString("album_art");
    }

    private void initArtLoader()
    {
        artLoader = new CursorLoader( this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?", null,
                null);
    }

    public ArrayList<Music> getTracks()
    {
        ArrayList<Music> musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getInstance(cursor, artLoader) );
        } while ( cursor.moveToNext() );
        return musics;
    }

    public Cursor getAlbums()
    {
        CursorLoader loader = new CursorLoader( this.getContext(),
                MediaStore.Audio.Artists.Albums.getContentUri( "external", artistId ), albumProjection,
                null, null, albumSort );
        Cursor cursor = loader.loadInBackground();

        return cursor;
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
        Toolbar appbar = view.findViewById(R.id.album_toolbar);
        appbar.setTitle( artist );
    }

    public void setToolbarImage(View view)
    {
        ImageView image = view.findViewById(R.id.album_art);
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( this, String.valueOf(artistId), MediaStore.Audio.Media.ARTIST_ID ) )
                .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final ArrayList<Music> tracks;
        private final Cursor albums;
        private final String artistKey;
        private final FragmentActivity activity;

        public SectionsPagerAdapter(FragmentManager fm, ArrayList<Music> tracks, Cursor albums, String artistKey, MainArtistFragment artistActivity)
        {
            super(fm);
            this.tracks = tracks;
            this.albums = albums;
            this.artistKey = artistKey;
            activity = artistActivity.getActivity();
        }

        @Override
        public Fragment getItem(int position)
        {
            switch ( position )
            {
                case 0:
                    ArtistTrackFragment artistTracks = new ArtistTrackFragment();
                    artistTracks.setMusics( tracks );
                    artistTracks.setActivity( activity );
                    return artistTracks;

                case 1:
                    ArtistAlbumFragment artistAlbum = new ArtistAlbumFragment();
                    artistAlbum.setAlbums( albums );
                    artistAlbum.setArtistKey( artistKey );
                    return artistAlbum;

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public static class ArtistTrackFragment extends Fragment
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

            listView.addItemDecoration( new UIConstance.AppItemDecorator( 1 ) );
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
//            holder.setImage( music.toUri() );
                Log.d( "Track_Fragment", "the art path " + music.getArtPath() );
//            holder.setImage( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));

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

    public static class ArtistAlbumFragment extends Fragment
    {
        private Cursor albums;
        private String artistKey;

        public void setAlbums(Cursor albums) {
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

        public void setArtistKey(String artistKey) {
            this.artistKey = artistKey;
        }

        public class CustomAdapter extends CursorRecyclerViewAdapter< AlbumItemHolder >
        {
            public CustomAdapter() {
                super(ArtistAlbumFragment.this.getContext(), albums);
            }

            @Override
            public AlbumItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card, parent, false);
                return new AlbumItemHolder(view);
            }

            @Override
            public void onBindViewHolder(AlbumItemHolder holder, Cursor cursor)
            {
                String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM));
                String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM_KEY));
                String artistId;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    artistId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ARTIST_ID));
                else
                     artistId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM_ART));

                holder.setTitle(albumName);
                holder.setSubtitle( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ARTIST)));
                holder.setImageByArtist(getContext(), artistKey, albumKey);

                holder.itemView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString( "from", "artist" );
                    args.putString( "album_key", albumKey );
                    args.putString( "artist_key", artistKey );
                    args.putString( "artist_id", artistId );
                    args.putString( "album_name", albumName );
                    args.putString( "album_art", albumArt );
                    NavHostFragment.findNavController(ArtistAlbumFragment.this.getParentFragment() ).navigate( R.id.action_mainArtistFragment_to_mainAlbumFragment, args);
                });
            }
        }

    }

}
