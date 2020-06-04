package com.gcodes.iplayer.music.folder;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.gcodes.iplayer.ui.UIConstance;

import java.io.File;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.gcodes.iplayer.helpers.GlideOptions.circleCropTransform;

public class FolderFragment extends Fragment
{
    private String selection = String.format( "%s != 0 and %s like ?", MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.DATA );

    private String[] selectionArgs;

    private String sort = null;

    private int level = 1;
    private File parent = null;
    private TreeMap< String, String > entry = new TreeMap<>();
    private TreeMap< String, Music > entryMusic = new TreeMap<>();
//    private TreeMap< String, Integer > entry = new TreeMap<>((o1, o2) ->
//    {
//        if ( ( o1.startsWith( "+") & o2.startsWith("+") ) || ( o1.startsWith( "-") & o2.startsWith("-") ) )
//            return o1.compareTo( o2 );
//        else
//            return o1.startsWith( "+" ) ? 1 : -1;
//    });

    private String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_KEY,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID
    };

    private static Cursor cursor;
    private CustomAdapter adapter;
    private String[] entryFiles;
    private CursorLoader artLoader;
    private MainActivity backActivity;
    private TextView folderPath;

    public FolderFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setPath("");
        load( false );
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if ( context instanceof MainActivity )
            backActivity = (MainActivity) context;
    }

    public void load(boolean notify )
    {
        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sort );
        cursor = loader.loadInBackground();
        artLoader = new CursorLoader( getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?", null,
                null);
        initialize();
        if ( notify )
            adapter.notifyDataSetChanged();
    }

    public void setPath( String path )
    {
        selectionArgs = new String[]{ path + "%" };
    }

    public void initialize()
    {
        entry.clear();
        entryMusic.clear();
        if  ( cursor.moveToFirst() )
        {
            do {
                String fPath = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String[] fPaths = fPath.split("/");
                String fName = fPaths[ level ];
                if ( ( fPaths.length - 1 ) == level )
                {
                    entry.put( "-" + fName, cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST ) ) );
                    entryMusic.put( "-" + fName, Music.getIntance( cursor, artLoader ) );
                }
                else
                {
                    fName = "+" + fName;
                    if ( entry.containsKey( fName ) )
                    {
                        entry.put( fName, String.valueOf( Integer.parseInt(entry.get( fName ) ) + 1 ) );
                    }
                    else
                    {
                        entry.put( fName, String.valueOf( 1 ) );
                    }
                }
            } while ( cursor.moveToNext() );
        }
        entryFiles = entry.keySet().toArray(new String[]{});
//        Log.d("Folder_Fragment", "Entry " + Arrays.toString(entryFiles));
//        Log.d("Folder_Fragment", "Entry " + entry );
    }

    public void enter( String name )
    {
//        if ( parent != null )
//        {
//            parent = new File( parent, name );
//            setPath( parent.getAbsolutePath() );
//        }
//        else
//            setPath( "" );
        Log.w("Folder_Path", String.format("entering %s %s", parent != null ? parent.getAbsolutePath() : null, name ) );

        parent = new File( parent, name );
        setPath( parent.getAbsolutePath() );
        ++level;
        load( true );
        updatePath( parent );

        Log.w("Folder_Path", String.format("entered %s %s %s", parent != null ? parent.getAbsolutePath() : null, name, parent.getParent() ) );
    }

    private void updatePath(File parent) {
        String path = parent.getAbsolutePath();
        Log.w("Folder_Path", path );
        folderPath.setText( path );
    }

    private void home() {
        Log.w("Folder_Path", "/" );
        folderPath.setText( "/" );
    }

    public boolean back()
    {
        if ( this.parent == null )
            return false;

        String parent = this.parent.getParent();

        if ( parent != null )
        {
            this.parent = new File( parent );
            setPath( this.parent.getAbsolutePath() );
            updatePath( this.parent );
        }
        else
        {
            this.parent = null;
            setPath("");
            home();
        }

        --level;
        load( true );
        Log.d("Folder_Fragment", "The level is " + level );
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.folder_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = view.findViewById(R.id.item_list);
        folderPath = view.findViewById(R.id.folder_path);
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);

        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1 ));

        listView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                backActivity.register( this::back );
            else
                backActivity.unregister();

            Log.w("Folder_Focus", hasFocus ? "Fragment has focus" : "Fragment doesn't has focus" );
        });

        return view;
    }


    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if ( viewType == 0 )
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_view_box, parent, false);
            else
                view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public int getItemViewType(int position) {
            String key = entryFiles[ position ];
            if ( !key.startsWith( "-" ) )
                return 0;
            else
                return 1;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            String key = entryFiles[ position ];
            String path = key.substring( 1 );
            String info = entry.get(key);
            holder.setTitle( path );

            if ( key.startsWith( "-" ) )
            {
                holder.setSubtitle( info );
                Music music = entryMusic.get(key);
                holder.setFileImage(music);
                holder.itemView.setOnClickListener( null );
                holder.itemView.setOnClickListener( v -> {
                    MusicPlayer.play( music );
                });
            }
            else
            {
                holder.setSubtitle( info + " Songs" );
                holder.setFolderImage();
                holder.itemView.setOnClickListener(v -> {
                    enter( path );
                });
            }
        }

        @Override
        public int getItemCount() {
            return entryFiles.length;
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {

        private TextView title;
        private TextView subtitle;
        private ImageView image;

        public ItemHolder(@NonNull View itemView) {
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

        public Bitmap getImage() {
            return image.getDrawingCache();
        }

        public void setFileImage()
        {
            int resId = getResources().getIdentifier("ic_music_black_24dp", "drawable",
                    getContext().getPackageName());
            this.image.setImageResource( resId );
        }

//        public void setFileImage( Music music )
//        {
//            Bitmap artBitmap = music.getArtBitmap(FolderFragment.this.getContext());
//            if ( artBitmap != null )
//                this.image.setImageBitmap( artBitmap );
//            else
//            {
//                int resId = getResources().getIdentifier("ic_music_black_24dp", "drawable",
//                        getContext().getPackageName());
//                this.image.setImageResource( resId );
//            }
//        }

        public void setFileImage(Music music)
        {
//            getResources().getDrawable()
            GlideApp.with( FolderFragment.this ).load( new ProcessModelLoaderFactory.MusicProcessFetcher( FolderFragment.this.getContext(), music ) )
                    .placeholder( R.drawable.u_song_art_padded ).apply( circleCropTransform() ).into( image );
        }

        public void setFolderImage()
        {
            int resId = getResources().getIdentifier("u_folder_art", "drawable",
                    getContext().getPackageName());
            this.image.setImageResource( resId );
        }
    }

}
