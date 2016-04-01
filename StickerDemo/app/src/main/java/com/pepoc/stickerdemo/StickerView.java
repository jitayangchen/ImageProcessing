package com.pepoc.stickerdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Yangchen on 2015/9/7.
 */
public class StickerView extends View {

    /**
     * 最大放大倍数
     */
    public static final float MAX_SCALE_SIZE = 5.0f;
    public static final float MIN_SCALE_SIZE = 0.5f;

    private RectF mViewRect;

    private float mLastPointX, mLastPointY, deviation;

    private Bitmap mControllerBitmap, mDeleteBitmap, bgBitmap;
    private float mControllerWidth, mControllerHeight, mDeleteWidth, mDeleteHeight;
    private boolean mInController, mInMove;

    private boolean mInDelete = false;

//    private Sticker currentSticker;
    private List<Sticker> stickers = new ArrayList<Sticker>();

    /**
     * 焦点贴纸索引
     */
    private int focusStickerPosition = -1;

    public StickerView(Context context) {
        this(context, null);
    }

    public StickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

        mControllerBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_sticker_control);
        mControllerWidth = mControllerBitmap.getWidth();
        mControllerHeight = mControllerBitmap.getHeight();

        mDeleteBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_sticker_delete);
        mDeleteWidth = mDeleteBitmap.getWidth();
        mDeleteHeight = mDeleteBitmap.getHeight();

    }

    public void setWaterMark(Bitmap bitmap, Bitmap bgBitmap) {
        this.bgBitmap = bgBitmap;
        Point point = Utils.getDisplayWidthPixels(getContext());
        Sticker sticker = new Sticker(bitmap, point.x, point.x);
        stickers.add(sticker);
        focusStickerPosition = stickers.size() - 1;
        setFocusSticker(focusStickerPosition);
        postInvalidate();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bgBitmap, 0, 0, null);

        if (stickers.size() <= 0) {
            return;
        }

        for (int i = 0; i < stickers.size(); i++) {
            stickers.get(i).getmMatrix().mapPoints(stickers.get(i).getMapPointsDst(), stickers.get(i).getMapPointsSrc());
            canvas.drawBitmap(stickers.get(i).getBitmap(), stickers.get(i).getmMatrix(), null);
            if (stickers.get(i).isFocusable()) {
                canvas.drawLine(stickers.get(i).getMapPointsDst()[0], stickers.get(i).getMapPointsDst()[1], stickers.get(i).getMapPointsDst()[2], stickers.get(i).getMapPointsDst()[3], stickers.get(i).getmBorderPaint());
                canvas.drawLine(stickers.get(i).getMapPointsDst()[2], stickers.get(i).getMapPointsDst()[3], stickers.get(i).getMapPointsDst()[4], stickers.get(i).getMapPointsDst()[5], stickers.get(i).getmBorderPaint());
                canvas.drawLine(stickers.get(i).getMapPointsDst()[4], stickers.get(i).getMapPointsDst()[5], stickers.get(i).getMapPointsDst()[6], stickers.get(i).getMapPointsDst()[7], stickers.get(i).getmBorderPaint());
                canvas.drawLine(stickers.get(i).getMapPointsDst()[6], stickers.get(i).getMapPointsDst()[7], stickers.get(i).getMapPointsDst()[0], stickers.get(i).getMapPointsDst()[1], stickers.get(i).getmBorderPaint());

                canvas.drawBitmap(mControllerBitmap, stickers.get(i).getMapPointsDst()[4] - mControllerWidth / 2, stickers.get(i).getMapPointsDst()[5] - mControllerHeight / 2, null);
                canvas.drawBitmap(mDeleteBitmap, stickers.get(i).getMapPointsDst()[0] - mDeleteWidth / 2, stickers.get(i).getMapPointsDst()[1] - mDeleteHeight / 2, null);
            }
        }

    }

    /**
     * 是否在控制点区域
     * @param x
     * @param y
     * @return
     */
    private boolean isInController(float x, float y) {
        int position = 4;
        float rx = stickers.get(focusStickerPosition).getMapPointsDst()[position];
        float ry = stickers.get(focusStickerPosition).getMapPointsDst()[position + 1];
        RectF rectF = new RectF(rx - mControllerWidth / 2,
                ry - mControllerHeight / 2,
                rx + mControllerWidth / 2,
                ry + mControllerHeight / 2);
        if (rectF.contains(x, y)) {
            return true;
        }
        return false;

    }

    /**
     * 是否在删除点区域
     * @param x
     * @param y
     * @return
     */
    private boolean isInDelete(float x, float y) {
        int position = 0;
        float rx = stickers.get(focusStickerPosition).getMapPointsDst()[position];
        float ry = stickers.get(focusStickerPosition).getMapPointsDst()[position + 1];
        RectF rectF = new RectF(rx - mDeleteWidth / 2,
                ry - mDeleteHeight / 2,
                rx + mDeleteWidth / 2,
                ry + mDeleteHeight / 2);
        if (rectF.contains(x, y)) {
            return true;
        }
        return false;

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mViewRect == null) {
            mViewRect = new RectF(0f, 0f, getMeasuredWidth(), getMeasuredHeight());
        }

        if (stickers.size() <= 0 || focusStickerPosition < 0) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInController(x, y)) {
                    mInController = true;
                    mLastPointY = y;
                    mLastPointX = x;

                    float nowLenght = caculateLength(stickers.get(focusStickerPosition).getMapPointsDst()[0], stickers.get(focusStickerPosition).getMapPointsDst()[1]);
                    float touchLenght = caculateLength(x, y);
                    deviation = touchLenght - nowLenght;
                    break;
                }

                if (isInDelete(x, y)) {
                    mInDelete = true;
                    break;
                }

                if (isFocusSticker(x, y)) {
                    mLastPointY = y;
                    mLastPointX = x;
                    mInMove = true;
                    invalidate();
                } else {
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isInDelete(x, y) && mInDelete) {
                    doDeleteSticker();
                }
            case MotionEvent.ACTION_CANCEL:
                mLastPointX = 0;
                mLastPointY = 0;
                mInController = false;
                mInMove = false;
                mInDelete = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mInController) {
                    stickers.get(focusStickerPosition).getmMatrix().postRotate(rotation(event), stickers.get(focusStickerPosition).getMapPointsDst()[8], stickers.get(focusStickerPosition).getMapPointsDst()[9]);
                    float nowLenght = caculateLength(stickers.get(focusStickerPosition).getMapPointsDst()[0], stickers.get(focusStickerPosition).getMapPointsDst()[1]);
                    float touchLenght = caculateLength(x, y) - deviation;
                    if (Math.sqrt((nowLenght - touchLenght) * (nowLenght - touchLenght)) > 0.0f) {
                        float scale = touchLenght / nowLenght;
                        float nowsc = stickers.get(focusStickerPosition).getScaleSize() * scale;
                        if (nowsc >= MIN_SCALE_SIZE && nowsc <= MAX_SCALE_SIZE) {
                            stickers.get(focusStickerPosition).getmMatrix().postScale(scale, scale, stickers.get(focusStickerPosition).getMapPointsDst()[8], stickers.get(focusStickerPosition).getMapPointsDst()[9]);
                            stickers.get(focusStickerPosition).setScaleSize(nowsc);
                        }
                    }

                    invalidate();
                    mLastPointX = x;
                    mLastPointY = y;
                    break;

                }

                if (mInMove == true) {
                    float cX = x - mLastPointX;
                    float cY = y - mLastPointY;
                    mInController = false;

                    if (Math.sqrt(cX * cX + cY * cY) > 2.0f  && canStickerMove(cX, cY)) {
                        stickers.get(focusStickerPosition).getmMatrix().postTranslate(cX, cY);
                        postInvalidate();
                        mLastPointX = x;
                        mLastPointY = y;
                    }
                    break;
                }


                return true;

        }
        return true;
    }

    /**
     * 删除所有贴纸
     */
    private void doDeleteSticker() {
        stickers.remove(focusStickerPosition);
        focusStickerPosition = stickers.size() - 1;
        invalidate();
    }

    private boolean canStickerMove(float cx, float cy) {
        float px = cx + stickers.get(focusStickerPosition).getMapPointsDst()[8];
        float py = cy + stickers.get(focusStickerPosition).getMapPointsDst()[9];
        if (mViewRect.contains(px, py)) {
            return true;
        } else {
            return false;
        }
    }


    private float caculateLength(float x, float y) {
        return (float)Utils.lineSpace(x, y, stickers.get(focusStickerPosition).getMapPointsDst()[8], stickers.get(focusStickerPosition).getMapPointsDst()[9]);
    }


    private float rotation(MotionEvent event) {
        float originDegree = calculateDegree(mLastPointX, mLastPointY);
        float nowDegree = calculateDegree(event.getX(), event.getY());
        return nowDegree - originDegree;
    }

    private float calculateDegree(float x, float y) {
        double delta_x = x - stickers.get(focusStickerPosition).getMapPointsDst()[8];
        double delta_y = y - stickers.get(focusStickerPosition).getMapPointsDst()[9];
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    /**
     * 是否点击在贴纸区域
     * @param x
     * @param y
     * @return
     */
    private boolean isFocusSticker(float x, float y) {
        for (int i = stickers.size() - 1; i >= 0; i--) {
            Sticker sticker = stickers.get(i);
            if (isInContent(x, y, sticker)) {
                setFocusSticker(i);
                return true;
            }
        }
        setFocusSticker(-1);
        return false;
    }

    /**
     * 判断点是否在指定区域内
     * @param x
     * @param y
     * @return
     */
    private boolean isInContent(float x, float y, Sticker currentSticker) {
        // long startTime = System.currentTimeMillis();
        // float[] pointsDst = currentSticker.getMapPointsDst();
        // PointD pointF_1 = Utils.getMidpointCoordinate(pointsDst[0], pointsDst[1], pointsDst[2], pointsDst[3]);
        // double a1 = Utils.lineSpace(pointsDst[8], pointsDst[9], pointF_1.getX(), pointF_1.getY());
        // double b1 = Utils.lineSpace(pointsDst[8], pointsDst[9], x, y);
        // if (b1 <= a1) {
        //     return true;
        // }
        // double c1 = Utils.lineSpace(pointF_1.getX(), pointF_1.getY(), x, y);
        // double p1 = (a1 + b1 + c1) / 2;
        // double s1 = Math.sqrt(p1 * (p1 - a1) * (p1 - b1) * (p1 - c1));
        // double d1 = 2 * s1 / a1;
        // if (d1 > a1) {
        //     return false;
        // }

        // PointD pointF_2 = Utils.getMidpointCoordinate(pointsDst[2], pointsDst[3], pointsDst[4], pointsDst[5]);
        // double a2 = a1;
        // double b2 = b1;
        // double c2 = Utils.lineSpace(pointF_2.getX(), pointF_2.getY(), x, y);
        // double p2 = (a2 + b2 + c2) / 2;
        // double temp = p2 * (p2 - a2) * (p2 - b2) * (p2 - c2);
        // double s2 = Math.sqrt(temp);
        // double d2 = 2 * s2 / a2;
        // if (d2 > a1) {
        //     return false;
        // }
        // long endTime = System.currentTimeMillis();
        // long time = endTime - startTime;

        // if (d1 <= a1 && d2 <= a1) {
        //     return true;
        // }

        // return false;
        float[] pointsDst = currentSticker.getMapPointsDst();
        if (x > pointsDst[0] && x < pointsDst[2] && y > pointsDst[3] && y < pointsDst[5])
            return true;
        return false;
    }

    public void saveBitmapToFile() {
//        int bgWidth = bgBitmap.getWidth();
//        int bgHeight = bgBitmap.getHeight();
//        Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
//        Canvas cv = new Canvas(newbmp);
//        cv.drawBitmap(bgBitmap, 0, 0, null);
//        cv.drawBitmap(stickers.get(focusStickerPosition).getBitmap(), stickers.get(focusStickerPosition).getmMatrix(), null);
//        cv.save(Canvas.ALL_SAVE_FLAG);
//        cv.restore();
//        bgBitmap = newbmp;

        stickers.clear();
        focusStickerPosition = -1;
        invalidate();
    }

    /**
     * 设置焦点贴纸
     * @param position
     */
    private void setFocusSticker(int position) {
        int focusPosition = stickers.size() - 1;
        for (int i = 0; i < stickers.size(); i++) {
            if (i == position) {
                focusPosition = i;
                stickers.get(i).setFocusable(true);
            } else {
                stickers.get(i).setFocusable(false);
            }
        }
        Sticker sticker = stickers.remove(focusPosition);
        stickers.add(sticker);
        focusStickerPosition = stickers.size() - 1;
    }

}
