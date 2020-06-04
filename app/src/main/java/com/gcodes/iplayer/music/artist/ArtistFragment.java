package com.gcodes.iplayer.music.artist;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

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

    private int getSpan()
    {
        return 2;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        GridLayoutManager layout = new GridLayoutManager(getContext(), getSpan());
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);

        listView.addItemDecoration( new UIConstance.AppItemDecorator( getSpan()) );
        return view;
    }


    public class CustomAdapter extends CursorRecyclerViewAdapter<ArtistItemHolder>
    {
        public CustomAdapter()
        {
            super( ArtistFragment.this.getContext(), cursor );
        }

        @Override
        public ArtistItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_full, parent, false);
            return new ArtistItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final ArtistItemHolder holder, Cursor cursor)
        {
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
            String artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
            long artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
//            String albumArt = getArtPath(artist);
            int albums = cursor.getInt( cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
            int tracks = cursor.getInt( cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));

            String subtitle = String.format("%d %s %d %s", albums, albums > 1 ? "Albums" : "Album",
                    tracks, tracks > 1 ? "Tracks" : "Track" );

            holder.setTitle(artist);
            holder.setSubtitle( subtitle );
//            holder.setImage( albumArt );
            holder.setImage( ArtistFragment.this, String.valueOf(artistId) );

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Bundle args = new Bundle();
                    args.putString( "artist_key", artistKey );
                    args.putLong( "artist_id", artistId );
                    args.putString( "artist", artist );
//                    intent.putExtra( "album_art", albumArt );
                    NavHostFragment.findNavController(ArtistFragment.this).navigate(R.id.action_musicFragment_to_mainArtistFragment, args);
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

}
