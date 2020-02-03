//package com.gcodes.iplayer.backup;
//
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import com.gcodes.iplayer.R;
//import com.gcodes.iplayer.music.Music;
//import com.gcodes.iplayer.music.player.MusicPlayer;
//
//import java.util.ArrayList;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.loader.content.CursorLoader;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//public class AlbumSession extends AppCompatActivity
//{
////    private String selection;
//    private final String selection = String.format( "%s != ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
//            MediaStore.Audio.Media.ALBUM_KEY );
//    private final String artistSelection = String.format( "%s != ? and %s = ? and %s = ?", MediaStore.Audio.Media.IS_MUSIC,
//            MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Artists.ARTIST_KEY );
//    private final String trackSelection = String.format( "%s = ?", MediaStore.Audio.Genres.Members.ALBUM_KEY );
//
////    private String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";
//    private final String sort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";
//    private final String genreSort = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";
//
//    private String albumKey;
//    private CursorLoader artLoader;
//
//    private final String[] projection = {
//            MediaStore.Audio.Media._ID,
//            MediaStore.Audio.Media.ALBUM,
//            MediaStore.Audio.Media.ALBUM_KEY,
//            MediaStore.Audio.Media.ARTIST,
//            MediaStore.Audio.Media.ARTIST_KEY,
//            MediaStore.Audio.Media.TITLE,
//            MediaStore.Audio.Media.ALBUM_ID
//    };
//
//    private final String[] genreProjection = {
//            MediaStore.Audio.Genres.Members._ID,
//            MediaStore.Audio.Genres.Members.ALBUM,
//            MediaStore.Audio.Genres.Members.ALBUM_KEY,
//            MediaStore.Audio.Genres.Members.ARTIST_KEY,
//            MediaStore.Audio.Genres.Members.ARTIST,
//            MediaStore.Audio.Genres.Members.TITLE,
//            MediaStore.Audio.Genres.Members.ALBUM_ID,
//    };
//
//    private ArrayList<Music> musics;
//    private Cursor cursor;
//    private Toolbar toolbar;
//    private String albumName;
//    private String albumArt;
//    private String from;
//    private long genreId;
//    private String artistKey;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_album);
//
//        toolbar = findViewById(R.id.album_toolbar);
//        setSupportActionBar(toolbar);
//
//        init();
//        initView();
//    }
//
//    private void init() {
//        initArgs();
//        initTracks();
//    }
//
//    private void initArgs()
//    {
//        from = getIntent().getStringExtra( "from" );
//        switch ( from )
//        {
//            case "album":
//                albumKey = getIntent().getStringExtra( "album_key" );
//                albumName = getIntent().getStringExtra("album_name");
//                albumArt = getIntent().getStringExtra("album_art");
//                break;
//
//            case "genre":
//                albumKey = getIntent().getStringExtra( "album_key" );
//                albumName = getIntent().getStringExtra("album_name");
//                albumArt = getIntent().getStringExtra("album_art");
//                genreId = getIntent().getLongExtra("genre_id", 0);
//                break;
//
//            case "artist":
//                albumKey = getIntent().getStringExtra( "album_key" );
//                artistKey = getIntent().getStringExtra( "artist_key" );
//                albumName = getIntent().getStringExtra("album_name");
//                albumArt = getIntent().getStringExtra("album_art");
//                break;
//        }
//
//    }
//
//    public void initTracks()
//    {
//        switch ( from )
//        {
//            case "album":
//                initAlbumTracks();
//                break;
//
//            case "genre":
//                initGenreTracks();
//                break;
//
//            case "artist":
//                initArtistTracks();
//                break;
//        }
//    }
//
//    public void initAlbumTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this,
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
//                selection, new String[]{ "0", String.valueOf(albumKey)}, sort );
//        cursor = loader.loadInBackground();
//        artLoader = new CursorLoader( this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
//                MediaStore.Audio.Albums._ID + "=?", null,
//                null);
//        loadMusic();
//    }
//
//    public void initArtistTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
//                artistSelection, new String[]{ "0", String.valueOf(albumKey), String.valueOf(artistKey)}, sort );
//        cursor = loader.loadInBackground();
//        artLoader = new CursorLoader( this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
//                MediaStore.Audio.Albums._ID + "=?", null,
//                null);
//        loadMusic();
//    }
//
//    public void initGenreTracks()
//    {
//        musics = new ArrayList<>();
//        CursorLoader loader = new CursorLoader( this,
//                MediaStore.Audio.Genres.Members.getContentUri( "external", genreId ), genreProjection,
//                trackSelection, new String[]{ String.valueOf(albumKey) }, genreSort );
//        cursor = loader.loadInBackground();
//        artLoader = new CursorLoader( this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
//                MediaStore.Audio.Albums._ID + "=?", null,
//                null);
//        loadGenreMusic();
//    }
//
//    private void loadMusic() {
//        cursor.moveToFirst();
//        do
//        {
//            musics.add( Music.getIntance(cursor, artLoader) );
//        } while ( cursor.moveToNext() );
//    }
//
//    private void loadGenreMusic() {
//        cursor.moveToFirst();
//        do
//        {
//            musics.add( Music.getGenreIntance(cursor, artLoader) );
//        } while ( cursor.moveToNext() );
//    }
//
//    public void initView() {
//        initRecycleView();
//        initToolbar();
//    }
//
//    private void initToolbar() {
//        setToolbarImage();
//        setTitles();
//    }
//
//    private void setTitles() {
//        getSupportActionBar().setTitle( albumName );
//    }
//
//    public void initRecycleView() {
//        RecyclerView listView = findViewById(R.id.list_track);
//        CustomAdapter adapter = new CustomAdapter();
//        listView.setLayoutManager( new LinearLayoutManager( this ) );
//        listView.setAdapter(adapter);
//    }
//
//
//
//    public void setToolbarImage()
//    {
//        ImageView image = findViewById(R.id.album_art);
//        if ( albumArt != null )
//        {
//            Bitmap bitmap = BitmapFactory.decodeFile(albumArt);
//            if ( bitmap != null )
//            {
//                image.setImageBitmap( bitmap );
//                return;
//            }
//        }
//        int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
//                AlbumSession.this.getPackageName());
//        image.setImageResource( resId );
//    }
//
//
//    public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
//    {
//        @Override
//        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.playlist_item_view, parent, false);
//            return new ItemHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
//            Music music = musics.get( position );
//            holder.setTitle( music.getName() );
//            holder.setSubtitle( music.getArtist() );
////            holder.setImage( music.toUri() );
//            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );
////            holder.setImage( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
//
//            holder.itemView.setOnClickListener(v -> {
//                MusicPlayer.play( music );
//            });
//        }
//
//        @Override
//        public int getItemCount() {
//            return musics.size();
//        }
//    }
//
//    public class ItemHolder extends RecyclerView.ViewHolder
//    {
//        private TextView title;
//        private TextView subtitle;
//
//        public ItemHolder(@NonNull View itemView) {
//            super(itemView);
//            title = itemView.findViewById(R.id.item_title);
//            subtitle = itemView.findViewById(R.id.item_subtitle);
//        }
//
//        public String getTitle() {
//            return title.getText().toString();
//        }
//
//        public void setTitle(String name) {
//            this.title.setText( name );
//        }
//
//        public String getSubtitle() {
//            return subtitle.getText().toString();
//        }
//
//        public void setSubtitle(String subtitle) {
//            this.subtitle.setText(subtitle);
//        }
//
//    }
//
//}
