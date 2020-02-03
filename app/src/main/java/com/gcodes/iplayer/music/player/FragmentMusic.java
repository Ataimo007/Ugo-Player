package com.gcodes.iplayer.music.player;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.player.PlayerManager;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.music.Music;
import com.google.android.exoplayer2.ui.PlayerControlView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment
 */
public class FragmentMusic extends Fragment {

    private View controlView;
    private int trackShutterId;
//    private SimpleExoPlayer player;
//    private DefaultDataSourceFactory factory;

    public FragmentMusic() {
        // Required empty public constructor
    }

    public static void render() {
        PlayerManager playerManager = PlayerManager.getInstance();
        FragmentManager manager = playerManager.getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FragmentMusic fragmentMusic = newInstance();
        View musicControl = playerManager.getActivity().findViewById(R.id.music_control);
        musicControl.setVisibility( View.VISIBLE );
        transaction.replace( R.id.music_control, fragmentMusic);
//        transaction.add( R.id.music_control, fragmentMusic);
        transaction.commit();
    }

    public static void render(AppCompatActivity activity ) {
        PlayerManager playerManager = PlayerManager.getInstance();
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FragmentMusic fragmentMusic = newInstance();
        View musicControl = activity.findViewById(R.id.music_control);
        musicControl.setVisibility( View.VISIBLE );
        transaction.replace( R.id.music_control, fragmentMusic);
//        transaction.add( R.id.music_control, fragmentMusic);
        transaction.commit();
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentMusic.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentMusic newInstance() {
        FragmentMusic fragment = new FragmentMusic();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        initPlayer();
//        initSources();
        init();
    }

    private void init() {
        trackShutterId = getResources().getIdentifier("ic_track_black_24dp", "drawable",
                getContext().getPackageName());
    }

//    private void initSources()
//    {
//        factory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), getResources().getString(R.string.app_name)));
//        String gsonMusic = getArguments().getString("music");
//        Music music = Music.fromGson(gsonMusic);
//        ExtractorMediaSource musicSource = getMusicSource(music);
//        player.prepare( musicSource );
//    }

//    private ExtractorMediaSource getMusicSource( Music music )
//    {
//        Uri media = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf( music.getMediaId() ) );
////        Log.d( "Music_Player", "Music " + id );
////        Log.d( "Music_Player", "Music " + media.getPath() );
//        ExtractorMediaSource source = new ExtractorMediaSource.Factory(factory).createMediaSource(media);
//        return source;
//    }

    private void initView()
    {
//        PlayerView view;
        PlayerControlView control = controlView.findViewById(R.id.music_control_view);
        control.setShowTimeoutMs( -1 );
        control.setPlayer( MusicPlayer.getInstance().getPlayerManager() );
        MusicPlayer.registerOnTrackChange(this::consumeTrack);
        MusicPlayer.consumeTrack( this::consumeTrack );
        controlView.setOnClickListener( v -> {
            showMusicPlayer();
        });
    }

    private void showMusicPlayer()
    {
        Intent intent = new Intent( getContext(), MusicPlayerActivity.class );
        getContext().startActivity( intent );
    }

    private void consumeTrack( Music music )
    {
        TextView trackName = controlView.findViewById(R.id.exo_track);
        trackName.setText( music.getName() );
        TextView artistName = controlView.findViewById(R.id.exo_artist);
        artistName.setText( music.getArtist() );
        ImageView art = controlView.findViewById(R.id.exo_album_art);
        setImage( music.getArtPath(), art);
    }

    public void setImage(String path, ImageView image)
    {
        if ( path != null )
        {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if ( bitmap != null )
            {
                image.setImageBitmap( bitmap );
                return;
            }
        }

        image.setImageResource( trackShutterId );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        controlView = inflater.inflate(R.layout.fragment_music, container, false);
        initView();
        return controlView;
    }

}
