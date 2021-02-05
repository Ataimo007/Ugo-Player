package com.gcodes.iplayer.video.model;

import android.util.Log;

import com.google.common.base.Objects;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static com.gcodes.iplayer.helpers.Helper.gson;

public final class Series implements Comparable< Series >
{
    private final String name;
    private final Video[] videos;
    private final long duration;

    public Series(String name, Video[] videos) {
        this.name = name;
        this.videos = videos;
        int dur = 0;
        for ( Video video : videos )
            dur += video.getRawDuration();
        duration = dur;
    }

    public static Series getIntance( Video[] videos, int similarity )
    {
        String name = similarity > 0 ? videos[ 0 ].getName().substring( 0, similarity ) : videos[ 0 ].getName();
//            String name = videos[ 0 ].getName().substring( 0, similarity );
        return new Series( name, videos );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Series)) return false;
        Series series = (Series) o;
        return duration == series.duration &&
                Objects.equal(name, series.name) &&
                Objects.equal(videos, series.videos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, videos, duration);
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

    public String toGson()
    {
        return gson.toJson(this );
    }

    public static Series fromGson( String json )
    {
        return gson.fromJson( json, Series.class );
    }

    public String getName() {
        return name;
    }

    public int getCount()
    {
        return videos.length;
    }

    public Video[] getVideos() {
        return videos;
    }

    @Override
    public int compareTo(Series o) {
        return getName().compareTo( o.getName() );
    }

    public Video getVideo(int position) {
        return videos[ position ];
    }
}
