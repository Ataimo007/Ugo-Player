package com.gcodes.iplayer.services;

import android.content.Context;
import android.util.Log;

import com.gcodes.iplayer.music.Music;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.Locale;

public class FFmpegService
{
    private final Context context;
    private final FFmpeg ffmpeg;
    private final String karaokeCmd = "-i %s -af pan\"stereo|c0=c0|c1=-1*c1\" -ac 1 %s";
    private Music music;

    private State state = State.LOADING;

    private enum State{LOADING,READY,UNUSABLE}

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
                }

                @Override
                public void onSuccess() {
                    super.onSuccess();
                    state = State.READY;
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private void execute(String cmd)
    {
        String[] command = cmd.split(" " );
        if ( command.length == 0 )
            return;

        try {
            ffmpeg.execute(command, getExecuteHandler());
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private ExecuteBinaryResponseHandler getExecuteHandler()
    {
        return new ExecuteBinaryResponseHandler(){
            @Override
            public void onSuccess(String message) {
                super.onSuccess(message);
                Log.w( "FFmpag_Service", "Karaoke for " + music );
                Log.w( "FFmpag_Service", "Path " + getKaraokePath(music) );
            }

            @Override
            public void onProgress(String message) {
                super.onProgress(message);
            }

            @Override
            public void onFailure(String message) {
                super.onFailure(message);
            }
        };
    }

    private void karaoke(Music music)
    {
        this.music = music;
        String command = String.format(Locale.ENGLISH, karaokeCmd, music.toUri().getPath(), getKaraokePath(music));
        execute(command);
    }

    public String getKaraokePath(Music music)
    {
        String name = String.valueOf(music.getMediaId());
        File parent = new File( context.getExternalFilesDir( null ), "karaoke" );
        File file = new File( context.getExternalFilesDir( null ), "karaoke/" + name + ".mp3" );
        parent.mkdirs();
        Log.w( "Subtitle_Activities", "create subtitle file " + file );
        return file.getPath();
    }
}
