package com.vimeo.download_sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.vimeo.download_sample.downloadqueue.DownloadManager;
import com.vimeo.download_sample.downloadqueue.DownloadTask;
import com.vimeo.download_sample.downloadqueue.exception.DownloadException;
import com.vimeo.turnstile.BaseTaskManager.TaskEventListener;
import com.vimeo.turnstile.TaskError;
import com.vimeo.turnstile.database.TaskCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private DownloadManager mTaskManager;
    private TextView mResultText;
    private ScrollView mScrollView;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && App.NOTIFICATION_INTENT_KEY.equals(getIntent().getAction())) {
            Toast.makeText(this, R.string.notification_click, Toast.LENGTH_LONG).show();
        }

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        mResultText = (TextView) findViewById(R.id.text_view_task_result);
        mImageView = (ImageView) findViewById(R.id.image_view);

        final TextView cacheView = (TextView) findViewById(R.id.text_view_cache);

        mTaskManager = DownloadManager.getInstance();
        mTaskManager.registerTaskEventListener(new TaskEventListener<DownloadTask>() {
            @Override
            public void onStarted(@NonNull DownloadTask task) {
                addTextLog(getString(R.string.started_text, task.getId()));
            }

            @Override
            public void onFailure(@NonNull DownloadTask task, @NonNull TaskError error) {
                addTextLog(getString(R.string.failure_to_run_text, task.getId()));
            }

            @Override
            public void onCanceled(@NonNull DownloadTask task) {
                addTextLog(getString(R.string.canceled_text, task.getId()));
            }

            @Override
            public void onSuccess(@NonNull DownloadTask task) {
                File file = task.getLocalFile();
                if (file != null) {
                    Glide.with(getApplicationContext()).load(file.getAbsolutePath()).into(mImageView);
                }
                addTextLog(getString(R.string.result_text, task.getId()));
            }

            @Override
            public void onAdded(@NonNull DownloadTask task) {
                //noinspection ConstantConditions
                cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));
            }
        });

        cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));

        findViewById(R.id.button_new_task).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String remoteFileUri = "http://www.catgifpage.com/gifs/318.gif";
                if (DownloadManager.getInstance().getTask(remoteFileUri) != null) {
                    try {
                        DownloadManager.getInstance().redownloadFile(remoteFileUri);
                    } catch (DownloadException e) {
                        e.printStackTrace();
                    }
                }
                final DownloadTask task = new DownloadTask(remoteFileUri);
                mTaskManager.addTask(task, new TaskCallback() {
                    @Override
                    public void onSuccess() {
                        addTextLog(getString(R.string.added_text, task.getId()));
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        addTextLog(getString(R.string.failure_text, task.getId()));
                    }
                });
            }
        });

        //noinspection ConstantConditions
        findViewById(R.id.button_clear_tasks).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTaskManager.cancelAll();
                //noinspection ConstantConditions
                cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));
            }
        });
    }

    private void addTextLog(String text) {
        String currentText = mResultText.getText().toString();
        currentText = currentText + "\n" + text;
        mResultText.setText(currentText);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.smoothScrollTo(0, mResultText.getBottom());
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (App.NOTIFICATION_INTENT_KEY.equals(intent.getAction())) {
            Toast.makeText(this, R.string.notification_click, Toast.LENGTH_LONG).show();
        }
    }
}
