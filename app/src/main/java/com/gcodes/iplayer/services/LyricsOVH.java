package com.gcodes.iplayer.services;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public class LyricsOVH {

    private static LyricsService service;

    public interface LyricsService {
        @Headers("Content-Type: application/json")
        @GET("{artist}/{title}")
        Call<Lyrics> getLyrics(@Path("artist") String artist, @Path("title") String title);
    }

    public static LyricsService createService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.lyrics.ovh/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(LyricsService.class);
    }

    public static LyricsService getService()
    {
        if ( service == null )
            service = createService();
        return service;
    }

    public static Lyrics getLyrics(String artist, String title )
    {
        LyricsService service = getService();
        Call<Lyrics> lyricsCall = service.getLyrics(artist, title);
        try {
            Response<Lyrics> response = lyricsCall.execute();
            if ( response.isSuccessful() )
                return response.body();
            else
                return null;
        } catch (IOException e) {
            return null;
        }
    }

    public class Lyrics
    {
        public String getLyrics() {
            return lyrics;
        }

        public void setLyrics(String lyrics) {
            this.lyrics = lyrics;
        }

        @SerializedName("lyrics")
        private String lyrics;

        public Lyrics(String lyrics) {
            this.lyrics = lyrics;
        }

        public boolean exist()
        {
            return lyrics != null && !lyrics.isEmpty();
        }

        @Override
        public String toString() {
            return "Lyrics{" +
                    "lyrics='" + lyrics + '\'' +
                    '}';
        }
    }
}

