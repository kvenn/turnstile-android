package com.vimeo.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.vimeo.sample.tasks.SimpleTask;
import com.vimeo.sample.tasks.SimpleTaskManager;
import com.vimeo.taskqueue.TaskConstants;
import com.vimeo.taskqueue.database.TaskCallback;

public class MainActivity extends AppCompatActivity {


    private SimpleTaskManager mTaskManager;
    private static final TaskIdGenerator sTaskIdGenerator = new TaskIdGenerator();
    private TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && App.NOTIFICATION_INTENT_KEY.equals(getIntent().getAction())) {
            Toast.makeText(this, R.string.notification_click, Toast.LENGTH_LONG).show();
        }

        mResultText = (TextView) findViewById(R.id.text_view_task_result);

        final TextView cacheView = (TextView) findViewById(R.id.text_view_cache);

        mTaskManager = SimpleTaskManager.getInstance();
        mTaskManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));
                String event = intent.getStringExtra(TaskConstants.TASK_EVENT);
                switch (event) {
                    case TaskConstants.EVENT_SUCCESS:
                        addTextLog(getString(R.string.result_text,
                                             intent.getStringExtra(TaskConstants.TASK_ID)));
                        break;
                    case TaskConstants.EVENT_FAILURE:
                        addTextLog(getString(R.string.failure_text,
                                             intent.getStringExtra(TaskConstants.TASK_ID)));
                        break;
                }
            }
        });

        cacheView.setText(getString(R.string.tasks_in_cache, mTaskManager.getTasks().size()));

        //noinspection ConstantConditions
        findViewById(R.id.button_new_task).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final SimpleTask task = new SimpleTask(sTaskIdGenerator.getId());
                mTaskManager.addTask(task, new TaskCallback() {
                    @Override
                    public void onSuccess() {
                        addTextLog(getString(R.string.started_text, task.getId()));
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        addTextLog(getString(R.string.failure_text, task.getId()));
                    }
                });
            }
        });
    }

    private void addTextLog(String text) {
        String currentText = mResultText.getText().toString();
        currentText = currentText + "\n" + text;
        mResultText.setText(currentText);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (App.NOTIFICATION_INTENT_KEY.equals(intent.getAction())) {
            Toast.makeText(this, R.string.notification_click, Toast.LENGTH_LONG).show();
        }
    }
}
