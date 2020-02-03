package com.gcodes.iplayer.video;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class FolderFragment extends Fragment
{
    private boolean inside = false;

    private TreeMap< String, ArrayList< Video >> entry = new TreeMap<>((o1, o2) ->
    {
        return o1.substring( o1.lastIndexOf( "/" ) ).compareTo( o2.substring( o2.lastIndexOf( "/" ) ) );
    });
    private ArrayList< Video > videos = new ArrayList<>();

    private String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
    };

    private static Cursor cursor;
    private String[] entryFiles;
    private CustomAdapter adapter;

    public FolderFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        load();
    }

    public void load()
    {
        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null );
        cursor = loader.loadInBackground();
        initialize();
        inside = false;
    }

    public void initialize()
    {
        entry.clear();
        if  ( cursor.moveToFirst() )
        {
            do {
                String fPath = cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                String fPaths = fPath.substring( 0, fPath.lastIndexOf( "/" ) );

                if ( entry.containsKey( fPaths ) )
                {
                    ArrayList<Video> videos = entry.get(fPaths);
                    videos.add( Video.getIntance( cursor ) );
//                    entry.put( fPaths, videos );
                }
                else
                    entry.put( fPaths, new ArrayList<>( Arrays.asList( Video.getIntance( cursor ) ) ) );

            } while ( cursor.moveToNext() );
        }
        entryFiles = entry.keySet().toArray(new String[]{});
//        Log.d("Folder_Fragment", "Entry " + Arrays.toString(entryFiles));
//        Log.d("Folder_Fragment", "Entry " + entry );
    }

    public void enter( String key )
    {
        videos.clear();
        videos.addAll( entry.get( key ) );
        inside = true;
        adapter.notifyDataSetChanged();
    }

    public void exit()
    {
        inside = false;
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter( adapter );
        return view;
    }


    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        private final int FOLDER_VIEW_TYPE = 1;
        private final int VIDEO_VIEW_TYPE = 2;

        @Override
        public int getItemViewType(int position)
        {
            if ( inside )
                return VIDEO_VIEW_TYPE;
            else
                return FOLDER_VIEW_TYPE;
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            if ( viewType == VIDEO_VIEW_TYPE )
                return onCreateVideoHolder( parent, viewType );
            else
                return onCreateFolderHolder( parent, viewType );
        }

        public ItemHolder onCreateFolderHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view, parent, false);
            return new ItemHolder(view);
        }

        public ItemHolder onCreateVideoHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position)
        {
            if ( inside )
                onBindVideoHolder( holder, position );
            else
                onBindFolderHolder( holder, position );
        }

        public void onBindFolderHolder(@NonNull ItemHolder holder, int position)
        {
            String key = entryFiles[ position ];
            String folder = key.substring( key.lastIndexOf( "/" ) + 1 );
            int videoCount = entry.get(key).size();
            holder.setTitle( folder );
            holder.setSubtitle(String.valueOf(videoCount));
            holder.setFolderImage();

            holder.itemView.setOnClickListener(v ->
            {
                enter( key );
            });
        }

        public void onBindVideoHolder(final ItemHolder holder, int position)
        {
            final Video video = videos.get(position);
            holder.setTitle( video.getName() );
            holder.setSubtitle( parseDuration( video.getDuration() ) );
            String path = video.getData();
            GlideApp.with( FolderFragment.this ).load( new CustomProcessFetcher( path ) )
                    .placeholder( R.drawable.ic_track_black_24dp ).apply( centerCropTransform() ).into( holder.getImage() );

            Video[] vids = videos.toArray( new Video[]{} );
            holder.itemView.setOnClickListener(v -> {
                VideoPlayerActivity.play( getContext(), position, vids );
            });
        }

        @Override
        public int getItemCount()
        {
            if ( inside )
                return videos.size();
            else
                return entryFiles.length;
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

        public Bitmap getImageBitmap() {
            return image.getDrawingCache();
        }

        public ImageView getImage() {
            return image;
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

    public class CustomProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
        private final String path;

        public CustomProcessFetcher( String path )
        {
            this.path = path;
        }

        @Override
        public Object getKey() {
            return path;
        }

        @Override
        public Bitmap load() {
            return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
        }
    }

}
