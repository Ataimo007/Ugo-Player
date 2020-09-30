package com.gcodes.iplayer.services;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.gcodes.iplayer.R;
import com.gcodes.iplayer.ui.UIConstance;
import com.gcodes.iplayer.video.Video;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;

public class OpenSubtitleService
{
    private static OpenSubtitleService subtitle;
    private final static String host = "https://api.opensubtitles.org/xml-rpc";
//    private final static String host = "http://api.opensubtitles.org/xml-rpc";
    private final static String username = "Ataimo7";
    private final static String password = "alex1234";
    private final static String userAgent = "Ultimate Player v1.0";

    private final XMLRPCClient client;
    private boolean login = false;
    private String token;

    private PopupWindow notification;
    private PopupWindow selection;
    private PopupWindow suggestion;

    private PlayerView player;
    private Handler notifier;
    private Handler suggestor;
    private Handler applier;
    private Handler hider;
    private Handler hideBeginner;
    private Handler dimisser = new Handler(Looper.getMainLooper());
    private final Runnable dismiss = () -> notification.dismiss();

    private HashMap<Video,MediaSource> shown;
//    private Handler selector;

    private class Title
    {
        private final String episode;
        private final String season;
        private final String kind;
        private final String name;
        private final String IMDBMovie;
        private final String IMDBEpisode;

        public Title( JsonObject guess )
        {
            name = guess.getAsJsonPrimitive("MovieName").getAsString();
            kind = guess.getAsJsonPrimitive("MovieKind").getAsString();
            IMDBMovie = guess.getAsJsonPrimitive("IDMovieIMDB").getAsString();
            if (kind.equals("tv series"))
            {
                episode = guess.has("Episode") ? guess.getAsJsonPrimitive("Episode").getAsString() : String.valueOf(1);
                season = guess.has("Season") ? guess.getAsJsonPrimitive("Season").getAsString() : String.valueOf(1);
                IMDBEpisode = guess.has("IMDBEpisode") ? guess.getAsJsonPrimitive("IMDBEpisode").getAsString() : null;
            }
            else
                episode = season = IMDBEpisode = null;
        }

        public Title( JsonObject stringGuest, JsonObject guest )
        {
            name = stringGuest.getAsJsonPrimitive("MovieName").getAsString();
            kind = stringGuest.getAsJsonPrimitive("MovieKind").getAsString();
            IMDBMovie = stringGuest.getAsJsonPrimitive("IDMovieIMDB").getAsString();
            if (kind.equals("tv series"))
            {
                episode = guest.getAsJsonPrimitive("episode").getAsString();
                season = guest.getAsJsonPrimitive("season").getAsString();
                IMDBEpisode = guest.has("IMDBEpisode") ? guest.getAsJsonPrimitive("IMDBEpisode").getAsString() : null ;
            }
            else
                episode = season = IMDBEpisode = null;
        }

        public boolean isSeries() {
            return kind.equals("tv series");
        }

        private HashMap< String, String > getMovieParams()
        {
            HashMap< String, String > movieParam = new HashMap<>();
            movieParam.put( "imdbid", IMDBMovie );
            if ( isSeries() )
            {
                movieParam.put( "episode", episode );
                movieParam.put( "season", season );
            }
            movieParam.put( "sublanguageid", "eng" );
            return movieParam;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s%s", name, isSeries() ? String.format(Locale.ENGLISH, " Season %s Episode %s", season, episode ) : "" );
        }
    }

    private void shown(Video video, MediaSource source)
    {
        if ( shown == null )
            shown = new HashMap<>();
        shown.put(video, source);
    }

    private void hidden( Video video )
    {
        if ( shown != null )
            shown.remove(video);
    }

    private boolean isShown(Video video)
    {
        Log.w("Subtitle_Activities", String.format("applying is shown %s null : %b contains : %b", shown, shown != null, shown != null && shown.containsKey(video)) );
        return shown != null && shown.containsKey(video);
    }

    private class TitleHolder extends RecyclerView.ViewHolder
    {
        private final TextView title;

        public TitleHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_name);
        }
    }

    public OpenSubtitleService(PlayerView player)
    {
        XMLRPCClient client1;
        try
        {
            client1 = new XMLRPCClient( new URL( host ), XMLRPCClient.FLAGS_DEBUG );
            initSubUI( player );
        } catch (MalformedURLException e) {
            client1 = null;
            e.printStackTrace();
        }
        client = client1;
    }

    private void initSubUI(PlayerView player)
    {
        this.player = player;
        LayoutInflater inflater = LayoutInflater.from(player.getContext());
        View notify = inflater.inflate(R.layout.subtitle_notification, null);
        View select = inflater.inflate(R.layout.subtitle_selection, null);
        View suggest = inflater.inflate(R.layout.subtitle_sugestion, null);
        notification = new PopupWindow(notify, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);
        selection = new PopupWindow(select, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);
        suggestion = new PopupWindow(suggest, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT,
                true);

        initNotifier();
        initSuggestor();
        initShower();
        initHider();
        initHiderBeginner();
    }

    private void initShower() {
        applier = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Pair<File,Video> fileSource = (Pair<File,Video>) msg.obj;
                VideoPlayerActivity videoPlayer = getVideoPlayer();
                if ( videoPlayer != null )
                {
                    Pair<ConcatenatingMediaSource, MediaSource> source = videoPlayer.generateSource(fileSource.first, fileSource.second);
                    videoPlayer.applySource( source.first );
                    shown(fileSource.second, source.second);
                    Log.w("Subtitle_Activities", "applying is shown " + shown );
                    notifyView("Done");
                }
            }
        };
    }

    private void initHider() {
        hider = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Video video = (Video) msg.obj;
                VideoPlayerActivity videoPlayer = getVideoPlayer();
                if ( videoPlayer != null )
                {
                    Pair<ConcatenatingMediaSource, MediaSource> source = videoPlayer.generateSource(shown.get(video), video);
                    videoPlayer.applySource( source.first );
                    hidden(video);
                    notifyView("Done");
                }
            }
        };
    }

    private void initHiderBeginner() {
        hideBeginner = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Video video = (Video) msg.obj;
                VideoPlayerActivity videoPlayer = getVideoPlayer();
                if ( videoPlayer != null )
                {
                    Pair<ConcatenatingMediaSource, MediaSource> source = videoPlayer.generateSource(shown.get(video), video);
                    videoPlayer.applySource( source.first );
                    hidden(video);
                    notifyView("Ready for a new Subtitle");

                    beginSubtitling(video);
                }
            }
        };
    }

    public HashMap<Video, MediaSource> getShown() {
        return shown;
    }

    public void destroy()
    {
        notifier = null;
        suggestor = null;
//        selector = null;
    }

    private void initNotifier()
    {
        notifier = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                String message = (String) msg.obj;

                TextView messageView = notification.getContentView().findViewById(R.id.subtitle_message);
                messageView.setText( message );
//                notification.showAtLocation( player, Gravity.TOP | Gravity.END, 0, 0 );
                show(notification);

                super.handleMessage(msg);
            }
        };
    }

    private void show( PopupWindow popup )
    {
        if ( popup == notification )
            dimisser.removeCallbacks(dismiss);
        else
            hidePrevious();
        showAt( popup );
    }

    private void hidePrevious() {
        if  (selection.isShowing())
            selection.dismiss();
        if (suggestion.isShowing())
            suggestion.dismiss();
    }

    private void showAt( PopupWindow popup )
    {
        if ( popup == notification )
        {
            popup.showAtLocation( player, Gravity.TOP | Gravity.END, 0, 0 );
            initDismisser();
        }
        if ( popup == suggestion )
        {
            popup.showAtLocation( player, Gravity.CENTER, 0, 0 );
        }
        if ( popup == selection )
        {
            popup.showAtLocation( player, Gravity.CENTER, 0, 0 );
        }
    }

    private void initDismisser() {
        dimisser.postDelayed( dismiss, 3000 );
    }

    private void suggest(JsonObject suggestions, Video video)
    {
        String query = suggestions.getAsJsonObject("data").keySet().toArray(new String[]{})[0];
        JsonElement bestGuess = suggestions.getAsJsonObject("data").getAsJsonObject(query).get("BestGuess");
        if ( bestGuess.isJsonObject() && bestGuess.getAsJsonObject().getAsJsonPrimitive("Reason").getAsString().equals("Data Intersection") )
        {
            Title title = new Title( bestGuess.getAsJsonObject() );
            downloadSubtitle( title, video );
        }
        else
        {
            suggestions.add("video", video.toJson() );
            Message suggestion = suggestor.obtainMessage(0, suggestions);
            suggestion.sendToTarget();
        }
    }

    private void initSuggestor()
    {
        suggestor = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                TextView title = suggestion.getContentView().findViewById(R.id.title_suggestion);
                Button yes = suggestion.getContentView().findViewById(R.id.suggestion_yes);
                Button no = suggestion.getContentView().findViewById(R.id.suggestion_no);

                JsonObject guesses = (JsonObject) msg.obj;
                Video video = Video.fromJson( guesses.getAsJsonObject("video") );

                String query = guesses.getAsJsonObject("data").keySet().toArray(new String[]{})[0];
                JsonElement guess = guesses.getAsJsonObject("data").getAsJsonObject(query).get("GuessIt");
                JsonElement bestGuess = guesses.getAsJsonObject("data").getAsJsonObject(query).get("BestGuess");
                JsonArray stringGuesses = guesses.getAsJsonObject("data").getAsJsonObject(query).getAsJsonArray("GuessMovieFromString");

                if ( bestGuess.isJsonObject() )
                {
                    JsonObject bestGuessObj = bestGuess.getAsJsonObject();
                    if ( ( bestGuessObj.getAsJsonPrimitive("Reason").getAsString().equals("GuessMovieFromString") ||
                            bestGuessObj.getAsJsonPrimitive("Reason").getAsString().equals("GetIMDBSuggest") ) &&  stringGuesses.size() == 1 )
                    {
                        JsonObject stringGuess = stringGuesses.get( 0 ).getAsJsonObject();
                        if ( stringGuess.getAsJsonPrimitive("MovieKind").getAsString().equals("tv series") && guess.isJsonObject() )
                        {
                            JsonObject guessObj = guess.getAsJsonObject();
                            title.setText( String.format(Locale.ENGLISH, "%s Season %d Epison %d", stringGuess.getAsJsonPrimitive("MovieName").getAsString(),
                                    guessObj.getAsJsonPrimitive("season").getAsInt(), guessObj.getAsJsonPrimitive("episode").getAsInt() ) );

                            yes.setOnClickListener(v -> {
                                suggestion.dismiss();
                                Title suggestion = new Title(stringGuess, guessObj);
                                downloadSubtitle( suggestion, video );
                            });
                        }
                        else {
                            if ( stringGuess.getAsJsonPrimitive("MovieKind").getAsString().equals("movie") )
                            {
                                title.setText( stringGuess.getAsJsonPrimitive("MovieName").getAsString() );
                            }

                            yes.setOnClickListener(v -> {
                                suggestion.dismiss();
                                Title suggestion = new Title(stringGuess);
                                downloadSubtitle( suggestion, video );
                            });
                        }
                    }
                    else
                    {
                        if ( bestGuessObj.getAsJsonPrimitive("MovieKind").getAsString().equals("tv series") )
                        {
                            title.setText( String.format(Locale.ENGLISH, "%s Season %d Epison %d", bestGuessObj.getAsJsonPrimitive("MovieName").getAsString(),
                                    bestGuessObj.getAsJsonPrimitive("Season").getAsInt(), bestGuessObj.getAsJsonPrimitive("Episode").getAsInt() ) );
                        }

                        if ( bestGuessObj.getAsJsonPrimitive("MovieKind").getAsString().equals("movie") )
                        {
                            title.setText( bestGuessObj.getAsJsonPrimitive("MovieName").getAsString() );
                        }

                        yes.setOnClickListener(v -> {
                            suggestion.dismiss();
                            Title suggestion = new Title(bestGuessObj);
                            downloadSubtitle( suggestion, video );
                        });
                    }
                }

                no.setOnClickListener(v -> {
                    manuallySuggest( guesses );
                });

                show(suggestion);

                super.handleMessage(msg);
            }
        };
    }

    private void manuallySuggest(JsonObject guesses) {
        Video video = Video.fromJson( guesses.get("video").getAsJsonObject() );
        RecyclerView suggestions = selection.getContentView().findViewById(R.id.suggestion_list);
        ArrayList<Title> tittles = getTittles(guesses);
        suggestions.setLayoutManager( new LinearLayoutManager( player.getContext() ) );
        suggestions.addItemDecoration(new UIConstance.AlternateItemDecorator());
        RecyclerView.Adapter<TitleHolder> adapter = new RecyclerView.Adapter<TitleHolder>() {
            @NonNull
            @Override
            public TitleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_title, parent, false);
                return new TitleHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull TitleHolder holder, int position) {
                Title title = tittles.get(position);
                holder.title.setText(title.toString());
                holder.itemView.setOnClickListener(v -> {
                    selection.dismiss();
                    downloadSubtitle(title, video);
                });
            }

            @Override
            public int getItemCount() {
                return tittles.size();
            }
        };
        suggestions.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Button ok = selection.getContentView().findViewById(R.id.suggestion_ok);
        ok.setOnClickListener(v -> {
            selection.dismiss();
            TextInputEditText manualGuest = selection.getContentView().findViewById(R.id.manual_suggestion);
            String guess = manualGuest.getText().toString();
            downloadSubtitle( guess, video );
        });
//        selection.showAtLocation( player, Gravity.CENTER, 0, 0 );
        show(selection);
    }

    private ArrayList<Title> getTittles(JsonObject guesses )
    {
        ArrayList<Title> titles = new ArrayList<>();
        String query = guesses.getAsJsonObject("data").keySet().toArray(new String[]{})[0];
        JsonElement guess = guesses.getAsJsonObject("data").getAsJsonObject(query).get("GuessIt");
        JsonArray stringGuesses = guesses.getAsJsonObject("data").getAsJsonObject(query).getAsJsonArray("GuessMovieFromString");
        JsonObject IMDBguesses = guesses.getAsJsonObject("data").getAsJsonObject(query).getAsJsonObject("GetIMDBSuggest");
        for (JsonElement stringGuess : stringGuesses)
            titles.add(new Title(stringGuess.getAsJsonObject()));
        for (Map.Entry<String, JsonElement> suggestion : IMDBguesses.entrySet())
            titles.add(new Title(suggestion.getValue().getAsJsonObject()));
        return titles;
    }

    private void downloadSubtitle(Title title, Video video)
    {
        Runnable download = () -> {
            String name = title.kind.equals("tv series") ? String.format(Locale.ENGLISH, "%s Season %s Episode %s", title.name, title.season, title.episode) : title.name;
            notifyView("Downloading Subtitle for " + name);
            HashMap<String, String> movieParams = title.getMovieParams();
            String subtitleFileID = getSubtitleFileID(movieParams);
            if ( subtitleFileID == null )
            {
                notifyView("Unable to Download Subtitle for " + name );
                return;
            }
            String subtitle = downloadSubtitle(subtitleFileID);
            String subtitleHash = osHash(String.valueOf(video.getId()), player.getContext());
            if ( subtitleHash == null )
            {
                notifyView("Unable to Save the Subtitle of " + name );
                return;
            }
            File subtitleFile = save(subtitle, subtitleHash);
            showSubtitle(subtitleFile, video);
        };

        if  ( Looper.myLooper() == Looper.getMainLooper() )
        {
            Thread thread = new Thread(download);
            thread.start();
        }
        else
            download.run();
    }

    private void downloadSubtitle(String title, Video video)
    {
        Thread thread = new Thread(() -> {
            JsonObject suggestions = guessMovie( title  );
            String query = suggestions.getAsJsonObject("data").keySet().toArray(new String[]{})[0];
            JsonElement bestGuess = suggestions.getAsJsonObject("data").getAsJsonObject(query).get("BestGuess");
            if ( bestGuess.isJsonObject() && bestGuess.getAsJsonObject().getAsJsonPrimitive("Reason").getAsString().equals("Data Intersection") )
            {
                Title suggestedTitle = new Title( bestGuess.getAsJsonObject() );
                downloadSubtitle( suggestedTitle, video );
            }
            else
            {
                notifyView("Unable to Identify the Suggested Title");
            }
        });
        thread.start();
    }

    private void showSubtitle(File subtitleFile, Video video) {
        notifyView("Applying the Subtitle");
        VideoPlayerActivity videoPlayer = getVideoPlayer();
        if ( videoPlayer != null )
        {
            Pair<File,Video> fileSource = new Pair<>(subtitleFile, video);
            Message message = applier.obtainMessage(0, fileSource);
            message.sendToTarget();
        }
    }

    private VideoPlayerActivity getVideoPlayer()
    {
        if  ( player.getContext() instanceof VideoPlayerActivity )
            return (VideoPlayerActivity) player.getContext();
        return null;
    }

    public File save(String subtitle, String name) {
        byte[] decode = Base64.decode(subtitle, Base64.DEFAULT);
        Log.w( "Subtitle_Activities", "Base64 decode " + subtitle );
        File strFile = getFile(player.getContext(), name);
        try ( FileOutputStream subFile = new FileOutputStream(strFile);
              GZIPInputStream unzipped = new GZIPInputStream(new ByteArrayInputStream(decode)))
        {
            byte[] raw = new byte[ 1024 ];
            for ( int len; ( len = unzipped.read( raw )) > 0; )
                subFile.write( raw, 0, len );

            Log.w( "Subtitle_Activities", "Written to file " + strFile );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strFile;
    }

    public boolean delete(String name) {
        Log.w( "Subtitle_Activities", "Base64 decode " + subtitle );
        File strFile = getFile(player.getContext(), name);
        return strFile.delete();
    }

    public String downloadSubtitle(String idSubtitleFile) {
        Log.w( "Subtitle_Activities", "downloading subtitle... " );
        Object subFileResponse = null;
        try {
            subFileResponse = client.call("DownloadSubtitles", token, new Object[]{idSubtitleFile});
            JsonObject subtitleFile = null;
            if ( subFileResponse != null )
            {
                subtitleFile = parseObject( subFileResponse );
                JsonArray subtitles = subtitleFile.getAsJsonArray("data");
                if ( subtitles.size() != 0 )
                {
                    String subtitle = subtitles.get(0).getAsJsonObject().getAsJsonPrimitive("data").getAsString();
                    return subtitle;
                }
            }
        } catch (XMLRPCException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public String getSubtitleFileID( HashMap< String, String > movieParam ) {
        Log.w( "Subtitle_Activities", "Search Subtitles Parameters " + movieParam );
        JsonObject subtitles = null;
        Object subResponse = null;
        try {
            subResponse = client.call("SearchSubtitles", token, new HashMap<?, ?>[]{movieParam});
            if ( subResponse != null )
            {
                Log.w( "Subtitle_Activities", "Subtitle Details " + subResponse );
                subtitles = parseObject(subResponse);
                Log.w( "Subtitle_Activities", "Subtitle Details " + subtitles );
                JsonArray subs = subtitles.getAsJsonArray("data");
                if ( subs.size() != 0 )
                {
                    String idSubtitleFile = subs.get(0).getAsJsonObject().getAsJsonPrimitive("IDSubtitleFile").getAsString();
                    return idSubtitleFile;
                }
            }
        } catch (XMLRPCException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

//    private HashMap< String, String > getMovieParams( Map< String, Object > movie )
//    {
//        HashMap< String, String > movieParam = new HashMap<>();
//        movieParam.put( "imdbid", String.valueOf( movie.get("IDMovieIMDB") ) );
//        String kind = String.valueOf(movie.get("MovieKind"));
//        if ( kind.contains( "series" ) )
//        {
//            movieParam.put( "episode", String.valueOf( movie.get("Episode") ) );
//            movieParam.put( "season", String.valueOf( movie.get("Season") ) );
//        }
//        movieParam.put( "sublanguageid", "eng" );
//        return movieParam;
//    }

    private void notifyView(String message)
    {
        Message notification = notifier.obtainMessage(0, message);
        notification.sendToTarget();
    }

    public void login( XMLRPCCallback listener )
    {
        client.callAsync( listener, "LogIn", username, password, "", userAgent );
    }

    public File getSavedSubtitle(Video video)
    {
        String name = osHash(String.valueOf(video.getId()), player.getContext());
        File file = new File( player.getContext().getExternalFilesDir( null ), "subtitle/" + name + ".str" );
        if ( file.exists() )
            return file;
        return null;
    }

    private void runOnBackground()
    {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    public void subtitle(Video video)
    {
        boolean shown = isShown(video);
        Log.w("Subtitle_Activities", "This is shown " + shown );
        if ( !shown)
            beginSubtitling( video );
        else
            subtitleOptions();
    }

    public void hideSubtitle( Video video )
    {
        VideoPlayerActivity videoPlayer = getVideoPlayer();
        if ( videoPlayer != null )
        {
            Message message = hider.obtainMessage(0, video);
            message.sendToTarget();
        }
    }

    public void changeSubtitle(Video video)
    {
        boolean removed = removeSubtitle(video);
        if ( removed )
        {
            Message message = hideBeginner.obtainMessage(0, video);
            message.sendToTarget();
        }
    }

    private boolean removeSubtitle(Video video) {
        String subtitleHash = osHash(String.valueOf(video.getId()), player.getContext());
        if ( subtitleHash == null )
            return false;
        boolean deleted = delete(subtitleHash);
        if ( deleted )
            notifyView("Deleted Previous Subtitle");
        else
            notifyView("Unable to Delete Previous Subtitle");
        return deleted;
    }

    private void subtitleOptions() {
        VideoPlayerActivity videoPlayer = getVideoPlayer();
        if ( videoPlayer == null )
            return;
        if ( videoPlayer.isSubtitleOptionsShown() )
            videoPlayer.hideSubtitleOptions();
        else
            videoPlayer.showSubtitleOptions();
    }

    public void beginSubtitling(Video video)
    {
        Thread thread = new Thread(() -> {
            boolean exist = showIfExist(video);
            if ( exist )
                return;

            notifyView("Suggesting a Title");
            boolean login = login();
            if ( !login )
            {
                notifyView("Please ensure you are connected to the internet");
                return;
            }

            Log.w( "Subtitle_Activities", "Suggesting Movie" );
            JsonObject guesses = guessMovie(video.getName());
            if ( guesses == null )
            {
                notifyView("Unable to Suggest Movie");
                return;
            }

            suggest( guesses, video );
        });
        thread.start();
    }

    private boolean showIfExist(Video video) {
        File savedSubtitle = getSavedSubtitle(video);
        if ( savedSubtitle != null )
        {
            showSubtitle( savedSubtitle, video );
            return true;
        }
        return false;
    }

//    public File getSubtitle(Video video, Context context, PlayerView player)
//    {
//        try {
//            login();
//            String idSubtitleFile = getIdSubtitleFile(video, context);
//            Log.w( "Subtitle_Activities", "Obtained Id SubtitleFile " + idSubtitleFile );
//
//            Object datum = getSubtitleFile( idSubtitleFile );
//            File subtitle = save(datum, context, osHash(String.valueOf(video.getId()), context));
//            return subtitle;
//        } catch (XMLRPCException e) {
//            e.printStackTrace();
//        } catch ( Exception e )
//        {
//            e.printStackTrace();
//            Log.w( "Subtitle_Activities", "Error in parsing" );
//        }
//        return null;
//    }

//    public File getSubtitle(Video video, Context context, Consumer< String > messager )
//    {
//        try {
//            login();
//            String idSubtitleFile = getIdSubtitleFile(video, context);
//            Log.w( "Subtitle_Activities", "Obtained Id SubtitleFile " + idSubtitleFile );
////            messager.accept( "found movie" );
//            Object datum = getSubtitleFile( idSubtitleFile );
////            messager.accept( "downloaded movie" );
//            File subtitle = save(datum, context, osHash(String.valueOf(video.getId()), context));
////            messager.accept( "saving subtitle" );
//            return subtitle;
//        } catch (XMLRPCException e) {
//            e.printStackTrace();
//        } catch ( Exception e )
//        {
//            e.printStackTrace();
//            Log.w( "Subtitle_Activities", "Error in parsing" );
//        }
//        return null;
//    }

//    private String getIdSubtitleFile( Video video, Context context ) throws IOException, XMLRPCException {
//        HashMap<String, String> oshashParams = addOSHash(String.valueOf(video.getId()), context);
//        String idSubtitleFile = getSubtitleFileID( oshashParams );;
//        if ( idSubtitleFile == null )
//        {
//            Log.w( "Subtitle_Activities", "Subtitle Search by OS Hash failed" );
////            HashMap<String, String> movieParams = guessMovie( video.getName() );
//            guessMovie( video.getName() );
////            idSubtitleFile = getSubtitleFileID( movieParams );;
//        }
//        return idSubtitleFile;
//    }

//    private String getSuggestions( Video video, Context context ) throws IOException, XMLRPCException {
//        HashMap<String, String> oshashParams = addOSHash(String.valueOf(video.getId()), context);
//        String idSubtitleFile = getSubtitleFileID( oshashParams );;
//        if ( idSubtitleFile == null )
//        {
//            Log.w( "Subtitle_Activities", "Subtitle Search by OS Hash failed" );
//            guessMovie( video.getName() );
//        }
//        return idSubtitleFile;
//    }

    private String osHash(String mediaId, Context context){
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( mediaId ) );
        ParcelFileDescriptor mediaDescriptor = null;
        try {
            mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
            FileDescriptor fileDescriptor = mediaDescriptor.getFileDescriptor();
            FileInputStream stream = new FileInputStream( fileDescriptor );
            String movieHash = OpenSubtitlesHasher.computeHash(stream, stream.getChannel().size());
            return movieHash;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getMovieName(String mediaId, Context context) {
        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( mediaId ) );
        ParcelFileDescriptor mediaDescriptor = null;
        try {
            mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
            FileDescriptor fileDescriptor = mediaDescriptor.getFileDescriptor();
            FileInputStream stream = new FileInputStream( fileDescriptor );
            String movieHash = OpenSubtitlesHasher.computeHash(stream, stream.getChannel().size());
            return movieHash;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
        Log.w( "Subtitle_Activities", "add os hash to params " + movieParams );
        return movieParams;
    }

//    public File save2(Object datum, Context context, String name) throws IOException, DataFormatException {
//        Inflater decompresser = new Inflater();
//        String data = String.valueOf( datum );
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            decompresser.setInput( data.getBytes(StandardCharsets.UTF_8) );
//        }
//        else
//        {
//            decompresser.setInput( data.getBytes(Charset.forName("UTF-8")) );
//        }
//        byte[] strRaw = new byte[ 0 ];
//        decompresser.inflate( strRaw );
//        Log.w( "Subtitle_Activities", "inflated " + strRaw );
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Log.w( "Subtitle_Activities", "inflated " + new String( strRaw, StandardCharsets.UTF_8) );
//        }
//        else
//        {
//            Log.w( "Subtitle_Activities", "inflated " + new String( strRaw, Charset.forName("UTF-8")) );
//        }
//        File strFile = getFile(context, name);
//        FileOutputStream str = new FileOutputStream( strFile );
//        str.write( strRaw );
//        Log.w( "Subtitle_Activities", "Written Subtitle to file" );
//        return strFile;
//    }

    public File getFile(Context context, String name)
    {
        File parent = new File( context.getExternalFilesDir( null ), "subtitle" );
        File file = new File( context.getExternalFilesDir( null ), "subtitle/" + name + ".str" );
        parent.mkdirs();
        Log.w( "Subtitle_Activities", "create subtitle file " + file );
        return file;
    }

    public JsonObject guessMovie( String name ) {
        JsonObject jsonObject = null;
        Object guessMovies = null;
        try {
            guessMovies = client.call( "GuessMovieFromString", token, new String[]{ name } );
            if ( guessMovies != null )
            {
                jsonObject = parseObject(guessMovies);
                Log.w( "Subtitle_Activities", "movies " + jsonObject );
            }
            return jsonObject;
        } catch (XMLRPCException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JsonObject parseObject( Object object )
    {
//        Log.w( "Subtitle_Activities", "parsing Object " + object );
        JsonObject jObject = new JsonObject();
        if ( object instanceof Map )
        {
            Map<String, Object> obj = (Map<String, Object>) object;
            for ( Map.Entry<String, Object> entry : obj.entrySet() )
            {
//                Log.w( "Subtitle_Activities", "parsing entry " + entry );

                Object value = entry.getValue();
                if ( value instanceof Map )
                {
                    JsonObject val = parseObject(value);
//                    Log.w( "Subtitle_Activities", "entry value " + val );
                    jObject.add( entry.getKey(), val);
                }
                else if ( value instanceof Object[] )
                {
                    Object[] values = (Object[]) value;
//                    Log.w( "Subtitle_Activities", "object value " + Arrays.toString(values) + " array length " + values.length );
                    JsonArray jArray = new JsonArray();
                    for ( Object val : values )
                    {
                        if ( val instanceof Map )
                            jArray.add( parseObject( val ) );
                        else
                            jObject.add( entry.getKey(), new JsonPrimitive( val.toString() ));
                    }
                    jObject.add( entry.getKey(), jArray );
                }
                else
                {
//                    Log.w( "Subtitle_Activities", "entry value " + entry.getValue() );
                    jObject.add( entry.getKey(), new JsonPrimitive( entry.getValue().toString() ));
                }
            }
        }
        return jObject;
    }

//    public void downloadSubtitle(Video video, Context context)
//    {
//        Worker.executeTask( () ->
//        {
//            File subtitle = getSavedSubtitle( video, context );
//            if ( subtitle == null )
//                subtitle = getSubtitle(video, context);
//
//            if ( subtitle == null )
//            {
//                Log.w( "Subtitle_Activities", "Unable to find subtitle" );
//                return () -> {};
//            }
//            return () -> {};
//        });
//    }

//    public void showSubtitle(Video video, int position, Context context, BiFunction< File, Integer, ConcatenatingMediaSource > build,
//                                    Consumer<ConcatenatingMediaSource> update, Consumer< String > messager)
//    {
//        renderSubtitle( video, position, context, build, update, messager, false );
//    }

    public boolean login() {
        if ( !login )
        {
            Map<String, Object> access = null;
            try {
                access = (Map<String, Object>) client.call( "LogIn", username, password, "", userAgent );
                token = String.valueOf(access.get("token"));
                login = true;
            } catch (XMLRPCException e) {
                e.printStackTrace();
//                Throwable[] suppressed = new Throwable[0];
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//                    suppressed = e.getSuppressed();
//                }
//                for (Throwable s:suppressed)
//                {
//                    s.printStackTrace();
//                }
                login = false;
            }
        }
        return login;
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
}