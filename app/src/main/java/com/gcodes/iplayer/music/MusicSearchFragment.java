package com.gcodes.iplayer.music;

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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.models.Album;
import com.gcodes.iplayer.music.models.Artist;
import com.gcodes.iplayer.music.models.Genre;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.music.track.TrackItemHolder;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicSearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, TextWatcher
{

    private LoaderManager loader;

    private final String musicAll = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private String filter;

    private ContentAdapter adapter;

    private ArrayList<Artist> artists;
    private ArrayList<Album> albums;
    private ArrayList<Genre> genre;

    private boolean tracksExpanded = false;
    private boolean expandable = false;
    private final int expandedLimit = 10;
    private ArrayList<Music> musics;
    private FloatingActionButton floating;

//    private LinkedList<Object> items;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        prepareItems();
        load();
    }

    @Override
    public void onStop() {
        super.onStop();
        floating.setImageResource(R.drawable.ic_media_shuffle);
        new ViewModelProvider(requireActivity()).get(MainActivity.NavigationModel.class).showNavigation.setValue(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        new ViewModelProvider(requireActivity()).get(MainActivity.NavigationModel.class).showNavigation.setValue(false);
        initVerticalScrollFloating();
    }

    private void initVerticalScrollFloating()
    {
        floating.setOnClickListener( v -> {
            loadLimit();
        });
        if (tracksExpanded)
        {
            floating.setImageResource(R.drawable.u_compress);
            floating.show();
        }
        else
            floating.hide();
    }

    private void initHorizontalScrollFloating(RecyclerView recyclerView, LinearLayoutManager layout)
    {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (layout.findFirstVisibleItemPosition() != 0)
                {
                    floating.setImageResource(R.drawable.u_back);
                    floating.show();
                    floating.setOnClickListener(v -> {
                        layout.scrollToPosition(0);
                    });
                }
                else
                {
                    floating.hide();
                }
            }
        });
    }

    //    private void prepareItems() {
//        items = new LinkedList<>();
//        items.add(new ArrayList<Artist>());
//        items.add(Music.EmptyMusic);
//        items.add(new ArrayList<Album>());
//        items.add(new ArrayList<>());
////        items.add(new ArrayList<Genre>());
//    }

//    private void updateAll(ArrayList items)
//    {
//        if (items == null)
//            this.items = items;
//        else
//        {
//            TreeSet<Object> set = getSet();
//            set.addAll(this.items);
//            set.addAll(items);
//            this.items = Arrays.asList(set.toArray());
//        }
//    }
//
//    private void update(ArrayList items)
//    {
//        if (items == null)
//            this.items = Arrays.asList(items);
//        else
//        {
//            TreeSet<Object> set = getSet();
//            set.addAll(this.items);
//            set.add(items);
//            this.items = Arrays.asList(set.toArray());
//        }
//    }

//    private TreeSet<Object> getSet()
//    {
//        return new TreeSet<>((o1, o2) -> {
//            int itemWeight1 = getItemWeight(o1);
//            int itemWeight2 = getItemWeight(o2);
//            if ( itemWeight1 == itemWeight2 && itemWeight1 == 2 )
//            {
//                return ((Music) o1).compareTo((Music) o2);
//            }
//            return itemWeight1 - itemWeight2;
//        });
//    }
//
//    private int getItemWeight( Object item )
//    {
//        if ( item instanceof ArrayList )
//        {
//            if (((ArrayList) item).get(0) instanceof Artist)
//                return 1;
//            if (((ArrayList) item).get(0) instanceof Album)
//                return 3;
////            if (((ArrayList) item).get(0) instanceof Genre)
////                return 3;
//        }
//        return 2;
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        floating = requireActivity().findViewById(R.id.action_floating);
        View view = inflater.inflate(R.layout.music_search, container, false);
        initView(view);
        TextInputEditText search = view.findViewById(R.id.search_field);
        search.addTextChangedListener(this);
//        load();
        return view;
    }

    private void initView(View view) {
        RecyclerView content = view.findViewById(R.id.main_content);
        adapter = new ContentAdapter();
        LinearLayoutManager layout = new LinearLayoutManager(view.getContext());
        content.setLayoutManager(layout);
        content.setAdapter(adapter);
        content.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));

        content.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != RecyclerView.SCROLL_STATE_IDLE)
                    initVerticalScrollFloating();
            }
        });
    }

    private void load() {
        if (loader == null)
            loader = LoaderManager.getInstance(requireActivity());
        loader.restartLoader(MainActivity.AppLoader.ARTIST.getId(), null, this);
        loader.restartLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
        loader.restartLoader(MainActivity.AppLoader.ALBUM.getId(), null, this);
        loader.restartLoader(MainActivity.AppLoader.GENRE.getId(), null, this);
    }

    private void reloadMusic() {
        if (loader == null)
            loader = LoaderManager.getInstance(requireActivity());

        if (filter != null)
        {
            Bundle args = new Bundle();
            args.putString("filter", filter);
            loader.restartLoader(MainActivity.AppLoader.TRACK.getId(), args, this);
        }
        else
        {
            loader.restartLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
        }
    }

    private void reload() {
        if (loader == null)
            loader = LoaderManager.getInstance(requireActivity());

        if (filter != null)
        {
            Bundle args = new Bundle();
            args.putString("filter", filter);
            loader.restartLoader(MainActivity.AppLoader.ARTIST.getId(), args, this);
            loader.restartLoader(MainActivity.AppLoader.TRACK.getId(), args, this);
            loader.restartLoader(MainActivity.AppLoader.ALBUM.getId(), args, this);
            loader.restartLoader(MainActivity.AppLoader.GENRE.getId(), args, this);
        }
        else
        {
            loader.restartLoader(MainActivity.AppLoader.ARTIST.getId(), null, this);
            loader.restartLoader(MainActivity.AppLoader.TRACK.getId(), null, this);
            loader.restartLoader(MainActivity.AppLoader.ALBUM.getId(), null, this);
            loader.restartLoader(MainActivity.AppLoader.GENRE.getId(), null, this);
        }

        Log.d("Music_Search", "Done reload " + filter);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        MainActivity.AppLoader appLoader = MainActivity.AppLoader.getInstance(id);
        switch (appLoader)
        {
            case ARTIST:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                            Artist.projection, null, null, Artist.sort );
                else
                {
                    String artistFilter = String.format("%s like '%%%s%%'", MediaStore.Audio.Artists.ARTIST, args.getString("filter") );
                    return new CursorLoader( getContext(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                            Artist.projection, artistFilter, null, Artist.sort );
                }

            case TRACK:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Music.projection, musicAll, null, Music.sort );
                else
                {
                    String musicFilter = String.format("%s != 0 and %s like '%%%s%%'", MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.TITLE, args.getString("filter") );
                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Music.projection, musicFilter, null, Music.sort );
                }

            case ALBUM:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            Album.projection, null, null, Album.sort );
                else
                {
                    String albumFilter = String.format("%s like '%%%s%%'", MediaStore.Audio.Albums.ALBUM, args.getString("filter") );
                    return new CursorLoader( getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Album.projection, albumFilter, null, Album.sort );
                }

            case GENRE:
                if ( args == null )
                    return new CursorLoader( getContext(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                            Genre.projection, null, null, Genre.sort );
                else
                {
                    String genreFilter = String.format("%s like '%%%s%%'", MediaStore.Audio.Genres.NAME, args.getString("filter"));
                    return new CursorLoader( getContext(), MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                            Genre.projection, genreFilter, null, Genre.sort );
                }

            default:
                return new Loader<>(getContext());
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
//        LinkedList<Object> newItems = new LinkedList<>(this.items);
        Log.d("Music_Search", "Cursor data " + cursor.getCount());
        MainActivity.AppLoader appLoader = MainActivity.AppLoader.getInstance(loader.getId());
        switch (appLoader)
        {
            case ARTIST:
                ArrayList<Artist> artists = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        artists.add( Artist.getInstance(cursor));
                    } while ( cursor.moveToNext() );
                }

                this.artists = artists;
                if (adapter != null)
                    adapter.notifyItemChanged(0);
                break;

            case TRACK:
                ArrayList<Music> musics = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    if (tracksExpanded)
                    {
                        do
                        {
                            musics.add( Music.getInstance(cursor));
                        } while ( cursor.moveToNext() );
                    }
                    else
                        for (int i = 0; (i == 0 || cursor.moveToNext()) && i < expandedLimit; ++i )
                            musics.add( Music.getInstance(cursor));
                }

                Log.d("Load_Visibility", "count is " + cursor.getCount());
                expandable = cursor.getCount() > expandedLimit;
                this.musics = musics;
                if (adapter != null)
                    adapter.notifyItemChanged(1);
                break;

            case ALBUM:
                TreeSet<Album> albums = new TreeSet<>((o1, o2) -> {
                    if (o1.getAlbum().equalsIgnoreCase(o2.getAlbum()))
                        return 0;
                    return o1.compareTo(o2);
                });
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        Album album = Album.getInstance(cursor);
                        Log.d("Search_Album", "album " + album);
                        albums.add(album);
                    } while ( cursor.moveToNext() );
                }

                this.albums = new ArrayList<>(albums);
                if (adapter != null)
                    adapter.notifyItemChanged(2);
                break;

            case GENRE:
                ArrayList<Genre> genre = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    do
                    {
                        genre.add( Genre.getInstance(cursor));
                    } while ( cursor.moveToNext() );
                }

                this.genre = genre;
                if (adapter != null)
                    adapter.notifyItemChanged(3);
        }
    }

    private void loadLimit()
    {
        tracksExpanded = false;
//        reload();
        reloadMusic();
        floating.hide();
    }

    private void loadAll()
    {
        tracksExpanded = true;
//        reload();
        reloadMusic();
        floating.setImageResource(R.drawable.u_compress);
        floating.show();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

//    public DiffUtil.ItemCallback<Object> SEARCH_DIFF_CALLBACK =
//            new DiffUtil.ItemCallback<Object>() {
//                @Override
//                public boolean areItemsTheSame(
//                        @NonNull Object oldUser, @NonNull Object newUser) {
//                    // User properties may have changed if reloaded from the DB, but ID is fixed
//                    if (oldUser instanceof Music && newUser instanceof Music)
//                    {
//                        Music oldMusic = (Music) oldUser;
//                        Music newMusic = (Music) newUser;
//                        return oldMusic.getMediaId() == newMusic.getMediaId();
//                    }
//                    if (oldUser instanceof ArrayList && newUser instanceof ArrayList)
//                    {
//                        return items.indexOf(oldUser) == items.indexOf(newUser);
//                    }
//                    return false;
//                }
//                @Override
//                public boolean areContentsTheSame(
//                        @NonNull Object oldUser, @NonNull Object newUser) {
//                    // NOTE: if you use equals, your object must properly override Object#equals()
//                    // Incorrectly returning false here will result in too many animations.
//                    if (oldUser instanceof Music)
//                    {
//                        Music oldMusic = (Music) oldUser;
//                        Music newMusic = (Music) newUser;
//                        return oldMusic.equals(newMusic);
//                    }
//                    if (oldUser instanceof ArrayList && newUser instanceof ArrayList)
//                    {
//                        ArrayList oldList = (ArrayList) oldUser;
//                        ArrayList newList = (ArrayList) newUser;
//                        return oldList.size() == newList.size();
//                    }
//                    return false;
//                }
//            };
    public DiffUtil.ItemCallback<Music> SEARCH_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Music>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Music oldUser, @NonNull Music newUser) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldUser.getMediaId() == newUser.getMediaId();
                }
                @Override
                public boolean areContentsTheSame(
                        @NonNull Music oldUser, @NonNull Music newUser) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                   return oldUser.equals(newUser);
                }
            };

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String query = s.toString();
        Log.d("Music_Search", "Query is " + query);
        query = !TextUtils.isEmpty(query) ? query : null;

        Log.d("Music_Search", "Query now is " + query);

        if (query == null && filter == null)
            return;

        if (query == null)
            filter = null;
        else if (filter == null || !filter.equalsIgnoreCase(query))
            filter = query;
        reload();
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

                case 3:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.search_list_category, parent, false);
                    return new GenreHolder(view);

                default:
                    return new RecyclerView.ViewHolder(new View(getContext())){};
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TrackHolder)
            {
                TrackHolder trackHolder = (TrackHolder) holder;
                trackHolder.submitList(musics);
            }

            if (holder instanceof ArtistHolder)
            {
                ArtistHolder artistHolder = (ArtistHolder) holder;
                artistHolder.submitList(artists);
            }

            if (holder instanceof AlbumHolder)
            {
                AlbumHolder albumHolder = (AlbumHolder) holder;
                albumHolder.submitList(albums);
            }
            if (holder instanceof GenreHolder)
            {
                GenreHolder genreHolder = (GenreHolder) holder;
                genreHolder.submitList(genre);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 4;
        }

//        private void bindItem(Music music, TrackItemHolder holder)
//        {
//            holder.setTitle( music.getName() );
//            holder.setSubtitle( music.getArtist() );
//            holder.setImage( getContext(), music );
//
//            holder.itemView.setOnClickListener(v -> {
//                Log.d("Player_Manager", "playing " + music);
//                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
//            });
//        }
    }

//    public class ContentAdapter extends ListAdapter<Object, RecyclerView.ViewHolder>
//    {
//        public ContentAdapter() {
//            super(SEARCH_DIFF_CALLBACK);
//        }
//
//        @NonNull
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view;
//            switch (viewType)
//            {
//                case 0:
//                    view = LayoutInflater.from(parent.getContext())
//                            .inflate(R.layout.search_list_artist, parent, false);
//                    return new ArtistHolder(view);
//
//                case 1:
//                    view = LayoutInflater.from(parent.getContext())
//                            .inflate(R.layout.item_view, parent, false);
//                    return new TrackItemHolder(view);
//
//                case 2:
//                    view = LayoutInflater.from(parent.getContext())
//                            .inflate(R.layout.search_list_category, parent, false);
//                    return new AlbumHolder(view);
//
//                default:
//                    return new RecyclerView.ViewHolder(new View(parent.getContext())){};
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            Object item = getCurrentList().get(position);
//            if (position == 0)
//                return 0;
//            if (item instanceof Music)
//                return 1;
//            if (position == getCurrentList().size() - 2)
//                return 2;
//            if (position == getCurrentList().size() - 1)
//                return 3;
//            return -1;
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//            if (holder instanceof TrackItemHolder)
//            {
//                TrackItemHolder tracksHolder = (TrackItemHolder) holder;
//                Music music = (Music) getCurrentList().get(position);
//                bindItem(music, tracksHolder);
//            }
//            if (holder instanceof ArtistHolder)
//            {
//                ArtistHolder artistsHolder = (ArtistHolder) holder;
//                ArrayList<Artist> artists = (ArrayList<Artist>) getCurrentList().get(position);
//                bindItem(artists, artistsHolder);
//            }
//            if (holder instanceof AlbumHolder)
//            {
//                AlbumHolder albumHolder = (AlbumHolder) holder;
//                ArrayList<Album> albums = (ArrayList<Album>) getCurrentList().get(position);
//                bindItem(albums, albumHolder);
//            }
//        }
//
//        private void bindItem(ArrayList<Artist> artists, ArtistHolder holder)
//        {
//            holder.submitList(artists);
//        }
//
//        private void bindItem(ArrayList<Album> artists, AlbumHolder holder)
//        {
//            holder.submitList(artists);
//        }
//
//        private void bindItem(Music music, TrackItemHolder holder)
//        {
//            holder.setTitle( music.getName() );
//            holder.setSubtitle( music.getArtist() );
//            holder.setImage( getContext(), music );
//
//            holder.itemView.setOnClickListener(v -> {
//                Log.d("Player_Manager", "playing " + music);
//                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
//            });
//        }
//
////        @Override
////        public int getItemCount() {
////            return items.size();
//////            return Math.min(items.size(), 20);
////        }
//
////        public void submitArtist() {
////            notifyItemChanged(0);
////        }
////
////        public void submitAlbum() {
////            notifyItemChanged(2);
////        }
//    }

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
        private final View load;

        public TrackHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView tracks = itemView.findViewById(R.id.track_list);
            trackAdapter = new TrackAdapter(DIFF_CALLBACK);
            tracks.setLayoutManager( new LinearLayoutManager( itemView.getContext(), RecyclerView.VERTICAL, false ) );
            tracks.setAdapter(trackAdapter);
            tracks.addItemDecoration(new UIConstance.NestedVerticalSpanDecorator());
            load = itemView.findViewById(R.id.load_more);
            initLoadButton();
        }

        private void initLoadButton() {
            load.setOnClickListener(v -> loadAll());
        }

        public void submitList(List<Music> list)
        {
            if (expandable && !tracksExpanded)
                load.setVisibility(View.VISIBLE);
            else
                load.setVisibility(View.GONE);
            if (list != null && list.isEmpty())
                itemView.findViewById(R.id.list_empty).setVisibility(View.VISIBLE);
            else
                itemView.findViewById(R.id.list_empty).setVisibility(View.GONE);
            trackAdapter.submitList(list);
        }

        public class TrackAdapter extends ListAdapter<Music, TrackItemHolder>
        {
//            private boolean expanded = false;
//            private final int collapseCount = 10;
//
//            public void expand()
//            {
//                expanded = true;
//            }
//
//            public void collapse()
//            {
//                expanded = false;
//            }

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
                holder.setImage( getContext(), music );

                holder.itemView.setOnClickListener(v -> {
                    new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).play(music);
                });
            }

//            @Override
//            public int getItemCount() {
//                if (!isExpanded())
//                    return Math.min(collapseCount, super.getItemCount());
//                return super.getItemCount();
//            }
//
//            private boolean isExpanded() {
//                return expanded;
//            }
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
            RecyclerView artists = itemView.findViewById(R.id.artist_list);
            artistAdapter = new ArtistAdapter(DIFF_CALLBACK);
            LinearLayoutManager layout = new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false);
            artists.setLayoutManager(layout);
            artists.setAdapter(artistAdapter);
            artists.addItemDecoration(new UIConstance.NestedHorizontalSpanDecorator());
            initHorizontalScrollFloating(artists, layout);
        }

        public void submitList(List<Artist> list)
        {
            if (list != null && list.isEmpty())
                itemView.findViewById(R.id.list_empty).setVisibility(View.VISIBLE);
            else
                itemView.findViewById(R.id.list_empty).setVisibility(View.GONE);
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
                    NavHostFragment.findNavController(MusicSearchFragment.this).navigate(R.id.action_searchFragment_to_mainArtistFragment, args);
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
        private final AlbumAdapter albumAdapter;
        public final DiffUtil.ItemCallback<Album> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Album>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Album oldUser,  @NonNull Album newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getAlbumKey().equals(newUser.getAlbumKey());
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Album oldUser,  @NonNull Album newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };

        public AlbumHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView albums = itemView.findViewById(R.id.category_list);
            albumAdapter = new AlbumAdapter(DIFF_CALLBACK);
            LinearLayoutManager layout = new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false);
            albums.setLayoutManager(layout);
            albums.setAdapter(albumAdapter);
            albums.addItemDecoration(new UIConstance.NestedHorizontalSpanDecorator());

            TextView category = itemView.findViewById(R.id.category_name);
            category.setText("Albums");

            TextView emptyText = itemView.findViewById(R.id.list_empty);
            emptyText.setText("No Album Found");

            initHorizontalScrollFloating(albums, layout);
        }

        public void submitList(List<Album> list)
        {
            if (list != null && list.isEmpty())
                itemView.findViewById(R.id.list_empty).setVisibility(View.VISIBLE);
            else
                itemView.findViewById(R.id.list_empty).setVisibility(View.GONE);
            albumAdapter.submitList(list);
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

                    Navigation.findNavController( getView() ).navigate( R.id.action_searchFragment_to_mainAlbumFragment, bundle );
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

    private class GenreHolder extends RecyclerView.ViewHolder
    {
        private final GenreAdapter genreAdapter;
        public final DiffUtil.ItemCallback<Genre> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Genre>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Genre oldUser,  @NonNull Genre newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getId() == newUser.getId();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Genre oldUser,  @NonNull Genre newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };

        public GenreHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView genres = itemView.findViewById(R.id.category_list);
            genreAdapter = new GenreAdapter(DIFF_CALLBACK);
            LinearLayoutManager layout = new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false);
            genres.setLayoutManager(layout);
            genres.setAdapter(genreAdapter);
            genres.addItemDecoration(new UIConstance.NestedHorizontalSpanDecorator());

            TextView category = itemView.findViewById(R.id.category_name);
            category.setText("Genre");

            TextView emptyText = itemView.findViewById(R.id.list_empty);
            emptyText.setText("No Genre Found");

            initHorizontalScrollFloating(genres, layout);
        }

        public void submitList(List<Genre> list)
        {
            if (list != null && list.isEmpty())
                itemView.findViewById(R.id.list_empty).setVisibility(View.VISIBLE);
            else
                itemView.findViewById(R.id.list_empty).setVisibility(View.GONE);
            genreAdapter.submitList(list);
        }

        public class GenreAdapter extends ListAdapter<Genre, GenreItemHolder>
        {
            protected GenreAdapter(@NonNull DiffUtil.ItemCallback<Genre> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public GenreItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_card_full3, parent, false);
                return new GenreItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull GenreItemHolder holder, int position) {
                Genre genre = getItem(position);
                holder.setTitle( genre.getName() );
                holder.setImage(genre.getId());

                holder.itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong( "genre_id", genre.getId() );
                    bundle.putString( "genre", genre.getName() );
                    Navigation.findNavController( getView() ).navigate( R.id.action_searchFragment_to_mainGenreFragment, bundle );
                });
            }
        }

        private class GenreItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;

            public GenreItemHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.item_image);
                title = itemView.findViewById(R.id.item_title);
            }

            public void setImage( long id )
            {
                GlideApp.with( itemView.getContext() ).load( new ProcessModelLoaderFactory.GenreProcessFetcher( itemView.getContext(), id ) ).placeholder( R.drawable.u_genre_solid ).apply( centerCropTransform() ).into( image );
            }

            public void setTitle(String title)
            {
                this.title.setText(title);
            }
        }
    }
}