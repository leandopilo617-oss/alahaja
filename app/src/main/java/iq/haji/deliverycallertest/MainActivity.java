
package iq.haji.deliverycallertest;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String PREFS = "caller_test";
    public static final String KEY_NUMBER = "latest_number";
    public static final String CHANNEL_ID = "caller_test_channel";

    private TextView numberView;
    private TextView statusView;

    private final Handler autoRefreshHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable autoRefreshTask = new Runnable() {
        @Override
        public void run() {
            if (numberView != null) {
                refreshNumber();
            }

            autoRefreshHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();
        showScreen();
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshNumber();
        refreshRoleStatus();

        autoRefreshHandler.removeCallbacks(autoRefreshTask);
        autoRefreshHandler.post(autoRefreshTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshTask);
    }

    @Override
    protected void onDestroy() {
        autoRefreshHandler.removeCallbacks(autoRefreshTask);
        super.onDestroy();
    }

    private void showScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(28, 28, 28, 28);
        layout.setBackgroundColor(0xFFF7F7F7);

        TextView title = new TextView(this);
        title.setText("دلفري الحجي");
        title.setTextSize(28);
        title.setTextColor(0xFFB3261E);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("استقبال رقم المتصل تلقائياً");
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 12, 0, 24);
        layout.addView(subtitle);

        numberView = new TextView(this);
        numberView.setTextSize(26);
        numberView.setTextColor(0xFF111111);
        numberView.setGravity(Gravity.CENTER);
        numberView.setPadding(0, 20, 0, 20);
        layout.addView(numberView);

        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 8, 0, 20);
        layout.addView(statusView);

        Button activateButton = new Button(this);
        activateButton.setText("تفعيل استقبال رقم المتصل");
        activateButton.setOnClickListener(v -> requestScreeningRole());
        layout.addView(activateButton);

        Button refreshButton = new Button(this);
        refreshButton.setText("تحديث الرقم");
        refreshButton.setOnClickListener(v -> refreshNumber());
        layout.addView(refreshButton);

        Button clearButton = new Button(this);
        clearButton.setText("مسح الرقم الأخير");
        clearButton.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_NUMBER)
                    .apply();

            refreshNumber();
        });
        layout.addView(clearButton);

        TextView note = new TextView(this);
        note.setText(
                "يعرض التطبيق آخر رقم مستلم تلقائياً أثناء فتحه. " +
                "الأرقام محفوظة محلياً ولا يرسلها التطبيق إلى الإنترنت."
        );
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 24, 0, 0);
        layout.addView(note);

        setContentView(layout);

        refreshNumber();
        refreshRoleStatus();
    }

    private void requestScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            statusView.setText("يتطلب أندرويد 10 أو أحدث");
            return;
        }

        RoleManager manager =
                (RoleManager) getSystemService(Context.ROLE_SERVICE);

        if (manager == null ||
                !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            statusView.setText(
                    "الجهاز لا يتيح صلاحية تعريف المكالمات لهذا التطبيق"
            );
            return;
        }

        if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            refreshRoleStatus();
        } else {
            startActivityForResult(
                    manager.createRequestRoleIntent(
                            RoleManager.ROLE_CALL_SCREENING
                    ),
                    42
            );
        }
    }

    private void refreshNumber() {
        if (numberView == null) {
            return;
        }

        String number = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_NUMBER, "");

        numberView.setText(
                number == null || number.isEmpty()
                        ? "بانتظار اتصال وارد"
                        : number
        );
    }

    private void refreshRoleStatus() {
        if (statusView == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager manager =
                    (RoleManager) getSystemService(Context.ROLE_SERVICE);

            boolean active = manager != null &&
                    manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);

            statusView.setText(
                    active
                            ? "الحالة: صلاحية تعريف المكالمات مفعّلة"
                            : "الحالة: اضغط زر التفعيل أولاً"
            );
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            "android.permission.POST_NOTIFICATIONS"
                    },
                    43
            );
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "رقم المتصل",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("إظهار آخر رقم متصل للاختبار");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
