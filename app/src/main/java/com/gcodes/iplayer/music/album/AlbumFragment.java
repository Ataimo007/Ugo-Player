package com.gcodes.iplayer.music.album;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.music.models.Album;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.navigation.NavHostController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class AlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private ArrayList<Album> albums = new ArrayList<>();
    private CustomAdapter adapter;

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    private int getSpan()
    {
        return 2;
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

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( this.getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                Album.projection, null, null, Album.sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if ( albums == null )
            albums = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                albums.add( Album.getInstance(cursor));
            } while ( cursor.moveToNext() );
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        albums.clear();
        adapter.notifyDataSetChanged();
    }


    public class CustomAdapter extends RecyclerView.Adapter<AlbumItemHolder>
    {
        @NonNull
        @Override
        public AlbumItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new AlbumItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlbumItemHolder holder, int position) {
            Album album = albums.get(position);
            holder.setTitle(album.getAlbum());
            holder.setSubtitle( album.getArtist() );
            holder.setImage(AlbumFragment.this, String.valueOf(album.getAlbumId()));

            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString( "from", "album" );
                bundle.putString( "album_key", album.getAlbumKey() );
                bundle.putString( "album_name", album.getAlbum() );

                Navigation.findNavController( getView() ).navigate( R.id.action_musicFragment_to_mainAlbumFragment, bundle );
            });
        }


        @Override
        public int getItemCount() {
            return albums.size();
        }
    }
}
