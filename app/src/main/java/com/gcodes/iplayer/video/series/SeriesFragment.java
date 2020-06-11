package com.gcodes.iplayer.video.series;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
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
import com.gcodes.iplayer.music.player.MusicPlayer;
import com.gcodes.iplayer.ui.UIConstance;
import com.gcodes.iplayer.video.Series;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.VideoFragment;
import com.gcodes.iplayer.video.player.VideoPlayer;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.loader.content.CursorLoader;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class SeriesFragment extends Fragment implements VideoFragment.SectionsPagerAdapter.PageTitle
{
    private String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA
    };

    private static Cursor cursor;
    private Series[] series;

    private CustomAdapter adapter;

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
    }

    public void initialize()
    {
        LinkedList<Video> videos = getVideos();
        this.series = toSeries(videos);
    }

    private Series[] toSeries( LinkedList<Video> videos )
    {
        ArrayList< Video > prototype;
        TreeSet< Series > collection = new TreeSet<>();
        int best;
        Iterator<Video> iterator = videos.iterator();
        while ( iterator.hasNext() )
        {
            Video last = iterator.next();
            iterator.remove();
            best = 0;
            prototype = new ArrayList<>();
            while ( iterator.hasNext() )
            {
                Video video = iterator.next();
                int sim = similarityIndex(last, video);
                if ( sim > 1 )
                {
                    if ( sim == best )
                        prototype.add( video );
                    else
                    {
                        if  ( sim > best )
                        {
                            best = sim;
                            prototype.clear();
                            prototype.add( video );
                        }
                    }
                }
            }
            videos.removeAll( prototype );
            prototype.add( last );
            Collections.sort( prototype ); // done on enter
            Series series = Series.getIntance(prototype.toArray(new Video[]{}), best);
            collection.add( series );
            iterator = videos.iterator();
        }
        return collection.toArray( new Series[]{} );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.w("Video_Player", String.format("Video Controller state handling result" ) );
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == VideoPlayer.REQUEST_PLAYER )
        {
            VideoPlayer.getInstance().tryRenderVideoPlayer( resultCode );
        }
    }

    private int similarityIndex(Video last, Video video)
    {
        int i;
        String name1 = last.getName();
        String name2 = video.getName();
        int length = Math.min( name1.length(), name2.length() );
        for ( i = 0; i < length && name1.charAt( i ) == name2.charAt( i ); ++i );
        return i;
    }

    private LinkedList< Video > getVideos()
    {
        LinkedList< Video > videos = new LinkedList<>();
        if  ( cursor.moveToFirst() )
        {
            do {
                Video video = Video.getIntance(cursor);
                videos.add( video );
            } while ( cursor.moveToNext() );
        }
        return videos;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        listView.setAdapter( adapter );

        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1, 20 ));
        return view;
    }

    @Override
    public String getTitle() {
        return "Series";
    }


    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
    {
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_view, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position)
        {
            Series aSeries = series[ position ];
            if ( aSeries.getCount() == 1 )
                onBindSingleFolderHolder( holder, aSeries );
            else
                onBindFolderHolder( holder, aSeries );
        }

        public void onBindSingleFolderHolder(@NonNull ItemHolder holder, Series aSeries)
        {
            final Video video = aSeries.getVideos()[ 0 ];
            holder.setTitle( video.getName() );
            holder.setSubtitle( parseDuration( video.getDuration() ) );
            holder.setDate( video.getDateString() );

            GlideApp.with( SeriesFragment.this ).load( new CustomProcessFetcher( video ) )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( holder.getImage() );

            holder.itemView.setOnClickListener(v -> {
                VideoPlayer.play( SeriesFragment.this, video );
            });
        }

        public void onBindFolderHolder(@NonNull ItemHolder holder, Series aSeries)
        {
            holder.setTitle( aSeries.getName() );
            holder.setSubtitle( aSeries.getDuration() );
            holder.setDate( String.format( "%d %s", aSeries.getCount(), aSeries.getCount() > 1 ? "Videos" : "Video" ) );
            GlideApp.with( SeriesFragment.this ).load( new CustomProcessFetcher( aSeries.getVideos() ) )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( holder.getImage() );

            holder.itemView.setOnClickListener(v ->
            {
                SeriesPlayerFragment.navigate( aSeries, SeriesFragment.this );
            });
        }

        @Override
        public int getItemCount()
        {
            return series.length;
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
        private final TextView title;
        private final TextView subtitle;
        private final TextView date;
        private final ImageView image;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            date = itemView.findViewById(R.id.item_description);
            image = itemView.findViewById(R.id.item_image);
        }

        public String getTitle() {
            return title.getText().toString();
        }

        public void setTitle(String name) {
            this.title.setText( name );
        }

        public void setDate(String name) {
            this.date.setText( name );
        }

        public String getSubtitle() {
            return subtitle.getText().toString();
        }

        public void setSubtitle(String subtitle) {
            this.subtitle.setText(subtitle);
        }

        public ImageView getImage() {
            return image;
        }
    }

    public class CustomProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
    {
        private final Video[] videos;

        public CustomProcessFetcher( Video[] videos )
        {
            this.videos = videos;
        }

        public CustomProcessFetcher( Video video )
        {
            this.videos = new Video[]{ video };
        }

        @Override
        public Object getKey() {
            return videos[ 0 ].getData();
        }

        @Override
        public Bitmap load()
        {
            for ( Video video : videos )
            {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(video.getData(), MediaStore.Video.Thumbnails.MINI_KIND);
                if ( thumbnail != null )
                    return thumbnail;
            }
            return null;
        }
    }
}
