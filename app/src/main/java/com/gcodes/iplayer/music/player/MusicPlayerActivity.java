package com.gcodes.iplayer.music.player;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.services.ACRService;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.JsonObject;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class MusicPlayerActivity extends AppCompatActivity{

    private PlayerView playerView;
    private Menu menu;
    private Toolbar toolbar;
    private int currentTrack = -1;
    private Music currentMusic;
    private Player.EventListener trackListener;
    private PlayerDatabase.MusicInfo musicInfo;
    private PlayerControlView control;
    private TextView musicName;
    private TextView artistName;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player2);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initView();
    }

    private void prepareMusicPlayer()
    {
        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
        videoScreen.setVisibility( View.GONE );
        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
        coverArt.setVisibility(View.VISIBLE);
    }

    private void prepareVideoPlayer()
    {
        AspectRatioFrameLayout videoScreen = findViewById(R.id.exo_player_content_frame);
        videoScreen.setVisibility( View.VISIBLE );
        ConstraintLayout coverArt = findViewById(R.id.player_art_view);
        coverArt.setVisibility(View.GONE);
    }

    private void initView()
    {
        initViews();
        initPlayer();
    }

    private void initViews() {
        control = findViewById(R.id.music_control_view2);
        musicName = findViewById( R.id.player_music );
        artistName = findViewById( R.id.player_artist );
        image = findViewById( R.id.player_art );
    }

    public void showLyrics(View view)
    {
        LyricsFragment.show( getSupportFragmentManager(), currentMusic, musicInfo );
    }

    public void showMusicVideo(View view)
    {
//        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic );
        MusicVideoFragment.show( getSupportFragmentManager(), currentMusic, musicInfo, t -> prepareVideoPlayer(),
                t1 -> prepareMusicPlayer() );
    }

    private void initPlayer()
    {
//        PlayerView playerView;
        control.setShowTimeoutMs( -1 );
        control.setPlayer( MusicPlayer.getInstance().getPlayerManager() );

//        playerView = findViewById(R.id.music_player_view);
//        playerView.showController();
//        playerView.setControllerShowTimeoutMs(-1);
//        playerView.setControllerHideOnTouch( false );
//        playerView.setPlayer( MusicPlayer.getInstance().getPlayerManager() );

        trackListener = MusicPlayer.registerOnTrackChange(this::consumeTrack);
        MusicPlayer.consumeTrack( this::consumeTrack );
        initRotateAnim();
    }

    private void initRotateAnim() {
        CardView art = findViewById(R.id.player_album_art);
        MusicPlayer.onStateChange( art );
    }

    @Override
    protected void onDestroy() {
        MusicPlayer.unRegisterOnTrackChange( trackListener );
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_music_activity, menu);
        prepareSearchMenu();
//        return super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch  ( id )
        {
            case R.id.action_playlist:
                if  ( item.isChecked() )
                    PlaylistFragment.hide( getSupportFragmentManager() );
                else
                    PlaylistFragment.show( getSupportFragmentManager() );
                item.setChecked( !item.isChecked() );
                break;
        }
        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_user)
//        {
//            Intent user = new Intent( this, LoginUser.class );
//            startActivity( user );
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareSearchMenu() {
        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
    }

    private void consumeTrack( Music music )
    {
        Log.d("Music_Player", " Consuming track " + music.getName() );
        int newTrack = MusicPlayer.getCurrentTrack();
        if ( currentTrack != -1 || currentTrack != newTrack )
        {
            setImage( music );
            musicName.setText( music.getName() );
            artistName.setText( music.getArtist() );
            currentTrack = newTrack;
//            getLyricsIfExist( music );
        }

        currentMusic = music;

        // get online music info
        PlayerDatabase database = PlayerDatabase.getInstance();
        Helper.Worker.executeTask( () -> {
            musicInfo = database.playerDao().getInfo(music);
            return () -> {};
        });

//        update();
//        recognizeMusic( music );
    }

    public void setImage(Music music)
    {
        GlideApp.with( this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( this, music ) )
                .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
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

    private void update() {

    }

    public void imageToToolbar(String path)
    {
        if ( path != null && !path.isEmpty() )
        {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if ( bitmap != null )
            {
                Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 70, 70, true);
                if ( bitmap != null )
                {
                    BitmapDrawable art = new BitmapDrawable(getResources(), smallBitmap);
                    Glide.with( this ).load( bitmap )
                            .apply( RequestOptions.bitmapTransform(new BlurTransformation( 10, 1 ) ) )
                            .into( ( ImageView ) findViewById(R.id.music_background) );
                    toolbar.setLogo( art );
                    return;
                }
            }
        }
        int resId = getResources().getIdentifier("ic_track_black_24dp", "drawable", getPackageName());
        Glide.with( this ).load( R.drawable.ic_track_black_24dp )
                .into( ( ImageView ) findViewById(R.id.music_background) );
        toolbar.setLogo( resId );
    }
}
