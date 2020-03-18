package com.gcodes.iplayer.music.player;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

import jp.wasabeef.glide.transformations.BlurTransformation;

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

    private String[] lyrics = new String[0];
    private RecyclerView listView;
    private CustomAdapter adapter;
    private ImageButton lyricsButton;
    private ImageButton videoButton;
    private Music currentMusic;

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
        videoButton = content.findViewById( R.id.player_show_video );
        initView();

        initRecycleView( content );
        initButton();

//        musicName = content.findViewById( R.id.song_name );
//        artistName = content.findViewById( R.id.artist_name );

        return content;
    }

    private void initButton() {
        lyricsButton.setOnClickListener( v -> {
            if ( isLyricsVisible() )
                hideLyrics();
            else
                showLyrics();
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
        }
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

//    private void recognizeMusic( Music music )
//    {
//        Helper.Worker.executeTask(() -> {
//            try {
//                String info = ACRService.getInstance().recognizeMusic(music);
//                Log.d( "ACR_Service", "The music info is " + info );
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.d( "ACR_Service", "Error in getting music info " + e.getMessage() );
//            }
//
//            return () -> {};
//
//        });
//    }

    private void recognizeMusic( Music music )
    {
        Log.d( "ACR_Service", "Getting musis info ..." );
        Helper.Worker.executeTask(() -> {



            return () -> {};

        });
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
