package com.gcodes.iplayer.video.model;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.loader.content.CursorLoader;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.Helper;
import com.gcodes.iplayer.music.models.Music;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.common.base.Objects;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import static com.gcodes.iplayer.helpers.Helper.gson;

public class Video implements Comparable< Video >, Serializable
{
    public final static String sort = MediaStore.Video.Media.DEFAULT_SORT_ORDER + " asc";
    public final static String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA
    };

    private final String name;
    private final String displayName;
    private final long id;
    private final long duration;
    private final long date;
    private final String data;
    private final boolean fromStore;

    public Video(String name, String displayName, long id, long duration, long date, String data, boolean fromStore) {
        this.name = name;
        this.displayName = displayName;
        this.id = id;
        this.date = date;
        this.data = data;
        this.duration = duration;
        this.fromStore = fromStore;
    }

    public Video(String name, String displayName, long id, long duration, long date, String data) {
        this(name, displayName, id, duration, date, data, true);
    }

    public boolean isFromStore() {
        return fromStore;
    }

    public static Video getInstance(Context context, Uri data) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, data);

        Cursor cursor = findMedia(context, data);

        if (cursor != null)
        {
            return getInstance(cursor, false);
        }
        else
        {
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            long date = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            Cursor contentCursor = context.getContentResolver().query(data, null, null, null, null);
            contentCursor.moveToFirst();
            String displayName = contentCursor.getString(contentCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            long mediaId = Objects.hashCode(title, displayName, duration, date);

            return new Video(title, displayName, mediaId, duration, date, data.toString(), false);
        }
    }

    private static Cursor findMedia(Context context, Uri data)
    {
        Log.d("music_info", String.format("Try to find music store, %s", data.toString()));

        String query = String.format("%s == '%s'", MediaStore.Video.Media.DATA, data.getPath());
        CursorLoader loader = new CursorLoader(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, query, null, sort);
        Cursor cursor = loader.loadInBackground();

        if (cursor != null && cursor.getCount() >= 1)
        {
            Log.d("video_info", "Found in path " + cursor.getCount());
            cursor.moveToFirst();
            return cursor;
        }
        else
            return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video video = (Video) o;
        return id == video.id &&
                duration == video.duration &&
                date == video.date &&
                Objects.equal(name, video.name) &&
                Objects.equal(data, video.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, duration, date, data);
    }

    public DateTime getRawDate()
    {
        return new DateTime( Duration.standardSeconds( date ).getMillis() );
    }

    public String getDateString()
    {
        return Helper.getDate( new DateTime( Duration.standardSeconds( date ).getMillis() ) );
    }

    public static Video getInstance(Bundle bundle )
    {
        return new Video( bundle.getString("name"), bundle.getString("display_name"), bundle.getLong("id"), bundle.getLong( "duration" ),
                bundle.getLong( "date" ), bundle.getString("data") );
    }

    public String getDuration()
    {
        Period period = new Period(duration);
        PeriodFormatter periodFormatter = new PeriodFormatterBuilder().minimumPrintedDigits(2).appendHours()
                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendMinutes()
                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).printZeroAlways().appendSeconds().toFormatter();
        String periodRep = String.format( "%s%s", period.toString( periodFormatter ), period.toStandardSeconds().getSeconds() < 60 ? " secs" : "");
        Log.d( "Video_Info", String.format( "duration %d period %s rep %s", duration, period, periodRep ) );
        return period.toString( periodFormatter );
    }

    public String getDate()
    {
        DateTime date = new DateTime(Duration.standardSeconds(this.date).getMillis());
        return Helper.getDate( date );
    }

//    public String getDuration()
//    {
//        String raw = String.valueOf(duration);
//        if ( raw == null || raw.isEmpty() )
//            return "";
//        long dur = Long.parseLong( raw );
//        long h = TimeUnit.MILLISECONDS.toHours(dur);
//        long m = TimeUnit.MILLISECONDS.toMinutes(dur) - TimeUnit.HOURS.toMinutes( h );
//        long s = TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds( m ) - TimeUnit.HOURS.toSeconds( h );
//        String hs = h > 0 ? String.format( "%02d:", h ) : "";
//        String ms = m > 0 || h > 0 ? String.format( "%02d:", m ) : "";
//        String ss = h == 0 && m == 0 ? String.format( "%02d secs", s ) : String.format( "%02d", s );
//        return hs + ms + ss;
//    }

    public static Video getInstance(Cursor cursor)
    {
        return new Video( cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DURATION)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DATA)) );
    }

    public static Video getInstance(Cursor cursor, boolean fromStore)
    {
        return new Video( cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DURATION)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DATA)), fromStore );
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public long getRawDuration() {
        return duration;
    }

    public String getData() {
        return data;
    }

    @Override
    public int compareTo(Video o) {
        return getName().compareTo( o.getName() );
    }

    @Override
    public String toString() {
        return "Video{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", id=" + id +
                ", duration=" + duration +
                ", date=" + date +
                ", data='" + data + '\'' +
                ", fromStore=" + fromStore +
                '}';
    }

    public Bundle toBundle()
    {

        Bundle vid = new Bundle();
        vid.putString( "name", name );
        vid.putLong( "id", id );
        vid.putLong( "duration", duration );
        vid.putLong( "date", date );
        vid.putString( "data", data );
        return vid;
    }

    public String toGson()
    {
        return gson.toJson(this );
    }

    public JsonObject toJson()
    {
        return JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
    }

    public static Video fromGson( String json )
    {
        return gson.fromJson( json, Video.class );
    }

    public static Video fromJson( JsonObject json )
    {
        return gson.fromJson( json.toString(), Video.class );
    }

    public static String[] toGson(Video[] videos)
    {
        String gsonVideos[] = new String[ videos.length ];
        for ( int i = 0; i < videos.length; ++i )
            gsonVideos[ i ] = videos[ i ].toGson();
        return gsonVideos;
    }

    public static Video[] fromGson(String[] gsonVideos)
    {
        Video videos[] = new Video[ gsonVideos.length ];
        for ( int i = 0; i < videos.length; ++i )
            videos[ i ] = Video.fromGson(gsonVideos[ i ]);
        return videos;
    }

    public ProgressiveMediaSource getMediaSource(Context context) {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( id ) );
        String userAgent = Util.getUserAgent(context, context.getResources().getString(R.string.app_name));
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(context, userAgent);
        return new ProgressiveMediaSource.Factory(factory).createMediaSource(media);
    }
}
