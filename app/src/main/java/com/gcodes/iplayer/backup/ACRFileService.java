//package com.gcodes.iplayer.services;
//
//import android.content.Context;
//import android.media.MediaCodec;
//import android.media.MediaExtractor;
//import android.media.MediaFormat;
//import android.media.MediaMuxer;
//import android.net.Uri;
//import android.os.Environment;
//import android.os.ParcelFileDescriptor;
//import android.util.Log;
//
//import com.acrcloud.rec.record.ACRCloudRecorder;
//import com.acrcloud.rec.sdk.ACRCloudClient;
//import com.acrcloud.rec.sdk.ACRCloudConfig;
//import com.acrcloud.rec.sdk.utils.ACRCloudUtils;
//import com.gcodes.iplayer.music.models.Music;
//import com.gcodes.iplayer.services.ACRUtils.ACRCloudRecognizer;
//
//import java.io.File;
//import java.io.FileDescriptor;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ACRFileService
//{
//    private static ACRFileService acrService;
//    private final ACRCloudRecognizer acrExtrService;
//    private final ACRCloudConfig mConfig;
//    private final Context context;
//    private final ACRCloudClient acr;
//
//    public static ACRFileService getInstance( Context context )
//    {
//        if ( acrService == null )
//            acrService = new ACRFileService( context );
//        return acrService;
//    }
//
//    public static ACRFileService getInstance()
//    {
//        return acrService;
//    }
//
//    public static void initialize( Context context )
//    {
//        acrService = new ACRFileService( context );
//    }
//
//    public ACRFileService( Context context )
//    {
//        Map<String, Object> config = new HashMap<String, Object>();
//        // Replace "xxxxxxxx" below with your project's host, access_key and access_secret.
//        config.put("access_key", "11a944736130d325297e21591541d556");
//        config.put("access_secret", "D4NI0m1kMhwfhtzppMlV9ByJJDJhcIcip4UtnFmv");
//        config.put("host", "identify-eu-west-1.acrcloud.com");
//        config.put("debug", false);
//        config.put("timeout", 5);
//
//        acrExtrService = new ACRCloudRecognizer(config);
//
//        mConfig = new ACRCloudConfig();
//        configure( context );
//        acr = new ACRCloudClient();
//        acr.initWithConfig( mConfig );
//        this.context = context;
////        String result = re.recognizeByFile(path + "/test.mp3", 10);
////        System.out.println(result);
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
//    public String recognizeMusic( Music music ) throws IOException
//    {
//        return recognizeMusic( music.toUri() );
////        return test();
//    }
//
//    private String test() throws IOException {
//        try {
////            ACRService.cutMusic( Uri.parse( "content://media/external/audio/media/12377" ), 10000, 22000, context );
//            String src = Environment.getExternalStorageDirectory() + "Test/" + "test1" + ".mp3";
//            String dest = Environment.getExternalStorageDirectory() + "Test/" + "test" + ".mp3";
//            ACRService.genVideoUsingMuxer( src, dest, -1, -1, true, false );
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        File file = new File(Environment.getExternalStorageDirectory(), "Test/" + "test" + ".mp3" );
//        FileInputStream stream = new FileInputStream( file );
//        byte raw[] = new byte[ stream.available() ];
//        stream.read( raw );
//
//        MediaExtractor mex = new MediaExtractor();
//        mex.setDataSource( file.getAbsolutePath() );
//        MediaFormat mf = mex.getTrackFormat(0);
//        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        int nChannels = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//
//        String result = acr.recognize( raw, raw.length, sampleRate, nChannels );
//        return  result;
//    }
//
//    public String recognizeMusic( Uri uri ) throws IOException {
//        Log.d( "ACR_Service", "uri is " + uri );
//
//        ParcelFileDescriptor musicParcel = context.getContentResolver().openFileDescriptor(uri, "r");
//        FileDescriptor musicDescriptor = musicParcel.getFileDescriptor();
////        FileInputStream musicStream = new FileInputStream(musicDescriptor);
//
////        ParcelFileDescriptor musicParcel = context.getContentResolver().openFileDescriptor(uri, "r");
//        MediaExtractor mex = new MediaExtractor();
//        mex.setDataSource( musicDescriptor );
//        MediaFormat mf = mex.getTrackFormat(0);
////        mf.getInteger( MediaFormat.KEY_BIT_RATE );
//        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        int nChannels = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//
//        Log.d( "ACR_Service", String.format("music format sample rate : %d, n Channels: %d", sampleRate, nChannels ) );
//
//        InputStream musicStream = context.getContentResolver().openInputStream( uri );
//
////        String result = acr.recognizeByFingerprint(fileBuffer, fileBufferLen);
//
//        byte[] stream = new byte[ musicStream.available() ];
//        musicStream.read( stream );
//        Log.d( "ACR_Service", "Stream is " + Arrays.toString(stream));
//
////        String result = acr.recognize( stream, stream.length, sampleRate, nChannels );
////        String result = acr.recognize( stream, stream.length, sampleRate, nChannels );
//        String result = acr.recognizeByFingerprint( stream, stream.length );
//        Log.d( "ACR_Service", "result is " + result );
//
//        return result;
//    }
//
//    public String recognizeMusic2( Uri uri ) throws IOException {
//        Log.d( "ACR_Service", "uri is " + uri );
//
////        ParcelFileDescriptor musicParcel = context.getContentResolver().openFileDescriptor(uri, "r");
////        FileInputStream musicStream = new FileInputStream( musicParcel.getFileDescriptor() );
//
////        ParcelFileDescriptor musicParcel = context.getContentResolver().openFileDescriptor(uri, "r");
//        InputStream musicStream = context.getContentResolver().openInputStream( uri );
//
//        byte[] stream = new byte[ musicStream.available() ];
//        musicStream.read( stream );
//
//        Log.d( "ACR_Service", "Stream is " + Arrays.toString(stream));
//        String info = recognizeByFileBuffer2( stream, stream.length );
//        return info;
//    }
//
//    public String recognizeByFileBuffer(byte[] fileBuffer, int fileBufferLen)
//    {
//        String result = acrExtrService.recognizeByFileBuffer(fileBuffer, fileBufferLen, 10);
//        return result;
//    }
//
//    public String recognizeByFileBuffer2(byte[] fileBuffer, int fileBufferLen)
//    {
////        byte[] fps = acrExtrService.getFingerPrintByFileBuffer(fileBuffer, fileBufferLen, 10);
////        Log.d( "ACR_Service", "finger print " + fps );
////        Log.d( "ACR_Service", "finger print is " + Arrays.toString(fps));
////        String result = acr.recognizeByFingerprint(fileBuffer, fileBufferLen);
//
//        String result = acr.recognize( fileBuffer, fileBufferLen );
//        Log.d( "ACR_Service", "result is " + result );
////        String result = acr.recognizeByFingerprint(fileBuffer, fileBufferLen);
//        return result;
//    }
//
//
//}
