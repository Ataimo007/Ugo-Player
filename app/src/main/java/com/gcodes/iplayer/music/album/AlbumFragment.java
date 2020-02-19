package com.gcodes.iplayer.music.album;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class AlbumFragment extends Fragment {
    private String selection = null;

//    private String sort = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER + " asc";
    private String sort = MediaStore.Audio.Albums._ID + " asc";

    private String[] projection = {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM_KEY,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ALBUM_ART
    };

    private static Cursor cursor;

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
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
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
        GridLayoutManager layout = new GridLayoutManager(getContext(), getSpan() );
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new UIConstance.AppItemDecorator( getSpan()));

        return view;
    }


    public class CustomAdapter extends CursorRecyclerViewAdapter<AlbumItemHolder>
    {
        public CustomAdapter()
        {
            super( AlbumFragment.this.getContext(), cursor );
        }

        @Override
        public AlbumItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new AlbumItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final AlbumItemHolder holder, Cursor cursor)
        {
            String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
            String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
            String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
            String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

            holder.setTitle(albumName);
            holder.setSubtitle( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
//            holder.setImage(albumArt);
            holder.setImage(AlbumFragment.this, albumId);

            holder.itemView.setOnClickListener(v -> {
//                AlbumSession album = new AlbumSession();
//                Bundle args = new Bundle();

                Intent intent = new Intent( AlbumFragment.this.getContext(), AlbumActivity.class );
                intent.putExtra( "from", "album" );
                intent.putExtra( "album_key", albumKey );
                intent.putExtra( "album_name", albumName );
                intent.putExtra( "album_art", albumArt );

//                args.putString( "from", "album" );
//                args.putString( "album_key", albumKey );
//                args.putString( "album_name", albumName );
//                args.putString( "album_art", albumArt );

//                album.setArguments( args );

                AlbumFragment.this.startActivity( intent );

//                MainActivity.renderSession( album );
            });
        }

    }
}
