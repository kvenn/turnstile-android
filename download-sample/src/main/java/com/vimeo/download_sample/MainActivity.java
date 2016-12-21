package com.vimeo.download_sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.vimeo.download_sample.downloadqueue.DownloadManager;
import com.vimeo.download_sample.downloadqueue.DownloadTask;
import com.vimeo.download_sample.downloadqueue.exception.DownloadException;
import com.vimeo.download_sample.downloadqueue.exception.NullRemoteFileUrlException;
import com.vimeo.download_sample.downloadqueue.exception.OutOfSpaceException;
import com.vimeo.turnstile.BaseTaskManager.TaskEventListener;
import com.vimeo.turnstile.TaskError;
import com.vimeo.turnstile.database.TaskCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private DownloadManager mTaskManager;
    private TextView mResultText;
    private ScrollView mScrollView;
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    private static final String[] sUrls = {
            "http://www.catgifpage.com/gifs/310.gif", "http://www.catgifpage.com/gifs/318.gif",
            "http://www.cutecatgifs.com/wp-content/uploads/2015/07/Cat-slots.gif",
            "http://viralgifs.com/wp-content/uploads/2014/03/cat_leg_crawl.gif"
    };

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
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

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
                showImageFromTask(task);
                addTextLog(getString(R.string.result_text, task.getId()));
            }

            @Override
            public void onAdded(@NonNull DownloadTask task) {
                cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));
            }
        });

        cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));

        findViewById(R.id.button_new_large_task).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // 50mb gif (71mb)
                addRemoteFileUrlToDownloadQueue("http://i.imgur.com/10EEeYU.gif");
            }
        });

        findViewById(R.id.button_new_task).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addRandomCatGifToDownloadQueue();
            }
        });

        findViewById(R.id.button_clear_tasks).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTaskManager.cancelAll();
                cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));
            }
        });
    }

    private void addRandomCatGifToDownloadQueue() {
        String remoteFileUri = sUrls[(int) (Math.random() * (sUrls.length))];
        addRemoteFileUrlToDownloadQueue(remoteFileUri);
    }

    private void addRemoteFileUrlToDownloadQueue(String remoteFileUri) {

        DownloadTask existingTask = DownloadManager.getInstance().getTask(remoteFileUri);
        if (existingTask != null) {
            if (existingTask.isComplete()) {
                addTextLog("The task you're trying to add is already done! " + existingTask.getId());
                showImageFromTask(existingTask);
                return;
            }
            // Only re-add it if it's not already complete
            // re-download will cancel/delete the current task and start over
            try {
                DownloadManager.getInstance().redownloadFile(remoteFileUri);
            } catch (NullRemoteFileUrlException e) {
                addTextLog("Failure: Null remote file url");
            } catch (OutOfSpaceException e) {
                addTextLog("Failure: Out of space");
            } catch (DownloadException e) {
                addTextLog("Failure: Couldn't add download to queue");
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

    private void showImageFromTask(@Nullable DownloadTask task) {
        if (task == null) {
            addTextLog("Null task passed to `showImageFromTask()`");
            return;
        }
        File file = task.getLocalFile();
        if (file != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext())
                    .load(file)
                    .listener(new RequestListener<File, GlideDrawable>() {

                        @Override
                        public boolean onException(Exception e, File model, Target<GlideDrawable> target,
                                                   boolean isFirstResource) {
                            mProgressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, File model,
                                                       Target<GlideDrawable> target,
                                                       boolean isFromMemoryCache, boolean isFirstResource) {
                            mProgressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(mImageView);
        }
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
