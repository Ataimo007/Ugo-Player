package com.gcodes.iplayer.music.track;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class TrackFragment extends Fragment
{
    private String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " asc";
//    private String sort = MediaStore.Audio.Media.ALBUM_ID + " asc";

    private String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private static Cursor cursor;
    private static ArrayList< Music > musics;
    private CursorLoader artLoader;

    public TrackFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
//        initFloatingAction();
    }

    @Override
    public void onStart() {
        super.onStart();
        initFloatingAction();
    }

    private void initFloatingAction()
    {
        FloatingActionButton floating = getActivity().findViewById(R.id.action_floating);
        floating.setOnClickListener( v -> {
            MusicPlayer.play( musics );
        });
    }

    public void load()
    {
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, null, sort );
            cursor = loader.loadInBackground();
            artLoader = new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + "=?", null,
                    null);
            loadMusic();
        }
    }

    private void loadMusic()
    {
        musics = new ArrayList<>();
        cursor.moveToFirst();
        do
        {
            musics.add( Music.getIntance(cursor, artLoader) );
        } while ( cursor.moveToNext() );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
//                outRect.top = 10;
                outRect.bottom = 10;
                super.getItemOffsets(outRect, view, parent, state);
            }
        });

        return view;
    }


    public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            Music music = musics.get( position );
            holder.setTitle( music.getName() );
            holder.setSubtitle( music.getArtist() );
            holder.setImage( music.getArtPath() );
//            holder.setImage( music.toUri() );
            Log.d( "Track_Fragment", "the art path " + music.getArtPath() );
//            holder.setImage( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));

            holder.itemView.setOnClickListener(v -> {
                MusicPlayer.play( music );
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

        public void setImage(Uri uri)
        {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource( TrackFragment.this.getContext(), uri);
            byte[] picture = retriever.getEmbeddedPicture();
            Bitmap albumArt = BitmapFactory.decodeByteArray(picture, 0, picture.length);
            image.setImageBitmap( albumArt );
        }
    }

}