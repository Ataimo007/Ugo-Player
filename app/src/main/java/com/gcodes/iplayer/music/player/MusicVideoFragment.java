package com.gcodes.iplayer.music.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.models.Music;
import com.gcodes.iplayer.player.PlayerDownloadService;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.services.YouTubeService;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.Video;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import at.huber.youtubeExtractor.YtFile;
import kotlin.Triple;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class MusicVideoFragment extends Fragment
{
    private final PlayerManager.MusicManager manager;
    private Music music;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private List<Video> videos = new ArrayList<>();
//    private Consumer<Void> showVideo;
//    private Consumer<Void> showMusic;
    private PlayerDatabase.MusicInfo musicInfo;

    private Handler updateProgress;
//    private Player.EventListener trackListener;
    private View musicVideoTab;
    private boolean loading = false;
    private MusicPlayerActivity player;
//    private ActivityResultLauncher<Intent> youtubeVideoContract;
    private ActivityResultLauncher<Video> youtubePlayerContract;
    private ActivityResultLauncher<Intent> youtubePlayerExternalContract;

    public MusicVideoFragment(PlayerManager.MusicManager manager) {
        this.manager = manager;
    }

    public void updateMusic(Music music) {
        this.music = music;
        videos.clear();
        adapter.notifyDataSetChanged();
    }

//    @Override
//    public void onStop() {
//        super.onStop();
//        if ( trackListener != null )
//            MusicPlayer.unRegisterOnTrackChange( trackListener );
//    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if ( context instanceof MusicPlayerActivity )
        {
            player = (MusicPlayerActivity) context;
            musicVideoTab = player.getMusicVideoTab();
        }
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        updateProgress = new Handler( getContext().getMainLooper() );
//        youtubeVideoContract = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//            manager.restoreCurrentState();
//            this.player.notLoading();
//        });
        youtubePlayerContract = registerForActivityResult(new ActivityResultContract<Video, Triple<Integer, String, Video>>(){
            private Video video;

            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Video video) {
                this.video = video;
                JacksonFactory jsonFactory = YouTubeService.getInstance().jsonFactory;
                Intent intent = new Intent(getContext(), YoutubePlayer.class);
                try {
                    String jsonVideo = jsonFactory.toString(video);
                    intent.putExtra("video", jsonVideo);
                } catch (IOException e) {
                    Helper.toast(getContext(), "Can't play this Video");
                }
                return intent;
            }

            @Override
            public Triple<Integer, String, Video> parseResult(int resultCode, @Nullable Intent intent) {
                if (intent == null)
                    return null;
                return new Triple<>(resultCode, Objects.requireNonNull(intent).getStringExtra("state"), video);
            }
        }, result -> {
            if (result != null && result.getFirst() != RESULT_OK && result.getSecond() != null && result.getSecond().equals("restricted") )
            {
                manager.play();
                this.player.notLoading();
                showExternalDialog(result.getThird());
            }
            else
            {
                manager.play();
                this.player.notLoading();
            }
        });
        youtubePlayerExternalContract = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            manager.play();
            this.player.notLoading();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_video_fragment, container, false);
        initRecycleView( view );
//        initButton( view );
//        initFrame();
//        initMusicVideos();
        return view;
    }

    public void updateMusicVideos( List<Video> videos ) {
        this.videos = videos;
        updateBadger( videos.size() );
        adapter.notifyDataSetChanged();
        loading = false;
    }

//    @Override
//    public void onAttach(@NonNull Context context) {
//        super.onAttach(context);
//        if ( context instanceof  MusicPlayerActivity )
//        {
//            MusicPlayerActivity player = ( MusicPlayerActivity ) context;
//            TabItem mVideoTab = player.findViewById(R.id.player_music_video);
//            mVideoTab.dra
//            Drawable d;
//            new Canvas();
//
//        }
//    }

//    private void initFrame()
//    {
//        FrameLayout lyricFrame = getActivity().findViewById(R.id.player_lyrics);
//        lyricFrame.setVisibility(VISIBLE);
//    }

    private void initRecycleView( View view )
    {
        adapter = new CustomAdapter();
        listView = view.findViewById( R.id.video_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(UIConstance.AppItemDecorator.AppItemDecoratorToolBarOffset(getContext()));
    }

    public void onLoading()
    {
        loading = true;
        if ( adapter != null )
            adapter.notifyDataSetChanged();
        if ( musicVideoTab != null )
        {
            updateBadger( 0, GONE );
        }
    }

    private void updateBadger( int count )
    {
        if ( count > 0 )
            updateBadger( count, VISIBLE );
    }

    private void updateBadger( int count, int visibility )
    {
        TextView badger = musicVideoTab.findViewById(R.id.badger_text);
        badger.setText( String.valueOf( count ) );
        badger.setVisibility( visibility );
        player.updateTabView();
    }

//    private void initButton( View view )
//    {
//        ImageButton minimize = view.findViewById(R.id.lyrics_minimize);
//        minimize.setOnClickListener(v -> {
//            hide( getFragmentManager() );
//        });
//    }

//    public void hide( FragmentManager fragmentManager ) {
//        Fragment playlist = fragmentManager.findFragmentByTag("Player_Lyrics");
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.remove( playlist );
//        FrameLayout lyricFrame = getActivity().findViewById(R.id.player_lyrics);
//        lyricFrame.setVisibility(GONE);
//        transaction.commit();
//    }

//    public static void show(FragmentManager fragmentManager, Music currentMusic) {
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        MusicVideoFragment musicFragment = new MusicVideoFragment();
//        musicFragment.setMusic( currentMusic );
//        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
//        transaction.commit();
//    }
//
//    public static void show(FragmentManager fragmentManager, Music currentMusic, PlayerDatabase.MusicInfo musicInfo, Consumer<Void> showVideo, Consumer<Void> showMusic) {
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        MusicVideoFragment musicFragment = new MusicVideoFragment();
//        musicFragment.setMusic( currentMusic );
//        musicFragment.setMusicInfo( musicInfo );
//        musicFragment.setMusicPrepare( showMusic );
//        musicFragment.setVideoPrepare( showVideo );
//        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
//        transaction.commit();
//    }

//    public void prepareVideoPlayer()
//    {
//        hide( getFragmentManager() );
//    }
//
//    public void setMusicInfo(PlayerDatabase.MusicInfo musicInfo) {
//        this.musicInfo = musicInfo;
//    }
//
//    public PlayerDatabase.MusicInfo getMusicInfo() {
//        return musicInfo;
//    }

    private void showExternalDialog(Video video) {
        YoutubeExternalDialog externalDialog = new YoutubeExternalDialog();
        externalDialog.setVideo(video);
        externalDialog.show(getChildFragmentManager(), "external_dialog");
    }

    public void playExternally(Video video) {

        Activity activity = getActivity();
        Intent intent;

        if  ( YouTubeIntents.canResolvePlayVideoIntentWithOptions(activity) )
            intent = YouTubeIntents.createPlayVideoIntentWithOptions(activity, video.getId(), false, true);
        else if  ( YouTubeIntents.canResolvePlayVideoIntent(activity) )
            intent = YouTubeIntents.createPlayVideoIntent(activity, video.getId());
        else
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + video.getId()));

        player.loading();
        manager.pause();

        youtubePlayerExternalContract.launch(intent);
//        startActivity(intent);
//        activity.finish();
    }

    public static class YoutubeExternalDialog extends DialogFragment
    {
        private Video video;

        public Video getVideo() {
            return video;
        }

        public void setVideo(Video video) {
            this.video = video;
        }


//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            View view = inflater.inflate(R.layout.external_youtube_player, container, false);
//            loadVideo();
//            initView(view);
//            return view;
//        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            LayoutInflater layoutInflater = requireActivity().getLayoutInflater();
            View view = layoutInflater.inflate(R.layout.external_youtube_player, null);
//            loadVideo();
            initView(view);
            builder.setView(view);
            return builder.create();
        }

        private void initView(View view) {
            setImage(video.getSnippet().getThumbnails().getHigh().getUrl(), view);
            Button playButton = view.findViewById(R.id.external_play);
            playButton.setOnClickListener(v -> {
                Fragment parentFragment = getParentFragment();
                if  ( parentFragment instanceof MusicVideoFragment )
                {
                    MusicVideoFragment musicVideo = (MusicVideoFragment) parentFragment;
                    musicVideo.playExternally(video);
                    dismiss();
                }
            });
        }

        public void setImage(String url, View container) {
            ImageView image = container.findViewById(R.id.video_image);
            Uri uri = Uri.parse(url);
            GlideApp.with( YoutubeExternalDialog.this ).load( uri  )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( image );
        }



//        private void loadVideo() {
//            jsonFactory = YouTubeService.getInstance().jsonFactory;
//            String videoRaw = getArguments().getString("video");
//            try {
//                video = jsonFactory.fromString(videoRaw, Video.class);
//            } catch (IOException e) {
//                Helper.toast(getActivity(), "Can't play this Video");
//            }
//        }


    }

    public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        private SparseArrayCompat< Runnable > progresses = new SparseArrayCompat<>();
        private final int PROGRESS_DELAY = 1000;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if ( viewType == 0 )
            {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_view_item, parent, false);
                return new ItemHolder(view);
            }
            else
            {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_video_empty, parent, false);
                return new EmptyHolder(view);
            }
        }

        private void showLoading()
        {

        }

        @Override
        public int getItemViewType(int position) {
            if ( videos.size() > 0 )
                return 0;
            else
                return 1;
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            Runnable current = progresses.get(pos);
            updateProgress.removeCallbacks( current );
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if ( videos.size() > 0 )
                bindMusicVideo((ItemHolder) holder, position);
            else
                bindEmptyVideos((EmptyHolder) holder);
        }

        private void bindEmptyVideos(EmptyHolder holder) {
            if ( loading )
            {
                holder.reason.setText(String.format( "Please Wait we are loading the music video for: \n%s", music.getName() ) );
                return;
            }
            if ( !Helper.isDeviceOnline( player ) )
            {
                holder.reason.setText(String.format( "Please ensure you are connected to the internet to get music video for: \n%s", music.getName() ));
                return;
            }
            holder.reason.setText(String.format( "No music video found for %s", music.getName() ));
        }

        private void bindMusicVideo(ItemHolder holder, int position) {
            Video video = videos.get(position);
            holder.setTitle( video.getSnippet().getTitle() );
            DateTime publishedAt = DateTime.parse(video.getSnippet().getPublishedAt().toString());
            String desc = String.format("%s - %s views", publishedAt.toString(DateTimeFormat.mediumDate() ),
                    video.getStatistics().getViewCount());
            holder.setDescription( desc );
            holder.setImage( video.getSnippet().getThumbnails().getHigh().getUrl() );
            Period period = Period.parse( video.getContentDetails().getDuration() );
            PeriodFormatter periodFormatter = new PeriodFormatterBuilder().minimumPrintedDigits(2).appendHours()
                    .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendMinutes()
                    .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).printZeroAlways().appendSeconds().toFormatter();
            holder.setSubtitle( period.toString( periodFormatter ) );
            continueProgress( holder );

            holder.itemView.setOnClickListener(v -> {
//                YouTubeService.consumeYoutubeVideo( video, this::loadVideoPlayer, this::loadYoutubePlayer, getContext() );
                loadYoutubePlayer(video);
            });

            holder.download.setOnClickListener(v -> {
                YouTubeService.consumeYoutubeVideo( video, ytFile -> download( video, ytFile, holder ),
                        youtubeVideo -> Helper.toast(getContext(), String.format("Can't obtain media for %s", youtubeVideo.getSnippet().getTitle())), getContext() );
            });
        }

//        private void loadVideoPlayer(YtFile ytFile)
//        {
//            player.loading();
//            manager.saveCurrentState();
//
//            manager.getMainPlayerManager().getVideoManager().initOnlineSources(ytFile.getUrl());
//            Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
////            youtubeVideoContract.launch(intent);
//        }

        private void loadYoutubePlayer(Video video)
        {
            manager.pause();
            player.loading();
            youtubePlayerContract.launch(video);
        }

        private void continueProgress(ItemHolder holder) {
            int pos = holder.getAdapterPosition();
            Runnable current = progresses.get(pos);
            if ( current != null )
            {
                updateProgress.postDelayed( current, PROGRESS_DELAY );
            }
        }

        private void download(Video video, YtFile ytFile, ItemHolder holder)
        {
            holder.beginDownload();
            PlayerDownloadService.download( video, ytFile.getUrl(), music, download -> {

                Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        if ( download != null )
                        {
                            switch ( download.state )
                            {
                                case Download.STATE_COMPLETED:
                                    holder.completeDownload();
                                    break;

                                case Download.STATE_FAILED:
                                    holder.stopDownload();
                                    break;

                                case Download.STATE_DOWNLOADING:
                                    holder.progress.setIndeterminate( false );
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                        holder.progress.setProgress((int) (download.getPercentDownloaded() * 100), true );
                                    else
                                        holder.progress.setProgress((int) (download.getPercentDownloaded() * 100) );
                                    break;
                            }
                        }
                    updateProgress.postDelayed( this, PROGRESS_DELAY);
                } };


                Runnable previous = progresses.get(holder.getAdapterPosition());
                if ( previous != null )
                    updateProgress.removeCallbacks( previous );
                updateProgress.postDelayed( update, PROGRESS_DELAY);
                progresses.put( holder.getAdapterPosition(), update );
            }, getContext());
        }

        @Override
        public int getItemCount() {
            int size = videos.size();
            if ( size == 0 )
                return 1;
            return size;
        }


    }

    public class EmptyHolder extends RecyclerView.ViewHolder
    {

        private final TextView reason;

        public EmptyHolder(@NonNull View itemView) {
            super(itemView);
            reason = itemView.findViewById(R.id.empty_reason);
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {
        private final TextView title;
        private final TextView subtitle;
        private final TextView description;
        private final ImageView image;
        private final ImageButton download;
        private final ProgressBar progress;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            description = itemView.findViewById(R.id.item_description);
            image = itemView.findViewById(R.id.item_image);
            download = itemView.findViewById(R.id.video_download);
            progress = itemView.findViewById(R.id.download_progress);
        }

        public void beginDownload()
        {
            progress.setVisibility(VISIBLE);
            download.setVisibility(INVISIBLE);
        }

        public void completeDownload()
        {
            progress.setVisibility(GONE);
            download.setVisibility(GONE);
        }

        public void stopDownload()
        {
            progress.setVisibility(GONE);
            download.setVisibility(VISIBLE);
        }

        public String getTitle() {
            return String.valueOf(title.getText());
        }

        public void setTitle(String text) {
            this.title.setText( text );
        }

        public void setSubtitle(String text) {
            this.subtitle.setText( text );
        }

        public void setDescription(String text) {
            this.description.setText( text );
        }

        public void setImage(String url) {
            Uri uri = Uri.parse(url);
            GlideApp.with( MusicVideoFragment.this ).load( uri  )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( image );
        }


    }
}
