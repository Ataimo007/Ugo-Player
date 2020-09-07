package com.gcodes.iplayer.video;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import com.gcodes.iplayer.helpers.Helper;
import com.google.common.base.Objects;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;

import static com.gcodes.iplayer.helpers.Helper.gson;

public class Video implements Comparable< Video >, Serializable
{
    private final String name;
    private final long id;
    private final long duration;
    private final long date;
    private final String data;

    public Video(String name, long id, long duration, long date, String data) {
        this.name = name;
        this.id = id;
        this.date = date;
        this.data = data;
        this.duration = duration;
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

    public DateTime getDate()
    {
        return new DateTime( Duration.standardSeconds( date ).getMillis() );
    }

    public String getDateString()
    {
        return Helper.getDate( new DateTime( Duration.standardSeconds( date ).getMillis() ) );
    }

    public static Video getIntance(Bundle bundle )
    {
        return new Video( bundle.getString("name"), bundle.getLong("id"), bundle.getLong( "duration" ),
                bundle.getLong( "date" ), bundle.getString("data") );
    }

    public static Video getIntance( Cursor cursor )
    {
        return new Video( cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DURATION)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)),
                cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.DATA)) );
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public long getDuration() {
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

}
