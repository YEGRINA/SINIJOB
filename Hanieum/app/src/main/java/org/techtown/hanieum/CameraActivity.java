package org.techtown.hanieum;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class CameraActivity extends AppCompatActivity {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001; //arbitrary number, can be changed accordingly
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO"}; //array w/ permissions from manifest

    private int levelCount;
    private PreviewView mPreviewView;
    private ProgressDialog progressDialog;
    private Button mCaptureButton;
    private TextView guideline;
    private String recordType;
    private String dirName;

    private boolean mIsRecordingVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreviewView = findViewById(R.id.previewView);
        mCaptureButton = findViewById(R.id.camera_capture_button);
        guideline = findViewById(R.id.textView10);
        levelCount = 1;

        // creating the progress dialog @@@@@@@@@@@@@@@@ 위치 옮길 것-> 영상 리스트 화면 코드로
        progressDialog = new ProgressDialog(CameraActivity.this);
        progressDialog.setMessage("동영상을 생성하는 중입니다.\n잠시만 기다려주세요...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        Intent intent = getIntent();
        recordType = intent.getStringExtra("recordType");
        dirName = intent.getStringExtra("dirName");
        Log.e("dirName",intent.getStringExtra("dirName"));
        if(recordType.equals("full")) {
            guideline.setText("전체 촬영");
        } else if(recordType.equals("introduce")) {
            guideline.setText("자기소개 촬영");
            levelCount = 4;
        } else if(recordType.equals("motive")) {
            guideline.setText("지원동기 촬영");
            levelCount = 5;
        } else if(recordType.equals("career")) { // recordType == "career"
            guideline.setText("경력소개 촬영");
            levelCount = 6;
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("RestrictedApi")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        // Create a configuration object for the video use case
        VideoCapture.Builder builder = new VideoCapture.Builder();

        final VideoCapture videoCapture = builder
//                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()) // orientation에 맞추어 촬영 방향 결정
                .setTargetRotation(Surface.ROTATION_90) // 무조건 화면 표시 방향으로 촬영
                .build();

        Intent intent = new Intent();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, videoCapture);

        mCaptureButton.setOnClickListener(v -> { //촬영f 시작

            if (!mIsRecordingVideo) {
                mIsRecordingVideo = true;

                File file = null;
                switch (levelCount) {
                    case 1:
                        file = new File(getBatchDirectoryName(), "cv_1.mp4");
                        levelCount = 2;
                        break;
                    case 2:
                        file = new File(getBatchDirectoryName(), "cv_2.mp4");
                        levelCount = 3;
                        break;
                    case 3:
                        file = new File(getBatchDirectoryName(), "cv_3.mp4");
                        intent.putExtra("filename","full");
                        levelCount = 0;
                        break;
                    case 4:
                        file = new File(getBatchDirectoryName(), "cv_1.mp4");
                        intent.putExtra("filename","introduce");
                        levelCount = 0;
                        Log.e("file","4");
                        break;
                    case 5:
                        file = new File(getBatchDirectoryName(), "cv_2.mp4");
                        intent.putExtra("filename","motive");
                        levelCount = 0;
                        Log.e("file","5");
                        break;
                    case 6:
                        file = new File(getBatchDirectoryName(), "cv_3.mp4");
                        intent.putExtra("filename","career");
                        levelCount = 0;
                        Log.e("file","6");
                        break;
                }

                mCaptureButton.setBackgroundColor(Color.GREEN);
                mCaptureButton.setText("종료");

                String[] files = this.fileList();

                VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(file).build();
                videoCapture.startRecording(outputFileOptions, executor, new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Log.d("tag", "Video Saved Successfully" + Arrays.toString(files)));
                        if (levelCount == 0) {
                            // 종료 후, 머지 시작
                            setResult(Activity.RESULT_OK, intent);
                            finishActivity();
                        }

                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        Log.i("tag", "Video Error: " + message);
                    }
                });
            } else {
                mIsRecordingVideo = false;
                mCaptureButton.setBackgroundColor(Color.RED);
                mCaptureButton.setText("시작");
                videoCapture.stopRecording();
                if (levelCount == 0) {
                    try {
                        concatVideos();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Log.d("tag", "Video stopped");
            }
        });
    }

    private void finishActivity() {
        this.finish();
    }

    // @@@@@@@@@@@@@@@ 위치 옮길 것-> 영상 리스트 화면 코드로
    private void concatVideos() throws Exception {
        progressDialog.show();
        String dir = getBatchDirectoryName();
        File dest = new File(getBatchDirectoryName(), "cv.mp4");
        String filePath = dest.getAbsolutePath();
        String exe;
        // the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        exe = "-y -i " + dir + "/cv_1.mp4" + " -i " + dir + "/cv_2.mp4" + " -i " + dir + "/cv_3.mp4"
                + " -filter_complex \"[0:v]setpts=PTS-STARTPTS,scale=1920x1080,fps=24,format=yuv420p[video0];" +
                "[0:a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[audio0];" +
                "[1:v]setpts=PTS-STARTPTS,scale=1920x1080,fps=24,format=yuv420p[video1];" +
                "[1:a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[audio1];" +
                "[2:v]setpts=PTS-STARTPTS,scale=1920x1080,fps=24,format=yuv420p[video2];" +
                "[2:a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[audio2];" +
                "[video0][audio0][video1][audio1][video2][audio2]" +
                "concat=n=3:v=1:a=1[outv][outa]\" -map \"[outv]\" -map \"[outa]\" " + filePath;

        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {

                    progressDialog.dismiss();
                    finishActivity();

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                    progressDialog.dismiss();
                    finishActivity();

                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                    progressDialog.dismiss();
                    finishActivity();

                }
            }
        });
    }

    public String getBatchDirectoryName() {

        String app_folder_path = this.getFilesDir().toString() + "/videocv_" + dirName;
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {
        }
        Log.d("TAG", "getBatchDirectoryName: " + app_folder_path);
        String[] testDir = dir.list();
        for(int i=0;i<testDir.length;i++) {
            Log.e("testDirfilepath", testDir[i]);
        }
        return app_folder_path;
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            Log.d("permission check", permission);
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

}