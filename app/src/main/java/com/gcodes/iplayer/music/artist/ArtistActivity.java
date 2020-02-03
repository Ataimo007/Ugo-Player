package com.gcodes.iplayer.music.artist;

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
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.backup.AlbumSession;
import com.gcodes.iplayer.music.album.AlbumActivity;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

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

public class ArtistActivity extends AppCompatActivity
{
    private SectionsPagerAdapter pagerAdapter;
    private ViewPager pager;
    private TabLayout tabs;

//    private String selection;
    private String[] trackProjection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";

    private String[] albumProjection = {
            MediaStore.Audio.Artists.Albums.ALBUM_KEY,
            MediaStore.Audio.Artists.Albums.ALBUM,
            MediaStore.Audio.Artists.Albums.ARTIST,
            MediaStore.Audio.Artists.Albums.ALBUM_ART
    };

//    private String[] albumProjection = {
//            MediaStore.Audio.Albums._ID,
//            MediaStore.Audio.Albums.ALBUM_KEY,
//            MediaStore.Audio.Albums.ALBUM,
//            MediaStore.Audio.Albums.ARTIST,
//            MediaStore.Audio.Albums.ALBUM_ART
//    };

//    private String albumSelection = String.format( "%s = ?", MediaStore.Audio.Albums.ARTIST );

    private String albumSort = MediaStore.Audio.Artists.Albums.ALBUM_KEY + " asc";

    private static CursorLoader artLoader;

    private String artistKey;
    private long artistId;
    private String artist;
    private String albumArt;


    private Toolbar toolbar;
    private ArrayList<Music> tracks;
    private Cursor albums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_full);

        toolbar = findViewById(R.id.album_toolbar);
        setSupportActionBar(toolbar);

        init();
        initView();
    }

    private void initPager()
    {
        pager = findViewById(R.id.artist_viewpager);
        tabs = findViewById(R.id.artist_tabs);
        pagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager(), tracks, albums, artistKey, this );
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
        artistKey = getIntent().getStringExtra( "artist_key" );
        artist = getIntent().getStringExtra("artist");
        artistId = getIntent().getLongExtra("artist_id", 0 );
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

        CursorLoader loader = new CursorLoader( this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getIntance(cursor, artLoader) );
        } while ( cursor.moveToNext() );
        return musics;
    }

    public Cursor getAlbums()
    {
        CursorLoader loader = new CursorLoader( this,
                MediaStore.Audio.Artists.Albums.getContentUri( "external", artistId ), albumProjection,
                null, null, albumSort );
        Cursor cursor = loader.loadInBackground();

        return cursor;
    }

//    public ArrayList<Music> getAlbums2()
//    {
//        ArrayList<Music> albums = new ArrayList<>();
//
//        CursorLoader loader = new CursorLoader( this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, albumProjection,
//                albumSelection, new String[]{ String.valueOf(artist) }, albumSort );
//        Cursor cursor = loader.loadInBackground();
//
//        cursor.moveToFirst();
//        do
//        {
//            albums.add( Music.getIntance(cursor, artLoader) );
//        } while ( cursor.moveToNext() );
//        return albums;
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
        getSupportActionBar().setTitle( artist );
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
                ArtistActivity.this.getPackageName());
        image.setImageResource( resId );
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final ArrayList<Music> tracks;
        private final Cursor albums;
        private final String artistKey;
        private final AppCompatActivity activity;

        public SectionsPagerAdapter(FragmentManager fm, ArrayList<Music> tracks, Cursor albums, String artistKey, ArtistActivity artistActivity)
        {
            super(fm);
            this.tracks = tracks;
            this.albums = albums;
            this.artistKey = artistKey;
            activity = artistActivity;
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
            listView.setLayoutManager( new GridLayoutManager(getContext(), 2) );
            listView.setAdapter(adapter);
            return listView;
        }

        public void setArtistKey(String artistKey) {
            this.artistKey = artistKey;
        }

        public class CustomAdapter extends CursorRecyclerViewAdapter< ItemHolder >
        {
            public CustomAdapter() {
                super(ArtistAlbumFragment.this.getContext(), albums);
            }

            @Override
            public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card, parent, false);
                return new ItemHolder(view);
            }

            @Override
            public void onBindViewHolder(ItemHolder holder, Cursor cursor)
            {
                String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM));
                String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM_KEY));
//            String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM_ART));

                holder.setTitle(albumName);
                holder.setSubtitle( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ARTIST)));
                holder.setImage(albumArt);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent( ArtistAlbumFragment.this.getContext(), AlbumActivity.class );
                    intent.putExtra( "from", "artist" );
                    intent.putExtra( "album_key", albumKey );
                    intent.putExtra( "artist_key", artistKey );
                    intent.putExtra( "album_name", albumName );
                    intent.putExtra( "album_art", albumArt );
                    ArtistAlbumFragment.this.startActivity( intent );
                });
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
