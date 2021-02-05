package com.gcodes.iplayer.music.artist;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.models.Artist;
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

import java.util.ArrayList;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class ArtistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private ArrayList<Artist> artists = new ArrayList<>();

    private CustomAdapter adapter;

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.ARTIST.getId(), null, this);
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
        GridLayoutManager layout = new GridLayoutManager(getContext(), getSpan());
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);

        listView.addItemDecoration( new UIConstance.AppItemDecorator( getSpan()) );
        return view;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( this.getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                Artist.projection, null, null, Artist.sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if ( artists == null )
            artists = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                artists.add( Artist.getInstance(cursor));
            } while ( cursor.moveToNext() );
        }
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        artists = null;
        adapter.notifyDataSetChanged();
    }


    public class CustomAdapter extends RecyclerView.Adapter<ArtistItemHolder>
    {
        @NonNull
        @Override
        public ArtistItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_full, parent, false);
            return new ArtistItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ArtistItemHolder holder, int position) {
            Artist artist = artists.get(position);
            holder.setTitle(artist.getArtist());
            String subtitle = String.format("%d %s %d %s", artist.getAlbumCount(), artist.getAlbumCount() > 1 ? "Albums" : "Album",
                    artist.getTrackCount(), artist.getTrackCount() > 1 ? "Tracks" : "Track" );
            holder.setSubtitle( subtitle );
            holder.setImage( ArtistFragment.this, String.valueOf(artist.getArtistId()) );

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Bundle args = new Bundle();
                    args.putString( "artist_key", artist.getArtistKey() );
                    args.putLong( "artist_id", artist.getArtistId() );
                    args.putString( "artist", artist.getArtist() );
                    NavHostFragment.findNavController(ArtistFragment.this).navigate(R.id.action_musicFragment_to_mainArtistFragment, args);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (artists != null)
                return artists.size();
            return 0;
        }
    }

}
