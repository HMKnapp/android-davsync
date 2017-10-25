package net.zekjur.davsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

public class NewMediaReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("davsync", "received pic or video intent");

		boolean isNewPic = intent.getAction().endsWith("NEW_PICTURE");
		boolean isNewVid = intent.getAction().endsWith("NEW_VIDEO");

		if (!isNewPic && !isNewVid) return;

		SharedPreferences preferences = context.getSharedPreferences("net.zekjur.davsync_preferences",
				Context.MODE_PRIVATE);

		if (isNewPic && !preferences.getBoolean("auto_sync_camera_pictures", true)) {
			Log.d("davsync", "automatic camera picture sync is disabled, ignoring");
			return;
		}

		if (isNewVid && !preferences.getBoolean("auto_sync_camera_videos", true)) {
			Log.d("davsync", "automatic camera video sync is disabled, ignoring");
			return;
		}

		Log.d("davsync", "New picture was taken");
		Uri uri = intent.getData();
		Log.d("davsync", "picture uri = " + uri);

		DavSyncOpenHelper helper = new DavSyncOpenHelper(context);
		// Always queue the image
		helper.queueUri(uri);

        checkAndCallUploadService(context, preferences);
	}

    private void checkAndCallUploadService(Context context, SharedPreferences preferences) {
        ConnectivityManager cs = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cs.getActiveNetworkInfo();
        // If we have WIFI connectivity, upload immediately
        boolean isWifi = info != null && info.isConnected()
                && (ConnectivityManager.TYPE_WIFI == info.getType());
        boolean syncOnWifiOnly = preferences.getBoolean("auto_sync_on_wifi_only", true);
        if (!syncOnWifiOnly || isWifi) {
            Log.d("davsync", "Trying to upload immediately (on WIFI)");
            Intent ulIntent = new Intent(context, UploadService.class);
            context.startService(ulIntent);
        }
    }

}
