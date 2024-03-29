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
import com.gcodes.iplayer.helpers.CursorRecyclerViewAdapter;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class AllFragment extends Fragment
{
    private String sort = MediaStore.Video.Media.DEFAULT_SORT_ORDER + " asc";

    private String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
    };

    private static Cursor cursor;

    public AllFragment() {
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
        if ( cursor == null )
        {
            CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sort );
            cursor = loader.loadInBackground();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        CustomAdapter adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        GridLayoutManager layout = new GridLayoutManager(getContext(), 2);
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);
        return view;
    }

    public class CustomAdapter extends CursorRecyclerViewAdapter< ItemHolder >
    {
        public CustomAdapter()
        {
            super(AllFragment.this.getContext(), cursor);
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_card, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemHolder holder, Cursor cursor)
        {
            holder.setTitle( cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.TITLE)));
            holder.setSubtitle( getDuration( cursor ) );
            String path = cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
            GlideApp.with( AllFragment.this ).load( new CustomProcessFetcher( path ) )
                    .placeholder( R.drawable.ic_track_black_24dp ).apply( centerCropTransform() ).into( holder.getImage() );

            final Video video = Video.getIntance(cursor);

            holder.itemView.setOnClickListener(v -> {
                VideoPlayerActivity.play( getContext(), video );
//                VideoPlayerActivity.render( getContext(), Video.getIntance( cursor ) );

//                Intent intent = new Intent( getContext(), ExoPlayerActivity.class );

//                Intent intent = new Intent( getContext(), VideoPlayerActivity.class );
//                intent.putExtra( "media", id );
//                startActivity( intent );
            });
        }

        public String getDuration( Cursor cursor )
        {
            String raw = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
            if ( raw == null || raw.isEmpty() )
                return "";
            long dur = Long.parseLong( raw );
            long h = TimeUnit.MILLISECONDS.toHours(dur);
            long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
            long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );
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
