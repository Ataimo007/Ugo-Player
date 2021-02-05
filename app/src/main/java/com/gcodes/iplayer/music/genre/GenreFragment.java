package com.gcodes.iplayer.music.genre;

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
import com.gcodes.iplayer.music.models.Genre;
import com.gcodes.iplayer.music.models.Music;
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

public class GenreFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private CustomAdapter adapter;
    private LoaderManager loader;
    private ArrayList<Genre> genres;

    public GenreFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        loader = LoaderManager.getInstance(requireActivity());
        loader.initLoader(MainActivity.AppLoader.GENRE.getId(), null, this);
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
                Genre.projection, null, null, Genre.sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if ( genres == null )
            genres = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                genres.add(Genre.getInstance(cursor));
            } while ( cursor.moveToNext() );
        }
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        genres = null;
        adapter.notifyDataSetChanged();
    }


    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        private final LoaderManager loader;
        public CustomAdapter() {
            loader = LoaderManager.getInstance(GenreFragment.this);
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_full, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            Genre genre = genres.get(position);
            holder.setTitle(genre.getName());
            holder.setImage(genre.getId());

            Integer genreCount = genre.getCount(position, loader, getContext(), count -> {
                holder.setSubtitle(String.format("%d %s", count, count > 1 ? "Tracks" : "Track"));
//                notifyItemChanged(position);
            });
            if (genreCount != null)
                holder.setSubtitle(String.format("%d %s", genreCount, genreCount > 1 ? "Tracks" : "Track"));

//            Integer genreCount = genre.getCount(getContext());
//            holder.setSubtitle(String.format("%d %s", genreCount, genreCount > 1 ? "Tracks" : "Track"));


            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Bundle args = new Bundle();
                    args.putLong( "genre_id", genre.getId() );
                    args.putString( "genre", genre.getName() );
                    NavHostFragment.findNavController( GenreFragment.this ).navigate(R.id.action_musicFragment_to_mainGenreFragment, args);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (genres != null)
                return genres.size();
            return 0;
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
