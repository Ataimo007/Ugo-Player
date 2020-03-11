package com.gcodes.iplayer.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Consumer;
import at.huber.youtubeExtractor.Format;
import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class YouTubeService
{
//    private static final String apiKey = "AIzaSyDpZNHV4XP-JMyKvPhz-vZbw_qEOCQjodw";
    private static final String apiKey = "AIzaSyBFboHVZz-Q-WIFSKF6Clb8qZn3BsxsFoM";
    public static final String YOUTUBE_TAG = "YouTube_API";

    private static final String BASE_URL = "https://www.youtube.com";
    private static final String PATH = "/watch?v=";

    private YouTube youtube;

    private static YouTubeService youtubeService;

    private YouTubeService()
    {
        initYoutube();
    }

    public static YouTubeService getIntance()
    {
        if ( youtubeService == null )
            youtubeService = new YouTubeService();
        return youtubeService;
    }

    public List<Video> getVideos(String queryTerm ) throws IOException {

//        YouTube.Search.List search = youtube.search().list("id,snippet");
        YouTube.Search.List search = youtube.search().list("id");
//        search.setKey(apiKey);
        search.setQ(queryTerm);
        search.setType("video");

        // As a best practice, only retrieve the fields that the
        // application uses.
        search.setFields("items(id/videoId)");
//        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

        // Call the API and print results.
        SearchListResponse searchResponse = search.execute();
        List<SearchResult> searchResultList = searchResponse.getItems();
        List<String> videoIds = new ArrayList<>();

        if (searchResultList != null) {

            // Merge video IDs
            for (SearchResult searchResult : searchResultList) {
                videoIds.add(searchResult.getId().getVideoId());
            }
            Joiner stringJoiner = Joiner.on(',');
            String videoId = stringJoiner.join(videoIds);

            // Call the YouTube Data API's youtube.videos.list method to
            // retrieve the resources that represent the specified videos.
            YouTube.Videos.List listVideosRequest = youtube.videos()
                    .list("id, snippet, contentDetails, statistics" ).setId(videoId);
            listVideosRequest.setFields("items(id,snippet/publishedAt,snippet/title,snippet/thumbnails," +
                    "contentDetails/duration,statistics/viewCount)" );
            VideoListResponse listResponse = listVideosRequest.execute();
            List<Video> videoList = listResponse.getItems();
//            if (videoList != null) {
//                prettyPrint(videoList.iterator(), queryTerm);
//            }
            return videoList;
        }
        return null;
    }

    private void initYoutube()
    {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        YouTubeRequestInitializer initializer = new YouTubeRequestInitializer( apiKey, null );
        youtube = new YouTube.Builder(transport, jsonFactory, request -> {} )
                .setApplicationName("Ultimate Player").setYouTubeRequestInitializer( initializer ).build();
    }

    public List<Channel> getChannels( String channel, String query ) throws IOException {
        ChannelListResponse result = youtube.channels().list(query)
                .setForUsername(channel).execute();
        List<Channel> channels = result.getItems();
        return channels;
    }

    public List<String> handleChannelInfo(List<Channel> channels )
    {
        List<String> channelInfo = new ArrayList<>();
        if (channels != null)
        {
            Channel channel = channels.get(0);
            channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                    "Its title is '" + channel.getSnippet().getTitle() + ", " +
                    "and it has " + channel.getStatistics().getViewCount() + " views.");
        }
        return channelInfo;
    }

    public void viewResult( List<String> output )
    {
        Log.d( YOUTUBE_TAG, "Executed Request" );
        if (output == null || output.size() == 0) {
            Log.d( YOUTUBE_TAG, "No results returned." );
        } else {
            output.add(0, "Data retrieved using the YouTube Data API:");
            Log.d( YOUTUBE_TAG, "Result " + TextUtils.join("\n", output) );
        }
    }

    @SuppressLint("StaticFieldLeak")
    public static void consumeYoutubeVideo(String id, Consumer<YtFile> consume, Context context) {
        String youtubeLink = BASE_URL + PATH + id;
        YouTubeExtractor youTubeExtractor = new YouTubeExtractor(context )
        {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                YtFile bestQuality = null;
                int minHeight = -1;
                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    Log.d( YOUTUBE_TAG, "Something went wrong will getting youtube videos" );
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++)
                {
                    itag = ytFiles.keyAt(i);
                    YtFile ytFile = ytFiles.get(itag);
                    Format format = ytFile.getFormat();
                    int height = format.getHeight();
                    int audioBitrate = format.getAudioBitrate();
                    if  ( ( height != -1 && audioBitrate != -1 ) && ( minHeight == -1 || height < minHeight ) )
                    {
                        minHeight = height;
                        bestQuality = ytFile;
                    }
                }

                analyseLinks( vMeta, bestQuality );
                consume.accept( bestQuality );
            }
        };
        youTubeExtractor.extract(youtubeLink, true, false);
    }


    public static void analyseLinks( VideoMeta vMeta, YtFile ytfile) {
        int tag = ytfile.getFormat().getItag();
        String filename = vMeta.getTitle() + "." + ytfile.getFormat().getExt();
        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
        String info = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                "Video " + ytfile.getFormat().getVideoCodec() + ytfile.getFormat().getAudioCodec() +
                ytfile.getFormat().getAudioBitrate() + ytfile.getFormat().getHeight() + "p";
        Log.d( YOUTUBE_TAG, String.format( "%d %s %s %s", tag, ytfile.getUrl(), filename, info ) );
//        downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
    }

//    @SuppressLint("StaticFieldLeak")
//    public static void consumeYoutubeVideo(String id, Context context) {
//        String youtubeLink = BASE_URL + PATH + id;
//        YouTubeExtractor youTubeExtractor = new YouTubeExtractor(context ) {
//
//            @Override
//            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
//                if (ytFiles == null) {
//                    // Something went wrong we got no urls. Always check this.
//                    Log.d( YOUTUBE_TAG, "Something went wrong will getting youtube videos" );
//                    return;
//                }
//                // Iterate over itags
//                for (int i = 0, itag; i < ytFiles.size(); i++)
//                {
//                    itag = ytFiles.keyAt(i);
//                    // ytFile represents one file with its url and meta data
//                    YtFile ytFile = ytFiles.get(itag);
//
//                    analyseLinks( vMeta, ytFile);
//                    // Just add videos in a decent format => height -1 = audio
////                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
////                        analyseLinks( vMeta, ytFile);
////                    }
//                }
//            }
//        };
//        youTubeExtractor.extract(youtubeLink, true, false);
//    }

//    public static void analyseLinks(VideoMeta vMeta, YtFile ytfile) {
//        String filename = vMeta.getTitle() + "." + ytfile.getFormat().getExt();
//        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
//        String info = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
//                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
//                ytfile.getFormat().getHeight() + "p";
//        Log.d( YOUTUBE_TAG, String.format( "%s %s %s", ytfile.getUrl(), filename, info ) );
////        downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
//    }


}
