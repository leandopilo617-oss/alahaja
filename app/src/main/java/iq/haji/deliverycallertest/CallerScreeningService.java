
package iq.haji.deliverycallertest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

public class CallerScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details details) {
        try {
            boolean incoming =
                details.getCallDirection()
                    == Call.Details.DIRECTION_INCOMING;

            Uri handle = details.getHandle();
            String number = "";

            if (handle != null &&
                "tel".equalsIgnoreCase(handle.getScheme())) {
                number = handle.getSchemeSpecificPart();
            }

            CallResponse response = new CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build();

            respondToCall(details, response);

            if (incoming && number != null &&
                !number.trim().isEmpty()) {
                getSharedPreferences(
                    MainActivity.PREFS, MODE_PRIVATE
                ).edit()
                    .putString(MainActivity.KEY_NUMBER, number)
                    .apply();

                showNumberNotification(number);
            }
        } catch (Exception ignored) {
        }
    }

    private void showNumberNotification(String number) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(
                    this, MainActivity.CHANNEL_ID
                )
                : new Notification.Builder(this);

        Notification notification = builder
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("دلفري الحجي - رقم المتصل")
            .setContentText(number)
            .setStyle(
                new Notification.BigTextStyle().bigText(number)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_CALL)
            .build();

        NotificationManager manager =
            getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.notify(1001, notification);
        }
    }
}
