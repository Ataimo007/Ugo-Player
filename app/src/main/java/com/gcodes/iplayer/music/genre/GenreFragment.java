package com.gcodes.iplayer.music.genre;

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
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.album.AlbumFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
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

public class GenreFragment extends Fragment
{
    private String selection = null;

    private String sort = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER + " asc";

    private String[] genreProjection = {
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    private String[] mediaProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.ARTIST_ID,
            MediaStore.Audio.Genres.Members.ARTIST_KEY,
    };

//    private String[] mediaProjection = {
//            MediaStore.Audio.Media._ID,
//            MediaStore.Audio.Media.ALBUM_ID
//    };

    private static Cursor cursor;

    public GenreFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
    }

    public void load()
    {
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    genreProjection, selection, null, sort );
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
            super(GenreFragment.this.getContext(), cursor);
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
            String genre = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME));
            long genreId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
            holder.setTitle(genre);
            String albumArt = bindHolder(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Genres._ID)), holder);

//            String artistKey = genreCursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY));

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent( GenreFragment.this.getContext(), GenreActivity.class );
//                    intent.putExtra( "artist_key", artistKey );
                    intent.putExtra( "genre_id", genreId );
                    intent.putExtra( "genre", genre );
                    intent.putExtra( "album_art", albumArt );
                    GenreFragment.this.startActivity( intent );
                }
            });
        }

        public String getArtPath( long id )
        {
            CursorLoader loader = new CursorLoader( GenreFragment.this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{ MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART },
                    MediaStore.Audio.Albums._ID + "=?",
                    new String[]{ String.valueOf( id ) },
                    null);
            Cursor cursor = loader.loadInBackground();
            String path = null;

            if ( cursor != null )
            {
                if ( cursor.moveToFirst()) {
                    path = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART) );
                    cursor.close();
                }
            }

            return path;
        }

        public String bindHolder(int id, ItemHolder holder )
        {
            CursorLoader loader = new CursorLoader( GenreFragment.this.getContext(),
                    MediaStore.Audio.Genres.Members.getContentUri("external", id ), mediaProjection,
                    null, null, null );
            Cursor cursor = loader.loadInBackground();
            int count = cursor.getCount();
            holder.setSubtitle( String.format( "%d %s", count, count > 1 ? "Tracks" : "Track" ) );

            // set album art
            if ( cursor.moveToFirst() )
            {
                do {
//                    String path = getArtPath( cursor.getLong( cursor.getColumnIndex( MediaStore.Audio.Genres.Members.ALBUM_ID ) ) );
//                    if ( path != null )
//                    {
//                        boolean success = holder.setAbsuluteImage(path);
//                        if ( success )
//                            return path;
//                    }
                    String path = getArtPath( cursor.getLong( cursor.getColumnIndex( MediaStore.Audio.Genres.Members.ALBUM_ID ) ) );
                }
                while ( cursor.moveToNext() );
            }
            else
                holder.setDefualtImage();

            return null;
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
//            int resId = getResources().getIdentifier("ic_playlist_black_24dp", "drawable",
//                    getContext().getPackageName());
//            this.image.setImageResource( resId );
//        }

        public void setDefualtImage()
        {
            int resId = getResources().getIdentifier("ic_playlist_black_24dp", "drawable",
                    getContext().getPackageName());
            this.image.setImageResource( resId );
        }

        public void setImage( String id )
        {
            GlideApp.with( GenreFragment.this ).load( new ProcessModelLoaderFactory.AlbumProcessFetcher( GenreFragment.this, id ) )
                    .placeholder( R.drawable.u_genre_solid ).apply( centerCropTransform() ).into( image );
        }

        public boolean setAbsuluteImage( String path )
        {
            if ( path != null )
            {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                if ( bitmap != null )
                {
                    image.setImageBitmap( bitmap );
                    return true;
                }
            }
            return false;
        }
    }

}
