package com.gcodes.iplayer.music.player;

import android.content.Context;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.services.FFmpegService;
import com.gcodes.iplayer.ui.UIConstance;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;

import org.joda.time.Duration;

import java.io.File;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicPlayerFragment extends Fragment
{

//    private PlayerView playerView;
//    private Menu menu;
//    private Toolbar toolbar;
//    private Music currentMusic;
//    private PlayerDatabase.MusicInfo musicInfo;

    private int currentTrack = -1;
//    private Player.EventListener trackListener;
    private PlayerControlView control;
    private TextView musicName;
    private TextView artistName;
    private ImageView image;
    private ImageView background;
    private CardView art;
    private Player.EventListener eventListener;

    private String[] lyrics;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private ToggleButton lyricsButton;
    private ToggleButton karaokeButton;
    private ImageButton videoButton;
    private Music currentMusic;

    private final int LYRICSYNCER = 1000;
    private final int LYRICIDLENESS = 20000;
    private Handler handler = new Handler();
    private int currentPos = 0;
    private long lyricSpanSec = 0;
    private FFmpegService karaokeService;
    private KaraokePlayerHandler karaokePlayerHandler;

    private boolean synced;
    private final Runnable syncer = new Runnable() {
        int count = 0;
        boolean flag = true;
        @Override
        public void run() {
            sync();
            handler.postDelayed(syncer, LYRICSYNCER);
        }
    };

    private void syncPost()
    {
        if ( !synced )
        {
            handler.postDelayed(syncer, LYRICSYNCER);
            synced = true;
        }
    }

    private void syncPost( long delay )
    {
        if ( !synced )
        {
            handler.postDelayed(syncer, delay);
            synced = true;
        }
    }

    private void deSync()
    {
        if ( synced )
        {
            handler.removeCallbacks( syncer );
            synced = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deSync();
        releaseKaraoke();
    }

    private void sync()
    {
        Duration playback = Duration.millis( PlayerManager.getInstance().getCurrentPosition() );
        int pos = (int) (playback.getStandardSeconds() / lyricSpanSec);
        Log.d("Lyrics_Sync", String.format( "Sync lyrics %d %d %d %d", playback.getStandardSeconds(), pos, lyricSpanSec, lyrics.length ) );
        if ( currentPos != pos )
        {
            currentPos = pos;
            listView.smoothScrollToPosition(currentPos);
        }
    }

    private void initScroll()
    {
        Duration duration = getDuration();
        lyricSpanSec = duration.getStandardSeconds() / lyrics.length;
        currentPos = 0;
        syncPost();
    }

    private Duration getDuration()
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(PlayerManager.getInstance().getContext(), currentMusic.toUri());
        String milliDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Duration.millis(Long.parseLong(milliDuration));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        AppCompatActivity activity = (AppCompatActivity) context;
        musicName = activity.findViewById( R.id.song_name );
        artistName = activity.findViewById( R.id.artist_name );
        background = activity.findViewById( R.id.player_background );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.activity_music_player2, container, false);

        control = content.findViewById(R.id.music_control_view2);
        image = content.findViewById( R.id.player_art );
        art = content.findViewById(R.id.player_album_art);
        lyricsButton = content.findViewById( R.id.player_show_lyrics );
        karaokeButton = content.findViewById( R.id.player_karaoke_button );
        videoButton = content.findViewById( R.id.player_show_video );
        initView();

        initRecycleView( content );
        initButton();
        initKaraoke(content);

//        musicName = content.findViewById( R.id.song_name );
//        artistName = content.findViewById( R.id.artist_name );

        return content;
    }

    private void initKaraoke(View content) {
        karaokeService = FFmpegService.getInstance();
        karaokePlayerHandler = new KaraokePlayerHandler(content);
        karaokeService.prepare(karaokePlayerHandler);
    }

    private void releaseKaraoke()
    {
        karaokeService.release();
    }

    private void initButton() {
        if ( lyrics == null )
            lyricsButton.setEnabled( false );

        lyricsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( isChecked )
                    showLyrics();
                else
                    hideLyrics();
            }
        });

//        if ( false ) // condition for no voice

        karaokeButton.setOnClickListener(v -> {
            karaokeService.toggleKaraoke(currentMusic);
        });
    }

    private void initView()
    {
        initPlayer();
    }


    private void showLyrics()
    {
        getView().findViewById( R.id.lyrics_view ).setVisibility( View.VISIBLE );
    }

    public void enableLyrics( boolean enable )
    {
        getView().findViewById( R.id.lyrics_view ).setEnabled( enable );
    }

    private boolean isLyricsVisible()
    {
        return getView().findViewById( R.id.lyrics_view ).getVisibility() == View.VISIBLE;
    }

    private void hideLyrics()
    {
        getView().findViewById( R.id.lyrics_view ).setVisibility( View.GONE );
    }

    private void initPlayer()
    {
//        PlayerView playerView;
        control.setShowTimeoutMs( -1 );
        control.setPlayer( MusicPlayer.getInstance().getPlayerManager() );

        if ( currentMusic != null )
            setImage( currentMusic );

//        playerView = findViewById(R.id.music_player_view);
//        playerView.showController();
//        playerView.setControllerShowTimeoutMs(-1);
//        playerView.setControllerHideOnTouch( false );
//        playerView.setPlayer( MusicPlayer.getInstance().getPlayerManager() );

//        trackListener = MusicPlayer.registerOnTrackChange(this::consumeTrack);
//        MusicPlayer.consumeTrack( this::consumeTrack );

    }

    @Override
    public void onStart() {
        super.onStart();
        initRotateAnim();
    }

    private void initRotateAnim() {
        eventListener = MusicPlayer.onStateChange(art);
    }

//    @Override
//    protected void onDestroy() {
//        MusicPlayer.unRegisterOnTrackChange( trackListener );
//        super.onDestroy();
//    }

    public void updateMusic(Music music)
    {
        Log.d("Music_Player", " Consuming track " + music.getName() );
        int newTrack = MusicPlayer.getCurrentTrack();
        if ( currentTrack != -1 || currentTrack != newTrack )
        {
            if ( isAdded() )
                setImage( music );
            currentMusic = music;
            currentTrack = newTrack;
        }
    }

    public void updateInfo(PlayerDatabase.MusicInfo info )
    {
        Log.d("Music_Player", " Consuming track " + info.getTitle() );
        //            setImage( info );
        //            setBackground( info );
        musicName.setText( info.getTitle() );
        artistName.setText( info.getAllArtists() );
    }

    public void updateLyrics(PlayerDatabase.MusicLyrics lyrics )
    {
        if ( lyrics != null )
        {
            String lyricsBody = lyrics.getLyricsBody();
            this.lyrics = lyricsBody.split("\n");
            adapter.notifyDataSetChanged();
            lyricsButton.setEnabled( true );
            initScroll();
        }
    }

    public void updateNoVoice()
    {
        karaokeButton.setEnabled(true);
    }

    public void setImage(Music music)
    {
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( this, music ) )
                .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
    }

    public void setBackground(Music music)
    {
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( this, music ) )
                .placeholder( R.drawable.u_song_art_padded ).into( background );
    }

    private void initRecycleView( View view )
    {
        adapter = new CustomAdapter();
        listView = view.findViewById( R.id.lyrics_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
//        listView.setLayoutManager( new CustomLinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(UIConstance.AppItemDecorator.AppItemDecoratorToolBarOffset(getContext(), UIConstance.AppItemDecorator.DEFAULT_TOP, 280 + UIConstance.AppItemDecorator.DEFAULT_TOP ));

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("Lyrics_Sync", "Sync lyrics remove" );
                deSync();
                syncPost( LYRICIDLENESS );
                return false;
            }
        });

//        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//                if ( newState != RecyclerView.SCROLL_STATE_IDLE )
//                {
//                    handler.removeCallbacks( runnable );
//                    handler.postDelayed(() -> {
//                        handler.postDelayed(runnable, lyricSyncChecker);
//                    }, 20000);
//                }
//            }
//        });
    }

    public void onLoading() {
        lyrics = null;
        if ( lyricsButton != null )
            lyricsButton.setEnabled( false );
        if ( karaokeButton != null )
            karaokeButton.setEnabled( false );
        deSync();
    }

    private class KaraokePlayerHandler implements FFmpegService.KaraokeHandler
    {
        private final TextView progress;
        private final ProgressBar progressBar;
        private final ToggleButton karaokeButton;
        private final MusicPlayer player = MusicPlayer.getInstance();
        private final ConcatenatingMediaSource mediaSource;
        private boolean applied = false;

        public KaraokePlayerHandler( View view )
        {
            karaokeButton = view.findViewById(R.id.player_karaoke_button);
            progressBar = view.findViewById(R.id.karaoke_progress_bar);
            progress = view.findViewById(R.id.karaoke_progress);
            mediaSource = player.getMediaSource();
        }

        @Override
        public void update(int percentage) {
            prepareProgress();
            setProgress(percentage);
        }

        private void setProgress(int percentage) {
            if ( percentage < 0 )
                progressBar.setIndeterminate(true);
            else
            {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(percentage);
            }
        }

        private void prepareProgress() {
            if ( !(progressBar.getVisibility() == View.VISIBLE) )
            {
                karaokeButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progress.setVisibility(View.VISIBLE);
                progressBar.setMax(100);
            }
        }

        private void finishProgress() {
            if ( !(karaokeButton.getVisibility() == View.VISIBLE) )
            {
                karaokeButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
            }
        }

        @Override
        public void apply(File file, Music music) {
            finishProgress();
            ProgressiveMediaSource musicSource = player.getMusicSource(file);
            int index = player.getIndex(music);
            Pair<ConcatenatingMediaSource, MediaSource> sourcePair = player.buildNewSource(musicSource, index);
            ConcatenatingMediaSource newSource = sourcePair.first;
            player.switchSources(newSource);
            setApplied(true);
        }

        private void setApplied(boolean apply)
        {
            applied = apply;
            karaokeButton.setChecked(applied);
        }

        @Override
        public boolean isApplied() {
            return applied;
        }

        @Override
        public void restore() {
            player.switchSources(mediaSource);
            setApplied(false);
        }
    }

    public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
    {
        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.single_item, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            String lyric = lyrics[position];
            holder.setItemName( lyric );
        }

        @Override
        public int getItemCount() {
            if ( lyrics == null )
                return 0;
            return lyrics.length;
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {
        private TextView itemName;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
        }

        public String getItemName() {
            return String.valueOf(itemName.getText());
        }

        public void setItemName(String text) {
            this.itemName.setText( text );
        }
    }

    public class CustomLinearLayoutManager extends LinearLayoutManager {
        public CustomLinearLayoutManager(Context context) {
            super(context);
        }

        public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public CustomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
            final LinearSmoothScroller linearSmoothScroller =
                    new LinearSmoothScroller(recyclerView.getContext()) {
                        private static final float MILLISECONDS_PER_INCH = 25f;

                        @Override
                        public PointF computeScrollVectorForPosition(int targetPosition) {
                            return CustomLinearLayoutManager.this
                                    .computeScrollVectorForPosition(targetPosition);
                        }

                        @Override
                        protected float calculateSpeedPerPixel
                                (DisplayMetrics displayMetrics) {
                            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                        }
                    };
            linearSmoothScroller.setTargetPosition(position);
            startSmoothScroll(linearSmoothScroller);
        }
    }

}
