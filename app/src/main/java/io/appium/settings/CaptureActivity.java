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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.appium.settings.helpers.RecordingHelpers.makeFile;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class CaptureActivity extends Activity {
    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final int REQUEST_MEDIA_PROJECTION = 1002;

    private MediaProjectionManager mMediaProjectionManager;
    private ProjectionService mProjectionService;
    private boolean mBound = false;
    private List<String> mPackages = Collections.emptyList();
    private String mFileName;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ProjectionService.LocalBinder binder = (ProjectionService.LocalBinder) service;
            mProjectionService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void loadParameters() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Log.e(TAG, "No parameters have been specified");
            return;
        }

        String dstPackages = bundle.getString("packages");
        if (dstPackages != null) {
            List<String> result = new ArrayList<>();
            for (String pkg : dstPackages.split(",")) {
                result.add(pkg.trim());
            }
            this.mPackages = result;
        }

        String fileName = bundle.getString("filename");
        if (fileName != null) {
            this.mFileName = fileName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadParameters();

        Intent intent = new Intent(this, ProjectionService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        requestMediaProjection();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && mBound) {
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            mProjectionService.startRecord(mediaProjection, makeFile(mFileName), mPackages);
            moveTaskToBack(true);
        }
    }

    private void requestMediaProjection() {
        if (mFileName == null) {
            Log.e(TAG, "The destination file name must be set in order to start recording");
            return;
        }
        Object projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (projectionManager == null) {
            Log.e(TAG, "Projection manager instance could not be retrieved");
            return;
        }
        Intent intent = ((MediaProjectionManager) projectionManager).createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
        mMediaProjectionManager = (MediaProjectionManager) projectionManager;
    }
}
