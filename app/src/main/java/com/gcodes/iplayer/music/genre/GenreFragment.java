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
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
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

public class GenreFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
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

    private CustomAdapter adapter;
    private LoaderManager loader;

    public GenreFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        loader = LoaderManager.getInstance(this);
        loader.initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        GridLayoutManager layout = new GridLayoutManager(getContext(), getSpan() );
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new UIConstance.AppItemDecorator( getSpan()));

        return view;
    }

    private int getSpan() {
        return 2;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( this.getContext(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                genreProjection, selection, null, sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    public class CustomAdapter extends CursorRecyclerViewAdapter< ItemHolder >
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_full, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemHolder holder, Cursor cursor)
        {
            String genre = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME));
            long genreId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
            holder.setTitle(genre);
            holder.setImage( genreId );
            bindHolder(genreId, holder);
//            bindHolder(genreId, holder);

//            String artistKey = genreCursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST_KEY));

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Bundle args = new Bundle();
//                    intent.putExtra( "artist_key", artistKey );
                    args.putLong( "genre_id", genreId );
                    args.putString( "genre", genre );
//                    intent.putExtra( "album_art", albumArt );
                    NavHostFragment.findNavController( GenreFragment.this ).navigate(R.id.action_musicFragment_to_mainGenreFragment, args);
                }
            });
        }

//        public void bindHolder(long id, ItemHolder holder )
//        {
//            CursorLoader loader = new CursorLoader( GenreFragment.this.getContext(),
//                    MediaStore.Audio.Genres.Members.getContentUri("external", id ), mediaProjection,
//                    null, null, null );
//            Cursor cursor = loader.loadInBackground();
//            int count = cursor.getCount();
//            holder.setSubtitle( String.format( "%d %s", count, count > 1 ? "Tracks" : "Track" ) );
//
//            // set album art
//            holder.setImage( id );
//        }

        public void bindHolder(long genreId, ItemHolder holder)
        {
            loader.initLoader((int) genreId, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                @NonNull
                @Override
                public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                    return new CursorLoader( GenreFragment.this.getContext(),
                            MediaStore.Audio.Genres.Members.getContentUri("external", genreId ), mediaProjection,
                            null, null, null );
                }

                @Override
                public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                    int count = data.getCount();
                    holder.setSubtitle( String.format( "%d %s", count, count > 1 ? "Tracks" : "Track" ) );
                }

                @Override
                public void onLoaderReset(@NonNull Loader<Cursor> loader) {}
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

        public void setImage( long id )
        {
            GlideApp.with( GenreFragment.this ).load( new ProcessModelLoaderFactory.GenreProcessFetcher( GenreFragment.this, id ) ).placeholder( R.drawable.u_genre_solid ).apply( centerCropTransform() ).into( image );
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
