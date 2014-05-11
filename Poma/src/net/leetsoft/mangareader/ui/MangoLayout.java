package net.leetsoft.mangareader.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import net.leetsoft.mangareader.Mango;

public class MangoLayout extends RelativeLayout
{
    private int mLayoutHeight = -1;
    private int mLayoutWidth = -1;
    private boolean mNoBackground = false;
    public boolean mBgSet = false;
    public int mJpResourceId = -1;
    private View mJpOffsetView = null;
    private int mJpOffset = 0;
    private boolean mOutOfMemory = false;
    private boolean mFirstLayout = false;

    public MangoLayout(Context context)
    {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public MangoLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public MangoLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public void setNoBackground(boolean bg)
    {
        mNoBackground = bg;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLayoutWidth = MeasureSpec.getSize(widthMeasureSpec);
        mLayoutHeight = MeasureSpec.getSize(heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
        if (mJpOffsetView != null)
        {
            mJpOffset = mJpOffsetView.getTop() + mJpOffsetView.getHeight();
        }
        if (!mFirstLayout)
            setBackground();
        mFirstLayout = true;
    }

    public int getRealHeight()
    {
        return mLayoutHeight;
    }

    public int getRealWidth()
    {
        return mLayoutWidth;
    }

    public void setJpResourceId(int id)
    {
        mJpResourceId = id;
    }

    public View getJpVerticalOffsetView()
    {
        return mJpOffsetView;
    }

    public void setJpVerticalOffsetView(View v)
    {
        mJpOffsetView = v;
    }

    public void clearBackground()
    {
        mBgSet = false;
        this.setBackgroundDrawable(null);
    }

    public void setBackground()
    {
        if (!this.isInEditMode() && Mango.getSharedPreferences().getBoolean("invertTheme", false))
            this.setBackgroundColor(Color.BLACK);

        if (mBgSet || mNoBackground)
            return;
        if (mLayoutHeight == -1)
            return;
        if (Mango.getSharedPreferences() == null)
        {
            // this.setBackgroundResource(com.ls.manga.R.drawable.img_background_portrait);
            return;
        }
        if (Mango.getSharedPreferences().getBoolean("disableBackgrounds", false))
            return;

        // long time = System.currentTimeMillis();

        try
        {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
            {
                Canvas c = null;
                Bitmap screenBg = Mango.getMenuBackgroundPortrait().copy(Config.RGB_565, true);
                screenBg = Bitmap.createScaledBitmap(screenBg, this.getRealWidth(), this.getRealHeight(), true);

                /*
                if (mJpResourceId != -1)
                {
                    c = new Canvas(screenBg);
                    Bitmap jpBackground = BitmapFactory.decodeResource(getResources(), mJpResourceId);
                    int w = jpBackground.getWidth();
                    int h = jpBackground.getHeight();
                    Matrix mtx = new Matrix();
                    mtx.postRotate(90);
                    jpBackground = Bitmap.createBitmap(jpBackground, 0, 0, w, h, mtx, true);
                    Paint paint = new Paint();
                    int alphaVal = 180;
                    paint.setAlpha(alphaVal);
                    c.drawBitmap(jpBackground, mLayoutWidth - jpBackground.getWidth(), mJpOffset, paint);
                }*/
                this.setBackgroundDrawable(new BitmapDrawable(getResources(), screenBg));
            }
            else
            {
                Canvas c = null;
                Bitmap screenBg = Mango.getMenuBackgroundLandscape().copy(Config.RGB_565, true);

                screenBg = Bitmap.createScaledBitmap(screenBg, this.getRealWidth(), this.getRealHeight(), true);

                /*
                if (mJpResourceId != -1)
                {
                    c = new Canvas(screenBg);
                    Bitmap jpBackground = BitmapFactory.decodeResource(getResources(), mJpResourceId);
                    Paint paint = new Paint();
                    int alphaVal = 180;
                    paint.setAlpha(alphaVal);
                    c.drawBitmap(jpBackground, 0, mJpOffset, paint);
                }
                */
                this.setBackgroundDrawable(new BitmapDrawable(getResources(), screenBg));
            }
        } catch (OutOfMemoryError e)
        {
            if (mOutOfMemory)
            {
                Mango.log("Not drawing menu background because of OutOfMemoryError.");
                mBgSet = true;
                return;
            }
            else
            {
                Mango.log("OutOfMemoryError: Retrying background generation...");
                System.gc();
                mOutOfMemory = true;
                setBackground();
                return;
            }
        } catch (IllegalArgumentException e)
        {
            // obscure error where bitmap height/width is sometimes 0
            Mango.log("Not drawing menu background because of IllegalArgumentException. " + e.getMessage());
            mBgSet = true;
            return;
        } catch (NoSuchMethodError e)
        {
            return;
        }
        mBgSet = true;
    }
}
