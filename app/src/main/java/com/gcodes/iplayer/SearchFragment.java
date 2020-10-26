package com.gcodes.iplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.models.Album;
import com.gcodes.iplayer.music.models.Artist;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class SearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private LoaderManager loader;

    private final String musicAll = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    private final String musicFilter = String.format("%s != 0 and %s like %%?%%", MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.TITLE );

    private final String artistFilter = String.format("%s like %%?%%", MediaStore.Audio.Artists.ARTIST );
    private final String albumFilter = String.format("%s like %%?%%", MediaStore.Audio.Albums.ALBUM );
    private String[] albumProjection = {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM_KEY,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ALBUM_ART
    };

    private final String genreFilter = String.format("%s like %%?%%", MediaStore.Audio.Genres.NAME);
    private String[] genreProjection = {
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    private String[] genreContentProjection = {
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.Members.ALBUM_ID,
            MediaStore.Audio.Genres.Members.ARTIST,
            MediaStore.Audio.Genres.Members.ARTIST_ID,
            MediaStore.Audio.Genres.Members.ARTIST_KEY,
    };
    private ContentAdapter adapter;

//    private final ArrayList<Music> musics = new ArrayList<>();
//    private final ArrayList<Artist> artists = new ArrayList<>();
//    private final ArrayList<Album> albums = new ArrayList<>();

    private List<Object> items;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        load();
    }

    private void updateAll(ArrayList items)
    {
        if (items == null)
            this.items = items;
        else
        {
            TreeSet<Object> set = getSet();
            set.addAll(this.items);
            set.addAll(items);
            this.items = Arrays.asList(set.toArray());
        }
    }

    private void update(ArrayList items)
    {
        if (items == null)
            this.items = Arrays.asList(items);
        else
        {
            TreeSet<Object> set = getSet();
            set.addAll(this.items);
            set.add(items);
            this.items = Arrays.asList(set.toArray());
        }
    }

    private TreeSet<Object> getSet()
    {
        return new TreeSet<>((o1, o2) -> {
            int itemWeight1 = getItemWeight(o1);
            int itemWeight2 = getItemWeight(o2);
            if ( itemWeight1 == itemWeight2 && itemWeight1 == 2 )
            {
                return ((Music) o1).compareTo((Music) o2);
            }
            return itemWeight1 - itemWeight2;
        });
    }

    private int getItemWeight( Object item )
    {
        if ( item instanceof ArrayList )
        {
            if (((ArrayList) item).get(0) instanceof Artist)
                return 1;
            if (((ArrayList) item).get(0) instanceof Album)
                return 3;
//            if (((ArrayList) item).get(0) instanceof Genre)
//                return 3;
        }
        return 2;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_search, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        RecyclerView content = view.findViewById(R.id.main_content);
        adapter = new ContentAdapter();
        content.setLayoutManager( new LinearLayoutManager( view.getContext() ) );
        content.setAdapter(adapter);
        content.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
    }

    private void load() {
        loader = LoaderManager.getInstance(this);
        loader.initLoader(0, null, this);
        loader.initLoader(1, null, this);
        loader.initLoader(2, null, this);
//        loader.initLoader(3, null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id)
        {
            case 0:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                            Artist.projection, null, null, Artist.sort );
                else
                    return new CursorLoader( getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                            Artist.projection, artistFilter, new String[]{args.getString("filter")}, Artist.sort );

            case 1:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Music.projection, musicAll, null, Music.sort );
                else
                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Music.projection, musicFilter, new String[]{args.getString("filter")}, Music.sort );
//
//            case 2:
//                if ( args == null )
//                    return new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                            Album.projection, null, null, Album.sort );
//                else
//                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                            Album.projection, albumFilter, new String[]{args.getString("filter")}, Album.sort );

//            case 3:
//                if ( args == null )
//                    return new CursorLoader( getContext(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
//                            genreProjection, null, null, sort );
//                else
//                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                            genreProjection, genreFilter, new String[]{args.getString("filter")}, sort );

            default:
                return new Loader<>(getContext());
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId())
        {
            case 0:
                ArrayList<Artist> artists = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        artists.add( Artist.getInstance(cursor));
                    } while ( cursor.moveToNext() );
                }
                update(artists);
                adapter.submitArtist(artists);
                break;

            case 1:
                ArrayList<Music> musics = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        musics.add( Music.getInstance(cursor));
                    } while ( cursor.moveToNext() );
                }
                updateAll(musics);
                adapter.submitMusic(musics);
                break;

            case 2:
                ArrayList<Album> albums = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        albums.add( Album.getInstance(cursor));
                    } while ( cursor.moveToNext() );
                }
                update(albums);
                adapter.submitAlbum(albums);
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType)
            {
                case 0:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.search_list_artist, parent, false);
                    return new ArtistHolder(view);

                case 1:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.search_list_track, parent, false);
                    return new TrackHolder(view);

                case 2:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.search_list_category, parent, false);
                    return new AlbumHolder(view);

            }
            return null;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TrackHolder)
            {
                TrackHolder tracksHolder = (TrackHolder) holder;
                tracksHolder.submitList(musics);
            }
            if (holder instanceof ArtistHolder)
            {
                ArtistHolder artistsHolder = (ArtistHolder) holder;
                artistsHolder.submitList(artists);
            }
            if (holder instanceof AlbumHolder)
            {
                AlbumHolder albumHolder = (AlbumHolder) holder;
                albumHolder.submitList(albums);
            }
        }

        public void submitMusic(List<Music> list)
        {
            notifyItemChanged(1);
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public void submitArtist(ArrayList<Artist> artists) {
            notifyItemChanged(0);
        }

        public void submitAlbum(ArrayList<Album> artists) {
            notifyItemChanged(2);
        }
    }

    private class TrackHolder extends RecyclerView.ViewHolder
    {
        private final TrackAdapter trackAdapter;
        public final DiffUtil.ItemCallback<Music> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Music>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Music oldUser,  @NonNull Music newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getMediaId() == newUser.getMediaId();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Music oldUser,  @NonNull Music newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };

        public TrackHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView tracks = itemView.findViewById(R.id.track_list);
            trackAdapter = new TrackAdapter(DIFF_CALLBACK);
            tracks.setLayoutManager( new LinearLayoutManager( itemView.getContext() ) );
            tracks.setAdapter(trackAdapter);
            tracks.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
        }

        public void submitList(List<Music> list)
        {
            trackAdapter.submitList(list);
        }

        public class TrackAdapter extends ListAdapter<Music, TrackItemHolder>
        {
            private static final int defaultCount = 10;
            private boolean expanded = false;

            protected TrackAdapter(@NonNull DiffUtil.ItemCallback<Music> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public TrackItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_view, parent, false);
                return new TrackItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull TrackItemHolder holder, int position) {
                Music music = getItem(position);
                holder.setTitle( music.getName() );
                holder.setSubtitle( music.getArtist() );
                holder.setImage( TrackHolder.this.itemView.getContext(), music );

                holder.itemView.setOnClickListener(v -> {
                    Log.d("Player_Manager", "playing " + music);
                    new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
                });
            }

            public void expand()
            {
                expanded = true;
            }

            @Override
            public int getItemCount() {
                if (expanded)
                    return super.getItemCount();
                return Math.min(super.getItemCount(), defaultCount);
            }
        }
    }

    private class ArtistHolder extends RecyclerView.ViewHolder
    {
        private final ArtistAdapter artistAdapter;
        public final DiffUtil.ItemCallback<Artist> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Artist>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Artist oldUser,  @NonNull Artist newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getArtistId() == newUser.getArtistId();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Artist oldUser,  @NonNull Artist newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };

        public ArtistHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView tracks = itemView.findViewById(R.id.artist_list);
            artistAdapter = new ArtistAdapter(DIFF_CALLBACK);
            tracks.setLayoutManager( new LinearLayoutManager( itemView.getContext(), RecyclerView.HORIZONTAL, false ) );
            tracks.setAdapter(artistAdapter);
            tracks.addItemDecoration(new UIConstance.HorizontalSpanDecorator());
        }

        public void submitList(List<Artist> list)
        {
            artistAdapter.submitList(list);
        }

        public class ArtistAdapter extends ListAdapter<Artist, ArtistItemHolder>
        {
            protected ArtistAdapter(@NonNull DiffUtil.ItemCallback<Artist> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public ArtistItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.artist_view, parent, false);
                return new ArtistItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ArtistItemHolder holder, int position) {
                Artist artist = getItem(position);
                holder.setTitle( artist.getArtist() );
                holder.setImage(artist.getArtistId());

                holder.itemView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString( "artist_key", artist.getArtistKey() );
                    args.putLong( "artist_id", artist.getArtistId() );
                    args.putString( "artist", artist.getArtist() );
                    NavHostFragment.findNavController(SearchFragment.this).navigate(R.id.action_musicFragment_to_mainArtistFragment, args);
                });
            }


        }

        private class ArtistItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;

            public ArtistItemHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.item_image);
                title = itemView.findViewById(R.id.item_title);
            }

            public void setImage(long id)
            {
                GlideApp.with( itemView.getContext() ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( itemView.getContext(), String.valueOf(id), MediaStore.Audio.Media.ARTIST_ID ) )
                        .placeholder( R.drawable.u_artist_avatar ).apply( centerCropTransform() ).into( image );
            }

            public void setTitle(String title)
            {
                this.title.setText(title);
            }
        }
    }

    private class AlbumHolder extends RecyclerView.ViewHolder
    {
        private final AlbumAdapter artistAdapter;
        public final DiffUtil.ItemCallback<Album> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Album>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Album oldUser,  @NonNull Album newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getAlbumId() == newUser.getAlbumId();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Album oldUser,  @NonNull Album newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }

                    @Nullable
                    @Override
                    public Object getChangePayload(@NonNull Album oldItem, @NonNull Album newItem) {
                        return super.getChangePayload(oldItem, newItem);
                    }
                };

        public AlbumHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView tracks = itemView.findViewById(R.id.category_list);
            artistAdapter = new AlbumAdapter(DIFF_CALLBACK);
            tracks.setLayoutManager( new LinearLayoutManager( itemView.getContext() ) );
            tracks.setAdapter(artistAdapter);
            tracks.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));
        }

        public void submitList(List<Album> list)
        {
            artistAdapter.submitList(list);
        }

        public class AlbumAdapter extends ListAdapter<Album, AlbumItemHolder>
        {
            protected AlbumAdapter(@NonNull DiffUtil.ItemCallback<Album> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public AlbumItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card_full2, parent, false);
                return new AlbumItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull AlbumItemHolder holder, int position) {
                Album album = getItem(position);
                holder.setTitle( album.getAlbum() );
                holder.setImage(album.getAlbumId());

                holder.itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString( "from", "album" );
                    bundle.putString( "album_key", album.getAlbumKey() );
                    bundle.putString( "album_name", album.getAlbum() );

                    Navigation.findNavController( getView() ).navigate( R.id.action_musicFragment_to_mainAlbumFragment, bundle );
                });
            }
        }

        private class AlbumItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;

            public AlbumItemHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.item_image);
                title = itemView.findViewById(R.id.item_title);
            }

            public void setImage(long id)
            {
                GlideApp.with( itemView.getContext() ).load( new ProcessModelLoaderFactory.MusicCategoryProcessFetcher( itemView.getContext(), String.valueOf(id), MediaStore.Audio.Media.ALBUM_ID ) )
                        .placeholder( R.drawable.u_song_solid ).apply( circleCropTransform() ).into( image );
            }

            public void setTitle(String title)
            {
                this.title.setText(title);
            }
        }
    }
}