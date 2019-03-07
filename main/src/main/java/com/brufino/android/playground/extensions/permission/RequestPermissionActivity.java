package com.brufino.android.playground.extensions.permission;

import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.os.ResultReceiver;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.brufino.android.playground.R;
import com.brufino.android.playground.extensions.AndroidUtils;

import java.util.Locale;

public class RequestPermissionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permission);
        TextView text = findViewById(R.id.text);
        text.setText(inspect(getIntent()));
        ResultReceiver receiver = getIntent().getParcelableExtra("receiver");
        Debug.waitForDebugger();
        receiver.send(24, null);
    }

    private static String inspect(Intent intent) {
        return String.format(
                Locale.US,
                "intent = %s%nextras:%n%s",
                intent.toString(),
                inspect(intent.getExtras()));
    }

    private static String inspect(@Nullable Bundle bundle) {
        if (bundle == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            builder.append(String.format(Locale.US, "%s = %s%n", key,
                    (value == null) ? "null" : value.toString()));
        }
        return builder.toString();
    }
}
