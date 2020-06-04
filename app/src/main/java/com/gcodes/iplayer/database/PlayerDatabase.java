package com.gcodes.iplayer.database;

import android.content.Context;
import android.util.Log;

import com.gcodes.iplayer.music.Music;
import com.gcodes.iplayer.services.ACRService;
import com.gcodes.iplayer.services.LyricsOVH;
import com.gcodes.iplayer.services.MusixMatchLyricsService;
import com.gcodes.iplayer.services.YouTubeService;
import com.google.api.services.youtube.model.Video;
import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.TrackData;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import retrofit2.Call;

@Database( entities = {PlayerDatabase.MusicInfo.class, PlayerDatabase.MusicLyrics.class,
        PlayerDatabase.MusicVideo.class}, version = 1, exportSchema = false )
@TypeConverters( {PlayerDatabase.Converters.class})
public abstract class PlayerDatabase extends RoomDatabase
{
    private static PlayerDatabase database;

    public abstract PlayerDao playerDao();

    public static PlayerDatabase getInstance(Context context)
    {
        if ( database == null )
            database = Room.databaseBuilder(context, PlayerDatabase.class, "ultimate_player").build();
        return database;
    }

    public MusicInfo getInfo( Music music )
    {
        MusicInfo info = playerDao().getMusicInfo(music);
        Log.d("ACR_Service", "from database info is " + info );
        if ( info == null )
        {
            info = ACRService.getInstance().recognizeMusic(music);
            if ( info != null )
                playerDao().addMusicInfo( info );
        }
        return info;
    }

    public List<Video> getMusicVideo(Music music )
    {
        YouTubeService youtube = YouTubeService.getIntance();
        String query = String.format( "%s %s", music.getName(), music.getArtist() ) ;

        List<Video> videos;
        try {
            videos = youtube.getVideos(query);
        } catch (IOException e) {
            e.printStackTrace();
            videos = new ArrayList<>();
        }
        Log.d( "YouTube_API", "The Videos " + videos );
        return videos;
    }

    public List<Video> getMusicVideo( MusicInfo music )
    {
        YouTubeService youtube = YouTubeService.getIntance();
        String query = String.format( "%s %s", music.getTitle(), music.getAllArtists() ) ;

        List<Video> videos;
        try {
            videos = youtube.getVideos(query);
        } catch (IOException e) {
            e.printStackTrace();
            videos = new ArrayList<>();
        }
        Log.d( "YouTube_API", "The Videos " + videos );
        return videos;
    }

    public MusicLyrics getLyrics(Music music)
    {
        MusicLyrics musicLyrics = playerDao().getDatabaseLyrics(music);
        Log.d("ACR_Service", "from database musicLyrics is " + musicLyrics);
        if ( musicLyrics == null )
        {
            MusixMatchLyricsService service = MusixMatchLyricsService.getInstance();
            TrackData track = service.getTrack(music);
            if ( track != null )
            {
                LyricsOVH.Lyrics lyric = LyricsOVH.getLyrics(music.getArtist(), music.getName());
                if ( lyric != null )
                {
                    musicLyrics = new MusicLyrics(music, track, lyric);
                    playerDao().addLyrics( musicLyrics );
                }
                else
                {
                    Lyrics lyrics = service.getLyrics(track);
                    if ( lyrics != null )
                    {
                        musicLyrics = new MusicLyrics(music, track, lyrics);
                        playerDao().addLyrics( musicLyrics );
                    }
                }
            }
        }
        return musicLyrics;
    }

    public MusicLyrics getLyrics(MusicInfo info)
    {
        MusicLyrics musicLyrics = playerDao().getDatabaseLyrics(info);
        Log.d("ACR_Service", "from database musicLyrics is " + musicLyrics);
        if ( musicLyrics == null )
        {
            MusixMatchLyricsService service = MusixMatchLyricsService.getInstance();
            TrackData track = service.getTrack(info);
            if ( track != null )
            {
                LyricsOVH.Lyrics lyric = LyricsOVH.getLyrics(info.getAllArtists(), info.getTitle());
                Log.d("ACR_Service", "from database Lyrics is " + lyric);
                if ( lyric != null )
                {
                    musicLyrics = new MusicLyrics(info, track, lyric);
                    playerDao().addLyrics( musicLyrics );
                }
                else {
                    Lyrics lyrics = service.getLyrics(track);
                    if (lyrics != null) {
                        musicLyrics = new MusicLyrics(info, track, lyrics);
                        playerDao().addLyrics(musicLyrics);
                    }
                }
            }
        }
        return musicLyrics;
    }

    public static PlayerDatabase getInstance()
    {
        return database;
    }

    public void clear()
    {
        this.clearAllTables();
    }

    public static void initialize(Context context)
    {
        database = Room.databaseBuilder(context, PlayerDatabase.class, "ultimate_player").build();
    }

    @Entity( indices = {@Index( value = {"video_id"}, unique = true ) })
    public static class MusicVideo
    {
        // id, videoId, mediaId, uri, title, publishedAt, statistics, thumbnail_uri, duration

        @PrimaryKey( autoGenerate = true )
        private int id;

        @ColumnInfo( name = "video_id" )
        private String videoId;

        @ColumnInfo( name = "media_id" )
        private long mediaId;

        @ColumnInfo( name = "title" )
        private String title;

        @ColumnInfo( name = "published_at" )
        private DateTime publishedAt;

        @ColumnInfo( name = "statistics" )
        private BigInteger statistics;

        @ColumnInfo( name = "video_uri" )
        private String videoUri;

        @ColumnInfo( name = "thumbnail_uri" )
        private String thumbnailUri;

        @ColumnInfo( name = "duration" )
        private Period duration;

        public MusicVideo(Video video, String videoUri, Music music)
        {
            this( video.getId(), music.getMediaId(), video.getSnippet().getTitle(),
                    DateTime.parse(video.getSnippet().getPublishedAt().toString()), video.getStatistics().getViewCount(),
                    videoUri, video.getSnippet().getThumbnails().getHigh().getUrl(), Period.parse( video.getContentDetails().getDuration() ) );
        }

        public MusicVideo(String videoId, long mediaId, String title, DateTime publishedAt, BigInteger statistics, String videoUri, String thumbnailUri, Period duration) {
            this.videoId = videoId;
            this.mediaId = mediaId;
            this.title = title;
            this.publishedAt = publishedAt;
            this.statistics = statistics;
            this.videoUri = videoUri;
            this.thumbnailUri = thumbnailUri;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return "MusicVideo{" +
                    "id=" + id +
                    ", videoId='" + videoId + '\'' +
                    ", mediaId=" + mediaId +
                    ", title='" + title + '\'' +
                    ", publishedAt=" + publishedAt +
                    ", statistics=" + statistics +
                    ", videoUri='" + videoUri + '\'' +
                    ", thumbnailUri='" + thumbnailUri + '\'' +
                    ", duration=" + duration +
                    '}';
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public long getMediaId() {
            return mediaId;
        }

        public void setMediaId(long mediaId) {
            this.mediaId = mediaId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public DateTime getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(DateTime publishedAt) {
            this.publishedAt = publishedAt;
        }

        public BigInteger getStatistics() {
            return statistics;
        }

        public void setStatistics(BigInteger statistics) {
            this.statistics = statistics;
        }

        public String getVideoUri() {
            return videoUri;
        }

        public void setVideoUri(String videoUri) {
            this.videoUri = videoUri;
        }

        public String getThumbnailUri() {
            return thumbnailUri;
        }

        public void setThumbnailUri(String thumbnailUri) {
            this.thumbnailUri = thumbnailUri;
        }

        public Period getDuration() {
            return duration;
        }

        public void setDuration(Period duration) {
            this.duration = duration;
        }
    }

    @Entity( indices = {@Index( value = {"acrid", "media_id", "track_id"}, unique = true ) } )
    public static class MusicLyrics
    {
        @PrimaryKey(autoGenerate = true)
        private int id;

        @ColumnInfo( name = "acrid")
        private String acrid;

        @ColumnInfo(name = "media_id")
        private long mediaId;

        @ColumnInfo(name = "track_id")
        private long trackId;

        @ColumnInfo(name = "album_art")
        private final String albumArt;

        @ColumnInfo(name = "lyrics_body")
        private String lyricsBody;

        @ColumnInfo(name = "lyrics_copyright")
        private String lyricsCopyright;

        @ColumnInfo(name = "lyrics_id")
        private int lyricsId;

        @ColumnInfo(name = "lyrics_language")
        private String lyricsLanguage;

        @ColumnInfo(name = "pixel_tracking_url")
        private String pixelTrackingURL;

        @ColumnInfo(name = "script_tracking_url")
        private String scriptTrackingURL;

        public MusicLyrics(MusicInfo info, TrackData data, Lyrics lyrics)
        {
            acrid = info.getAcrid();
            mediaId = info.getMediaId();
            trackId = data.getTrackId();
            albumArt = data.getAlbumCoverart800x800();
            lyricsBody = lyrics.getLyricsBody();
            lyricsCopyright = lyrics.getLyricsCopyright();
            lyricsId = lyrics.getLyricsId();
            lyricsLanguage = lyrics.getLyricsLang();
            scriptTrackingURL = lyrics.getScriptTrackingURL();
            pixelTrackingURL = lyrics.getPixelTrackingURL();
        }

        public MusicLyrics(Music music, TrackData data, Lyrics lyrics)
        {
            mediaId = music.getMediaId();
            trackId = data.getTrackId();
            albumArt = data.getAlbumCoverart800x800();
            lyricsBody = lyrics.getLyricsBody();
            lyricsCopyright = lyrics.getLyricsCopyright();
            lyricsId = lyrics.getLyricsId();
            lyricsLanguage = lyrics.getLyricsLang();
            scriptTrackingURL = lyrics.getScriptTrackingURL();
            pixelTrackingURL = lyrics.getPixelTrackingURL();
        }

        public MusicLyrics(Music music, TrackData data, LyricsOVH.Lyrics lyrics )
        {
            mediaId = music.getMediaId();
            trackId = data.getTrackId();
            albumArt = data.getAlbumCoverart800x800();
            lyricsBody = lyrics.getLyrics();
        }

        public MusicLyrics(String acrid, long mediaId, long trackId, String albumArt, String lyricsBody, String lyricsCopyright, int lyricsId, String lyricsLanguage, String pixelTrackingURL, String scriptTrackingURL) {
            this.acrid = acrid;
            this.mediaId = mediaId;
            this.trackId = trackId;
            this.albumArt = albumArt;
            this.lyricsBody = lyricsBody;
            this.lyricsCopyright = lyricsCopyright;
            this.lyricsId = lyricsId;
            this.lyricsLanguage = lyricsLanguage;
            this.pixelTrackingURL = pixelTrackingURL;
            this.scriptTrackingURL = scriptTrackingURL;
        }

        public MusicLyrics(MusicInfo info, TrackData data, LyricsOVH.Lyrics lyric) {
            acrid = info.getAcrid();
            mediaId = info.getMediaId();
            trackId = data.getTrackId();
            albumArt = data.getAlbumCoverart800x800();
            lyricsBody = lyric.getLyrics();
        }

        @Override
        public String toString() {
            return "MusicLyrics{" +
                    "id=" + id +
                    ", acrid='" + acrid + '\'' +
                    ", mediaId=" + mediaId +
                    ", trackId=" + trackId +
                    ", albumArt='" + albumArt + '\'' +
                    ", lyricsBody='" + lyricsBody + '\'' +
                    ", lyricsCopyright='" + lyricsCopyright + '\'' +
                    ", lyricsId=" + lyricsId +
                    ", lyricsLanguage='" + lyricsLanguage + '\'' +
                    ", pixelTrackingURL='" + pixelTrackingURL + '\'' +
                    ", scriptTrackingURL='" + scriptTrackingURL + '\'' +
                    '}';
        }

        public String getAlbumArt() {
            return albumArt;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getAcrid() {
            return acrid;
        }

        public void setAcrid(String acrid) {
            this.acrid = acrid;
        }

        public long getMediaId() {
            return mediaId;
        }

        public void setMediaId(long mediaId) {
            this.mediaId = mediaId;
        }

        public long getTrackId() {
            return trackId;
        }

        public void setTrackId(long trackId) {
            this.trackId = trackId;
        }

        public String getLyricsBody() {
            return lyricsBody;
        }

        public void setLyricsBody(String lyricsBody) {
            this.lyricsBody = lyricsBody;
        }

        public String getLyricsCopyright() {
            return lyricsCopyright;
        }

        public void setLyricsCopyright(String lyricsCopyright) {
            this.lyricsCopyright = lyricsCopyright;
        }

        public int getLyricsId() {
            return lyricsId;
        }

        public void setLyricsId(int lyricsId) {
            this.lyricsId = lyricsId;
        }

        public String getLyricsLanguage() {
            return lyricsLanguage;
        }

        public void setLyricsLanguage(String lyricsLanguage) {
            this.lyricsLanguage = lyricsLanguage;
        }

        public String getPixelTrackingURL() {
            return pixelTrackingURL;
        }

        public void setPixelTrackingURL(String pixelTrackingURL) {
            this.pixelTrackingURL = pixelTrackingURL;
        }

        public String getScriptTrackingURL() {
            return scriptTrackingURL;
        }

        public void setScriptTrackingURL(String scriptTrackingURL) {
            this.scriptTrackingURL = scriptTrackingURL;
        }
    }

    @Entity( indices = {@Index( value = {"acrid", "media_id"}, unique = true ) } )
    public static class MusicInfo
    {
        @PrimaryKey( autoGenerate = true )
        private int id;

        @ColumnInfo( name = "acrid")
        private String acrid;

        @ColumnInfo( name = "title")
        private String title;

        @ColumnInfo( name = "media_id")
        private long mediaId;

        @ColumnInfo( name = "album")
        private String album;

        @ColumnInfo( name = "label")
        private String label;

        @ColumnInfo( name = "duration")
        private Period duration;

        @ColumnInfo( name = "artists")
        private ArrayList<String> artists;

        public MusicInfo(String title, long mediaId, String acrid, String album, String label, Period duration, ArrayList<String> artists) {
            this.title = title;
            this.mediaId = mediaId;
            this.acrid = acrid;
            this.album = album;
            this.label = label;
            this.duration = duration;
            this.artists = artists;
        }

        public MusicInfo(JsonObject info, Music extMusic) {
            JsonObject music = info.getAsJsonObject("metadata").getAsJsonArray("music").get(0).getAsJsonObject();
            title = music.getAsJsonPrimitive("title").getAsString();
            label = music.has( "label" ) ? music.getAsJsonPrimitive("label").getAsString()
                : "No Record Label Yet";
            mediaId = extMusic.getMediaId();
            acrid = music.getAsJsonPrimitive("acrid").getAsString();
            int durationMs = music.getAsJsonPrimitive("duration_ms").getAsInt();
            album = music.getAsJsonObject("album").getAsJsonPrimitive("name").getAsString();
            duration = Period.millis(durationMs);
            artists = new ArrayList<>();
            for (JsonElement artist : music.getAsJsonArray( "artists" ) )
            {
                artists.add( artist.getAsJsonObject().getAsJsonPrimitive("name" ).getAsString() );
            }
        }

        @Override
        public String toString() {
            return "MusicInfo{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", mediaId=" + mediaId +
                    ", album='" + album + '\'' +
                    ", label='" + label + '\'' +
                    ", duration=" + duration +
                    ", artists=" + artists +
                    '}';
        }

        public String getAcrid() {
            return acrid;
        }

        public void setAcrid(String acrid) {
            this.acrid = acrid;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Period getDuration() {
            return duration;
        }

        public void setDuration(Period duration) {
            this.duration = duration;
        }

        public ArrayList<String> getArtists() {
            return artists;
        }

        public String getAllArtists() {
            Joiner joiner = Joiner.on( ", " ).skipNulls();
            return joiner.join( artists );
        }

        public void setArtists(ArrayList<String> artists) {
            this.artists = artists;
        }

        public void addArtist(String artists) {
            this.artists.add( artists );
        }

        public long getMediaId() {
            return mediaId;
        }

        public void setMediaId(long mediaId) {
            this.mediaId = mediaId;
        }
    }

    @Dao
    public static abstract class PlayerDao
    {
        @Insert
        public abstract void addInfo(MusicInfo... musicInfo );

        @Insert
        public abstract void addMusicVideo(MusicVideo... musicVideos );

        @Query("select * from musicinfo where media_id = :mediaKey")
        public abstract MusicInfo findInfo(long mediaKey);

        @Insert
        public abstract void addLyrics(MusicLyrics... lyrics );

        @Query("select * from MusicLyrics where media_id = :mediaKey")
        public abstract MusicLyrics findLyrics(long mediaKey );

        @Query("select * from MusicVideo where media_id = :mediaId")
        public abstract List<MusicVideo> findMusicVideos(long mediaId );

        @Query("select * from MusicLyrics where acrid = :acrid")
        public abstract MusicLyrics findLyrics(String acrid );

        @Transaction
        public void addMusicInfo( MusicInfo info )
        {
            addInfo( info );
        }

        @Transaction
        public void addLyrics( MusicInfo info, TrackData data, Lyrics lyrics)
        {
            addLyrics( new MusicLyrics( info, data, lyrics) );
        }

        @Transaction
        public void addLyrics( Music music, TrackData data, Lyrics lyrics)
        {
            addLyrics( new MusicLyrics( music, data, lyrics) );
        }

        @Transaction
        public void addMusicInfo( String title, long mediaKey, String acrid, String album, String label, Period duration, ArrayList<String> artists )
        {
            MusicInfo info = new MusicInfo( title, mediaKey, acrid, album, label, duration, artists );
            addInfo( info );
        }

        @Transaction
        public MusicInfo getMusicInfo( long id )
        {
            MusicInfo musicInfo = findInfo(id);
            return musicInfo;
        }

        @Transaction
        public MusicInfo getMusicInfo( Music music )
        {
            MusicInfo musicInfo = findInfo(music.getMediaId());
            return musicInfo;
        }

        @Transaction
        public MusicLyrics getDatabaseLyrics(Music music )
        {
            MusicLyrics musicLyrics = findLyrics( music.getMediaId() );
            return musicLyrics;
        }

        @Transaction
        public MusicLyrics getDatabaseLyrics(MusicInfo info )
        {
            MusicLyrics musicLyrics = findLyrics( info.getAcrid() );
            return musicLyrics;
        }

        @Transaction
        public boolean ifMusicInfoExist(long key )
        {
            MusicInfo musicInfo = findInfo(key);
            return musicInfo != null;
        }
    }

    public static class Converters
    {
        @TypeConverter
        public Period fromDuration( int durationMs )
        {
            return Period.millis( durationMs );
        }

        @TypeConverter
        public int toDuration( Period period )
        {
            return period.getMillis();
        }

        @TypeConverter
        public DateTime fromDateTime( String date )
        {
            return DateTime.parse( date );
        }

        @TypeConverter
        public String toDateTime( DateTime date )
        {
            return date.toString();
        }

        @TypeConverter
        public BigInteger fromBigInteger( String value )
        {
            return new BigInteger( value );
        }

        @TypeConverter
        public String toDateTime( BigInteger value )
        {
            return value.toString();
        }

        @TypeConverter
        public ArrayList< String > fromList( String list )
        {
            String[] names = list.split(",");
            ArrayList< String > allNames = new ArrayList<>( Arrays.asList( names ) );
            return allNames;
        }

        @TypeConverter
        public String toList( ArrayList< String > list )
        {
            Joiner joiner = Joiner.on( ", " ).skipNulls();
            return joiner.join( list );
        }

    }
}