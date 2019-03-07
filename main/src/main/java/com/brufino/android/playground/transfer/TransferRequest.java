package com.brufino.android.playground.transfer;

import android.content.Context;
import android.content.Intent;
import com.brufino.android.playground.transfer.service.TransferService;

public class TransferRequest {
    private static final String EXTRA_TASK_CODE = "extra_task_code";
    private static final String EXTRA_TASK_CONFIGURATION = "extra_task_configuration";

    static Intent getIntent(Context context, int code, TransferConfiguration configuration) {
        return new TransferRequest(code, configuration).getIntent(context);
    }

    static TransferRequest create(Intent intent) {
        Object deserializedConfiguration = intent.getSerializableExtra(EXTRA_TASK_CONFIGURATION);
        TransferConfiguration configuration =
                (deserializedConfiguration instanceof TransferConfiguration)
                        ? (TransferConfiguration) deserializedConfiguration
                        : TransferConfiguration.DEFAULT;
        int code = intent.getIntExtra(EXTRA_TASK_CODE, -1);
        return new TransferRequest(code, configuration);
    }

    static TransferRequest create(int code, TransferConfiguration configuration) {
        return new TransferRequest(code, configuration);
    }

    public final int code;
    public final TransferConfiguration configuration;

    private TransferRequest(int code, TransferConfiguration configuration) {
        this.code = code;
        this.configuration = configuration;
    }

    Intent getIntent(Context context) {
        Intent intent = new Intent(context, TransferService.class);
        intent.putExtra(TransferRequest.EXTRA_TASK_CODE, code);
        intent.putExtra(TransferRequest.EXTRA_TASK_CONFIGURATION, configuration);
        return intent;
    }

    @Override
    public String toString() {
        return "TransferRequest{code=" + code + "}";
    }
}
