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

package io.appium.settings.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import io.appium.settings.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecordingHelpers {
    public static Notification getNotification(Context context) {
        String channelId = "io.appium.settings";
        String channelName = "Audio Recording Channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Objects.requireNonNull(manager).createNotificationChannel(notificationChannel);
        }
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setTicker("Nature")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Appium Settings")
                .setContentText("Appium Settings Audio Recorder")
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
    }

    public static List<Integer> getAppUids(Context context, List<String> dstPackageNames) {
        List<Integer> uids = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (dstPackageNames.isEmpty() || dstPackageNames.contains(packageInfo.packageName)) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(packageInfo.packageName, 0);
                    uids.add(ai.uid);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return uids;
    }

    public static File makeFile(final String name) {
        return new File(Environment.getExternalStorageDirectory(), name);
    }
}