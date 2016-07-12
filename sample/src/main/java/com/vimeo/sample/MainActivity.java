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

import com.vimeo.sample.tasks.SampleTask;
import com.vimeo.sample.tasks.SampleTaskManager;
import com.vimeo.taskqueue.TaskConstants;
import com.vimeo.taskqueue.database.TaskCallback;

public class MainActivity extends AppCompatActivity {


    private SampleTaskManager mTaskManager;
    private TaskIdGenerator mTaskIdGenerator = new TaskIdGenerator();
    private TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultText = (TextView) findViewById(R.id.text_view_task_result);

        mTaskManager = SampleTaskManager.getInstance();
        mTaskManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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

        //noinspection ConstantConditions
        findViewById(R.id.button_new_task).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final SampleTask task = new SampleTask(mTaskIdGenerator.getId());
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
