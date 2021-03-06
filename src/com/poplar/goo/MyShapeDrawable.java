package com.poplar.goo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.poplar.goo.util.GeometryUtil;

/**
 * This view should be added to WindowManager, so we can drag it to anywhere.
 * @author Poplar
 *
 */
public class MyShapeDrawable extends View {
	
	interface OnDisappearListener {
		void onDisappear();
		void onReset(boolean isOutOfRange);
	}

	protected static final String TAG = "TAG";
	
	private PointF mInitCenter;
	private PointF mDragCenter;
	private PointF mStickCenter;
	float dragCircleRadius = 15.0f;
	float stickCircleRadius = 15.0f;
	float stickCircleMinRadius = 5.0f;
	float stickCircleTempRadius = stickCircleRadius;
	float farest = 120f;
	String text = "";

	private Paint mPaintRed;
	private Paint mTextPaint;
	private ValueAnimator mAnim;
	private boolean isOutOfRange = false;
	private boolean isDisappear = false;
	
	private OnDisappearListener mListener;
	private ValueAnimator mEndAnim;
	private boolean hasUp;
	private Rect rect;
	private int mStatusBarHeight;
	
	
	public MyShapeDrawable(Context context) {
		super(context);

		mPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintRed.setColor(Color.RED);
		
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTextSize(dragCircleRadius * 1.2f);
		
		rect = new Rect(0, 0, 50, 50);
		
	}

	/**
	 * 设置固定圆的半径
	 * @param r
	 */
	public void setDargCircleRadius(float r){
		dragCircleRadius = r;
	}
	
	/**
	 * 设置拖拽圆的半径
	 * @param r
	 */
	public void setStickCircleRadius(float r){
		stickCircleRadius = r;
	}
	
	/**
	 * 设置数字
	 * @param num
	 */
	public void setNumber(int num){
		text = String.valueOf(num);
	}
	
	/**
	 * 初始化圆的圆心坐标
	 * @param x
	 * @param y
	 */
	public void initCenter(float x, float y){
		mDragCenter = new PointF(x, y);
		mStickCenter = new PointF(x, y);
		
		mInitCenter = new PointF(x, y);
	}
	
	/**
	 * 更新拖拽圆的圆心坐标，重绘View
	 * @param x
	 * @param y
	 */
	private void updateDragCenter(float x, float y) {
		this.mDragCenter.x = x;
		this.mDragCenter.y = y;
		invalidate();
	}
	
	/**
	 * 更新固定圆的圆心坐标，重绘View
	 * @param x
	 * @param y
	 */
	private void updateStickCenter(float x, float y) {
		this.mStickCenter.x = x;
		this.mStickCenter.y = y;
		invalidate();
	}

	
	/**
	 * 获得需要被绘制的drawable
	 * @return
	 */
	private ShapeDrawable drawGooView() {
		Path path = new Path();
		
		float xDiff = mStickCenter.x - mDragCenter.x;
		Double dragLineK = null;
		if(xDiff != 0){
			dragLineK = (double) ((mStickCenter.y - mDragCenter.y) / xDiff);
		}
		float distance = (float) GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
		stickCircleTempRadius = getCurrentRadius(distance);
		
		PointF pointByPercent = GeometryUtil.getPointByPercent(mDragCenter, mStickCenter, 0.618f);
		
		PointF[] dragPoints = GeometryUtil.getResults(mDragCenter, dragCircleRadius, dragLineK);
		PointF[] stickPoints = GeometryUtil.getResults(mStickCenter, stickCircleTempRadius, dragLineK);

		path.moveTo((float)stickPoints[0].x, (float)stickPoints[0].y);
		path.quadTo((float)pointByPercent.x, (float)pointByPercent.y,
				(float)dragPoints[0].x, (float)dragPoints[0].y);
		
		path.lineTo((float)dragPoints[1].x, (float)dragPoints[1].y);
		path.quadTo((float)pointByPercent.x, (float)pointByPercent.y,
				(float)stickPoints[1].x, (float)stickPoints[1].y);
		
		path.close();
		
//		path.addCircle((float)dragPoints[0].x, (float)dragPoints[0].y, 5, Direction.CW);
//		path.addCircle((float)dragPoints[1].x, (float)dragPoints[1].y, 5, Direction.CW);
//		path.addCircle((float)stickPoints[0].x, (float)stickPoints[0].y, 5, Direction.CW);
//		path.addCircle((float)stickPoints[1].x, (float)stickPoints[1].y, 5, Direction.CW);
		
		ShapeDrawable shapeDrawable = new ShapeDrawable(new PathShape(path, 50f, 50f));
		shapeDrawable.getPaint().setColor(Color.RED);
		return shapeDrawable;
	}
	
	/**
	 * 根据距离获得当前固定圆的半径
	 * @param distance
	 * @return
	 */
	private float getCurrentRadius(float distance) {
		
		distance = Math.min(distance, farest);
		
		// Start from 20%
		float fraction = 0.2f + 0.8f * distance / farest;
		
		// Distance -> Farthest
		// stickCircleRadius(15f) -> stickCircleMinRadius(5f)
		float evaluateValue = (float) GeometryUtil.evaluateValue(fraction, stickCircleRadius, stickCircleMinRadius);
		return evaluateValue;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if(isAnimRunning()){
			return false;
		}
		return super.dispatchTouchEvent(event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int actionMasked = MotionEventCompat.getActionMasked(event);
		switch (actionMasked) {
			case MotionEvent.ACTION_DOWN:{

				if(isAnimRunning()){
					return false;
				}
			
				hasUp = false;
				isDisappear = false;
				isOutOfRange = false;
				updateDragCenter(event.getRawX() , event.getRawY());
				
				break;
			}
			case MotionEvent.ACTION_MOVE:{
				
				PointF p0 = new PointF(mDragCenter.x, mDragCenter.y);
				PointF p1 = new PointF(mStickCenter.x, mStickCenter.y);
				if(GeometryUtil.getDistanceBetween2Points(p0,p1) > farest){

					mEndAnim = ValueAnimator.ofFloat(4.0f);
					mEndAnim.setInterpolator(new OvershootInterpolator());
	
					final PointF startPoint = p0;
					final PointF endPoint = p1;
					mEndAnim.addUpdateListener(new AnimatorUpdateListener() {
						
						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							float fraction = animation.getAnimatedFraction();
							PointF pointByPercent = GeometryUtil.getPointByPercent(startPoint, endPoint, 1 - fraction);
							updateStickCenter((float)pointByPercent.x, (float)pointByPercent.y);
						}
					});
					mEndAnim.addListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							isOutOfRange = true;
							
							if(hasUp){
								hasUp = false;
								disappeared();
							}
						}
					});
					mEndAnim.setDuration(200);
					mEndAnim.start();
					
					updateDragCenter(event.getRawX() , event.getRawY());
					return false;
				}
				
				updateDragCenter(event.getRawX() , event.getRawY());
				break;
			}
			case MotionEvent.ACTION_UP:{
				if(mEndAnim != null && mEndAnim.isRunning()){
					hasUp = true;
					return false;
				}
				handleActionUp();

				break;
			}
			default:{
				isOutOfRange = false;
				break;
			}
		}
		return true;
	}

	private boolean isAnimRunning() {
		if(mAnim != null && mAnim.isRunning()){
			return true;
		}
		if(mEndAnim != null && mEndAnim.isRunning()){
			return true;
		}
		return false;
	}
	/**
	 * 清除
	 */
	private void disappeared() {
		isDisappear = true;
		invalidate();
		
		if(mListener != null){
			mListener.onDisappear();
		}
	}
	
	private void handleActionUp() {
		if(isOutOfRange){
			// When user drag it back, we should call onReset().
			if(GeometryUtil.getDistanceBetween2Points(mDragCenter, mInitCenter) < 30.0f){
				if(mListener != null)
					mListener.onReset(isOutOfRange);
				return;
			}
			
			// Otherwise
			disappeared();
		}else {
			mAnim = ValueAnimator.ofFloat(1.0f);
			mAnim.setInterpolator(new OvershootInterpolator(4.0f));

			final PointF startPoint = new PointF(mDragCenter.x, mDragCenter.y);
			final PointF endPoint = new PointF(mStickCenter.x, mStickCenter.y);
			mAnim.addUpdateListener(new AnimatorUpdateListener() {
				
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					float fraction = animation.getAnimatedFraction();
					PointF pointByPercent = GeometryUtil.getPointByPercent(startPoint, endPoint, fraction);
					updateDragCenter((float)pointByPercent.x, (float)pointByPercent.y);
				}
			});
			mAnim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					if(mListener != null)
						mListener.onReset(isOutOfRange);
				}
			});
			if(GeometryUtil.getDistanceBetween2Points(startPoint,endPoint) < 10)	{
				mAnim.setDuration(10);
			}else {
				mAnim.setDuration(500);
			}
			mAnim.start();
		}
	}

	
	@Override
	protected void onDraw(Canvas canvas) {
		
		canvas.save();
		canvas.translate(0, -mStatusBarHeight);
		
		if(!isDisappear){
			if(!isOutOfRange){
				ShapeDrawable drawGooView = drawGooView();
				drawGooView.setBounds(rect);
				drawGooView.draw(canvas);
				
				canvas.drawCircle(mStickCenter.x, mStickCenter.y, stickCircleTempRadius, mPaintRed);
			}
			canvas.drawCircle(mDragCenter.x , mDragCenter.y, dragCircleRadius, mPaintRed);
			canvas.drawText(text, mDragCenter.x , mDragCenter.y + dragCircleRadius /2f, mTextPaint);
		}
		
		canvas.restore();
		
	}

	public OnDisappearListener getOnDisappearListener() {
		return mListener;
	}

	public void setOnDisappearListener(OnDisappearListener mListener) {
		this.mListener = mListener;
	}

	public void setStatusBarHeight(int statusBarHeight) {
		this.mStatusBarHeight = statusBarHeight;
	}

}
