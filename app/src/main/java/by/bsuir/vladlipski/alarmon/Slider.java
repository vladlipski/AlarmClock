package by.bsuir.vladlipski.alarmon;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class Slider extends ViewGroup {
  public interface OnCompleteListener {
    void complete();
  }

  private static final int FADE_MILLIS = 200;
  private static final int SLIDE_MILLIS = 200;
  private static final float SLIDE_ACCEL = (float) 1.0;
  private static final double PERCENT_REQUIRED = 0.72;

  private ImageView dot;
  private TextView tray;
  private boolean tracking;
  private OnCompleteListener completeListener;

  public Slider(Context context) {
    this(context, null, 0);
  }

  public Slider(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Slider(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    tray = new TextView(getContext());
    tray.setBackgroundResource(R.drawable.slider_background);
    tray.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    tray.setGravity(Gravity.CENTER);

    if (Build.VERSION.SDK_INT < 23) {
        tray.setTextAppearance(getContext(), R.style.SliderText);
    } else {
        tray.setTextAppearance(R.style.SliderText);
    }

    tray.setText(R.string.dismiss);
    addView(tray);

    dot = new ImageView(getContext());
    dot.setImageResource(R.drawable.ic_forward);
    dot.setBackgroundResource(R.drawable.slider_btn);
    dot.setScaleType(ScaleType.CENTER);
    dot.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    dot.setPadding(30, 10, 25, 15);
    addView(dot);

    reset();
  }

  public void setOnCompleteListener(OnCompleteListener listener) {
    completeListener = listener;
  }

  public void reset() {
    tracking = false;
    if (getVisibility() != View.VISIBLE) {
      dot.offsetLeftAndRight(getLeft() - dot.getLeft());
      setVisibility(View.VISIBLE);
      Animation fadeIn = new AlphaAnimation(0, 1);
      fadeIn.setDuration(FADE_MILLIS);
      startAnimation(fadeIn);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (!changed) {
      return;
    }
    dot.layout(0, 7, dot.getMeasuredWidth(), dot.getMeasuredHeight());
    tray.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    tray.measure(widthMeasureSpec, heightMeasureSpec);
    dot.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        heightMeasureSpec);
    setMeasuredDimension(
        Math.max(tray.getMeasuredWidth(), dot.getMeasuredWidth()),
        Math.max(tray.getMeasuredHeight(), dot.getMeasuredHeight()));
  }

  private boolean withinX(View v, float x) {
    return !(x < v.getLeft() || x > v.getRight());
  }

  private boolean withinY(View v, float y) {
    return !(y < v.getTop() || y > v.getBottom());
  }

  private void slideDotHome() {
    int distanceFromStart = dot.getLeft() - getLeft();
    dot.offsetLeftAndRight(-distanceFromStart);
    Animation slideBack = new TranslateAnimation(distanceFromStart, 0, 0, 0);
    slideBack.setDuration(SLIDE_MILLIS);
    slideBack.setInterpolator(new DecelerateInterpolator(SLIDE_ACCEL));
    dot.startAnimation(slideBack);
  }

  private boolean isComplete() {
    double dotCenterY = dot.getLeft() + dot.getMeasuredWidth()/2.0;
    float progressPercent = (float)(dotCenterY - getLeft()) / (float)(getRight() - getLeft());
    return progressPercent > PERCENT_REQUIRED;
  }

  private void finishSlider() {
    setVisibility(View.INVISIBLE);
    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setDuration(FADE_MILLIS);
    fadeOut.setAnimationListener(new AnimationListener() {
      @Override
      public void onAnimationEnd(Animation animation) {
        if (completeListener != null) {
          completeListener.complete();
        }
      }
      @Override
      public void onAnimationRepeat(Animation animation) {}
      @Override
      public void onAnimationStart(Animation animation) {}
    });
    startAnimation(fadeOut);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    final float x = event.getX();
    final float y = event.getY();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        tracking = withinX(dot, x) && withinY(dot, y);
        return tracking || super.onTouchEvent(event);

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        tracking = false;
        if (isComplete()) {
          finishSlider();
        } else {
          slideDotHome();
        }
        return true;

      case MotionEvent.ACTION_MOVE:
        if (!tracking) {
          return super.onTouchEvent(event);
        }
        dot.offsetLeftAndRight((int) (x - dot.getLeft() - dot.getWidth()/2.0 ));

        if (isComplete()) {
          tracking = false;
          finishSlider();
          return true;
        }

        if (!withinY(dot, y)) {
          tracking = false;
          slideDotHome();
        } else {
          invalidate();
        }
        return true;

      default:
        return super.onTouchEvent(event);
    }
  }
}
