package com.brufino.android.playground.transfer.task;

import android.content.Context;
import android.util.Log;
import com.brufino.android.common.CommonConstants;
import com.brufino.android.playground.extensions.ApplicationContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class TaskHistory {
    private static final String HISTORY_FILE_NAME = "history.txt";

    private final Context mContext;

    public TaskHistory(ApplicationContext context) {
        mContext = context.getContext();
    }

    @SuppressWarnings("unchecked")
    List<TaskEntry> open() {
        try (ObjectInputStream objectInput =
                     new ObjectInputStream(new FileInputStream(getFile(mContext)))) {
            return (List<TaskEntry>) objectInput.readObject();
        } catch (NoSuchFileException | FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            Log.e(CommonConstants.TAG, "IOException while trying to load history", e);
            return new ArrayList<>();
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    void save(List<TaskEntry> tasks) throws IOException {
        try (ObjectOutputStream objectOutput =
                     new ObjectOutputStream(new FileOutputStream(getFile(mContext)))) {
            objectOutput.writeObject(tasks);
        }
    }

    private static File getFile(Context context) {
        return context.getFilesDir().toPath().resolve(HISTORY_FILE_NAME).toFile();
    }
}
