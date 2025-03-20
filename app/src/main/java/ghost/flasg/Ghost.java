package ghost.flasg;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Ghost extends Activity {

	private static final int UPDATE_INTERVAL_MS = 1000;
	private static final String FORMAT_TIME = "hh:mm";
	private static final String FORMAT_DATE = "EEEE, dd MMMM yyyy";

	private CameraManager cameraManager;
	private String cameraId;
	private ImageView flashImageView;
	private boolean isFlashOn;
	private RelativeLayout mainLayout;
	private TextView timeTextView, dateTextView;
	private ScheduledExecutorService executorService;
	private Handler handler = new Handler(Looper.getMainLooper());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.flash);

		flashImageView = findViewById(R.id.flashImageView);
		mainLayout = findViewById(R.id.laymain);
		timeTextView = findViewById(R.id.time_txt);
		dateTextView = findViewById(R.id.other_txt);

		configureUI();
		initializeCamera();

		flashImageView.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				toggleFlash();
				return true;
			}
			return false;
		});

		executorService = Executors.newScheduledThreadPool(2);
		executorService.scheduleAtFixedRate(this::updateTime, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);

		setBackgroundAuto();
	}

	private void configureUI() {
		View decorView = getWindow().getDecorView();
		int flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(flags);
		getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
		getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
	}

	private void initializeCamera() {
		cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		try {
			if (cameraManager != null)
				cameraId = cameraManager.getCameraIdList()[0];
		} catch (CameraAccessException e) {
		}
	}

	private void toggleFlash() {
		try {
			if (isFlashOn)
				turnFlashOff();
			else
				turnFlashOn();
		} catch (CameraAccessException e) {
		}
	}

	private void turnFlashOn() throws CameraAccessException {
		cameraManager.setTorchMode(cameraId, true);
		isFlashOn = true;
		setFlashImageResource(R.drawable.on);
	}

	private void turnFlashOff() throws CameraAccessException {
		cameraManager.setTorchMode(cameraId, false);
		isFlashOn = false;
		setFlashImageResource(R.drawable.off);
	}

	private void setFlashImageResource(int resourceId) {
		flashImageView.setImageDrawable(getResources().getDrawable(resourceId));
	}

	private void updateTime() {
		handler.post(() -> {
			timeTextView.setText(new SimpleDateFormat(FORMAT_TIME, Locale.getDefault()).format(new Date()));
			dateTextView.setText(new SimpleDateFormat(FORMAT_DATE, Locale.getDefault()).format(new Date()));
		});
	}

	private void setBackgroundAuto() {
		String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
		int backgroundResource = getBackgroundResource(currentTime);
		mainLayout.setBackgroundResource(backgroundResource);
	}

	private int getBackgroundResource(String currentTime) {
		try {
			String[][] timeRanges = { { "02:00", "04:00", "bg1" }, { "04:00", "10:00", "bg2" },
					{ "10:00", "16:00", "bg3" }, { "16:00", "18:30", "bg4" }, { "18:30", "24:00", "bg5" } };
			for (String[] range : timeRanges) {
				if (isBetween(currentTime, range[0], range[1]))
					return getResourceId(range[2]);
			}
		} catch (ParseException e) {
		}
		return getResourceId("bg2");
	}

	private boolean isBetween(String currentTime, String startTime, String endTime) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
		Date current = sdf.parse(currentTime);
		Date start = sdf.parse(startTime);
		Date end = sdf.parse(endTime);
		return current != null && start != null && end != null && !current.before(start) && current.before(end);
	}

	private int getResourceId(String resourceName) {
		return getResources().getIdentifier(resourceName, "drawable", getPackageName());
	}

	public Bitmap takeScreenshot() {
		View rootView = getWindow().getDecorView().getRootView();
		Bitmap screenshot = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(screenshot);
		rootView.draw(canvas);
		return screenshot;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (executorService != null && !executorService.isShutdown())
			executorService.shutdown();
	}
}