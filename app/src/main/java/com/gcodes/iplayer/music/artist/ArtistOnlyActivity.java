package com.gcodes.iplayer.music.artist;

import android.content.Context;
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
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.gcodes.iplayer.music.track.TrackFragment;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class ArtistOnlyActivity extends AppCompatActivity
{
//    private String selection;
    private String[] trackProjection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private String[] genreProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM,
            MediaStore.Audio.Genres.Members.ALBUM_KEY,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.TITLE,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
    };

    private String trackSelection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.ARTIST_KEY );

    private String trackSort = MediaStore.Audio.Genres.Members.TITLE + " COLLATE LOCALIZED ASC";

    private static CursorLoader artLoader;

    private String artistKey;
    private long artistId;
    private String artist;
    private String albumArt;


    private Toolbar toolbar;
    private ArrayList<Music> musics;
    private String from;
    private long genreId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_only_full);

        toolbar = findViewById(R.id.album_toolbar);
        setSupportActionBar(toolbar);

        init();
        initView();
    }

    private void init() {
        initArgs();
        initArtLoader();
        initTracks();
    }

    private void initArgs()
    {
        from = getIntent().getStringExtra( "from" );

        switch ( from )
        {
            case "track":
                artistKey = getIntent().getStringExtra( "artist_key" );
                artist = getIntent().getStringExtra("artist");
                artistId = getIntent().getLongExtra("artist_id", 0 );
                albumArt = getIntent().getStringExtra("album_art");
                break;

            case "genre":
                artistKey = getIntent().getStringExtra( "artist_key" );
                artist = getIntent().getStringExtra("artist");
                genreId = getIntent().getLongExtra("genre_id", 0 );
                artistId = getIntent().getLongExtra("artist_id", 0 );
                albumArt = getIntent().getStringExtra("album_art");
                break;

        }
    }

    private void initArtLoader()
    {
        artLoader = new CursorLoader( this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?", null,
                null);
    }

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

        CursorLoader loader = new CursorLoader( this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getIntance(cursor, artLoader) );
        } while ( cursor.moveToNext() );
    }

    public void initGenreTracks()
    {
        musics = new ArrayList<>();

        CursorLoader loader = new CursorLoader( this,
                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
                trackSelection, new String[]{ "0", String.valueOf(artistKey)}, trackSort );
        Cursor cursor = loader.loadInBackground();

        cursor.moveToFirst();
        do
        {
            musics.add( Music.getIntance(cursor, artLoader) );
        } while ( cursor.moveToNext() );
    }

    public void initView() {
        initToolbar();
        initRecycleView();
    }

    private void initToolbar() {
        setToolbarImage();
        setTitles();
    }

    public void initRecycleView() {
        RecyclerView listView = findViewById(R.id.list_track);
        CustomAdapter adapter = new CustomAdapter();
        listView.setLayoutManager( new LinearLayoutManager( this ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
    }

    private void setTitles() {
        getSupportActionBar().setTitle( artist );
    }

    public void setToolbarImage()
    {
        ImageView image = findViewById(R.id.album_art);
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
            holder.setImage( ArtistOnlyActivity.this, music );
            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );

            holder.itemView.setOnClickListener(v -> {
                MusicPlayer.play( music );
            });
        }

        @Override
        public int getItemCount() {
            return musics.size();
        }
    }

}
