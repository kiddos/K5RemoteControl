package com.kiddos.k4remotecontrol;

import android.content.*;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;

public class JoystickView extends View implements View.OnTouchListener {
	private Paint borderPaint, controlPaint;
	private int borderSize;
	private float mx, my, dx, dy;
	private double stopSpeed, baseSpeed;
	private double r, theta;

	public JoystickView(Context context) {
		super(context);
		init();
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public JoystickView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		borderPaint = new Paint();
		borderPaint.setColor(Color.BLACK);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(6);

		controlPaint = new Paint();
		controlPaint.setColor(Color.BLACK);

		this.setOnTouchListener(this);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final int width = this.getWidth();
		final int height = this.getHeight();
		final int cx = width / 2;
		final int cy = height / 2;

		borderSize = Math.min((width/2-this.getPaddingLeft()*2), (height/2-this.getPaddingTop()*2));
		final int controlSize = (int) (borderSize * 0.6);
		canvas.drawCircle(cx, cy, borderSize, borderPaint);
		canvas.drawCircle(cx + dx, cy - dy, controlSize, controlPaint);
	}

	public double getR() {
		final double val = r * baseSpeed + stopSpeed;
		return val > (stopSpeed + baseSpeed) ? (stopSpeed + baseSpeed) : val;
	}

	public double getTheta() {
		return theta;
	}

	public void setBaseSpeed(final double baseSpeed) {
		this.baseSpeed = Math.abs(baseSpeed);
	}

	public void setStopSpeed(final double stopSpeed) {
		this.stopSpeed = stopSpeed;
	}

	private double computeTheta(final double dx, final double dy) {
		if (dy == 0) {
			if (dx == 0) {
				return 0;
			} else if (dx > 0) {
				return Math.PI;
			} else {
				return 3 * Math.PI / 2;
			}
		} else if (dy > 0) {
			if (dx >= 0) {
				return Math.atan(dx / dy);
			} else {
				return 2 * Math.PI + Math.atan(dx / dy);
			}
		} else {
			return Math.PI + Math.atan(dx / dy);
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		final int actionEvent = motionEvent.getAction();
		double deltaX, deltaY;
		switch (actionEvent) {
			case MotionEvent.ACTION_DOWN:
				Log.d("JoyStickView", "action down");
				mx = motionEvent.getX();
				my = motionEvent.getY();
				dx = motionEvent.getX() - mx;
				dy = -(motionEvent.getY() - my);
				break;
			case MotionEvent.ACTION_UP:
				Log.d("JoyStickView", "action up");
				dx = dy = 0;
				break;
			case MotionEvent.ACTION_MOVE:
				dx = motionEvent.getX() - mx;
				dy = -(motionEvent.getY() - my);
				break;
		}
		deltaX = dx / borderSize;
		deltaY = dy / borderSize;
		r = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		theta = this.computeTheta(deltaX, deltaY);

		invalidate();
		return true;
	}
}
