package com.gcodes.iplayer.music.artist;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class ArtistFragment extends Fragment {
    private String selection = null;

    private String sort = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER + " asc";

    private String[] projection = {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.ARTIST_KEY,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
    };

    private static Cursor cursor;

    public ArtistFragment()
    {

    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        load();
    }

    public void load()
    {
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    projection, selection, null, sort );
            cursor = loader.loadInBackground();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        GridLayoutManager layout = new GridLayoutManager(getContext(), 2);
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);
        return view;
    }


    public class CustomAdapter extends CursorRecyclerViewAdapter< ItemHolder >
    {
        public CustomAdapter()
        {
            super( ArtistFragment.this.getContext(), cursor );
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemHolder holder, Cursor cursor)
        {
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
            String artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
            long artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            String albumArt = getArtPath(artist);
            int albums = cursor.getInt( cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
            int tracks = cursor.getInt( cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));

            String subtitle = String.format("%d %s %d %s", albums, albums > 1 ? "Albums" : "Album",
                    tracks, tracks > 1 ? "Tracks" : "Track" );

            holder.setTitle(artist);
            holder.setSubtitle( subtitle );
            holder.setImage( albumArt );

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent( ArtistFragment.this.getContext(), ArtistActivity.class );
                    intent.putExtra( "artist_key", artistKey );
                    intent.putExtra( "artist_id", artistId );
                    intent.putExtra( "artist", artist );
                    intent.putExtra( "album_art", albumArt );
                    ArtistFragment.this.startActivity( intent );
                }
            });
        }

        public String getArtPath( String artist )
        {
            CursorLoader loader = new CursorLoader( ArtistFragment.this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST},
                    MediaStore.Audio.Albums.ARTIST + "=?",
                    new String[]{ artist },
                    null);
            Cursor cursor = loader.loadInBackground();
            String path = null;

            if ( cursor != null )
            {
//                Log.d( "Albums_Detail", "Albums " + Arrays.toString(cursor.getColumnNames()));
                if ( cursor.moveToFirst()) {
                    path = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART) );
                    // do whatever you need to do
                    cursor.close();
                }
            }

            return path;
        }

    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {

        private TextView title;
        private TextView subtitle;
        private ImageView image;

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

        public void setImage( int id )
        {
            String path = getPath( id );
            setImage( path );
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

        public String getPath( int id )
        {
            CursorLoader loader = new CursorLoader( ArtistFragment.this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + "=?",
                    new String[]{ String.valueOf(id)},
                    null);
            Cursor cursor = loader.loadInBackground();
            String path = null;

            if ( cursor != null )
            {
//                Log.d( "Albums_Detail", "Albums " + Arrays.toString(cursor.getColumnNames()));
                if ( cursor.moveToFirst()) {
                    path = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART) );
                    // do whatever you need to do
                    cursor.close();
                }
            }

            return path;
        }
    }

}
