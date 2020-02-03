package com.gcodes.iplayer.music.genre;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.backup.AlbumSession;
import com.gcodes.iplayer.music.album.AlbumActivity;
import com.gcodes.iplayer.music.artist.ArtistOnlyActivity;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import static com.gcodes.iplayer.music.Music.*;

public class GenreActivity extends AppCompatActivity
{
    private SectionsPagerAdapter pagerAdapter;
    private ViewPager pager;
    private TabLayout tabs;

//    private String selection;
    private final String[] trackProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private final String[] albumProjection = {
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private final String[] artistProjection = {
            MediaStore.Audio.Genres.Members.ARTIST_KEY,
            MediaStore.Audio.Genres.Members.ARTIST_ID,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ALBUM_ID
    };

//    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
//            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";

//    private String albumSort = MediaStore.Audio.Artists.Albums.ALBUM_KEY + " asc";

    private static CursorLoader artLoader;

//    private String artistKey;
    private long genreId;
    private String genre;
    private String albumArt;


    private Toolbar toolbar;
    private ArrayList<Music> tracks;
    private Album[] albums;
    private Artist[] artist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_full);

        toolbar = findViewById(R.id.genre_toolbar);
        setSupportActionBar(toolbar);

        init();
        initView();
    }

    private void initPager()
    {
        pager = findViewById(R.id.genre_viewpager);
        tabs = findViewById(R.id.genre_tabs);
        pagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager(), tracks, albums, artist, genreId, this );
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));
    }

    private void init() {
        initArgs();
        initArtLoader();
        tracks = getTracks();
        albums = getAlbums();
        artist = getArtist();
    }

    private void initArgs() {
//        artistKey = getIntent().getStringExtra( "artist_key" );
        genre = getIntent().getStringExtra("genre");
        genreId = getIntent().getLongExtra("genre_id", 0 );
        albumArt = getIntent().getStringExtra("album_art");
    }

    private void initArtLoader()
    {
        artLoader = new CursorLoader( this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?", null,
                null);
    }

    public ArrayList<Music> getTracks()
    {
        ArrayList<Music> musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this,
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), trackProjection,
                null, null, trackSort );
        Cursor cursor = loader.loadInBackground();

//        Log.d("Genre_Activity", "Genre Track View" );
        if  ( cursor.moveToFirst() )
        {
            do
            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
                musics.add( Music.getIntance(cursor, artLoader) );
            } while ( cursor.moveToNext() );
        }
        return musics;
    }

    public Artist[] getArtist()
    {
//        TreeSet<Album> albums = new TreeSet<>();
        HashSet<Artist> albums = new HashSet<>();
        CursorLoader loader = new CursorLoader( this,
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ),  artistProjection,
                null, null, MediaStore.Audio.Genres.Members.ARTIST_KEY );
        Cursor cursor = loader.loadInBackground();

        if  ( cursor.moveToFirst() )
        {
            do
            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
                albums.add( Artist.getInstance( cursor ) );
            } while ( cursor.moveToNext() );
        }

        Log.d("Genre_Activity", "Albums " + albums );
        return albums.toArray( new Artist[]{} );
    }

    public Album[] getAlbums()
    {
//        TreeSet<Album> albums = new TreeSet<>();
        HashSet<Album> albums = new HashSet<>();
        CursorLoader loader = new CursorLoader( this,
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), albumProjection,
                null, null, MediaStore.Audio.Genres.Members.ALBUM_KEY );
        Cursor cursor = loader.loadInBackground();

        if  ( cursor.moveToFirst() )
        {
            do
            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
                albums.add( Album.getInstance( cursor ) );
            } while ( cursor.moveToNext() );
        }

        Log.d("Genre_Activity", "Albums " + albums );
        return albums.toArray( new Album[]{} );
    }

//    public Cursor getAlbums()
//    {
//        String selection = String.format( "'true' ) GROUP BY ( %s", MediaStore.Audio.Genres.Members.ALBUM );
//        CursorLoader loader = new CursorLoader( this,
//                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), albumProjection,
//                selection, null, MediaStore.Audio.Genres.Members.ALBUM_KEY );
//        Cursor cursor = loader.loadInBackground();
//
//        Log.d("Genre_Activity", "Genre Album View" );
//        if  ( cursor.moveToFirst() )
//        {
//            do
//            {
//                Log.d("Genre_Activity", "Genre " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM)));
//            } while ( cursor.moveToNext() );
//        }
//
//        return cursor;
//    }

    public void initView() {
        initToolbar();
        initPager();
    }

    private void initToolbar() {
        setToolbarImage();
        setTitles();
    }

    private void setTitles() {
        getSupportActionBar().setTitle( genre );
    }

    public void setToolbarImage()
    {
        ImageView image = findViewById(R.id.album_art);
        if ( albumArt != null )
        {
            Bitmap bitmap = BitmapFactory.decodeFile(albumArt);
            if ( bitmap != null )
            {
                image.setImageBitmap( bitmap );
                return;
            }
        }
        int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
                GenreActivity.this.getPackageName());
        image.setImageResource( resId );
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final long genreId;
        private final ArrayList<Music> tracks;
        private final Album[] albums;
        private final Artist[] artist;
        private final AppCompatActivity activity;

        public SectionsPagerAdapter(FragmentManager fm, ArrayList<Music> tracks, Album[] albums, Artist[] artist, long genreId, GenreActivity genreActivity)
        {
            super(fm);
            this.tracks = tracks;
            this.albums = albums;
            this.genreId = genreId;
            this.artist = artist;
            activity = genreActivity;
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
                    GenreAlbumFragment genreAlbum = new GenreAlbumFragment();
                    genreAlbum.setAlbums( albums );
                    genreAlbum.setGenreId( genreId );
                    return genreAlbum;

                case 2:
                    GenreArtistFragment genreArtist = new GenreArtistFragment();
                    genreArtist.setArtist( artist );
                    genreArtist.setGenreId( genreId );
                    return genreArtist;

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
        private AppCompatActivity activity;

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
            return listView;
        }

        public void setActivity(AppCompatActivity activity) {
            this.activity = activity;
        }

        public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
        {
            @Override
            public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.playlist_item_view, parent, false);
                return new ItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
                Music music = musics.get( position );
                holder.setTitle( music.getName() );
                holder.setSubtitle( music.getArtist() );
//            holder.setImage( music.toUri() );
                Log.d( "Track_Fragment", "the art path " + music.getArtPath() );
//            holder.setImage( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));

                holder.itemView.setOnClickListener(v -> {
                    MusicPlayer.play( music, activity );
                });
            }

            @Override
            public int getItemCount() {
                return musics.size();
            }
        }

        public class ItemHolder extends RecyclerView.ViewHolder
        {

            private TextView title;
            private TextView subtitle;

            public ItemHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.item_title);
                subtitle = itemView.findViewById(R.id.item_subtitle);
            }

            public String getTitle() {
                return title.getText().toString();
            }

            public void setTitle(String name) {
                this.title.setText( name );
            }

            public String getSubtitle() {
                return subtitle.getText().toString();
            }

            public void setSubtitle(String subtitle) {
                this.subtitle.setText(subtitle);
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
            listView.setLayoutManager( new GridLayoutManager(getContext(), 2) );
            listView.setAdapter(adapter);
            return listView;
        }

        public void setGenreId(long genreId) {
            this.genreId = genreId;
        }

        public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
        {
//            public CustomAdapter() {
//                super(GenreAlbumFragment.this.getContext(), albums);
//            }

            @Override
            public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card, parent, false);
                return new ItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
                Album album = albums[position];
//                String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM));
//                String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_KEY));
//                long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID));
                holder.setSubtitle( album.getArtist() );
                String albumArt = Music.getArtPath(album.getAlbumId(), artLoader);

                holder.setTitle(album.getAlbum());
                holder.setImage(albumArt);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent( GenreAlbumFragment.this.getContext(), AlbumActivity.class );
                    intent.putExtra( "from", "genre" );
                    intent.putExtra( "genre_id", genreId );
                    intent.putExtra( "album_key", album.getAlbumKey() );
                    intent.putExtra( "album_name", album.getAlbum() );
                    intent.putExtra( "album_art", albumArt );
                    GenreAlbumFragment.this.startActivity( intent );
                });
            }

            @Override
            public int getItemCount() {
                return albums.length;
            }
        }

        public class ItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;
            private final TextView subtitle;

            public ItemHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.item_title);
                subtitle = itemView.findViewById(R.id.item_subtitle);
                image = itemView.findViewById(R.id.item_image);
            }

            public String getTitle() {
                return title.getText().toString();
            }

            public void setTitle(String name) {
                this.title.setText( name );
            }

            public String getSubtitle() {
                return subtitle.getText().toString();
            }

            public void setSubtitle(String subtitle) {
                this.subtitle.setText(subtitle);
            }

            public Bitmap getImage() {
                return image.getDrawingCache();
            }

            public void setImage( String path )
            {
                if ( path != null )
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if ( bitmap != null )
                    {
                        image.setImageBitmap( bitmap );
                        return;
                    }
                }
                int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
                        getContext().getPackageName());
                this.image.setImageResource( resId );
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
            listView.setLayoutManager( new GridLayoutManager(getContext(), 2) );
            listView.setAdapter(adapter);
            return listView;
        }

        public void setGenreId(long genreId) {
            this.genreId = genreId;
        }

        public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
        {
//            public CustomAdapter() {
//                super(GenreAlbumFragment.this.getContext(), albums);
//            }

            @Override
            public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card, parent, false);
                return new ItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
                Artist artist = artists[position];
                holder.setSubtitle( artist.getAlbum() );
                String albumArt = Music.getArtPath(artist.getAlbumId(), artLoader);

                holder.setTitle(artist.getArtist());
                holder.setImage(albumArt);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent( GenreArtistFragment.this.getContext(), ArtistOnlyActivity.class );
                    intent.putExtra( "from", "genre" );
                    intent.putExtra( "genre_id", genreId );
                    intent.putExtra( "artist", artist.getArtist() );
                    intent.putExtra( "artist_id", artist.getArtistId() );
                    intent.putExtra( "artist_key", artist.getArtistKey() );
                    intent.putExtra( "album_art", albumArt );
                    GenreArtistFragment.this.startActivity( intent );
                });
            }

            @Override
            public int getItemCount() {
                return artists.length;
            }
        }

        public class ItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;
            private final TextView subtitle;

            public ItemHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.item_title);
                subtitle = itemView.findViewById(R.id.item_subtitle);
                image = itemView.findViewById(R.id.item_image);
            }

            public String getTitle() {
                return title.getText().toString();
            }

            public void setTitle(String name) {
                this.title.setText( name );
            }

            public String getSubtitle() {
                return subtitle.getText().toString();
            }

            public void setSubtitle(String subtitle) {
                this.subtitle.setText(subtitle);
            }

            public Bitmap getImage() {
                return image.getDrawingCache();
            }

            public void setImage( String path )
            {
                if ( path != null )
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if ( bitmap != null )
                    {
                        image.setImageBitmap( bitmap );
                        return;
                    }
                }
                int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
                        getContext().getPackageName());
                this.image.setImageResource( resId );
            }

        }
    }

}
