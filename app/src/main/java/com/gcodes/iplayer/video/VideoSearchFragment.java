package com.gcodes.iplayer.video;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.ui.UIConstance;
import com.gcodes.iplayer.video.model.Series;
import com.gcodes.iplayer.video.model.Video;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import static android.app.Activity.RESULT_OK;
import static com.gcodes.iplayer.helpers.GlideOptions.centerCropTransform;
import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class VideoSearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, TextWatcher
{

    private LoaderManager loader;

//    private final String musicAll = MediaStore.Audio.Media.IS_MUSIC + " != 0";

    private String filter;

    private ContentAdapter adapter;

    private ArrayList<Video> videos;
    private ArrayList<Series> allSeries;
    private ArrayList<Series> series;

    private boolean tracksExpanded = false;
    private boolean expandable = false;
    private final int expandedLimit = 10;
    private FloatingActionButton floating;
    private ActivityResultLauncher<Intent> player;

//    private LinkedList<Object> items;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        prepareItems();
        load();
        player = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Log.d("Video_Controller", "Rendering Video Controller");
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).showVideoController(requireActivity().getSupportFragmentManager());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        floating = requireActivity().findViewById(R.id.action_floating);
        View view = inflater.inflate(R.layout.video_search, container, false);
        initView(view);
        TextInputEditText search = view.findViewById(R.id.search_field);
        search.addTextChangedListener(this);
//        initRecentPlay(view);
//        load();
        return view;
    }

    private void initRecentPlay(View view) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if  ( sharedPreferences.contains(getString(R.string.preference_recent_play)) )
        {
            MainActivity.PlayerModel playerModel = new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class);
            if  ( !playerModel.isVideoRender() )
            {
                PlayerView playerView = view.findViewById(R.id.video_control_view);
                ExoPlayer player = ExoPlayerFactory.newSimpleInstance( requireContext(), new DefaultTrackSelector() );
                playerView.setPlayer(player);
                playerView.setVisibility(View.VISIBLE);
                String rawVideo = sharedPreferences.getString(getString(R.string.preference_recent_play), null);
                Video video = Video.fromGson(rawVideo);
                ProgressiveMediaSource mediaSource = video.getMediaSource(requireContext());
                player.prepare(mediaSource);
                player.setPlayWhenReady(true);

                playerView.setOnClickListener(v -> {
                    new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).initSource();
                    Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                    String[] gsonVideos = {video.toGson()};
                    intent.putExtra( "video", gsonVideos );
                    this.player.launch(intent);
                });
            }
            else
            {
                PlayerView playerView = view.findViewById(R.id.video_control_view);
                playerView.setVisibility(View.GONE);
            }
        }
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
        loader.restartLoader(MainActivity.AppLoader.VIDEO.getId(), null, this);
        loader.restartLoader(MainActivity.AppLoader.SERIES.getId(), null, this);
    }

    private void reload()
    {
        reloadVideo();
        reloadSeries();
    }

    private void reloadVideo() {
        if (loader == null)
            loader = LoaderManager.getInstance(requireActivity());

        if (filter != null)
        {
            Bundle args = new Bundle();
            args.putString("filter", filter);
            loader.restartLoader(MainActivity.AppLoader.VIDEO.getId(), args, this);
        }
        else
        {
            loader.restartLoader(MainActivity.AppLoader.VIDEO.getId(), null, this);
        }

        Log.d("Music_Search", "Done reload " + filter);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        MainActivity.AppLoader appLoader = MainActivity.AppLoader.getInstance(id);
        switch (appLoader)
        {
            case VIDEO:
                if ( args == null )
                    return new CursorLoader( requireContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            Video.projection, null, null, Video.sort );
                else
                {
                    String artistFilter = String.format("%s like '%%%s%%' or %s like '%%%s%%'", MediaStore.Video.Media.TITLE, args.getString("filter"),
                            MediaStore.Video.Media.DISPLAY_NAME, args.getString("filter") );
                    return new CursorLoader( requireContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            Video.projection, artistFilter, null, Video.sort );
                }

            case SERIES:
                return new CursorLoader( requireContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        Video.projection, null, null, null );

            default:
                return new Loader<>(getContext());
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        Log.d("Music_Search", "Cursor data " + cursor.getCount());
        MainActivity.AppLoader appLoader = MainActivity.AppLoader.getInstance(loader.getId());
        switch (appLoader)
        {
            case VIDEO:
                ArrayList<Video> videos = new ArrayList<>();
                if  ( cursor.getCount() > 0 )
                {
                    cursor.moveToFirst();
                    if (tracksExpanded)
                    {
                        do
                        {
                            videos.add( Video.getInstance(cursor));
                        } while ( cursor.moveToNext() );
                    }
                    else
                        for (int i = 0; (i == 0 || cursor.moveToNext()) && i < expandedLimit; ++i )
                            videos.add( Video.getInstance(cursor));

                }

                expandable = cursor.getCount() > expandedLimit;
                this.videos = videos;
                if (adapter != null)
                    adapter.notifyItemChanged(0);
                break;

            case SERIES:
                load(cursor);
                if (adapter != null)
                    adapter.notifyDataSetChanged();
        }
    }

    public void load(Cursor cursor)
    {
        LinkedList<Video> videos = getVideos(cursor);
        allSeries = new ArrayList<>(toSeries(videos));
        this.series = allSeries;
    }

    private void reloadSeries()
    {
        ArrayList<Series> newSeries = new ArrayList<>();
        if (filter != null)
        {
            Iterator<Series> iterator = allSeries.iterator();
            while (iterator.hasNext())
            {
                Series series = iterator.next();
                if ( series.getName().toLowerCase().contains(filter.toLowerCase()))
                    newSeries.add(series);
            }
            this.series = newSeries;
            if (adapter != null)
                adapter.notifyItemChanged(1);
        }
    }

    private LinkedList< Video > getVideos(Cursor cursor)
    {
        LinkedList< Video > videos = new LinkedList<>();
        if  ( cursor.moveToFirst() )
        {
            do {
                Video video = Video.getInstance(cursor);
                videos.add( video );
            } while ( cursor.moveToNext() );
        }
        return videos;
    }

    private TreeSet<Series> toSeries(LinkedList<Video> videos )
    {
        ArrayList< Video > prototype;
        TreeSet< Series > collection = new TreeSet<>();
        int best;
        Iterator<Video> iterator = videos.iterator();
        while ( iterator.hasNext() )
        {
            Video last = iterator.next();
            iterator.remove();
            best = 0;
            prototype = new ArrayList<>();
            while ( iterator.hasNext() )
            {
                Video video = iterator.next();
                int sim = similarityIndex(last, video);
                if ( sim > 1 )
                {
                    if ( sim == best )
                        prototype.add( video );
                    else
                    {
                        if  ( sim > best )
                        {
                            best = sim;
                            prototype.clear();
                            prototype.add( video );
                        }
                    }
                }
            }
            videos.removeAll( prototype );
            prototype.add( last );
            Collections.sort( prototype ); // done on enter
            Series series = Series.getIntance(prototype.toArray(new Video[]{}), best);
            if ( series.getCount() > 1 )
                collection.add( series );
            iterator = videos.iterator();
        }

        return collection;
    }

    private int similarityIndex(Video last, Video video)
    {
        int i;
        String name1 = last.getName();
        String name2 = video.getName();
        int length = Math.min( name1.length(), name2.length() );
        for ( i = 0; i < length && name1.charAt( i ) == name2.charAt( i ); ++i );
        return i;
    }

    private void loadLimit()
    {
        tracksExpanded = false;
        reloadVideo();
        floating.hide();
    }

    private void loadAll()
    {
        tracksExpanded = true;
        reloadVideo();
        floating.setImageResource(R.drawable.u_compress);
        floating.show();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

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
                            .inflate(R.layout.search_list_video, parent, false);
                    return new VideoHolder(view);

                case 1:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.search_list_category, parent, false);
                    return new SeriesHolder(view);


                default:
                    return new RecyclerView.ViewHolder(new View(getContext())){};
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof VideoHolder)
            {
                VideoHolder videoHolder = (VideoHolder) holder;
                videoHolder.submitList(videos);
            }

            if (holder instanceof SeriesHolder)
            {
                SeriesHolder seriesHolder = (SeriesHolder) holder;
                seriesHolder.submitList(series);
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
    }

    private class VideoHolder extends RecyclerView.ViewHolder
    {
        private final VideoAdapter videoAdapter;
        public final DiffUtil.ItemCallback<Video> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Video>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Video oldUser,  @NonNull Video newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getId() == newUser.getId();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Video oldUser,  @NonNull Video newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };
        private final View load;

        public VideoHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView videos = itemView.findViewById(R.id.video_list);
            videoAdapter = new VideoAdapter(DIFF_CALLBACK);
            videos.setLayoutManager( new GridLayoutManager( itemView.getContext(), 2 ) );
            videos.setAdapter(videoAdapter);
//            videos.addItemDecoration(new UIConstance.NestedVerticalSpanDecorator());
            load = itemView.findViewById(R.id.load_more);
            initLoadButton();
        }

        private void initLoadButton() {
            load.setOnClickListener(v -> loadAll());
        }

        public void submitList(List<Video> list)
        {
            if (expandable && !tracksExpanded)
                load.setVisibility(View.VISIBLE);
            else
                load.setVisibility(View.GONE);
            videoAdapter.submitList(list);
        }

        public class VideoAdapter extends ListAdapter<Video, VideoItemHolder>
        {
            protected VideoAdapter(@NonNull DiffUtil.ItemCallback<Video> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public VideoItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_video_card, parent, false);
                return new VideoItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull VideoItemHolder holder, int position) {
                Video video = getItem(position);
                holder.setTitle( video.getName() );
                holder.setSubtitle( video.getDuration() );

                GlideApp.with( requireContext() ).load( video.getData() )
                        .placeholder( R.drawable.u_video2 ).apply( RequestOptions.centerCropTransform() ).into( holder.getImage() );

                holder.itemView.setOnClickListener(v -> {
                    new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).initSource();
                    Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                    String[] gsonVideos = {video.toGson()};
                    intent.putExtra( "video", gsonVideos );
                    player.launch(intent);
                });
            }
        }

        public class VideoItemHolder extends RecyclerView.ViewHolder
        {
            private final TextView title;
            private final TextView subtitle;
            private final ImageView image;

            public VideoItemHolder(@NonNull View itemView) {
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

            public ImageView getImage() {
                return image;
            }
        }
    }

    private class SeriesHolder extends RecyclerView.ViewHolder
    {
        private final SeriesAdapter seriesAdapter;
        public final DiffUtil.ItemCallback<Series> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Series>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Series oldUser,  @NonNull Series newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.getName() == newUser.getName();
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Series oldUser,  @NonNull Series newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.equals(newUser);
                    }
                };

        public SeriesHolder(@NonNull View itemView) {
            super(itemView);
            RecyclerView albums = itemView.findViewById(R.id.category_list);
            seriesAdapter = new SeriesAdapter(DIFF_CALLBACK);
            LinearLayoutManager layout = new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false);
            albums.setLayoutManager(layout);
            albums.setAdapter(seriesAdapter);
            albums.addItemDecoration(new UIConstance.NestedHorizontalSpanDecorator());

            TextView category = itemView.findViewById(R.id.category_name);
            category.setText("Series");

            initHorizontalScrollFloating(albums, layout);
        }

        public void submitList(List<Series> list)
        {
            seriesAdapter.submitList(list);
        }

        public class SeriesAdapter extends ListAdapter<Series, SeriesItemHolder>
        {
            protected SeriesAdapter(@NonNull DiffUtil.ItemCallback<Series> diffCallback) {
                super(diffCallback);
            }

            @NonNull
            @Override
            public SeriesItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_video_card, parent, false);
                return new SeriesItemHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull SeriesItemHolder holder, int position) {
                Series series = getItem(position);
                holder.setTitle( String.format("%s ( %d Videos )", series.getName(), series.getCount()) );
                holder.setSubtitle( series.getDuration() );
                holder.setImage(series);

                holder.itemView.setOnClickListener(v ->
                {
                    new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).initSource(series);
                    NavController navController = NavHostFragment.findNavController(VideoSearchFragment.this);
                    navController.navigate( R.id.action_videoSearchFragment_to_seriesPlayerFragment );
                });
            }
        }

        private class SeriesItemHolder extends RecyclerView.ViewHolder
        {
            private final ImageView image;
            private final TextView title;
            private final TextView subtitle;

            public SeriesItemHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.item_image);
                title = itemView.findViewById(R.id.item_title);
                subtitle = itemView.findViewById(R.id.item_subtitle);
                title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }

            public void setImage(Series series)
            {
                GlideApp.with( itemView.getContext() ).load( series.getVideos()[0].getData() )
                        .placeholder( R.drawable.u_video2 ).apply( RequestOptions.centerCropTransform() ).into( image );
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

            public ImageView getImage() {
                return image;
            }

        }
    }
}