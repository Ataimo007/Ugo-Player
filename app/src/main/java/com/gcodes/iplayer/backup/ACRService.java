//package com.gcodes.iplayer.services;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.media.MediaCodec;
//import android.media.MediaExtractor;
//import android.media.MediaFormat;
//import android.media.MediaMetadataRetriever;
//import android.media.MediaMuxer;
//import android.net.Uri;
//import android.os.Environment;
//import android.os.ParcelFileDescriptor;
//import android.provider.MediaStore;
//import android.util.Log;
//
//import com.acrcloud.rec.sdk.ACRCloudClient;
//import com.acrcloud.rec.sdk.ACRCloudConfig;
//import com.gcodes.iplayer.music.models.Music;
//import com.gcodes.iplayer.video.Video;
//
//import java.io.File;
//import java.io.FileDescriptor;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//
//import androidx.core.os.EnvironmentCompat;
//
//public class ACRService
//{
//    private static ACRService acrService;
//
//    private final ACRCloudConfig mConfig;
//    private final ACRCloudClient acr;
//    private final Context context;
//
//    public static void initialize( Context context )
//    {
//        acrService = new ACRService( context );
//    }
//
//    public static ACRService getInstance()
//    {
//        return acrService;
//    }
//
//    public static ACRService getInstance( Context context )
//    {
//        if ( acrService == null )
//            initialize( context );
//        return acrService;
//    }
//
//    private ACRService( Context context )
//    {
//        mConfig = new ACRCloudConfig();
//        configure( context );
//        acr = new ACRCloudClient();
//        acr.initWithConfig( mConfig );
//        this.context = context;
//    }
//
//    public String recognizeMusic(Music music ) throws IOException {
//        Uri uri = music.toUri();
//        ParcelFileDescriptor musicParcel = context.getContentResolver().openFileDescriptor(uri, "r");
//        FileInputStream musicStream = new FileInputStream( musicParcel.getFileDescriptor() );
//        byte[] stream = new byte[0];
//        musicStream.read( stream );
//        Log.d( "ACR_Service", "Stream is " + stream );
//        String info = acr.recognize(stream, stream.length);
//        return info;
//    }
//
//    public void recognizeMovieFromAudio() throws IOException {
//        String fileName = "/storage/emulated/0/Android/data/com.gcodes.iplayer/files/Test/test.mp3";
//        Log.d( "Subtitle_Activities", "Get movie " + fileName );
//        File file = new File( fileName );
//        FileInputStream stream = new FileInputStream( file );
//        byte[] raw = new byte[0];
//        Log.d( "Subtitle_Activities", "Begin Reading Bytes" );
//        stream.read( raw );
//        Log.d( "Subtitle_Activities", "Begin Recognizing movie" );
//        acr.recognize( raw, raw.length, s -> {
//            Log.d( "Subtitle_Activities", "The result " + s );
//        });
//    }
//
//    public static void getMovieFromAudio( ACRCloudClient acrCloudClient ) throws IOException
//    {
//        String fileName = "/storage/emulated/0/Android/data/com.gcodes.iplayer/files/Test/test.mp3";
//        Log.d( "Subtitle_Activities", "Get movie " + fileName );
//        File file = new File( fileName );
//        FileInputStream stream = new FileInputStream( file );
//        byte[] raw = new byte[0];
//        Log.d( "Subtitle_Activities", "Begin Reading Bytes" );
//        stream.read( raw );
//        Log.d( "Subtitle_Activities", "Begin Recognizing movie" );
//        acrCloudClient.recognize( raw, raw.length, s -> {
//            Log.d( "Subtitle_Activities", "The result " + s );
//        });
//    }
//
//    public void configure( Context context )
//    {
//        mConfig.context = context;
//        mConfig.host = "identify-eu-west-1.acrcloud.com";
////        mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
//        mConfig.accessKey = "11a944736130d325297e21591541d556";
//        mConfig.accessSecret = "D4NI0m1kMhwfhtzppMlV9ByJJDJhcIcip4UtnFmv";
//        mConfig.protocol = ACRCloudConfig.ACRCloudNetworkProtocol.PROTOCOL_HTTP; // PROTOCOL_HTTPS
//        mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE;
//        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;
//        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_BOTH;
//    }
//
////    private void listener()
////    {
////        mConfig.acrcloudListener =
////        mConfig.acrcloudResultWithAudioListener = new IACRCloudResultWithAudioListener()
////        {
////
////            @Override
////            public void onResult(ACRCloudResult acrCloudResult) {
////                String result = acrCloudResult.getResult();
////                Log.d( "Subtitle_Activities", "The result " + result );
////            }
////
////            @Override
////            public void onVolumeChanged(double v) {
////
////            }
////        };
//
//        // If you implement IACRCloudResultWithAudioListener and override "onResult(ACRCloudResult result)", you can get the Audio data.
//        //this.mConfig.acrcloudResultWithAudioListener = this;
////    }
//
////    {
////        Log.d( "Subtitle_Activities", "Initializing ACRCloud Movie Recognition" );
////        ACRCloudClient acrCloudClient = acrConfig(context);
////
////        Worker.executeTask( () -> {
////            try {
//////                getAudioBytes( video, context );
////                getMovieFromAudio( acrCloudClient );
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////
////            return () -> {};
////        });
////    }
//
//    /**
//     * @param srcPath  the path of source video file.
//     * @param dstPath  the path of destination video file.
//     * @param startMs  starting time in milliseconds for trimming. Set to
//     *                 negative if starting from beginning.
//     * @param endMs    end time for trimming in milliseconds. Set to negative if
//     *                 no trimming at the end.
//     * @param useAudio true if keep the audio track from the source.
//     * @param useVideo true if keep the video track from the source.
//     * @throws IOException
//     */
//    @SuppressLint("NewApi")
//    public static void genVideoUsingMuxer(String srcPath, String dstPath, int startMs, int endMs, boolean useAudio,
//                                          boolean useVideo) throws IOException {
//        final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024;
//        final String TAG = "AudioExtractorDecoder";
//
//        // Set up MediaExtractor to read from the source.
//        MediaExtractor extractor = new MediaExtractor();
//        extractor.setDataSource(srcPath);
//        int trackCount = extractor.getTrackCount();
//        // Set up MediaMuxer for the destination.
//        MediaMuxer muxer;
//        muxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        // Set up the tracks and retrieve the max buffer size for selected
//        // tracks.
//        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
//        int bufferSize = -1;
//        for (int i = 0; i < trackCount; i++) {
//            MediaFormat format = extractor.getTrackFormat(i);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            boolean selectCurrentTrack = false;
//            if (mime.startsWith("audio/") && useAudio) {
//                selectCurrentTrack = true;
//            } else if (mime.startsWith("video/") && useVideo) {
//                selectCurrentTrack = true;
//            }
//            if (selectCurrentTrack) {
//                extractor.selectTrack(i);
//                int dstIndex = muxer.addTrack(format);
//                indexMap.put(i, dstIndex);
//                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
//                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
//                }
//            }
//        }
//        if (bufferSize < 0) {
//            bufferSize = DEFAULT_BUFFER_SIZE;
//        }
//        // Set up the orientation and starting time for extractor.
//        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
//        retrieverSrc.setDataSource(srcPath);
//        String degreesString = retrieverSrc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
//        if (degreesString != null) {
//            int degrees = Integer.parseInt(degreesString);
//            if (degrees >= 0) {
//                muxer.setOrientationHint(degrees);
//            }
//        }
//        if (startMs > 0) {
//            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//        }
//        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
//        // for copying each sample and stop when we get to the end of the source
//        // file or exceed the end time of the trimming.
//        int offset = 0;
//        int trackIndex = -1;
//        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        muxer.start();
//        while (true) {
//            bufferInfo.offset = offset;
//            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
//            if (bufferInfo.size < 0) {
//                Log.d(TAG, "Saw input EOS.");
//                bufferInfo.size = 0;
//                break;
//            } else {
//                bufferInfo.presentationTimeUs = extractor.getSampleTime();
//                if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
//                    Log.d(TAG, "The current sample is over the trim end time.");
//                    break;
//                } else {
//                    bufferInfo.flags = extractor.getSampleFlags();
//                    trackIndex = extractor.getSampleTrackIndex();
//                    muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
//                    extractor.advance();
//                }
//            }
//        }
//        muxer.stop();
//        muxer.release();
//        return;
//    }
//
///**
//     * @param startMs  starting time in milliseconds for trimming. Set to
//     *                 negative if starting from beginning.
//     * @param endMs    end time for trimming in milliseconds. Set to negative if
//     *                 no trimming at the end.
//     * @throws IOException
//     */
//    @SuppressLint("NewApi")
//    public static void cutMusic(Uri uri, int startMs, int endMs, Context context ) throws IOException
//    {
//        boolean useAudio = true;
//        boolean useVideo = false;
//        final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024;
//        final String TAG = "AudioExtractorDecoder";
//
//        // Set up MediaExtractor to read from the source.
//        MediaExtractor extractor = new MediaExtractor();
//        FileDescriptor src = context.getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
//        extractor.setDataSource(src);
//        int trackCount = extractor.getTrackCount();
//        // Set up MediaMuxer for the destination.
//        MediaMuxer muxer;
//        File root = new File(Environment.getExternalStorageDirectory(), "Test/" );
//        root.mkdirs();
//        File file = new File(Environment.getExternalStorageDirectory(), "Test/" + "test" + ".mp3" );
//        muxer = new MediaMuxer( file.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        // Set up the tracks and retrieve the max buffer size for selected
//        // tracks.
//        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
//        int bufferSize = -1;
//        for (int i = 0; i < trackCount; i++) {
//            MediaFormat format = extractor.getTrackFormat(i);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            boolean selectCurrentTrack = false;
//            if (mime.startsWith("audio/") && useAudio) {
//                selectCurrentTrack = true;
//            } else if (mime.startsWith("video/") && useVideo) {
//                selectCurrentTrack = true;
//            }
//            if (selectCurrentTrack) {
//                extractor.selectTrack(i);
//                int dstIndex = muxer.addTrack(format);
//                indexMap.put(i, dstIndex);
//                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
//                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
//                }
//            }
//        }
//        if (bufferSize < 0) {
//            bufferSize = DEFAULT_BUFFER_SIZE;
//        }
//        // Set up the orientation and starting time for extractor.
//        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
//        retrieverSrc.setDataSource( src );
//        String degreesString = retrieverSrc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
//        if (degreesString != null) {
//            int degrees = Integer.parseInt(degreesString);
//            if (degrees >= 0) {
//                muxer.setOrientationHint(degrees);
//            }
//        }
//        if (startMs > 0) {
//            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//        }
//        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
//        // for copying each sample and stop when we get to the end of the source
//        // file or exceed the end time of the trimming.
//        int offset = 0;
//        int trackIndex = -1;
//        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        muxer.start();
//        while (true) {
//            bufferInfo.offset = offset;
//            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
//            if (bufferInfo.size < 0) {
//                Log.d(TAG, "Saw input EOS.");
//                bufferInfo.size = 0;
//                break;
//            } else {
//                bufferInfo.presentationTimeUs = extractor.getSampleTime();
//                if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
//                    Log.d(TAG, "The current sample is over the trim end time.");
//                    break;
//                } else {
//                    bufferInfo.flags = extractor.getSampleFlags();
//                    trackIndex = extractor.getSampleTrackIndex();
//                    muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
//                    extractor.advance();
//                }
//            }
//        }
//        muxer.stop();
//        muxer.release();
//        return;
//    }
//
//    /**
//     * @param video    The video file to be converted to audio
//     * @param context  The context of the activity trigering the method
//     *
//     * @throws IOException
//     */
//    @SuppressLint("NewApi")
//    public static void getAudioBytes(Video video, Context context ) throws IOException {
//        final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024;
//        final String TAG = "Subtitle_Activities";
//
//        int startMs = -1;
//        int endMs = -1;
//        boolean useAudio = true;
//        boolean useVideo = false;
//
//        Log.d( "Subtitle_Activities", "Video to audio conversion begin" );
//
//        Uri media = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf( video.getId() ) );
//        ParcelFileDescriptor mediaDescriptor = context.getContentResolver().openFileDescriptor(media, "r");
//        FileDescriptor srcFileDescriptor = mediaDescriptor.getFileDescriptor();
//
//        File parent = new File( context.getExternalFilesDir( null ), "Test" );
//        File file = new File( context.getExternalFilesDir( null ), "Test/" + "test" + ".mp3" );
//        parent.mkdirs();
//
//        Log.d( "Subtitle_Activities", "Destination file created " + file.getAbsolutePath() );
//
//        // Set up MediaExtractor to read from the source.
//        MediaExtractor extractor = new MediaExtractor();
//        extractor.setDataSource(srcFileDescriptor);
//        int trackCount = extractor.getTrackCount();
//        // Set up MediaMuxer for the destination.
//        MediaMuxer muxer;
//        muxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        // Set up the tracks and retrieve the max buffer size for selected
//        // tracks.
//        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
//        int bufferSize = -1;
//        for (int i = 0; i < trackCount; i++) {
//            MediaFormat format = extractor.getTrackFormat(i);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            boolean selectCurrentTrack = false;
//            if (mime.startsWith("audio/") && useAudio) {
//                selectCurrentTrack = true;
//            } else if (mime.startsWith("video/") && useVideo) {
//                selectCurrentTrack = true;
//            }
//            if (selectCurrentTrack) {
//                extractor.selectTrack(i);
//                int dstIndex = muxer.addTrack(format);
//                indexMap.put(i, dstIndex);
//                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
//                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
//                }
//            }
//        }
//        if (bufferSize < 0) {
//            bufferSize = DEFAULT_BUFFER_SIZE;
//        }
//        // Set up the orientation and starting time for extractor.
//        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
//        retrieverSrc.setDataSource( srcFileDescriptor );
//        String degreesString = retrieverSrc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
//        if (degreesString != null) {
//            int degrees = Integer.parseInt(degreesString);
//            if (degrees >= 0) {
//                muxer.setOrientationHint(degrees);
//            }
//        }
//        if (startMs > 0) {
//            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//        }
//        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
//        // for copying each sample and stop when we get to the end of the source
//        // file or exceed the end time of the trimming.
//        int offset = 0;
//        int trackIndex = -1;
//        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        muxer.start();
//        while (true) {
//            bufferInfo.offset = offset;
//            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
//            if (bufferInfo.size < 0) {
//                Log.d(TAG, "Saw input EOS.");
//                bufferInfo.size = 0;
//                break;
//            } else {
//                bufferInfo.presentationTimeUs = extractor.getSampleTime();
//                if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
//                    Log.d(TAG, "The current sample is over the trim end time.");
//                    break;
//                } else {
//                    bufferInfo.flags = extractor.getSampleFlags();
//                    trackIndex = extractor.getSampleTrackIndex();
//                    muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
//                    extractor.advance();
//                }
//            }
//        }
//        muxer.stop();
//        muxer.release();
//
//        Log.d( "Subtitle_Activities", "Audio to video Conversion ended" );
//
//        return;
//    }
//
//}
