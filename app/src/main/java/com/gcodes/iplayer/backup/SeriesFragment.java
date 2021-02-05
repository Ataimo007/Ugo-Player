//package com.gcodes.iplayer.backup;
//
//import android.content.Context;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.media.ThumbnailUtils;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.loader.content.CursorLoader;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.gcodes.iplayer.MainActivity;
//import com.gcodes.iplayer.R;
//import com.gcodes.iplayer.helpers.GlideApp;
//import com.gcodes.iplayer.helpers.ProcessModelLoaderFactory;
//import com.gcodes.iplayer.ui.UIConstance;
//import com.gcodes.iplayer.video.model.Video;
//import com.gcodes.iplayer.video.player.VideoPlayer;
//
//import org.joda.time.Period;
//import org.joda.time.format.PeriodFormatter;
//import org.joda.time.format.PeriodFormatterBuilder;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.TreeSet;
//import java.util.concurrent.TimeUnit;
//
//import static com.bumptech.glide.request.RequestOptions.centerCropTransform;
//
//public class SeriesFragment extends Fragment
//{
//    private boolean inside = false;
//    private MainActivity backActivity;
//
//
//    private String[] projection = {
//            MediaStore.Video.Media._ID,
//            MediaStore.Video.Media.DISPLAY_NAME,
//            MediaStore.Video.Media.TITLE,
//            MediaStore.Video.Media.DURATION,
//            MediaStore.Video.Media.DATE_MODIFIED,
//            MediaStore.Video.Media.DATA
//    };
//
//    private static Cursor cursor;
//    private Series[] series;
////    private Video[] videos;
//    private CustomAdapter adapter;
//
//    public SeriesFragment() {
//    }
//
////    public static void main(String[] args) {
////        System.out.println( "AS".hashCode() );
////        System.out.println( "AS".hashCode() );
////    }
//
//    // try joining audio column to media column
//    @Override
//    public void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        load();
//    }
//
//    public void load()
//    {
//        CursorLoader loader = new CursorLoader( this.getContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//                projection, null, null, null );
//        cursor = loader.loadInBackground();
//        initialize();
//        inside = false;
//    }
//
//    public void initialize()
//    {
//        LinkedList<Video> videos = getVideos();
//        this.series = toSeries(videos);
//    }
//
//    private Series[] toSeries( LinkedList<Video> videos )
//    {
//        ArrayList< Video > prototype;
//        TreeSet< Series > collection = new TreeSet<>();
//        int best;
//        Iterator<Video> iterator = videos.iterator();
//        while ( iterator.hasNext() )
//        {
//            Video last = iterator.next();
//            iterator.remove();
//            best = 0;
//            prototype = new ArrayList<>();
//            while ( iterator.hasNext() )
//            {
//                Video video = iterator.next();
//                int sim = similarityIndex(last, video);
//                if ( sim > 1 )
//                {
//                    if ( sim == best )
//                        prototype.add( video );
//                    else
//                    {
//                        if  ( sim > best )
//                        {
//                            best = sim;
//                            prototype.clear();
//                            prototype.add( video );
//                        }
//                    }
//                }
//            }
//            videos.removeAll( prototype );
//            prototype.add( last );
//            Collections.sort( prototype ); // done on enter
//            Series series = Series.getIntance(prototype.toArray(new Video[]{}), best);
//            collection.add( series );
//            iterator = videos.iterator();
//        }
//        return collection.toArray( new Series[]{} );
//    }
//
//    private int similarityIndex(Video last, Video video)
//    {
//        int i;
//        String name1 = last.getName();
//        String name2 = video.getName();
//        int length = Math.min( name1.length(), name2.length() );
//        for ( i = 0; i < length && name1.charAt( i ) == name2.charAt( i ); ++i );
//        return i;
//    }
//
//    private LinkedList< Video > getVideos()
//    {
//        LinkedList< Video > videos = new LinkedList<>();
//        if  ( cursor.moveToFirst() )
//        {
//            do {
//                Video video = Video.getIntance(cursor);
//                videos.add( video );
//            } while ( cursor.moveToNext() );
//        }
//        return videos;
//    }
//
//    @Override
//    public void onAttach(@NonNull Context context) {
//        super.onAttach(context);
//        if ( context instanceof MainActivity)
//        {
//            backActivity = (MainActivity) context;
//            backActivity.register( this::back );
//        }
//    }
//
//    public boolean back()
//    {
//        if ( inside)
//        {
//            exit();
//            return true;
//        }
//        else
//            return false;
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        backActivity.unregister();
//    }
//
//    public void enter(Series series)
//    {
////        videos = series.getVideos();
//        adapter.setVideos(series.getVideos());
//        inside = true;
//        adapter.notifyDataSetChanged();
//    }
//
//    public void exit()
//    {
////        adapter.setVideos( videos );
//        inside = false;
//        adapter.notifyDataSetChanged();
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.item_list, container, false);
//        adapter = new CustomAdapter();
//        RecyclerView listView = (RecyclerView) view;
//        listView.setLayoutManager( new LinearLayoutManager( getContext() ) );
//        listView.setAdapter( adapter );
//
//        listView.addItemDecoration(new UIConstance.AppItemDecorator( 1, 20 ));
//        return view;
//    }
//
//
//    public class CustomAdapter extends RecyclerView.Adapter< ItemHolder >
//    {
//        private final int FOLDER_VIEW_TYPE = 1;
//        private final int VIDEO_VIEW_TYPE = 2;
//        private final int SINGLE_FOLDER_VIEW_TYPE = 3;
//
//        public CustomAdapter(Video[] videos) {
//            this.videos = videos;
//        }
//
//        private Video[] videos;
//
//        public CustomAdapter() {
//
//        }
//
//        @Override
//        public int getItemViewType(int position)
//        {
////            if ( inside )
//            if ( inside )
//                return VIDEO_VIEW_TYPE;
//            else
//            {
//                Series current = series[ position ];
//                if ( current.getCount() == 1 )
//                    return SINGLE_FOLDER_VIEW_TYPE;
//                else
//                    return FOLDER_VIEW_TYPE;
//            }
//
//        }
//
//        @Override
//        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType)
//        {
////            if ( viewType == VIDEO_VIEW_TYPE )
////                return onCreateVideoHolder( parent, viewType );
////            else
////                return onCreateFolderHolder( parent, viewType );
//            if ( viewType == FOLDER_VIEW_TYPE )
//                return onCreateFolderHolder( parent, viewType );
//            else
//                return onCreateVideoHolder( parent, viewType );
//        }
//
//        public ItemHolder onCreateFolderHolder(ViewGroup parent, int viewType)
//        {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.video_view, parent, false);
//            return new ItemHolder(view);
//        }
//
//        public ItemHolder onCreateVideoHolder(ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.video_view, parent, false);
//            return new ItemHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull ItemHolder holder, int position)
//        {
//            int itemViewType = holder.getItemViewType();
//            switch ( itemViewType )
//            {
//                case VIDEO_VIEW_TYPE:
//                    onBindVideoHolder( holder, position );
//                    break;
//
//                case FOLDER_VIEW_TYPE:
//                    onBindFolderHolder( holder, position );
//                    break;
//
//                case SINGLE_FOLDER_VIEW_TYPE:
//                    onBindSingleFolderHolder( holder, position );
//                    break;
//            }
////            if ( inside )
////                onBindVideoHolder( holder, position );
////            else
////                onBindFolderHolder( holder, position );
//        }
//
//        public void onBindSingleFolderHolder(@NonNull ItemHolder holder, int position)
//        {
//            final Video video = series[ position ].getVideos()[ 0 ];
//            holder.setTitle( video.getName() );
//            holder.setSubtitle( parseDuration( video.getDuration() ) );
//            holder.setDate( video.getDateString() );
//
//            GlideApp.with( SeriesFragment.this ).load( new CustomProcessFetcher( video ) )
//                    .placeholder( R.drawable.u_song_art_padded ).apply( centerCropTransform() ).into( holder.getImage() );
//
//            holder.itemView.setOnClickListener(v -> {
//                VideoPlayer.play( getActivity(), video );
//            });
//        }
//
//        public void onBindFolderHolder(@NonNull ItemHolder holder, int position)
//        {
//            Series current = series[ position ];
//            holder.setTitle( current.getName() );
//            holder.setSubtitle( current.getDuration() );
//            holder.setDate( String.format( "%d %s", current.getCount(), current.getCount() > 1 ? "Videos" : "Video" ) );
////            holder.setFolderImage();
//            GlideApp.with( SeriesFragment.this ).load( new CustomProcessFetcher( current.getVideos() ) )
//                    .placeholder( R.drawable.u_song_art_padded ).apply( centerCropTransform() ).into( holder.getImage() );
//
//            holder.itemView.setOnClickListener(v ->
//            {
//                enter( current );
//            });
//        }
//
//        public void onBindVideoHolder(final ItemHolder holder, int position)
//        {
//            final Video video = videos[ position ];
//            holder.setTitle( video.getName() );
//            holder.setSubtitle( parseDuration( video.getDuration() ) );
//            holder.setDate( video.getDateString() );
//
//            long id = video.getId();
//
////            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
////            String path = video.getData();
//
//            GlideApp.with( SeriesFragment.this ).load( new CustomProcessFetcher( video ) )
//                    .placeholder( R.drawable.u_song_art_padded ).apply( centerCropTransform() ).into( holder.getImage() );
//
////            Video[] vids = Arrays.copyOf(SeriesFragment.this.videos, videos.length, Video[].class);
//            holder.itemView.setOnClickListener(v -> {
//                VideoPlayer.play( getActivity(), position, videos );
//            });
//        }
//
//        @Override
//        public int getItemCount()
//        {
//            if ( inside )
//                return videos.length;
//            else
//                return series.length;
//        }
//
//
//        public String parseDuration( long dur )
//        {
//            long h = TimeUnit.MILLISECONDS.toHours(dur);
//            long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
//            long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );;
//            String hs = h > 0 ? String.format( "%02d:", h ) : "";
//            String ms = m > 0 || h > 0 ? String.format( "%02d:", m ) : "";
//            String ss = h == 0 && m == 0 ? String.format( "%02d secs", s ) : String.format( "%02d", s );
//            return hs + ms + ss;
//        }
//
//        public void setVideos(Video[] videos) {
//            this.videos = videos;
//        }
//    }
//
//    public class ItemHolder extends RecyclerView.ViewHolder
//    {
//
//        private final TextView title;
//        private final TextView subtitle;
//        private final TextView date;
//        private final ImageView image;
//
//        public ItemHolder(@NonNull View itemView) {
//            super(itemView);
//            title = itemView.findViewById(R.id.item_title);
//            subtitle = itemView.findViewById(R.id.item_subtitle);
//            date = itemView.findViewById(R.id.item_description);
//            image = itemView.findViewById(R.id.item_image);
//        }
//
//        public String getTitle() {
//            return title.getText().toString();
//        }
//
//        public void setTitle(String name) {
//            this.title.setText( name );
//        }
//
//        public void setDate(String name) {
//            this.date.setText( name );
//        }
//
//        public String getSubtitle() {
//            return subtitle.getText().toString();
//        }
//
//        public void setSubtitle(String subtitle) {
//            this.subtitle.setText(subtitle);
//        }
//
//        public Bitmap getImageBitmap() {
//            return image.getDrawingCache();
//        }
//
//        public ImageView getImage() {
//            return image;
//        }
//
//        public void setFileImage()
//        {
//            int resId = getResources().getIdentifier("ic_music_black_24dp", "drawable",
//                    getContext().getPackageName());
//            this.image.setImageResource( resId );
//        }
//
//        public void setFolderImage()
//        {
//            int resId = getResources().getIdentifier("ic_folder_black_24dp", "drawable",
//                    getContext().getPackageName());
//            this.image.setImageResource( resId );
//        }
//    }
//
//    public class CustomProcessFetcher implements ProcessModelLoaderFactory.ProcessFetcher
//    {
//        private final Video[] videos;
//
//        public CustomProcessFetcher( Video[] videos )
//        {
//            this.videos = videos;
//        }
//
//        public CustomProcessFetcher( Video video )
//        {
//            this.videos = new Video[]{ video };
//        }
//
//        @Override
//        public Object getKey() {
//            return videos[ 0 ].getData();
//        }
//
//        @Override
//        public Bitmap load()
//        {
//            for ( Video video : videos )
//            {
//                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(video.getData(), MediaStore.Video.Thumbnails.MINI_KIND);
//                if ( thumbnail != null )
//                    return thumbnail;
//            }
//            return null;
//        }
//    }
//
//    private static final class Series implements Comparable< Series >
//    {
//        private final String name;
//        private final Video[] videos;
//        private final long duration;
//
//        public Series(String name, Video[] videos) {
//            this.name = name;
//            this.videos = videos;
//            int dur = 0;
//            for ( Video video : videos )
//                dur += video.getDuration();
//            duration = dur;
//        }
//
//        public static Series getIntance( Video[] videos, int similarity )
//        {
//            String name = similarity > 0 ? videos[ 0 ].getName().substring( 0, similarity ) : videos[ 0 ].getName();
////            String name = videos[ 0 ].getName().substring( 0, similarity );
//            return new Series( name, videos );
//        }
//
//        public String getDuration()
//        {
//            Period period = new Period(duration);
//            PeriodFormatter periodFormatter = new PeriodFormatterBuilder().minimumPrintedDigits(2).appendHours()
//                    .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendMinutes()
//                    .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).printZeroAlways().appendSeconds().toFormatter();
//            String periodRep = String.format( "%s%s", period.toString( periodFormatter ), period.toStandardSeconds().getSeconds() < 60 ? " secs" : "");
//            Log.d( "Video_Info", String.format( "duration %d period %s rep %s", duration, period, periodRep ) );
//            return period.toString( periodFormatter );
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public int getCount()
//        {
//            return videos.length;
//        }
//
//        public Video[] getVideos() {
//            return videos;
//        }
//
//        @Override
//        public int compareTo(Series o) {
//            return getName().compareTo( o.getName() );
//        }
//    }
//}
