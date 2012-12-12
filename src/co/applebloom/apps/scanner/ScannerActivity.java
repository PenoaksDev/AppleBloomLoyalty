package co.applebloom.apps.scanner;

import java.util.Timer;
import java.util.TimerTask;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import co.applebloom.apps.rewards.R;

public class ScannerActivity extends Activity
{
    private Timer mTimerSeconds;
    private int mIntIdleSeconds;
    private boolean mBoolInitialized=false;
    private int MAX_IDLE_TIME_SECONDS = 30;
	
	private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    ImageScanner scanner;

    @SuppressWarnings("unused")
	private boolean barcodeScanned = false;
    private boolean previewing = true;
    
    private String TAG = "Scanner";

    static {
        System.loadLibrary("iconv");
    }
    
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);

        setContentView(R.layout.scanner);
        
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        
        Button cancelButton = (Button)findViewById(R.id.button1);
        
        cancelButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		ScannerActivity.this.setResult(Activity.RESULT_CANCELED);
        		ScannerActivity.this.finish();
        	}
        });
        
        	/*	
        	scanButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (barcodeScanned) {
                        barcodeScanned = false;
                        scanText.setText("Scanning...");
                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                        previewing = true;
                        mCamera.autoFocus(autoFocusCB);
                    }
                }
            });
            */
        
        startIdleTimer();
    }
    
    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private static Camera openFrontFacingCamera() 
    {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT  ) {
                try {
                    cam = Camera.open( camIdx );
                } catch (RuntimeException e) {
                    Log.e("DBG", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            //c = Camera.open();
        	c = openFrontFacingCamera();
        } catch (Exception e){
        	Log.d("DBG", "Fatal Exception occured while trying to obtain the camera device.");
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    
    private void returnResult(String result) {
    	final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        tg.startTone(ToneGenerator.TONE_PROP_ACK);
    	
    	Intent intent = new Intent(getIntent().getAction());
        intent.putExtra("SCAN_RESULT", result);
        this.setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    private Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (previewing)
                    mCamera.autoFocus(autoFocusCB);
            }
        };

    PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);
                
                if (result != 0) {
                    previewing = false;
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    
                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {
                        //scanText.setText("barcode result " + sym.getData());
                    	Log.d(TAG, "Successly scanned a barcode which returned \"" + sym.getData() + "\"");
                        barcodeScanned = true;
                        
                        returnResult(sym.getData());
                    }
                }
            }
        };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
        };
        
        @Override
        protected void onDestroy()
        {
            if (mTimerSeconds != null)
            {
                mTimerSeconds.cancel();
            }
            super.onDestroy();
        }

        public void onTouch()
        {
            mIntIdleSeconds=0;
        }

        /** start the idle timer */
        public void startIdleTimer()
        {
            if (mBoolInitialized == false)
            {
                mBoolInitialized = true;

                //initialize idle counter
                mIntIdleSeconds=0;

                //create timer to tick every second
                mTimerSeconds = new Timer();
                mTimerSeconds.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        timerSecondsCounter();
                    }
                }, 0, 1000);
            }
        }

        private void timerSecondsCounter()
        {
            mIntIdleSeconds++;
            
            //if ( (MAX_IDLE_TIME_SECONDS - mIntIdleSeconds) < 10 )
            	Log.v("APPLEBLOOM", "This activity will finish in " + (MAX_IDLE_TIME_SECONDS - mIntIdleSeconds) +  " seconds.");

            if (mIntIdleSeconds >= MAX_IDLE_TIME_SECONDS)
            {
            	setResult(Activity.RESULT_CANCELED);
            	finish();
            }

        }
}