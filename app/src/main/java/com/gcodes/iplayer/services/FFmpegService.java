package com.gcodes.iplayer.services;

import android.content.Context;
import android.util.Log;

import com.gcodes.iplayer.music.Music;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService
{
    private static FFmpegService service;

    private final Context context;
    private final FFmpeg ffmpeg;
    private final String karaokeCmd = "-i \"%s\" -af pan=stereo|c0=c0|c1=-1*c1 -ac 1 \"%s\"";
    private final String[] karaokeCmds = { "-i", "", "-af", "pan=stereo|c0=c0|c1=-1*c1", "-ac", "1", "" };

    private State state = State.LOADING;

    private enum State{LOADING,READY,UNUSABLE}

    private KaraokeHandler karaokeHandler;

    public FFmpegService(Context context)
    {
        ffmpeg = FFmpeg.getInstance(context);
        this.context = context;
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler(){
                @Override
                public void onFailure() {
                    super.onFailure();
                    state = State.UNUSABLE;
                    Log.w("FFmpag_Service", "Binaries failed to load" );
                }

                @Override
                public void onSuccess() {
                    super.onSuccess();
                    state = State.READY;
                    Log.w("FFmpag_Service", "Binaries are ready" );
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void prepare( KaraokeHandler karaokeHandler)
    {
        this.karaokeHandler = karaokeHandler;
    }

    public void release()
    {
        karaokeHandler = null;
    }

    public static void initialize(Context context)
    {
        service = new FFmpegService( context );
    }

    public static FFmpegService getInstance(Context context)
    {
        if ( service == null )
            service = new FFmpegService(context);
        return  service;
    }

    public static FFmpegService getInstance()
    {
        return  service;
    }

    private void execute(String cmd, Music music)
    {
        String[] command = cmd.split(" " );
        if ( command.length == 0 )
            return;

        try {
            ffmpeg.execute(command, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {

                }

                @Override
                public void onProgress(String message) {
                    Log.w("FFmpag_Service", "Karaoke Creation in progress " + message );
                }

                @Override
                public void onFailure(String message) {

                }

                @Override
                public void onStart() {
                    Log.w("FFmpag_Service", "Karaoke Creation started" );
                }

                @Override
                public void onFinish() {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute(String[] command, Music music)
    {
        if ( command.length == 0 )
            return;

        try {
            ffmpeg.execute(command, new KaraokeExecutor(music));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleKaraoke(Music music)
    {
        if (karaokeHandler == null)
            return;

        if (karaokeHandler.isApplied())
            karaokeHandler.restore();
        else
        {
            File karaoke = getKaraoke(music);
            if (karaoke.exists())
                karaokeHandler.apply(karaoke, music);
            else
                karaoke(music);
        }
    }

    private File getKaraoke(Music music) {
        String karaokePath = getKaraokePath(music);
        File file = new File(karaokePath);
        return file;
    }

    public String karaoke(Music music)
    {
        String karaokePath = getKaraokePath(music);
        String musicPath = music.getData();
        Log.w( "FFmpag_Service", "music file path " + musicPath );
        String[] command = getCommand(musicPath, karaokePath);
//        String command = String.format(Locale.ENGLISH, karaokeCmd, musicPath, karaokePath);
        execute(command, music);
        Log.w( "FFmpag_Service", "executed karaoke command " + Arrays.toString(command));
        return karaokePath;
    }

    public String[] getCommand( String input, String output )
    {
        karaokeCmds[ 1 ] = input;
        karaokeCmds[ 6 ] = output;
        return karaokeCmds;
    }

    public String getKaraokePath(Music music)
    {
        String name = String.valueOf(music.getMediaId());
        File parent = new File( context.getExternalFilesDir( null ), "karaoke" );
        File file = new File( context.getExternalFilesDir( null ), "karaoke/" + name + ".mp3" );
        if ( !parent.exists() )
            parent.mkdirs();
        Log.w( "FFmpag_Service", "karaoke file path " + file );
        return file.getPath();
    }

    public static interface KaraokeHandler
    {
        void update( int percentage );
        void apply(File file, Music music);
        boolean isApplied();
        void restore();
    }

    private class KaraokeExecutor extends ExecuteBinaryResponseHandler
    {
        private final String periodRegex = "time=[\\d]{2}:[\\d]{2}:[\\d]{2}";
        private final Pattern pattern = Pattern.compile(periodRegex);
        private final Music music;
        private final PeriodFormatter periodFormatter = new PeriodFormatterBuilder().minimumPrintedDigits(2).appendHours()
                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).appendMinutes()
                .appendSeparatorIfFieldsBefore(":").minimumPrintedDigits(2).printZeroAlways().appendSeconds().toFormatter();
        private Period progress = null;

        private KaraokeExecutor(Music music) {
            super();
            this.music = music;
        }

        @Override
        public void onSuccess(String message) {
            super.onSuccess(message);
            Log.w( "FFmpag_Service", "created karaoke file at " + getKaraokePath(music) );
            Log.w( "FFmpag_Service", "Karaoke for " + music );
            Log.w( "FFmpag_Service", "Path " + getKaraokePath(music) );
            Log.w("FFmpag_Service", "Karaoke Created Successfully " + message );

            if (karaokeHandler != null)
                karaokeHandler.apply(getKaraoke(music), music);
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.w("FFmpag_Service", "Karaoke Have Started Creating" );
        }

        @Override
        public void onFinish() {
            super.onFinish();
            Log.w("FFmpag_Service", "Karaoke Have Finish Creating" );
        }

        @Override
        public void onProgress(String message) {
            super.onProgress(message);
            Log.w("FFmpag_Service", "Karaoke Creation in progress " + message );

            if (karaokeHandler != null)
            {
                int percentage = extractProgress(message);
                Log.w( "FFmpag_Service", "Karaoke Update progress" + percentage);
                karaokeHandler.update(percentage);
            }
        }

        private int extractProgress(String message)
        {
            Log.w( "FFmpag_Service", "String Processing " + message);
            Matcher matcher = pattern.matcher(message);
            if  ( matcher.find() )
            {
                String time = matcher.group();
                Log.w( "FFmpag_Service", "String Processing found" + time);
                String[] split = time.split("=");
                time = split[ 1 ];
                Log.w( "FFmpag_Service", "String Processing extracted" + time);
                progress = Period.parse(time, periodFormatter);
            }
            if ( progress == null )
                return -1;
            else
            {
                Period duration = music.getDuration();
                Log.w( "FFmpag_Service", "String Processing progress" + duration);
                double result = (double) progress.toStandardSeconds().getSeconds() / duration.toStandardSeconds().getSeconds() * 100;
                Log.w( "FFmpag_Service", "String Processing progress percentage" + result);
                return (int) result;
            }
        }

        @Override
        public void onFailure(String message) {
            super.onFailure(message);
            Log.w("FFmpag_Service", "Karaoke Failed to create reason " + message );
        }
    };
}
