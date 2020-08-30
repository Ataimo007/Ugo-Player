package com.gcodes.iplayer.video.series;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.CustomVideoGesture;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.ui.UIConstance;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.player.VideoPlayer;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class SeriesPlayerFragment extends Fragment
{
    private static NavController navController;
    private Series series;
    private CustomAdapter adapter;
    private PlayerManager.VideoManager player;
    private PlayerManager playerManager;

//    private boolean playing = false;
    private PlayerView control;
    private FloatingActionButton controlButton;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
    }

    public void load()
    {
        series = Series.fromGson( getArguments().getString("series") );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.series_player, container, false);
        initView( view );
        initList( view );
        initPlayer( view );
        return view;
    }

    private void initView(View view) {
        TextView name = view.findViewById(R.id.series_name);
        name.setText( series.getName() );
    }

    private void initPlayer(View view) {
        control = view.findViewById(R.id.video_control_view);
//        control.setControllerShowTimeoutMs( -1 );
//        player = VideoPlayer.getInstance();
        player = new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).getVideoManager();

        playerManager = player.getPlayerManager();
        playerManager.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        playerManager.setView( control );
        control.showController();

        prepareTouch( control );

        if ( !player.isPlaying() )
        {
            initSource();
            player.playNow();
        }

        initControlButton(view);
        player.tryHideVideoPlayer();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == PlayerManager.REQUEST_VIDEO_PLAYER )
        {
            playerManager.setView( control );
            control.showController();
            player.continuePlay();
        }
    }

    private void initControlButton(View view) {
        controlButton = view.findViewById(R.id.series_player_control);
        NavBackStackEntry backStackEntry = NavHostFragment.findNavController( this ).getBackStackEntry(R.id.videoFragment);
        MutableLiveData<Boolean> isPlaying = backStackEntry.getSavedStateHandle().getLiveData("is_playing");
        isPlaying.observe(getViewLifecycleOwner(), isPlaying1 -> {
            if (isPlaying1)
                controlButton.setImageResource(R.drawable.u_pause);
            else
                controlButton.setImageResource(R.drawable.u_play);
        });

        controlButton.setOnClickListener(v -> {
            if ( player.isInPlayingState() )
            {
                player.pause();
                controlButton.setImageResource(R.drawable.u_play);
            }
            else
            {
                player.play();
                controlButton.setImageResource(R.drawable.u_pause);
            }
        });
    }

//    public static void tryUpdateControllerButton( boolean isPlaying ) {
//        if ( navController != null )
//        {
//            try {
//                navController.getBackStackEntry(R.id.seriesPlayerFragment).getSavedStateHandle().set("is_playing", isPlaying );
//            } catch (IllegalArgumentException ex) {
//                navController.getBackStackEntry(R.id.seriesPlayerFragment).getSavedStateHandle().remove("is_playing");
//            }
//        }
//    }

    public void updateControllerButton() {
        if ( player.isInPlayingState() )
            controlButton.setImageResource(R.drawable.u_pause);
        else
            controlButton.setImageResource(R.drawable.u_play);
    }

    private void initSource() {
        player.initVideoSources( series, control );
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        control.showController();
//
////        updateControllerButton();
//
//        Log.w( "Series_Player", "playing" );
////        if ( playing && player.hasState() )
//        if ( player.isPlaying() && player.hasState() )
//        {
//            Log.w( "Series_Player", "playing again" );
//            playerManager.setView( control );
//            player.continuePlay();
//        }
//        else
//        {
//            Log.w( "Series_Player", "playing once" );
//            player.playNow();
////            player.continuePlay();
//            playing = true;
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();
        Log.w("Series_Player", "on resume play" );
        playerManager.setView( control );
        control.showController();
        player.play();
    }

    @Override
    public void onPause() {
        super.onPause();
        player.saveState();
//        playerManager.stop();
        player.pause();
        control.setPlayer(null);
    }

    private void prepareTouch(PlayerView control) {
        CustomVideoGesture gesture = new CustomVideoGesture( getContext(), new Helper.VideoGestureChangeListener(), () ->
                playerManager.getPlayer(), new CustomVideoGesture.GestureAction(){
            @Override
            public void onClick() {
//                player.saveState();
                showVideoPlayer();
            }
        }
        );
        control.setOnTouchListener( gesture );
    }

    private void showVideoPlayer() {
        Intent intent = new Intent( getContext(), VideoPlayerActivity.class );
        intent.putExtra( "data_type", "controller" );
        startActivity( intent );
    }

    private void initList(View view)
    {
        adapter = new CustomAdapter();
        RecyclerView listView = view.findViewById(R.id.list_video);
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter( adapter );
        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1, 20 ));
    }

//    public static void navigate(Series aSeries)
//    {
//        if ( navController != null )
//            navigate( aSeries, navController );
//    }
//
//    public static void navigate(Series aSeries, Fragment fragment )
//    {
//        navController = NavHostFragment.findNavController( fragment );
//        navigate( aSeries, navController );
//    }

//    public static void navigate(Series aSeries, NavController navController)
//    {
//        Bundle bundle = new Bundle();
//        bundle.putString( "series", aSeries.toGson() );
//        navController.navigate( R.id.action_videoFragment_to_seriesPlayerFragment, bundle );
//
//        navController.getBackStackEntry(R.id.seriesPlayerFragment).getLifecycle().addObserver(new DefaultLifecycleObserver() {
//            @Override
//            public void onStop(@NonNull LifecycleOwner owner) {
//                try {
//                    navController.getBackStackEntry(R.id.seriesPlayerFragment);
//                } catch (IllegalArgumentException ex)
//                {
//                    VideoPlayer.getInstance().tryRenderVideoPlayer(VideoPlayer.RESULT_PLAYING);
//                }
//            }
//
//
//        });
//    }

    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position)
        {
            Video video = series.getVideo(position);
            holder.setTitle( video.getName() );
            holder.setSubtitle( parseDuration( video.getDuration() ) );
            holder.setDate( video.getDateString() );

            GlideApp.with( SeriesPlayerFragment.this ).load( new CustomProcessFetcher( video ) )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( holder.getImage() );

            holder.itemView.setOnClickListener(v -> {
                playerManager.playAt( position );
//                player.saveState();
                showVideoPlayer();
//                VideoPlayer.play( getActivity(), position, video );
            });
        }

        @Override
        public int getItemCount()
        {
            return series.getCount();
        }

        public String parseDuration( long dur )
        {
            long h = TimeUnit.MILLISECONDS.toHours(dur);
            long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
            long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );;
            String hs = h > 0 ? String.format( "%02d:", h ) : "";
            String ms = m > 0 || h > 0 ? String.format( "%02d:", m ) : "";
            String ss = h == 0 && m == 0 ? String.format( "%02d secs", s ) : String.format( "%02d", s );
            return hs + ms + ss;
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {

        private final TextView title;
        private final TextView subtitle;
        private final TextView date;
        private final ImageView image;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            date = itemView.findViewById(R.id.item_description);
            image = itemView.findViewById(R.id.item_image);
        }

        public String getTitle() {
            return title.getText().toString();
        }

        public void setTitle(String name) {
            this.title.setText( name );
        }

        public void setDate(String name) {
            this.date.setText( name );
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

    public class CustomProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
        private final Video[] videos;

        public CustomProcessFetcher( Video[] videos )
        {
            this.videos = videos;
        }

        public CustomProcessFetcher( Video video )
        {
            this.videos = new Video[]{ video };
        }

        @Override
        public Object getKey() {
            return videos[ 0 ].getData();
        }

        @Override
        public Bitmap load()
        {
            for ( Video video : videos )
            {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(video.getData(), MediaStore.Video.Thumbnails.MINI_KIND);
                if ( thumbnail != null )
                    return thumbnail;
            }
            return null;
        }
    }
}
