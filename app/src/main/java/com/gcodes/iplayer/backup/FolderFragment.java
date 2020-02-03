package com.gcodes.iplayer.backup;

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

import com.gcodes.iplayer.R;

import java.io.File;
import java.util.Arrays;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FolderFragment extends Fragment
{
    private String selection = String.format( "%s != 0 and %s like ?", MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.DATA );

    private String[] selectionArgs;

    private String sort = null;

    private int level = 1;
    private File parent = null;
    private TreeMap< String, String > entry = new TreeMap<>();
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

    public void load( boolean notify )
    {
        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sort );
        cursor = loader.loadInBackground();
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
        if  ( cursor.moveToFirst() )
        {
            do {
                String fPath = cursor.getString( cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String[] fPaths = fPath.split("/");
                String fName = fPaths[ level ];
                if ( ( fPaths.length - 1 ) == level )
                    entry.put( "-" + fName, cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST ) ) );
                else
                {
                    fName = "+" + fName;
                    if ( entry.containsKey( fName ) )
                        entry.put( fName, String.valueOf( Integer.parseInt(entry.get( fName ) ) + 1 ) );
                    else
                        entry.put( fName, String.valueOf( 1 ) );
                }
            } while ( cursor.moveToNext() );
        }
        entryFiles = entry.keySet().toArray(new String[]{});
        Log.d("Folder_Fragment", "Entry " + Arrays.toString(entryFiles));
        Log.d("Folder_Fragment", "Entry " + entry );
    }

    public void enter( String name )
    {
        parent = new File( parent, name );
        setPath( parent.getAbsolutePath() );
        ++level;
        load( true );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter(adapter);
        return view;
    }


    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            String key = entryFiles[ position ];
            String path = key.substring( 1 );
            String info = entry.get(key);
            holder.setTitle( path );
            holder.setSubtitle( info );
            if ( key.startsWith( "-" ) )
                holder.setFileImage();
            else
                holder.setFolderImage();

            holder.itemView.setOnClickListener(v -> {
                enter( path );
            });
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

        public void setFolderImage()
        {
            int resId = getResources().getIdentifier("ic_folder_black_24dp", "drawable",
                    getContext().getPackageName());
            this.image.setImageResource( resId );
        }
    }

}
