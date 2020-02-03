package com.gcodes.iplayer.services;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.gcodes.iplayer.video.Video;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;

import static com.gcodes.iplayer.helpers.Helper.*;

public class OpenSubtitleService
{
    private static OpenSubtitleService subtitle;
    private final static String host = "https://api.opensubtitles.org/xml-rpc";
    private final static String username = "Ataimo7";
    private final static String password = "alex1234";
    private final static String userAgent = "Ultimate Player v1.0";

    private final XMLRPCClient client;
    private boolean login = false;
    private String token;

    public static OpenSubtitleService getIntance()
    {
        if ( subtitle == null )
            subtitle = new OpenSubtitleService();
        return subtitle;
    }

    public OpenSubtitleService()
    {
        XMLRPCClient client1;
        try
        {
            client1 = new XMLRPCClient( new URL( host ));
        } catch (MalformedURLException e) {
            client1 = null;
            e.printStackTrace();
        }
        client = client1;
    }


    public void login( XMLRPCCallback listener )
    {
        client.callAsync( listener, "LogIn", username, password, "", userAgent );
    }

    public File getSavedSubtitle(Video video, Context context)
    {
        try {
            String name = osHash(String.valueOf(video.getId()), context);
            File file = new File( context.getExternalFilesDir( null ), "subtitle/" + name + ".str" );
            if ( file.exists() )
                return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File getSubtitle(Video video, Context context )
    {
        try {
            login();
            String idSubtitleFile = getIdSubtitleFile(video, context);
            Log.d( "Subtitle_Activities", "Obtained Id SubtitleFile " + idSubtitleFile );

            Object datum = getSubtitleFile( idSubtitleFile );
            File subtitle = save(datum, context, osHash(String.valueOf(video.getId()), context));
            return subtitle;
        } catch (XMLRPCException e) {
            e.printStackTrace();
        } catch ( Exception e )
        {
            e.printStackTrace();
            Log.d( "Subtitle_Activities", "Error in parsing" );
        }
        return null;
    }

    public File getSubtitle(Video video, Context context, Consumer< String > messager )
    {
        try {
            login();
            String idSubtitleFile = getIdSubtitleFile(video, context);
            Log.d( "Subtitle_Activities", "Obtained Id SubtitleFile " + idSubtitleFile );
//            messager.accept( "found movie" );
            Object datum = getSubtitleFile( idSubtitleFile );
//            messager.accept( "downloaded movie" );
            File subtitle = save(datum, context, osHash(String.valueOf(video.getId()), context));
//            messager.accept( "saving subtitle" );
            return subtitle;
        } catch (XMLRPCException e) {
            e.printStackTrace();
        } catch ( Exception e )
        {
            e.printStackTrace();
            Log.d( "Subtitle_Activities", "Error in parsing" );
        }
        return null;
    }

    private String getIdSubtitleFile( Video video, Context context ) throws IOException, XMLRPCException {
        HashMap<String, String> oshashParams = addOSHash(String.valueOf(video.getId()), context);
        String idSubtitleFile = getSubtitleFileID( oshashParams );;
        if ( idSubtitleFile == null )
        {
            Log.d( "Subtitle_Activities", "Subtitle Search by OS Hash failed" );
            HashMap<String, String> movieParams = guessMovie( video.getName() );
            idSubtitleFile = getSubtitleFileID( movieParams );;
        }
        return idSubtitleFile;
    }

    private String osHash(String mediaId, Context context) throws IOException {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( mediaId ) );
        ParcelFileDescriptor mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
        FileDescriptor fileDescriptor = mediaDescriptor.getFileDescriptor();
        FileInputStream stream = new FileInputStream( fileDescriptor );
        String movieHash = OpenSubtitlesHasher.computeHash(stream, stream.getChannel().size());
        return movieHash;
    }

    private String getMovieName(String mediaId, Context context) throws IOException {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( mediaId ) );
        ParcelFileDescriptor mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
        FileDescriptor fileDescriptor = mediaDescriptor.getFileDescriptor();
        FileInputStream stream = new FileInputStream( fileDescriptor );
        String movieHash = OpenSubtitlesHasher.computeHash(stream, stream.getChannel().size());

        return movieHash;
    }

    private HashMap<String, String> addOSHash(String mediaId, Context context) throws IOException
    {
        HashMap<String, String> movieParams = new HashMap<>();
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( mediaId ) );
        ParcelFileDescriptor mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
        FileDescriptor fileDescriptor = mediaDescriptor.getFileDescriptor();
        FileInputStream stream = new FileInputStream( fileDescriptor );
        String movieHash = OpenSubtitlesHasher.computeHash(stream, stream.getChannel().size());
        movieParams.put( "moviehash", movieHash );
        movieParams.put( "moviebytesize", String.valueOf(stream.getChannel().size()));
        Log.d( "Subtitle_Activities", "add os hash to params " + movieParams );
        return movieParams;
    }

    public void downloadSubtitle()
    {

    }

    public File save(Object datum, Context context, String name ) throws IOException {
        String data = String.valueOf(datum);
//        byte[] decode = Base64.decode(data, Base64.DEFAULT);
        byte[] decode = Base64.decode(data, Base64.DEFAULT);
//        String subZip = new String(decode, StandardCharsets.UTF_8);
//        Log.d( "Subtitle_Activities", "Base64 decode " + subZip );

        File strFile = getFile(context, name);
        try ( FileOutputStream subFile = new FileOutputStream(strFile);
              GZIPInputStream unzipped = new GZIPInputStream(new ByteArrayInputStream(decode)))
        {
            byte[] raw = new byte[ 1024 ];
            for ( int len; ( len = unzipped.read( raw )) > 0; )
                subFile.write( raw, 0, len );

            Log.d( "Subtitle_Activities", "Written to file " + strFile );
        }
        return strFile;
    }

    public File save2(Object datum, Context context, String name) throws IOException, DataFormatException {
        Inflater decompresser = new Inflater();
        String data = String.valueOf( datum );
        decompresser.setInput( data.getBytes(StandardCharsets.UTF_8) );
        byte[] strRaw = new byte[ 0 ];
        decompresser.inflate( strRaw );
        Log.d( "Subtitle_Activities", "inflated " + strRaw );
        Log.d( "Subtitle_Activities", "inflated " + new String( strRaw, StandardCharsets.UTF_8) );

        File strFile = getFile(context, name);
        FileOutputStream str = new FileOutputStream( strFile );
        str.write( strRaw );
        Log.d( "Subtitle_Activities", "Written Subtitle to file" );
        return strFile;
    }

    public File getFile(Context context, String name)
    {
        File parent = new File( context.getExternalFilesDir( null ), "subtitle" );
        File file = new File( context.getExternalFilesDir( null ), "subtitle/" + name + ".str" );
        parent.mkdirs();
        Log.d( "Subtitle_Activities", "create subtitle file " + file );
        return file;
    }

    public HashMap< String, String > guessMovie( String name ) throws XMLRPCException {
        Map<String, Object> movies = (Map<String, Object>) client.call( "GuessMovieFromString",
                token, new String[]{ name } );
        Log.d( "Subtitle_Activities", "movies " + movies );
        Map< String, Object > gueses = (Map<String, Object>) movies.get( "data" );
        Log.d( "Subtitle_Activities", "movies data " + gueses );
        Object[] guese = gueses.values().toArray();
        Map<String, Object> bestMovie = (Map<String, Object>) guese[0];
        Map< String, Object > movie = (Map<String, Object>) bestMovie.get( "BestGuess" );
        Log.d( "Subtitle_Activities", "best guess for first movies " + movie );
//        Object idMovieIMDB = movie.get("IDMovieIMDB");
        HashMap<String, String> movieParams = getMovieParams(movie);
        Log.d( "Subtitle_Activities", "params of the guess movie " + movieParams );
//        return String.valueOf( idMovieIMDB );
        return movieParams;
    }

    private HashMap< String, String > getMovieParams( Map< String, Object > movie )
    {
        HashMap< String, String > movieParam = new HashMap<>();
        movieParam.put( "imdbid", String.valueOf( movie.get("IDMovieIMDB") ) );
        String kind = String.valueOf(movie.get("MovieKind"));
        if ( kind.contains( "series" ) )
        {
            movieParam.put( "episode", String.valueOf( movie.get("Episode") ) );
            movieParam.put( "season", String.valueOf( movie.get("Season") ) );
        }
        movieParam.put( "sublanguageid", "eng" );
        return movieParam;
    }

    public String getSubtitleFileID( HashMap< String, String > movieParam ) throws XMLRPCException {
//        HashMap< String, String > movieParam = new HashMap<>();
//        movieParam.put( "imdbid", imdb );
//        movieParam.put( "sublanguageid", "eng" );
        Log.d( "Subtitle_Activities", "Search Subtitles Parameters " + movieParam );
        Map< String, Object > subtitles = (Map<String, Object>) client.call("SearchSubtitles", token, new HashMap<?, ?>[]{movieParam});
        Log.d( "Subtitle_Activities", "subtitles " + subtitles );
        Object[] subtitleData = (Object[]) subtitles.get("data");
        Log.d( "Subtitle_Activities", "subtitles data " + subtitleData );
        if ( subtitleData.length == 0 )
            return null;
        Map< String, Object > subtitle = (Map<String, Object>) subtitleData[ 0 ];
        Log.d( "Subtitle_Activities", "first subtitles data " + subtitle );
        String idSubtitleFile = (String) subtitle.get("IDSubtitleFile");
        Log.d( "Subtitle_Activities", "first subtitles data file id " + idSubtitleFile );
        return idSubtitleFile;
    }

    public Object getSubtitleFile(String idSubtitleFile ) throws XMLRPCException {
        Log.d( "Subtitle_Activities", "downloading subtitle... " );
        Map< String, Object > subtitleFile = (Map<String, Object>) client.call("DownloadSubtitles", token, new Object[]{idSubtitleFile});
        Log.d( "Subtitle_Activities", "subtitleFile " + subtitleFile );
        Object data[] = (Object[]) subtitleFile.get("data");
        Map< String, Object > subtitleDatum = (Map<String, Object>) data[0];
        Object datum = subtitleDatum.get("data");
        Log.d( "Subtitle_Activities", "datum " + datum );
        return  datum;
    }

    public void downloadSubtitle(Video video, Context context)
    {
        Worker.executeTask( () ->
        {
            File subtitle = getSavedSubtitle( video, context );
            if ( subtitle == null )
                subtitle = getSubtitle(video, context);

            if ( subtitle == null )
            {
                Log.d( "Subtitle_Activities", "Unable to find subtitle" );
                return () -> {};
            }
            return () -> {};
        });
    }

    public void showSubtitle(Video video, int position, Context context, BiFunction< File, Integer, ConcatenatingMediaSource > build,
                                    Consumer<ConcatenatingMediaSource> update, Consumer< String > messager)
    {
        renderSubtitle( video, position, context, build, update, messager, false );
    }

    public File retrieveSubtitle(Video video, Context context, Consumer< String > messager )
    {
        File subtitle = getSavedSubtitle( video, context );
        if ( subtitle == null )
            subtitle = getSubtitle(video, context, messager);
        return subtitle;
    }

    public void renderSubtitle(Video video, int position, Context context, BiFunction< File, Integer, ConcatenatingMediaSource > build,
                                    Consumer<ConcatenatingMediaSource> update, Consumer< String > messager, boolean connectivity )
    {
        Worker.executeTask( () ->
        {
            File subtitle = getSavedSubtitle( video, context );
            if ( subtitle == null )
                subtitle = getSubtitle(video, context, messager);

            if ( subtitle == null )
            {
                Log.d( "Subtitle_Activities", "Unable to find subtitle" );
                return () -> {};
            }

//            File finalSubtitle = subtitle;
            ConcatenatingMediaSource source = build.apply(subtitle,position);

//            if ( connectivity )
//                doneWithConnectivity();

            return () -> {
//                messager.accept( "rendering movie" );
                update.accept(source);
            };
        });
    }

    public void login() throws XMLRPCException {
        if ( !login )
        {
            Map<String, Object> access = (Map<String, Object>) client.call( "LogIn", username, password, "", userAgent );
            token = String.valueOf(access.get("token"));
            login = true;
        }
    }

    public void displaySubtitle() {
        XMLRPCCallback action = generateCallback(o ->
        {
            Log.d("Subtitle_Activities", "login...");
            Log.d("Subtitle_Activities", "result " + o);
        });
        login( action );
    }

    private XMLRPCCallback generateCallback( Consumer< Object > success )
    {
        return generateCallback( success, null, null );
    }

    private XMLRPCCallback generateCallback( Consumer< Object > success, Consumer< XMLRPCException > rError,
                                                    Consumer< XMLRPCServerException > sError )
    {
        return new XMLRPCCallback() {
            @Override
            public void onResponse(long id, Object result) {
                if ( success != null )
                    success.accept( result );
            }

            @Override
            public void onError(long id, XMLRPCException error) {
                if ( rError != null )
                    rError.accept( error );
            }

            @Override
            public void onServerError(long id, XMLRPCServerException error) {
                if ( sError != null )
                    sError.accept( error );
            }
        };
    }


    /**
     * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
     * checksum of the first and last 64k (even if they overlap because the file is smaller than
     * 128k).
     */
    public static class OpenSubtitlesHasher {

        /**
         * Size of the chunks that will be hashed in bytes (64 KB)
         */
        private static final int HASH_CHUNK_SIZE = 64 * 1024;


        public static String computeHash(File file) throws IOException {
            long size = file.length();
            long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

            FileChannel fileChannel = new FileInputStream(file).getChannel();

            try {
                long head = computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, chunkSizeForFile));
                long tail = computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));

                return String.format("%016x", size + head + tail);
            } finally {
                fileChannel.close();
            }
        }


        public static String computeHash(InputStream stream, long length) throws IOException {

            int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, length);

            // buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
            byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, length)];

            DataInputStream in = new DataInputStream(stream);

            // first chunk
            in.readFully(chunkBytes, 0, chunkSizeForFile);

            long position = chunkSizeForFile;
            long tailChunkPosition = length - chunkSizeForFile;

            // seek to position of the tail chunk, or not at all if length is smaller than two chunks
            while (position < tailChunkPosition && (position += in.skip(tailChunkPosition - position)) >= 0);

            // second chunk, or the rest of the data if length is smaller than two chunks
            in.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);

            long head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
            long tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));

            return String.format("%016x", length + head + tail);
        }


        private static long computeHashForChunk(ByteBuffer buffer) {

            LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
            long hash = 0;

            while (longBuffer.hasRemaining()) {
                hash += longBuffer.get();
            }

            return hash;
        }

    }

    public interface BiFunction<T1, T2, R> {
        @NonNull
        R apply(@NonNull T1 t1, @NonNull T2 t2);
    }

    public interface Action {
        void perform();
    }

}