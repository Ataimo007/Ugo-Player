package com.gcodes.iplayer.backup;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;


//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.music.Music;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TrackFragment extends Fragment
{
    private String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";

    private String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private static Cursor cursor;
    private CursorLoader artLoader;

    public TrackFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
        initFloatingAction();
    }

    private void initFloatingAction()
    {
        FloatingActionButton floating = getActivity().findViewById(R.id.action_floating);
        floating.setOnClickListener( v -> {
            Music musics[] = new Music[ cursor.getCount() ];
            Music music;
            cursor.moveToFirst();
            for ( int i = 0; cursor.moveToNext(); ++i )
            {
//                music = Music.getIntance(cursor);
//                musics[ i ] = music;
            }
//            MusicPlayer.play( TrackFragment.this, musics );
        });
    }

    public void load()
    {
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, null, sort );
            cursor = loader.loadInBackground();
        }
        artLoader = new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?", null,
                null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        return view;
    }


    public class CustomAdapter extends CursorRecyclerViewAdapter< ItemHolder >
    {
        public CustomAdapter()
        {
            super(TrackFragment.this.getContext(), cursor);
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemHolder holder, Cursor cursor)
        {
//            Music music = Music.getIntance( cursor );
//            holder.setTitle( music.getName() );
//            holder.setSubtitle( music.getArtist() );
//            holder.setImage( music.getArtPath() );
//            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );
//            holder.setImage( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));

            holder.itemView.setOnClickListener(v -> {
//                MusicPlayer.play( TrackFragment.this, music );
            });
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
