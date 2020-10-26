package com.gcodes.iplayer.services;

import android.util.Log;

import com.gcodes.iplayer.database.PlayerDatabase;
import com.gcodes.iplayer.music.models.Music;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.entity.track.TrackData;
import org.jmusixmatch.subtitle.Subtitle;

public class MusixMatchLyricsService
{
    private static MusixMatchLyricsService lyricsService;
    private final static String apiKey = "7d07b4f39f7722d3cd1f52cd45da73a2";

    private MusixMatch musixMatch;

    public static MusixMatchLyricsService getInstance()
    {
        if ( lyricsService == null )
            lyricsService = new MusixMatchLyricsService();
        return lyricsService;
    }

    public MusixMatchLyricsService()
    {
        musixMatch = new MusixMatch( apiKey );
    }

    public Lyrics getLyrics(TrackData data ){
        int trackID = data.getTrackId();
        try {
            return musixMatch.getLyrics(trackID);
        } catch (MusixMatchException e) {
            e.printStackTrace();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    public TrackData getTrack(Music music ){
        TrackData data;
        try {
            Track track = musixMatch.getMatchingTrack(music.getName(), music.getArtist());
            data = track.getTrack();
        } catch (MusixMatchException e) {
            e.printStackTrace();
            data = null;
        } catch (Exception e) {
            Log.d( "Music_Player", "lyrics error cause by " + e.getMessage() );
            data = null;
        }
        return data;
    }

    public TrackData getTrack(PlayerDatabase.MusicInfo info){
        TrackData data;
        try {
            Track track = musixMatch.getMatchingTrack(info.getTitle(), info.getAllArtists() );
            data = track.getTrack();
        } catch (MusixMatchException e) {
            e.printStackTrace();
            data = null;
        } catch (Exception e) {
            Log.d( "Music_Player", "lyrics error cause by " + e.getMessage() );
            data = null;
        }
        return data;
    }

    public Subtitle getSubtitle(Music music ){
        Subtitle subtitle;
        try {
            Track track = musixMatch.getMatchingTrack(music.getName(), music.getArtist());
            TrackData data = track.getTrack();
            int trackID = data.getTrackId();
            subtitle = musixMatch.getSubtitle(trackID);
        } catch (MusixMatchException e) {
            e.printStackTrace();
            subtitle = null;
        } catch (Exception e) {
            Log.d( "Music_Player", "lyrics error cause by " + e.getMessage() );
            subtitle = null;
        }
        return subtitle;
    }
}
