package com.gcodes.iplayer.music.player;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.player.PlayerDownloadService;
import com.gcodes.iplayer.services.YouTubeService;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;
import com.google.android.exoplayer2.offline.Download;
import com.google.api.services.youtube.model.Video;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import at.huber.youtubeExtractor.YtFile;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class MusicVideoFragment extends Fragment implements MusicPlayerActivity.PlayerBar
{
    private Music music;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private List<Video> videos = new ArrayList<>();
    private Consumer<Void> showVideo;
    private Consumer<Void> showMusic;
    private PlayerDatabase.MusicInfo musicInfo;

    private Handler updateProgress;

    public MusicVideoFragment() {
    }

    public void setMusic(Music music) {
        this.music = music;
    }

    public void setVideoPrepare(Consumer<Void> showVideo )
    {
        this.showVideo = showVideo;
    }

    public void setMusicPrepare(Consumer<Void> showMusic )
    {
        this.showMusic = showMusic;
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        updateProgress = new Handler( getContext().getMainLooper() );
    }

    private void getMusicVideo()
    {
        YouTubeService youtube = YouTubeService.getIntance();
        String query = musicInfo != null ? String.format( "%s %s", musicInfo.getTitle(), musicInfo.getAllArtists() ) :
                String.format( "%s %s", music.getName(), music.getArtist() ) ;

        Helper.Worker.executeTask(() -> {
            try {
                videos = youtube.getVideos(query);
            } catch (IOException e) {
                e.printStackTrace();
                videos = new ArrayList<>();
            }
            return () -> {
                Log.d( "YouTube_API", "The Videos " + videos );
                adapter.notifyDataSetChanged();
            };
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_video_fragment, container, false);
//        initRecycleView( view );
//        initButton( view );
//        initFrame();
//        initMusicVideos();
        return view;
    }

    private void initMusicVideos() {
        getMusicVideo();
    }

    private void initFrame()
    {
        FrameLayout lyricFrame = getActivity().findViewById(R.id.player_lyrics);
        lyricFrame.setVisibility(View.VISIBLE);
    }

    private void initRecycleView( View view )
    {
        adapter = new CustomAdapter();
        listView = view.findViewById( R.id.video_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
    }

    private void initButton( View view )
    {
        ImageButton minimize = view.findViewById(R.id.lyrics_minimize);
        minimize.setOnClickListener(v -> {
            hide( getFragmentManager() );
        });
    }

    public void hide( FragmentManager fragmentManager ) {
        Fragment playlist = fragmentManager.findFragmentByTag("Player_Lyrics");
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove( playlist );
        FrameLayout lyricFrame = getActivity().findViewById(R.id.player_lyrics);
        lyricFrame.setVisibility(View.GONE);
        transaction.commit();
    }

    public static void show(FragmentManager fragmentManager, Music currentMusic) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        MusicVideoFragment musicFragment = new MusicVideoFragment();
        musicFragment.setMusic( currentMusic );
        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
        transaction.commit();
    }

    public static void show(FragmentManager fragmentManager, Music currentMusic, PlayerDatabase.MusicInfo musicInfo, Consumer<Void> showVideo, Consumer<Void> showMusic) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        MusicVideoFragment musicFragment = new MusicVideoFragment();
        musicFragment.setMusic( currentMusic );
        musicFragment.setMusicInfo( musicInfo );
        musicFragment.setMusicPrepare( showMusic );
        musicFragment.setVideoPrepare( showVideo );
        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
        transaction.commit();
    }

    public void prepareVideoPlayer()
    {
        hide( getFragmentManager() );
    }

    public void setMusicInfo(PlayerDatabase.MusicInfo musicInfo) {
        this.musicInfo = musicInfo;
    }

    public PlayerDatabase.MusicInfo getMusicInfo() {
        return musicInfo;
    }

    @Override
    public Fragment getBar() {
        return new MusicVideoBar();
    }

    public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
    {
        private SparseArrayCompat< Runnable > progresses = new SparseArrayCompat<>();
        private final int PROGRESS_DELAY = 1000;

        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_view_item2, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onViewRecycled(@NonNull ItemHolder holder) {
            int pos = holder.getAdapterPosition();
            Runnable current = progresses.get(pos);
            updateProgress.removeCallbacks( current );
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
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
                    .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendSeconds().toFormatter();
            holder.setSubtitle( period.toString( periodFormatter ) );
            continueProgress( holder );

            holder.itemView.setOnClickListener(v -> {
                YouTubeService.consumeYoutubeVideo( video.getId(), ytFile -> {
//                    MusicPlayer musicPlayer = MusicPlayer.getInstance();
//                    musicPlayer.playVideo( ytFile.getUrl(), music );
//                    showVideo.accept(null);
                    VideoPlayerActivity.play( getContext(), ytFile.getUrl() );
                }, getContext() );
            });

            holder.download.setOnClickListener(v -> {
                YouTubeService.consumeYoutubeVideo( video.getId(), ytFile -> {
                    download( video, ytFile, holder );
                }, getContext() );
            });
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
            } );
        }

        @Override
        public int getItemCount() {
            return videos.size();
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
            progress.setVisibility(View.VISIBLE);
            download.setVisibility(View.INVISIBLE);
        }

        public void completeDownload()
        {
            progress.setVisibility(View.GONE);
            download.setVisibility(View.GONE);
        }

        public void stopDownload()
        {
            progress.setVisibility(View.GONE);
            download.setVisibility(View.VISIBLE);
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
                    .placeholder( R.drawable.ic_track_black_24dp ).apply( centerCropTransform() ).into( image );
        }


    }

    public static class MusicVideoBar extends MusicPlayerActivity.SimplePlayerBar {
        @Override
        protected String getTitle() {
            return "Available Music Videos";
        }
    }
}
