package com.gcodes.iplayer.video;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;

public class Video implements Comparable< Video >, Serializable
{
    private final String name;
    private final long id;
    private final long duration;
    private final String data;
    private static final Gson gson = new Gson();


    public Video(String name, long id, long duration, String data) {
        this.name = name;
        this.id = id;
        this.data = data;
        this.duration = duration;
    }

    public static Video getIntance( Bundle bundle )
    {
        return new Video( bundle.getString("name"), bundle.getLong("id"), bundle.getLong( "duration" ),
                bundle.getString("data") );
    }

    public static Video getIntance( Cursor cursor )
    {
        return new Video( cursor.getString( cursor.getColumnIndex(MediaStore.Video.Media.TITLE)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media._ID)),
                cursor.getLong( cursor.getColumnIndex(MediaStore.Video.Media.DURATION)),
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
        vid.putString( "data", data );
        return vid;
    }

    public String toGson()
    {
//        Gson gson = new Gson();
        return gson.toJson(this );
    }

    public static Video fromGson( String json )
    {
//        Gson gson = new Gson();
        return gson.fromJson( json, Video.class );
    }

}
