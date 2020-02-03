package com.gcodes.iplayer.music.player;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.services.MusixMatchLyricsService;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.entity.track.TrackData;
import org.jmusixmatch.snippet.Snippet;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import android.support.annotation.NonNull;
//import androidx.core.app.Fragment;
//import androidx.core.content.CursorLoader;
//import androidx.core.widget.SimpleCursorAdapter;
//
//import android.support.v7.widget.RecyclerView;

public class LyricsFragment extends Fragment
{
    private Music music;
    private PlayerDatabase.MusicInfo info;
    private RecyclerView listView;
    private CustomAdapter adapter;
    private String[] lyrics = new String[0];
    private PlayerDatabase database;
//    private boolean lyricsEnable = true;

//    private final Handler lyricSnippet = new Handler();
//    private TextView lyrics;
//    private Runnable lyricSnip;

    public LyricsFragment() {
    }

    public void updataMusic(Music music) {

    }

    public void setMusic(Music music) {
        this.music = music;
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        database = PlayerDatabase.getInstance();
    }

//    private void initHandlers( Snippet snippet )
//    {
//        lyricSnippet.removeCallbacks( lyricSnip );
//        lyricSnip = () -> {
//            SimpleExoPlayer player = MusicPlayer.getInstance().getPlayer();
//            MusicPlayer.getCurrentTrack();
//            long position = player.getCurrentPosition();
//            long pos = TimeUnit.SECONDS.toSeconds(position);
//            snippet.setUpdatedTime(String.valueOf(pos));
//            String snippetBody = snippet.getSnippetBody();
//            lyrics.setText( snippetBody );
//        };
//        lyricSnippet.postDelayed( lyricSnip, 1000 );
//    }

    private void getLyrics()
    {
        Helper.Worker.executeTask(() -> {
            Log.d( "Music_Player", "getting lyrics" );
            PlayerDatabase.MusicLyrics lyric = info != null ? database.playerDao().getLyrics( info ) : database.playerDao().getLyrics( music );
            if ( lyric != null )
            {
                String lyricsBody = lyric.getLyricsBody();
                lyrics = lyricsBody.split("\n");
            }
            else
                lyrics = new String[0];

            return () -> {
                adapter.notifyDataSetChanged();
                Log.d( "Music_Player", "lyrics body " + Arrays.toString(lyrics));
            };
        });
    }

    private void getLyric()
    {
        getLyrics();
    }

//    private void syncLyrics(Music music )
//    {
//        Helper.Worker.executeTask(() -> {
//            Log.d( "Music_Player", "getting lyrics" );
//            Track track = null;
//
//            lyricsService.getSubtitle( music );
////                initHandlers( snippet );
//
//            return () -> {};
//        });
//
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_lyrics, container, false);
        initRecycleView( view );
        initButton( view );
        initFrame();
        initLyrics();
        return view;
    }

    private void initLyrics() {
        getLyric();
    }

    private void initFrame()
    {
        FrameLayout lyricFrame = getActivity().findViewById(R.id.player_lyrics);
        lyricFrame.setVisibility(View.VISIBLE);
    }

    private void initRecycleView( View view )
    {
        adapter = new CustomAdapter();
        listView = view.findViewById( R.id.lyrics_list );
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.top = 20;
                outRect.bottom = 20;

                if ( parent.getChildAdapterPosition( view ) == 0 )
                    outRect.top = 10;

                if ( parent.getChildAdapterPosition( view ) == parent.getAdapter().getItemCount() - 1 )
                    outRect.bottom = 10;
            }
        });
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
        LyricsFragment musicFragment = new LyricsFragment();
        musicFragment.setMusic( currentMusic );
        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
        transaction.commit();
    }

    public static void show(FragmentManager fragmentManager, Music currentMusic, PlayerDatabase.MusicInfo musicInfo) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        LyricsFragment musicFragment = new LyricsFragment();
        musicFragment.setMusic( currentMusic );
        musicFragment.setInfo( musicInfo );
        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
        transaction.commit();
    }

    public void setInfo(PlayerDatabase.MusicInfo info) {
        this.info = info;
    }

//    public static void show(AppCompatActivity activity, Music currentMusic) {
//        FragmentManager fragmentManager = activity.getSupportFragmentManager();
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        LyricsFragment musicFragment = new LyricsFragment();
//        musicFragment.setMusic( currentMusic );
//        FrameLayout lyricFrame = activity.findViewById(R.id.player_lyrics);
//        lyricFrame.setVisibility(View.VISIBLE);
//        transaction.add( R.id.player_lyrics, musicFragment, "Player_Lyrics" );
//        transaction.commit();
//    }

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

}
