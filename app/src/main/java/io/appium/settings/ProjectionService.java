/*
  Copyright 2012-present Appium Committers
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.appium.settings;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import io.appium.settings.helpers.RecordingHelpers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class ProjectionService extends Service {
    private static final String TAG = ProjectionService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    private final static int SAMPLE_RATE = 44100;
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private int mBufferSizeInBytes;
    private AudioRecord mRecorder;
    private boolean mIsRecording;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, RecordingHelpers.getNotification(getApplicationContext()),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecord();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public ProjectionService getService() {
            return ProjectionService.this;
        }
    }

    public void startRecord(MediaProjection mediaProjection, File dstFile, List<String> dstPackageNames) {
        stopRecord();
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        mRecorder = createAudioRecorder(mediaProjection, dstPackageNames);
        if (null == mRecorder) {
            return;
        }
        mRecorder.startRecording();
        mIsRecording = true;
        startBufferedWrite(dstFile);
    }

    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] buffer = new byte[mBufferSizeInBytes];
                try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                    while (mIsRecording) {
                        int readSize = mRecorder.read(buffer, 0, buffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(buffer[i]);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                }
            }
        }).start();
    }

    public void stopRecord() {
        mIsRecording = false;
        if (null != mRecorder) {
            mRecorder.stop();
            mRecorder = null;
        }
    }

    private AudioRecord createAudioRecorder(MediaProjection mediaProjection, List<String> dstPackageNames) {
        List<Integer> uids = RecordingHelpers.getAppUids(getApplicationContext(), dstPackageNames);
        if (uids.isEmpty()) {
            Log.e(TAG, String.format("Cannot figure out UIDs for '%s' packages", dstPackageNames));
        }
        AudioPlaybackCaptureConfiguration.Builder builder = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);
        for (Integer uid : uids) {
            builder.addMatchingUid(uid);
            builder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN);
            builder.addMatchingUsage(AudioAttributes.USAGE_GAME);
            builder.addMatchingUsage(AudioAttributes.USAGE_MEDIA);
        }
        return new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(builder.build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build())
                .setBufferSizeInBytes(mBufferSizeInBytes)
                .build();
    }
}