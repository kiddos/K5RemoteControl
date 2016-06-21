package com.kiddos.k4remotecontrol;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
	private static final int SERVER_PORT = 8080;
	private static final int STOP_SPEED = 255;
	private TextView serverIP, serverStatus;
	private JoystickView joystickView;
	private SeekBar sbBaseSpeed;
	private EditText etBaseSpeed;
	private CheckBox cbLock;
	private ImageButton reconnect;
	private Handler handler;
	private SendDataTask dataTask;
	private boolean taskStarted, connecting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		serverIP = (TextView) findViewById(R.id.tvServerIP);
		serverStatus = (TextView) findViewById(R.id.tvServerStatus);
		joystickView = (JoystickView) findViewById(R.id.jvControl);
		sbBaseSpeed = (SeekBar) findViewById(R.id.sbBaseSpeed);
		etBaseSpeed = (EditText) findViewById(R.id.etBaseSpeed);
		cbLock = (CheckBox) findViewById(R.id.cbLock);
		reconnect = (ImageButton) findViewById(R.id.ibReconnect);
		handler = new Handler();

		init();
		trySendingData();
	}

	private void init() {
		if (sbBaseSpeed != null && etBaseSpeed != null && cbLock != null && joystickView != null) {
			sbBaseSpeed.setMax(STOP_SPEED);
			etBaseSpeed.setText(String.valueOf(sbBaseSpeed.getProgress()));

			joystickView.setBaseSpeed(sbBaseSpeed.getProgress());
			joystickView.setStopSpeed(STOP_SPEED);

			sbBaseSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int pos, boolean fromUser) {
					etBaseSpeed.setText(String.valueOf(sbBaseSpeed.getProgress()));
					joystickView.setBaseSpeed(sbBaseSpeed.getProgress());
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
			});
			cbLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
					sbBaseSpeed.setEnabled(!isChecked);
					etBaseSpeed.setEnabled(!isChecked);
				}
			});
			etBaseSpeed.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					final String newText = s.toString();
					if (newText.length() > 3) {
						etBaseSpeed.setText(newText.substring(0, 3));
					}

					final String progressText = etBaseSpeed.getText().toString();
					etBaseSpeed.setSelection(progressText.length());
					if (progressText.length() > 0) {
						try {
							final int val = Integer.parseInt(progressText);
							sbBaseSpeed.setProgress(val);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}

				@Override
				public void afterTextChanged(Editable editable) {
				}
			});
		}
		if (reconnect != null) {
			reconnect.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Animation anim = new RotateAnimation(0f, 720, Animation.RELATIVE_TO_SELF,
							0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					anim.setDuration(2000);
					anim.setRepeatCount(-1);
					anim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						@Override
						public void onAnimationEnd(Animation animation) {}

						@Override
						public void onAnimationRepeat(Animation animation) {
							if (!connecting) {
								animation.cancel();
							}
						}
					});

					reconnect.startAnimation(anim);
					trySendingData();
				}
			});
		}
	}

	private void setServerStatus(final boolean success) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (success) {
					serverStatus.setText(getResources().getString(R.string.status_success));
					serverStatus.setTextColor(getResources().getColor(R.color.colorSuccess));
				}
			}
		});
	}

	private void trySendingData() {
		if (!taskStarted) {
			dataTask = new SendDataTask();
			Thread task = new Thread(dataTask);
			task.start();
			Log.d("MainActivity", "sending data task started");
			taskStarted = true;
			connecting = true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (dataTask != null) {
			Log.i("MainActivity", "stopping sending data task");
			dataTask.stopRunning();
		}
	}

	private class SendDataTask implements Runnable {
		private boolean running;
		private final String dataFormat = "K%s,%s";

		public SendDataTask() {
			running = true;
		}

		public void stopRunning() {
			running = false;
		}

		private void sendData(final PrintWriter writer) {
			DecimalFormat decimalFormat = new DecimalFormat("000.000000");
			final String data = String.format(Locale.TAIWAN, dataFormat,
					decimalFormat.format(joystickView.getR()),
					decimalFormat.format(joystickView.getTheta()));
			writer.print(data);
			writer.flush();

			Log.d("MainActivity", "data: " + data);
		}

		@Override
		public void run() {
			try {
				Log.i("MainActivity", "connecting to server");
				Socket socket = new Socket(serverIP.getText().toString(), SERVER_PORT);
				connecting = false;
				if (socket.isConnected()) {
					setServerStatus(true);
					OutputStream out = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(out);

					while (running) {
						sendData(writer);

						try {
							Thread.sleep(50);
						} catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
					writer.close();
				}
				socket.close();
				Log.i("MainActivity", "socket closed");
			} catch (IOException e) {
				connecting = false;
				e.printStackTrace();
				Log.i("MainActivity", "socket io failure");
			}

			taskStarted = false;
			setServerStatus(false);
			Log.i("MainActivity", "ending task...");
		}
	}
}
