package com.brufino.android.playground.components.main;

import android.content.Context;
import com.brufino.android.playground.extensions.ApplicationContext;
import com.brufino.android.playground.components.main.TaskStatisticsUtils.Parameters;
import com.brufino.android.playground.extensions.AndroidUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskSheet {
    private static final String SHEET_FILE_NAME = "experiment.csv";
    private static final String SHEET_HEADER =
            "Producer data size (B),"
                    + "Producer interval (ms),"
                    + "Producer chunk size (B),"
                    + "Transfer buffer size (B), "
                    + "Consumer interval (ms),"
                    + "Consumer buffer size (B),"
                    + "Time (ms)";

    private final Context mContext;

    public TaskSheet(ApplicationContext context) {
        mContext = context.getContext();
    }

    Path save(Map<Parameters, Double> results) throws IOException {
        List<String> content = new ArrayList<>();
        content.add(SHEET_HEADER);
        results.entrySet()
                .stream()
                .map(TaskSheet::getLine)
                .forEachOrdered(content::add);
        Path file = getFile();
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        return file;
    }

    private static String getLine(Map.Entry<Parameters, Double> results) {
        Parameters parameters = results.getKey();
        double result = results.getValue();
        return String.format(
                Locale.US,
                "%d,%d,%d,%d,%d,%d,%.4f",
                parameters.producerDataSize,
                parameters.producerInterval,
                parameters.producerChunkSize,
                parameters.transferBufferSize,
                parameters.consumerInterval,
                parameters.consumerBufferSize,
                result);
    }

    private static Path getFile() throws IOException {
        if (!AndroidUtils.isExternalStorageWritable()) {
            throw new IOException("Directory unavailable.");
        }
        return AndroidUtils.getDocumentPath(SHEET_FILE_NAME);
    }
}
