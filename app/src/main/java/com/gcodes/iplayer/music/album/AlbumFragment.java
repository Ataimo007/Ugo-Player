package com.gcodes.iplayer.music.album;

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

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.track.TrackFragment;

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
            super( AlbumFragment.this.getContext(), cursor );
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
            String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
            String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
            String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
            String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

            holder.setTitle(albumName);
            holder.setSubtitle( cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
//            holder.setImage(albumArt);
            holder.setImage(albumId);

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

//        public void setImage( String path )
//        {
//            if ( path != null )
//            {
//                Bitmap bitmap = BitmapFactory.decodeFile(path);
//                if ( bitmap != null )
//                {
//                    image.setImageBitmap( bitmap );
//                    return;
//                }
//            }
//            int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
//                    getContext().getPackageName());
//            this.image.setImageResource( resId );
//        }

        public void setImage( String id )
        {
            GlideApp.with( AlbumFragment.this ).load( new ProcessModelLoaderFactory.AlbumProcessFetcher( AlbumFragment.this, id ) )
                    .placeholder( R.drawable.u_album_solid ).apply( circleCropTransform() ).into( image );
        }
    }

}
