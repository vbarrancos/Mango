//package com.ls.manga.activities;
//
//import android.app.AlertDialog;
//import android.content.*;
//import android.content.pm.ActivityInfo;
//import android.content.res.Configuration;
//import android.database.SQLException;
//import android.graphics.Bitmap;
//import android.graphics.Bitmap.Config;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.os.*;
//import android.text.format.DateFormat;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewConfiguration;
//import android.view.WindowManager;
//import android.view.animation.Animation;
//import android.view.animation.AnimationSet;
//import android.view.animation.AnimationUtils;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//import com.actionbarsherlock.view.Menu;
//import com.actionbarsherlock.view.MenuInflater;
//import com.actionbarsherlock.view.MenuItem;
//import com.ls.manga.*;
//import com.ls.manga.activities.JumpToPageDialog.JumpToPageListener;
//import com.ls.manga.services.DownloaderService;
//import com.ls.manga.services.DownloaderService.DownloaderBinder;
//import com.ls.manga.ui.MangoTutorialHandler;
//import com.ls.manga.ui.MangoZoomableImageView;
//import com.mobclix.android.sdk.MobclixIABRectangleMAdView;
//import org.xml.sax.Attribute/**/s;
//import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;
//import org.xml.sax.XMLReader;
//import org.xml.sax.helpers.DefaultHandler;
//
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.parsers.SAXParser;
//import javax.xml.parsers.SAXParserFactory;
//import java.io.*;
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.Date;
//
//public class NewPagereaderActivity extends MangoActivity
//{
//    public static final int MODE_STREAMING = 0;
//    public static final int MODE_LIBRARY = 1;
//    public static final int MODE_FILESYSTEM = 2;
//
//    // UX components and views
//    private MangoZoomableImageView mPageView;
//    private LinearLayout mStatusbarContainer;
//    private TextView mStatusText;
//    private RelativeLayout mTitlebarContainer;
//    private TextView mTitlebarTextView;
//    private ImageView mAnimatorSlideView;
//    private ImageView mAnimatorStaticView;
//    private TextView mTitlebarClockView;
//    private ImageView mSoftMenuButton;
//
//    // Ad UI components
//    private RelativeLayout mAdOverlay;
//    private LinearLayout mAdContainer;
//    private TextView mAdLoaderTextView;
//
//    // misc helper variables (don't need to be persisted)
//    private AnimationSet mPulseAnimation;
//    private AnimationSet mSlideInAnimation;
//    private AnimationSet mSlideOffAnimation;
//    private boolean mAnimatePage;
//    private boolean mAnimateSlideIn;
//    private long mLastSetTitle;
//    private boolean mSkipRestore;
//    private String mCurrentPageImageUrl; // used for share intent
//    private boolean mActionBarVisible;
//
//    // state variables (must be persisted)
//    private int mReadingMode;
//    private boolean mAdVisible;
//    private Manga mActiveManga;
//    private LibraryChapter mActiveLibraryChapter;
//    private Page mPages;
//    private ArrayList<Page> mPageDownloadQueue;
//
//    private int mPageIndex;
//    private int mChapterIndex;
//    private String mChapterId;
//    private int mInitialPage;
//    private int mPendingPage;
//    private boolean mInstantiated;
//    private String mTitlebarText;
//    private String mSubstringStart;
//    private String mSubstringAltStart;
//    private String mImageUrlPrefix;
//    private boolean mBusy;
//    private XmlDownloader mXmlTask;
//    private ImageDownloader[] mLoaders;
//    private int mTrackReadingProgress; // 0=unchecked, 1=do track progress, 2=don't track
//
//    // when rotating, pack anything important in the class below and pass it
//    // along to the new activity
//    private class InstanceBundle
//    {
//        private Manga activeManga;
//        private LibraryChapter activeLibraryChapter;
//        private Page pages;
//        private ArrayList<Page> pageDownloadQueue;
//
//        private int readingMode;
//
//        private int pageIndex;
//        private int chapterIndex;
//        private String chapterId;
//        private int initialPage;
//        private int pendingPage;
//        private boolean instantiated;
//        private String titlebarText;
//        private String substringAltStart;
//        private String substringStart;
//        private String imageUrlPrefix;
//        private boolean busy;
//        private ImageDownloader[] loaders;
//        private XmlDownloader xmlTask;
//        private String statusText;
//        private int trackReadingProgress;
//        private boolean adVisible;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//
//        this.getSupportActionBar().setDisplayShowHomeEnabled(false);
//
//        // Start the long and arduous task of initializing the pagereader!
//        // Clear the menu background image cache to free up some memory
//        Mango.recycleMenuBackgrounds();
//
//        // Apply some user prefs that must be set prior to adding UI elements
//        if (Mango.getSharedPreferences().getBoolean("fullscreenReading", false))
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        if (Mango.getSharedPreferences().getString("pagereaderOrientation", "-1").equals("1"))
//            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        else if (Mango.getSharedPreferences().getString("pagereaderOrientation", "-1").equals("2"))
//            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//
//        // Assign UI member variables to their respective views
//        inflateLayoutManager(this, R.layout.pagereader);
//        super.setNoBackground(true);
//        mPageView = (MangoZoomableImageView) findViewById(R.id.prPagefield2);
//        mStatusbarContainer = (LinearLayout) findViewById(R.id.prStatusbar);
//        mStatusText = (TextView) findViewById(R.id.prStatusText);
//        mTitlebarContainer = (RelativeLayout) findViewById(R.id.prTitlebar);
//        mTitlebarTextView = (TextView) findViewById(R.id.prTitleText);
//        mAnimatorSlideView = (ImageView) findViewById(R.id.prAnimationView);
//        mAnimatorStaticView = (ImageView) findViewById(R.id.prAnimationStaticView);
//        mTitlebarClockView = (TextView) findViewById(R.id.prTitleSystemText);
//        mAdOverlay = (RelativeLayout) findViewById(R.id.prAdContainer);
//        mAdOverlay.setVisibility(View.GONE);
//        mSoftMenuButton = (ImageView) findViewById(R.id.prMenuSoftButton);
//        mSoftMenuButton.bringToFront();
//        mSoftMenuButton.setOnClickListener(new OnClickListener()
//        {
//
//            @Override
//            public void onClick(View v)
//            {
//                NewPagereaderActivity.this.openOptionsMenu();
//            }
//        });
//
//        // Actionbar Init
//
//        if (Build.VERSION.SDK_INT >= 11)
//            mPageView.setSystemUiVisibility(1);
//        mActionBarVisible = true;
//        mSoftMenuButton.setVisibility(View.VISIBLE);
//        this.getSupportActionBar().show();
//
//        if (Build.VERSION.SDK_INT < 11)
//        {
//            // Pre-Honeycomb
//            mActionBarVisible = false;
//            this.getSupportActionBar().hide();
//            mSoftMenuButton.setVisibility(View.GONE);
//        }
//        else
//        {
//            // Post-Honeycomb
//            // Tablets
//            if ((this.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE)
//            {
//                if (Mango.getSharedPreferences().getBoolean("fullscreenReading", false))
//                {
//                    mActionBarVisible = false;
//                    this.getSupportActionBar().hide();
//                    mSoftMenuButton.setVisibility(View.VISIBLE);
//                    if (Build.VERSION.SDK_INT >= 14)
//                        mSoftMenuButton.setVisibility((ViewConfiguration.get(this).hasPermanentMenuKey() ? View.GONE : View.VISIBLE));
//                }
//                else
//                    mSoftMenuButton.setVisibility(View.GONE);
//            }
//            else
//            // Phones
//            {
//                mActionBarVisible = false;
//                this.getSupportActionBar().hide();
//                mSoftMenuButton.setVisibility(View.VISIBLE);
//                if (Build.VERSION.SDK_INT >= 14)
//                    mSoftMenuButton.setVisibility((ViewConfiguration.get(this).hasPermanentMenuKey() ? View.GONE : View.VISIBLE));
//            }
//        }
//
//        if (Mango.getSharedPreferences().getBoolean("suppressMenuButton", false))
//            mSoftMenuButton.setVisibility(View.GONE);
//
//        // Title bar and status bar setup
//        mStatusbarContainer.setVisibility(View.INVISIBLE);
//        mStatusbarContainer.bringToFront();
//        mTitlebarContainer.setVisibility(View.INVISIBLE);
//
//        // Set up animations and their respective views
//        mPulseAnimation = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.anim_pulse);
//        mPulseAnimation.setRepeatMode(Animation.REVERSE);
//        mPulseAnimation.setRepeatCount(Animation.INFINITE);
//        mSlideInAnimation = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.pageslidein);
//        mSlideOffAnimation = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.pageslideoff);
//        mAnimatorSlideView.setVisibility(View.INVISIBLE);
//        mAnimatorStaticView.setVisibility(View.INVISIBLE);
//        mAnimatorSlideView.setAnimation(mSlideInAnimation);
//
//        setTouchControlCallbacks();
//
//        // Set next page callback for animations and zoom initialization
//        mPageView.setOnUpdateCallback(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                newPageCallback();
//            }
//        });
//
//        // Show tutorial on first run
//        if (Mango.getSharedPreferences().getInt("pagereaderShowTutorial", 0) != 2)
//        {
//            Intent myIntent = new Intent();
//            myIntent.setClass(Mango.CONTEXT, TutorialActivity.class);
//            startActivity(myIntent);
//        }
//
//        if (Mango.getSharedPreferences().getBoolean("keepScreenOn", false))
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        // Apply some saved state variables in the case of a screen rotation
//        if (getLastCustomNonConfigurationInstance() != null)
//        {
//            mSkipRestore = true;
//            InstanceBundle save = (InstanceBundle) getLastCustomNonConfigurationInstance();
//            mActiveManga = save.activeManga;
//            mActiveLibraryChapter = save.activeLibraryChapter;
//            mPages = save.pages;
//            mPageDownloadQueue = save.pageDownloadQueue;
//
//            mReadingMode = save.readingMode;
//
//            mBusy = save.busy;
//            mChapterIndex = save.chapterIndex;
//            mChapterId = save.chapterId;
//            mImageUrlPrefix = save.imageUrlPrefix;
//            mInitialPage = save.initialPage;
//            mInstantiated = save.instantiated;
//            mLoaders = save.loaders;
//            mPageIndex = save.pageIndex;
//            mPendingPage = save.pendingPage;
//            mSubstringStart = save.substringStart;
//            mSubstringAltStart = save.substringAltStart;
//            mTitlebarText = save.titlebarText;
//            mXmlTask = save.xmlTask;
//            mAdVisible = save.adVisible;
//            if (mAdVisible)
//            {
//                showInterstitialAd();
//                if (mInstantiated)
//                    adContinueActivate();
//            }
//
//            mTrackReadingProgress = save.trackReadingProgress;
//            setTitle(mActiveManga.chapters[mChapterIndex].title);
//
//            checkReadingProgress();
//
//            if (save.statusText != null && save.statusText.length() > 0)
//                setStatus(save.statusText);
//
//            for (int i = 0; i < mLoaders.length; i++)
//            {
//                if (mLoaders[i] != null)
//                    mLoaders[i].setReference(NewPagereaderActivity.this);
//            }
//
//            if (mXmlTask != null)
//                mXmlTask.attach(this);
//            save = null;
//
//            mAnimatePage = false;
//
//            if (mInstantiated)
//            {
//                Bitmap bm = MangoCache.readBitmapFromCache("page/", generateFileName(mActiveManga.chapters[mChapterIndex].pages[mPageIndex].id), 1);
//                if (bm == null)
//                {
//                    Intent intent = new Intent(getIntent());
//                    intent.putExtra("initialpage", (mPageIndex == -1 ? mInitialPage : mPageIndex));
//                    intent.putExtra("chapterid", mChapterId);
//                    setIntent(intent);
//                    restartApp();
//                    return;
//                }
//                mPageView.setImageBitmap(bm);
//
//                if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                    mPageView.scrollToTopLeftCorner();
//                else
//                    mPageView.scrollToTopRightCorner();
//            }
//            return;
//        }
//
//        // Warn if cache is not accessible
//        String state = Environment.getExternalStorageState();
//        if (!state.startsWith(Environment.MEDIA_MOUNTED))
//        {
//            Mango.log("SD card isn't available, cannot display any images. (state=" + state + ")");
//            Mango.alert(
//                    "Mango cannot access the SD card. This might be because you have connected your device to a computer and turned on USB Mass Storage mode.\n\nMango cannot display any pages until it can access the SD card again.",
//                    "Warning", this);
//        }
//
//        if (savedInstanceState != null)
//            return;
//
//        // Finally, open our chapter, but only if we are not initializing from a saved bundle
//        Bundle arguments = getIntent().getExtras();
//
//        mReadingMode = arguments.getInt("readingmode");
//
//        if (mReadingMode == MODE_STREAMING)
//        {
//
//            super.logEvent("Read Chapter", null);
//        }
//        else if (mReadingMode == MODE_LIBRARY)
//        {
//
//            super.logEvent("Read Library Chapter", null);
//        }
//        else
//        {
//            mReadingMode = MODE_FILESYSTEM;
//            super.logEvent("Read Filesystem Chapter", null);
//        }
//
//
//
//        openChapter((Manga) arguments.getSerializable("manga"), arguments.getString("chapterid"), arguments.getInt("initialpage"));
//
//
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent)
//    {
//        setIntent(intent);
//        Bundle arguments = getIntent().getExtras();
//        openChapter((Manga) arguments.getSerializable("manga"), arguments.getString("chapterid"), arguments.getInt("initialpage"));
//    }
//
//    @Override
//    public Object onRetainCustomNonConfigurationInstance()
//    {
//        InstanceBundle save = new InstanceBundle();
//        save.activeManga = mActiveManga;
//        save.activeManga.chapters = mActiveManga.chapters;
//        save.activeManga.chapters[mChapterIndex].pages = mActiveManga.chapters[mChapterIndex].pages;
//        save.busy = mBusy;
//        save.chapterIndex = mChapterIndex;
//        save.chapterId = mChapterId;
//        save.imageUrlPrefix = mImageUrlPrefix;
//        save.initialPage = mInitialPage;
//        save.instantiated = mInstantiated;
//        save.loaders = mLoaders;
//        save.pageIndex = mPageIndex;
//        save.pendingPage = mPendingPage;
//        save.substringStart = mSubstringStart;
//        save.substringAltStart = mSubstringAltStart;
//        save.titlebarText = mTitlebarText;
//        save.xmlTask = mXmlTask;
//        save.adVisible = mAdVisible;
//        save.trackReadingProgress = mTrackReadingProgress;
//        if (mStatusbarContainer.getVisibility() == View.VISIBLE)
//            save.statusText = (String) mStatusText.getText();
//        if (mXmlTask != null)
//            mXmlTask.detach();
//        mActiveManga = null;
//        return save;
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle save)
//    {
//        super.onSaveInstanceState(save);
//        save.putSerializable("manga", mActiveManga);
//        save.putInt("page", mPageIndex);
//        save.putString("chapter", mChapterId);
//    }
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState)
//    {
//        super.onRestoreInstanceState(savedInstanceState);
//        if (mSkipRestore)
//            return;
//        mActiveManga = (Manga) savedInstanceState.get("manga");
//        openChapter(mActiveManga, savedInstanceState.getString("chapter"), savedInstanceState.getInt("page"));
//    }
//
//    private void openChapter(Manga manga, String chapterId, int pageIndex)
//    {
//        openChapter(manga, chapterId, pageIndex, false);
//    }
//
//    private void openChapter(Manga manga, String chapterId, int pageIndex, boolean refreshCache)
//    {
//        try
//        {
//            MangoCache.wipeImageCache(false);
//            if (mActiveManga != null) // clear out page information for the last chapter
//            {
//                mActiveManga.chapters[mChapterIndex].pages = null;
//                cancelPageLoad(mPendingPage);
//            }
//
//            mSubstringStart = null;
//            mSubstringAltStart = null;
//            mInitialPage = pageIndex;
//            mActiveManga = manga;
//            mChapterId = chapterId;
//            mChapterIndex = -1;
//            for (int i = 0; i < manga.chapters.length; i++)
//            {
//                if (manga.chapters[i].id.equals(chapterId))
//                    mChapterIndex = i;
//            }
//            if (mChapterIndex == -1)
//                mChapterIndex = manga.chapters.length - 1;
//            mPageIndex = -1;
//            mPageView.setImageBitmap(null);
//            mPendingPage = -1;
//            mLoaders = new ImageDownloader[Integer.parseInt(Mango.getSharedPreferences().getString("preloaders", "3"))];
//            for (int i = 0; i < mLoaders.length; i++)
//            {
//                mLoaders[i] = new ImageDownloader(NewPagereaderActivity.this);
//            }
//            mInstantiated = false;
//            clearStatus();
//            setTitle(mActiveManga.chapters[mChapterIndex].title);
//            setTitlebarText(mActiveManga.title + " " + mActiveManga.chapters[mChapterIndex].id, true);
//            addRecent();
//            checkReadingProgress();
//            setStatus("Downloading pagelist...");
//            System.gc();
//            mXmlTask = new XmlDownloader(this);
//            String url = "http://%SERVER_URL%/getpages.aspx?pin=" + Mango.getPin() + "&mangaid=" + mActiveManga.id + "&chapterid=" + mActiveManga.chapters[mChapterIndex].url + "&site="
//                    + Mango.getSiteId();
//            if (refreshCache)
//                url += "&skipcache=true";
//            mXmlTask.execute(url);
//
//            Mango.getSharedPreferences().edit().putInt("chaptersRead", Mango.getSharedPreferences().getInt("chaptersRead", 0) + 1).commit();
//        }
//        catch (Exception ex)
//        {
//            Mango.alert(
//                    "Mango encountered an error while trying to load this chapter. Please try again.\n\nIf you are loading the chapter from a bookmark, try browsing to it from the All Manga list instead.",
//                    NewPagereaderActivity.this);
//            Mango.log(ex.toString() + " in openChapter.");
//            cleanup();
//            finish();
//        }
//        showInterstitialAd();
//    }
//
//    public void showInterstitialAd()
//    {
//        if (Mango.DISABLE_ADS)
//            return;
//
//        File file;
//        BufferedReader br = null;
//        try
//        {
//            file = new File(this.getFilesDir() + "/adtimer.txt");
//            if (file.exists())
//            {
//                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
//                String l = br.readLine();
//                if (Math.abs(System.currentTimeMillis() - Long.parseLong(l)) < 1000 * 60 * 90)
//                    return;
//            }
//        }
//        catch (Exception e)
//        {
//            Mango.log("Adtimer problem. " + e.toString());
//            return;
//        }
//        finally
//        {
//            try
//            {
//                if (br != null)
//                    br.close();
//                br = null;
//            }
//            catch (IOException e)
//            {
//
//            }
//        }
//
//        try
//        {
//            if (mAdContainer == null)
//            {
//                View.inflate(this, R.layout.ad_overlay, mAdOverlay);
//                mAdContainer = (LinearLayout) mAdOverlay.findViewById(R.id.prAdBanner);
//                TextView mAdExplainTextView = (TextView) mAdOverlay.findViewById(R.id.prAdExplainText);
//                mAdExplainTextView.setOnClickListener(new OnClickListener()
//                {
//
//                    @Override
//                    public void onClick(View v)
//                    {
//                        adExplainClicked();
//                    }
//                });
//                mAdLoaderTextView = (TextView) mAdOverlay.findViewById(R.id.prAdTextview);
//                mAdLoaderTextView.setOnClickListener(new OnClickListener()
//                {
//
//                    @Override
//                    public void onClick(View v)
//                    {
//                        adContinueClicked();
//                    }
//                });
//            }
//
//            mAdContainer.removeAllViews();
//
//            mAdContainer.setVisibility(View.VISIBLE);
//            MobclixIABRectangleMAdView ad = new MobclixIABRectangleMAdView(this);
//            mAdContainer.addView(ad);
//
//            mAdLoaderTextView.setText("Loading chapter...");
//            mAdLoaderTextView.startAnimation(mPulseAnimation);
//
//            mAdOverlay.setVisibility(View.VISIBLE);
//            mAdOverlay.bringToFront();
//            mAdVisible = true;
//        }
//        catch (Exception e)
//        {
//            Mango.log("showInterstitialAd crashed. " + Log.getStackTraceString(e));
//        }
//    }
//
//    public void adContinueActivate()
//    {
//        mAdLoaderTextView.setText("Tap here to start reading!");
//        mAdLoaderTextView.clearAnimation();
//    }
//
//    public void adContinueClicked()
//    {
//        if (!mInstantiated)
//            return;
//        try
//        {
//            File f = new File(this.getFilesDir() + "/adtimer.txt");
//            BufferedWriter out = null;
//
//            if (f.exists())
//                f.delete();
//            f.getParentFile().mkdirs();
//            f.createNewFile();
//            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
//            out.write(String.valueOf(System.currentTimeMillis()));
//            out.close();
//        }
//        catch (Exception e)
//        {
//
//        }
//
//        mAdOverlay.setVisibility(View.GONE);
//        Animation fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);
//        fadeout.setDuration(300);
//        mAdOverlay.startAnimation(fadeout);
//        mAdVisible = false;
//    }
//
//    public void adExplainClicked()
//    {
//        Mango.alert(
//                "These ads allow you to use Mango for free by helping me pay for the server bill (about $150 per month).  You're also helping me pay for part of my college tuition every semester, so thanks!\n\nThese larger ads only appear once every 90 minutes.  To get rid of them, please consider upgrading to Bankai!",
//                this);
//    }
//
//    @Override
//    public void onPause()
//    {
//        super.onPause();
//    }
//
//    @Override
//    public void onResume()
//    {
//        super.onResume();
//    }
//
//    @Override
//    public void onDestroy()
//    {
//        cleanup();
//        super.onDestroy();
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        MenuInflater inflater = getSupportMenuInflater();
//        inflater.inflate(R.menu.pagereadermenu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public void openOptionsMenu()
//    {
//        if (mInstantiated)
//            setTitlebarText(mActiveManga.title + " " + mActiveManga.chapters[mChapterIndex].id + " page " + mActiveManga.chapters[mChapterIndex].pages[mPageIndex].id + " (" + (mPageIndex + 1) + "/"
//                    + mActiveManga.chapters[mChapterIndex].pages.length + ")", true);
//        super.openOptionsMenu();
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu)
//    {
//        if (menu == null || mActiveManga == null)
//            return false;
//
//        MangoSqlite db = new MangoSqlite(this);
//        db.open();
//        Favorite f = db.getFavoriteForManga(mActiveManga);
//        db.close();
//        if (mTrackReadingProgress == 3)
//        {
//            menu.getItem(0).setIcon(R.drawable.ic_action_add);
//            menu.getItem(0).setTitle("Add Favorite");
//            menu.getItem(0).setTitleCondensed("Add Favorite");
//        }
//        else if (mTrackReadingProgress == 2 || !isReadingProgressBehind(f))
//        {
//            menu.getItem(0).setIcon(R.drawable.ic_action_reset);
//            menu.getItem(0).setTitle("Set Progress Here");
//            menu.getItem(0).setTitleCondensed("Reset Progress");
//        }
//        else if (mTrackReadingProgress == 1)
//        {
//            menu.getItem(0).setIcon(R.drawable.ic_action_remove);
//            menu.getItem(0).setTitle("Remove Favorite");
//            menu.getItem(0).setTitleCondensed("Remove Favorite");
//            menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER); // DOES NOT WORK PROPERLY!!!
//        }
//
//        if (!mActionBarVisible)
//        {
//            for (int i = 0; i < menu.size(); i++)
//            {
//                menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//            }
//        }
//
//        return super.onPrepareOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        if (item.getItemId() == R.id.menuAddBookmark)
//        {
//            super.logEvent("Add Favorite (Pagereader)", null);
//            addRevertFavorite();
//            return true;
//        }
//        else if (item.getItemId() == R.id.menuShare)
//        {
//            launchShareIntent();
//            return true;
//        }
//        else if (item.getItemId() == R.id.menuJumpToPage)
//        {
//            super.logEvent("Jump-to-Page", null);
//            JumpToPageDialog jumptopage = new JumpToPageDialog(NewPagereaderActivity.this, new JumpToPageListener()
//            {
//                @Override
//                public void jumpToPageCallback(int selection)
//                {
//                    NewPagereaderActivity.this.goToPage(selection);
//                }
//            });
//            jumptopage.show();
//            jumptopage.initializeAdapter(mActiveManga.id, mChapterId, mActiveManga.chapters[mChapterIndex].pages, mPageIndex, false, false);
//            return true;
//        }
//        else if (item.getItemId() == R.id.menuBookmarksLst)
//        {
//            Intent myIntent = new Intent();
//            myIntent.setClass(Mango.CONTEXT, FavoritesActivity.class);
//            myIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            startActivity(myIntent);
//        }
//        else if (item.getItemId() == R.id.menuChapterList)
//        {
//            Intent chaptersIntent = new Intent();
//            chaptersIntent.setClass(Mango.CONTEXT, ChaptersActivity.class);
//            Manga argManga = new Manga();
//            argManga.bookmarked = mActiveManga.bookmarked;
//            argManga.id = mActiveManga.id;
//            argManga.title = mActiveManga.title;
//            chaptersIntent.putExtra("manga", argManga);
//            chaptersIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            startActivity(chaptersIntent);
//        }
//        else if (item.getItemId() == R.id.menuRefreshPage)
//        {
//            if (!mInstantiated || mBusy)
//                return super.onOptionsItemSelected(item);
//            MangoCache.wipeImageCache(true);
//            mPageView.setImageBitmap(null);
//            mPageView.invalidate();
//            goToPage(mPageIndex);
//        }
//        else if (item.getItemId() == R.id.menuNextChapter)
//        {
//            if (mChapterIndex == mActiveManga.chapters.length - 1)
//            {
//                Mango.alert("This is the last available chapter of this manga!", this);
//                return super.onOptionsItemSelected(item);
//            }
//            openChapter(mActiveManga, mActiveManga.chapters[mChapterIndex + 1].id, 0);
//        }
//        else if (item.getItemId() == R.id.menuPreviousChapter)
//        {
//            if (mChapterIndex == 0)
//            {
//                Mango.alert("This is the first chapter of this manga!", this);
//                return super.onOptionsItemSelected(item);
//            }
//            openChapter(mActiveManga, mActiveManga.chapters[mChapterIndex - 1].id, 0);
//        }
//        else if (item.getItemId() == R.id.menuSavePage)
//        {
//            if (MangoCache.copyPageToUserFolder(generateFileName(mActiveManga.chapters[mChapterIndex].pages[mPageIndex].id), mActiveManga.id + "_" + mChapterId + "_" + (mPageIndex + 1) + ".jpg") == true)
//                Mango.alert("This page was saved to your SD card. You can find it in the /Mango/user/ folder.", this);
//            else
//                Mango.alert("Mango wasn't able to save the page to the SD card.  :'(", this);
//        }
//        else if (item.getItemId() == R.id.menuSaveToLibrary)
//        {
//            ServiceConnection sConnection = new ServiceConnection()
//            {
//
//                @Override
//                public void onServiceConnected(ComponentName className, IBinder service)
//                {
//                    DownloaderBinder binder = (DownloaderBinder) service;
//                    binder.getService().addToQueue(mActiveManga, mChapterId, Mango.getSiteId(), false);
//                    unbindService(this);
//                }
//
//                @Override
//                public void onServiceDisconnected(ComponentName arg0)
//                {}
//            };
//            startService(new Intent(NewPagereaderActivity.this, DownloaderService.class));
//            bindService(new Intent(NewPagereaderActivity.this, DownloaderService.class), sConnection, Context.BIND_AUTO_CREATE);
//
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void addRevertFavorite()
//    {
//        MangoSqlite db = new MangoSqlite(this);
//        db.open();
//        Favorite f = db.getFavoriteForManga(mActiveManga);
//        if (f == null)
//        {
//            f = new Favorite();
//            f.coverArtUrl = mActiveManga.coverart;
//            f.isOngoing = !mActiveManga.completed;
//            f.mangaId = mActiveManga.id;
//            f.mangaTitle = mActiveManga.title;
//            f.generateSimpleName();
//            f.notificationsEnabled = false;
//            f.siteId = Mango.getSiteId();
//            db.insertFavorite(f);
//            mTrackReadingProgress = 0;
//            checkReadingProgress();
//            updateReadingProgress();
//            Mango.alert(
//                    f.mangaTitle
//                            + " has been added to your Favorites!\n\nMango will keep track of your progress as you read. Later, you can quickly resume where you left off by going to the Favorites screen.",
//                    NewPagereaderActivity.this);
//        }
//        else
//        {
//            if (mTrackReadingProgress == 2 || !isReadingProgressBehind(f))
//            {
//                String reason = "oops, reason text is uninitialized! :o";
//                if (f.siteId != Mango.getSiteId())
//                    reason = "Mango isn't updating your progress because you're reading on a different manga site.";
//                else if (isReadingProgressBehind(f))
//                    reason = "Mango isn't updating your progress because you've skipped too far ahead of your saved progress.";
//                else if (!isReadingProgressBehind(f))
//                    reason = "Mango isn't updating your progress because you're reading behind your saved progress.";
//                AlertDialog alert = new AlertDialog.Builder(NewPagereaderActivity.this).create();
//                alert.setTitle("Set Reading Progress Here?");
//                alert.setMessage(reason + "\n\nWould you like to manually set your reading progress to this page?");
//                alert.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {
//                        updateReadingProgress(true);
//                        mTrackReadingProgress = 0;
//                        checkReadingProgress();
//                    }
//                });
//                alert.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {}
//                });
//                alert.show();
//            }
//            else
//            {
//                AlertDialog alert = new AlertDialog.Builder(NewPagereaderActivity.this).create();
//                alert.setTitle("Remove Favorite?");
//                alert.setMessage("This manga will be deleted from your Favorites and Mango will stop tracking your reading progress.\n\nAre you certain you wish to delete this favorite?");
//                alert.setButton(DialogInterface.BUTTON_POSITIVE, "Yes, delete", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {
//                        MangoSqlite db = new MangoSqlite(NewPagereaderActivity.this);
//                        db.open();
//                        db.deleteFavorite(db.getFavoriteForManga(mActiveManga).rowId);
//                        db.close();
//                        Mango.alert("This manga has been removed from your favorites. Mango will not track your reading progress.", NewPagereaderActivity.this);
//                        mTrackReadingProgress = 0;
//                        checkReadingProgress();
//                    }
//                });
//                alert.setButton(DialogInterface.BUTTON_NEGATIVE, "No, never mind!", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {}
//                });
//                alert.show();
//            }
//        }
//        db.close();
//    }
//
//    private void callback(String data)
//    {
//        if (data.startsWith("Exception"))
//        {
//            Mango.alert("Mango was unable to fetch the requested data.\n\nPlease try again in a moment or try another manga source.\n\n<strong>Error Details:</strong>\n" + data, "Connectivity Error", this,
//                    new DialogInterface.OnClickListener()
//                    {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which)
//                        {
//                            clearStatus();
//                            finish();
//                            return;
//                        }
//                    });
//            return;
//        }
//        if (data.startsWith("error"))
//        {
//            Mango.alert("The Mango Service gave the following error:\n\n" + data, "Error", this, new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    clearStatus();
//                    finish();
//                    return;
//                }
//            });
//            return;
//        }
//        parseXml(data);
//    }
//
//    private void parseXml(String data)
//    {
//        ArrayList<Page> pageArrayList = new ArrayList<Page>();
//
//        try
//        {
//            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
//            SAXParser parser = saxFactory.newSAXParser();
//            XMLReader reader = parser.getXMLReader();
//            PagelistSaxHandler handler = new PagelistSaxHandler();
//            reader.setContentHandler(handler);
//            reader.parse(new InputSource(new StringReader(data)));
//            pageArrayList.addAll(handler.getAllPages());
//            mSubstringStart = handler.getSubstringStart();
//            mSubstringStart.replace("&quot;", "\"");
//            mSubstringStart.replace("&amp;", "&");
//            mSubstringStart.replace("&lt;", "<");
//            mSubstringStart.replace("&gt;", ">");
//            mSubstringAltStart = handler.getSubstringAltStart();
//            mSubstringAltStart.replace("&quot;", "\"");
//            mSubstringAltStart.replace("&amp;", "&");
//            mSubstringAltStart.replace("&lt;", "<");
//            mSubstringAltStart.replace("&gt;", ">");
//            mImageUrlPrefix = handler.getImageUrlPrefix();
//        }
//        catch (SAXException ex)
//        {
//            Mango.alert("Mango wasn't able process the XML for the following reason:\n\n" + ex.toString(), "Malformed XML! :'(", this);
//            return;
//        }
//        catch (ParserConfigurationException e)
//        {
//            e.printStackTrace();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//
//        mActiveManga.chapters[mChapterIndex].pages = new Page[pageArrayList.size()];
//        pageArrayList.toArray(mActiveManga.chapters[mChapterIndex].pages);
//        pageArrayList = null;
//        if (mInitialPage > mActiveManga.chapters[mChapterIndex].pages.length)
//            mInitialPage = mActiveManga.chapters[mChapterIndex].pages.length - 1;
//
//        goToPage(mInitialPage);
//
//        if (!Mango.getSharedPreferences().getBoolean("tutorial" + MangoTutorialHandler.READER + "Done", false))
//            MangoTutorialHandler.startTutorial(MangoTutorialHandler.READER, this);
//    }
//
//    public class PagelistSaxHandler extends DefaultHandler
//    {
//        ArrayList<Page> allPages;
//        Page currentPage;
//        String baseUrl;
//        String substringStart = "";
//        String substringAltStart = "";
//
//        public ArrayList<Page> getAllPages()
//        {
//            return this.allPages;
//        }
//
//        public String getImageUrlPrefix()
//        {
//            return baseUrl;
//        }
//
//        public String getSubstringStart()
//        {
//            return substringStart;
//        }
//
//        public String getSubstringAltStart()
//        {
//            return substringAltStart;
//        }
//
//        @Override
//        public void startDocument() throws SAXException
//        {
//            super.startDocument();
//            allPages = new ArrayList<Page>();
//        }
//
//        @Override
//        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
//        {
//            super.startElement(uri, localName, name, attributes);
//            if (localName.equalsIgnoreCase("page"))
//            {
//                currentPage = new Page();
//            }
//            else if (localName.equalsIgnoreCase("url"))
//            {
//                if (baseUrl == null)
//                    baseUrl = attributes.getValue(0);
//                else
//                    currentPage.id = attributes.getValue(0);
//            }
//            else if (localName.equalsIgnoreCase("alturl"))
//            {
//                currentPage.url = attributes.getValue(0);
//            }
//            else if (localName.equalsIgnoreCase("string"))
//            {
//                substringStart = attributes.getValue(0);
//            }
//            else if (localName.equalsIgnoreCase("string2"))
//            {
//                substringAltStart = attributes.getValue(0);
//            }
//        }
//
//        @Override
//        public void endElement(String uri, String localName, String name) throws SAXException
//        {
//            super.endElement(uri, localName, name);
//            if (currentPage != null)
//            {
//                if (localName.equalsIgnoreCase("page"))
//                {
//                    allPages.add(currentPage);
//                }
//            }
//        }
//    }
//
//    public void cleanup()
//    {
//        try
//        {
//            if (mXmlTask != null)
//                mXmlTask.detach();
//            mPageView.unregisterCallbacks();
//            mPageView.setOnCreateContextMenuListener(null);
//            mPageView.removeCallbacks(mTitlebarHideRunnable);
//            mPageView.setImageBitmap(null);
//        }
//        catch (Exception ex)
//        {
//
//        }
//        finally
//        {
//            mTitlebarHideRunnable = null;
//            mPageView = null;
//            mBusy = false;
//            System.gc();
//        }
//    }
//
//    class ImageDownloader
//    {
//        int currentChapter = -1;
//        int currentIndex = -1;
//        int retries = 0;
//        private Page currentPage;
//        private BitmapDownloader bitmapTask;
//        private HtmlDownloader htmlTask;
//        private WeakReference<NewPagereaderActivity> activity;
//        private String currentUrl;
//        private boolean modifiedUrl;
//
//        public ImageDownloader(NewPagereaderActivity context)
//        {
//            activity = new WeakReference<NewPagereaderActivity>(context);
//        }
//
//        public void setReference(NewPagereaderActivity context)
//        {
//            Mango.log("Loader 0x" + Integer.toHexString(this.hashCode()) + ": Changing PagereaderActivity reference from 0x" + Integer.toHexString(activity.get().hashCode()) + " to 0x"
//                    + Integer.toHexString(context.hashCode()));
//            activity = new WeakReference<NewPagereaderActivity>(context);
//        }
//
//        public void downloadImage(String url, Page page, int index, int chapter)
//        {
//            currentChapter = chapter;
//            currentIndex = index;
//            currentPage = page;
//            if (!modifiedUrl)
//            {
//                modifiedUrl = true;
//
//                // MangaFox requires that we use Page.url instead of Page.id.
//                // Mangable usually works fine if we just use Page.id + ".jpg", but sometimes
//                // we need to use Page.url. To determine if this is the case, we'll just check
//                // to see if mSubstringStart is an empty string.
//
//                if (!mSubstringStart.equals(""))
//                    url = activity.get().mImageUrlPrefix + currentPage.url;
//                else
//                    url += ".jpg";
//            }
//            currentUrl = url;
//            if (!currentUrl.toLowerCase().endsWith("jpg") && !currentUrl.toLowerCase().endsWith("png") && !currentUrl.toLowerCase().endsWith("gif"))
//            {
//                htmlTask = new HtmlDownloader(this);
//                htmlTask.execute(currentUrl);
//            }
//            else
//            {
//                bitmapTask = new BitmapDownloader(this);
//                bitmapTask.execute(currentUrl);
//            }
//        }
//
//        public void callbackHtml(String data)
//        {
//            if (data.startsWith("Exception"))
//            {
//                Mango.log("EXCEPTION: Pagereader callbackHtml (index " + String.valueOf(currentIndex) + ") >> " + data);
//                retries++;
//                if (currentIndex == activity.get().mPendingPage && retries >= 3)
//                {
//                    Mango.alert("Mango couldn't download page " + currentPage.id + " after three attempts. Try again in a moment, or switch to another manga site.\n\n" + data
//                            + "\nAttempted URL: " + currentUrl + "\n\n", NewPagereaderActivity.this);
//                    activity.get().displayImage(null, currentIndex);
//                    activity.get().cancelPageLoad(currentIndex);
//                    currentIndex = -1;
//                    retries = 0;
//                    return;
//                }
//                else if (retries >= 3)
//                {
//                    currentIndex = -1;
//                    retries = 0;
//                    return;
//                }
//                if (currentIndex == activity.get().mPendingPage)
//                    activity.get().setStatus("Retrying page " + activity.get().mActiveManga.chapters[currentChapter].pages[currentIndex].id + "... (attempt " + String.valueOf(retries + 1) + ")");
//
//                downloadImage(currentUrl, currentPage, currentIndex, currentChapter);
//                return;
//            }
//
//            currentUrl = extractImageUrlFromHtml(data);
//
//            if (currentUrl.contains("Exception"))
//            {
//                Mango.log(currentUrl + " when parsing url (try loading it again)");
//                callbackHtml("Exception: couldn't parse url from HTML (" + currentUrl + ")");
//                return;
//            }
//
//            // Mango.Log("Parsed url " + currentUrl + " from html, initializing download.");
//            downloadImage(currentUrl, currentPage, currentIndex, currentChapter);
//        }
//
//        // does some crazy voodoo magic shit to extract a url from the HTML
//        // without using regex. some sites require more crazy voodoo shit than
//        // others (ie. mangable)
//        private String extractImageUrlFromHtml(String data)
//        {
//            try
//            {
//                if (mSubstringAltStart.equals(""))
//                {
//                    int srcStart = data.indexOf(activity.get().mSubstringStart) + activity.get().mSubstringStart.length();
//                    int srcEnd = data.indexOf("\"", srcStart);
//                    return data.substring(srcStart, srcEnd);
//                }
//                int substringOffset = data.indexOf(activity.get().mSubstringAltStart) + activity.get().mSubstringAltStart.length();
//                substringOffset -= 150; // lolmagic literal
//                // Mango.Log(substringOffset + ": " + data.substring(substringOffset, substringOffset + 30));
//                int srcStart = data.indexOf(activity.get().mSubstringStart, substringOffset) + activity.get().mSubstringStart.length();
//                // Mango.Log(srcStart + ": " + data.substring(srcStart, srcStart + 10));
//                int srcEnd = data.indexOf("\"", srcStart);
//                // Mango.Log(srcEnd + ": " + data.substring(srcEnd, srcEnd + 10));
//
//                return data.substring(srcStart, srcEnd);
//            }
//            catch (Exception ex)
//            {
//                Mango.log("extractImageUrlFromHtml Exception " + ex.toString());
//                return ex.getClass().getSimpleName();
//            }
//        }
//
//        public void callbackImage(String status)
//        {
//            if (currentChapter != activity.get().mChapterIndex)
//            {
//                Mango.log("We've changed chapters, drop the download.");
//                return;
//            }
//            if (!status.equals("ok"))
//            {
//                Mango.log("EXCEPTION: Pagereader callbackImage (index " + String.valueOf(currentIndex) + ") >> " + status);
//                retries++;
//                if (currentIndex == activity.get().mPendingPage && retries >= 3)
//                {
//                    Mango.alert("Mango couldn't download page " + currentPage.id + " after three attempts. Try again in a moment, or switch to another manga site.\n\n" + status
//                            + "\nAttempted URL: " + currentUrl + "\n\n", NewPagereaderActivity.this);
//                    activity.get().displayImage(null, currentIndex);
//                    activity.get().cancelPageLoad(currentIndex);
//                    currentIndex = -1;
//                    retries = 0;
//                    return;
//                }
//                else if (retries >= 3)
//                {
//                    currentIndex = -1;
//                    retries = 0;
//                    return;
//                }
//                if (currentIndex == activity.get().mPendingPage)
//                    activity.get().setStatus("Retrying page " + activity.get().mActiveManga.chapters[currentChapter].pages[currentIndex].id + "... (attempt " + String.valueOf(retries + 1) + ")");
//
//                downloadImage(currentUrl, currentPage, currentIndex, currentChapter);
//                return;
//            }
//
//            if (activity.get().mPendingPage == currentIndex && activity.get().mBusy)
//            {
//                mCurrentPageImageUrl = currentUrl;
//
//                currentIndex = -1;
//                Bitmap bm = MangoCache.readBitmapFromCache("page/", activity.get().generateFileName(currentPage.id), 1);
//                if (bm == null)
//                {
//                    Intent intent = new Intent(activity.get().getIntent());
//                    intent.putExtra("initialpage", (activity.get().mPageIndex == -1 ? activity.get().mInitialPage : activity.get().mPageIndex));
//                    intent.putExtra("chapterid", activity.get().mChapterId);
//                    activity.get().setIntent(intent);
//                    restartApp();
//                    return;
//                }
//                activity.get().displayImage(bm, activity.get().mPendingPage);
//                activity.get().mPendingPage = -1;
//            }
//            currentIndex = -1;
//        }
//
//        private class HtmlDownloader extends AsyncTask<String, Void, String>
//        {
//            WeakReference<ImageDownloader> downloader;
//
//            HtmlDownloader(ImageDownloader ref)
//            {
//                downloader = new WeakReference<ImageDownloader>(ref);
//            }
//
//            @Override
//            protected String doInBackground(String... params)
//            {
//                return MangoHttp.downloadHtml(params[0], downloader.get().activity.get());
//            }
//
//            @Override
//            protected void onPostExecute(String data)
//            {
//                if (downloader.get().activity.get() != null)
//                    downloader.get().callbackHtml(data);
//            }
//        }
//
//        private class BitmapDownloader extends AsyncTask<String, Void, String>
//        {
//            WeakReference<ImageDownloader> downloader;
//
//            BitmapDownloader(ImageDownloader ref)
//            {
//                downloader = new WeakReference<ImageDownloader>(ref);
//            }
//
//            @Override
//            protected String doInBackground(String... params)
//            {
//                String status = MangoHttp.downloadEncodedImage(params[0], "page/", downloader.get().activity.get().generateFileName(downloader.get().currentPage.id), 0,
//                        downloader.get().activity.get());
//                if (status == null)
//                    status = "Exception: downloadEncodedImage returned null.";
//                return status;
//            }
//
//            @Override
//            protected void onPostExecute(String status)
//            {
//                if (downloader.get().activity.get() != null)
//                    downloader.get().callbackImage(status);
//            }
//        }
//    }
//
//    private void showPreviousPage()
//    {
//        if (!mInstantiated)
//            return;
//        if (mPageIndex == 0)
//        {
//            if (mChapterIndex == 0)
//            {
//                Mango.alert("This is the first page of " + mActiveManga.title + "!", this);
//                return;
//            }
//            openChapter(mActiveManga, mActiveManga.chapters[mChapterIndex - 1].id, 9999);
//        }
//        else
//            goToPage(mPageIndex - 1);
//    }
//
//    private void showNextPage()
//    {
//        if (!mInstantiated)
//            return;
//        if (mPageIndex == mActiveManga.chapters[mChapterIndex].pages.length - 1)
//        {
//            if (mChapterIndex == mActiveManga.chapters.length - 1)
//            {
//                Mango.alert("This is the last available page of " + mActiveManga.title + "! Check periodically to see if a new chapter is available.", this);
//                return;
//            }
//            openChapter(mActiveManga, mActiveManga.chapters[mChapterIndex + 1].id, 0);
//        }
//        else
//            goToPage(mPageIndex + 1);
//    }
//
//    public void goToPage(int index)
//    {
//        if (mBusy)
//            return;
//        mBusy = true;
//
//        if (mPageIndex == -1)
//            mPageIndex = 0;
//
//        try
//        {
//
//            if (MangoCache.checkCacheForImage("page/", generateFileName(mActiveManga.chapters[mChapterIndex].pages[index].id)))
//            {
//                Bitmap bm = MangoCache.readBitmapFromCache("page/", generateFileName(mActiveManga.chapters[mChapterIndex].pages[index].id), 1);
//                if (bm == null)
//                {
//                    Intent intent = new Intent(getIntent());
//                    intent.putExtra("initialpage", (mPageIndex == -1 ? mInitialPage : mPageIndex));
//                    intent.putExtra("chapterid", mChapterId);
//                    setIntent(intent);
//                    restartApp();
//                    return;
//                }
//                displayImage(bm, index);
//            }
//            else
//            {
//                int titleIndex = mPageIndex == -1 ? mInitialPage : mPageIndex;
//                setTitlebarText(mActiveManga.title + " " + mActiveManga.chapters[mChapterIndex].id + " page " + mActiveManga.chapters[mChapterIndex].pages[titleIndex].id + " (" + (mPageIndex + 1)
//                        + "/" + mActiveManga.chapters[mChapterIndex].pages.length + ")", false);
//                mPendingPage = index;
//                String id = mActiveManga.chapters[mChapterIndex].pages[index].id;
//                setStatus("Downloading page " + id + "...");
//
//                // First check to see if this page is already being downloaded
//                for (int i = 0; i < mLoaders.length; i++)
//                {
//                    if (mLoaders[i].currentIndex == index)
//                    {
//                        if (mLoaders[i].retries > 0)
//                            setStatus("Downloading page " + id + "... (attempt " + String.valueOf(mLoaders[i].retries + 1) + ")");
//                        return;
//                    }
//                }
//
//                // If not, look for a free instance of mLoader to download it.
//                for (int i = 0; i < mLoaders.length; i++)
//                {
//                    if (mLoaders[i].currentIndex == -1)
//                    {
//                        mLoaders[i] = new ImageDownloader(NewPagereaderActivity.this);
//                        mLoaders[i].downloadImage(mImageUrlPrefix + id, mActiveManga.chapters[mChapterIndex].pages[index], index, mChapterIndex);
//                        break;
//                    }
//                }
//            }
//        }
//        catch (ArrayIndexOutOfBoundsException e)
//        {
//            Mango.log("Arrayindexoutofbounds: " + mActiveManga.chapters.length + ", " + mChapterIndex + ", " + mPageIndex);
//            // for some reason the Mango Service returned an empty pagelist. we'll try to re-open the chapter
//            // while forcing the Mango Service to skip the cache.
//            AlertDialog alert = new AlertDialog.Builder(NewPagereaderActivity.this).create();
//            alert.setTitle("Problem! T___T");
//            alert.setMessage("The list of pages for this chapter is not available. This might just be a temporary issue, or else the chapter simply isn't available on "
//                    + Mango.getSiteName(Mango.getSiteId()) + ".\n\nWould you like to try again?");
//            alert.setButton(DialogInterface.BUTTON_POSITIVE, "Reload Chapter", new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    openChapter(mActiveManga, mChapterId, mInitialPage, true);
//                }
//            });
//            alert.setButton(DialogInterface.BUTTON_NEGATIVE, "Go Back", new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    finish();
//                }
//            });
//            alert.show();
//        }
//    }
//
//    private void preloadPage(int index)
//    {
//        for (int i = 0; i < mLoaders.length; i++)
//        {
//            if (!(index >= mActiveManga.chapters[mChapterIndex].pages.length))
//            {
//                String id = mActiveManga.chapters[mChapterIndex].pages[index].id;
//
//                // make sure page isn't already being preloaded
//                boolean alreadyLoading = false;
//                for (int j = 0; j < mLoaders.length; j++)
//                {
//                    if (mLoaders[j].currentIndex == index)
//                        alreadyLoading = true;
//                }
//
//                if (alreadyLoading || MangoCache.checkCacheForImage("page/", generateFileName(id)))
//                {
//                    // too many cache hits?
//                    index++;
//                    continue;
//                }
//
//                if (mLoaders[i].currentIndex == -1)
//                {
//                    mLoaders[i] = new ImageDownloader(NewPagereaderActivity.this);
//                    mLoaders[i].downloadImage(mImageUrlPrefix + id, mActiveManga.chapters[mChapterIndex].pages[index], index, mChapterIndex);
//                    index++;
//                }
//            }
//        }
//    }
//
//    private void displayImage(Bitmap bitmap, int index)
//    {
//        boolean fail = false;
//        if (bitmap == null)
//        {
//            System.gc();
//            fail = true;
//            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_decodefailure);
//        }
//        if (index == mInitialPage)
//            mInstantiated = true;
//        if (mAdLoaderTextView != null && mAdVisible)
//            adContinueActivate();
//        mBusy = false;
//
//        if (index < mPageIndex)
//            mAnimateSlideIn = false;
//        else
//            mAnimateSlideIn = true;
//
//        if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//            mAnimateSlideIn = !mAnimateSlideIn;
//
//        mPageIndex = index;
//
//        updateReadingProgress();
//        clearStatus();
//
//        try
//        {
//            setTitlebarText(mActiveManga.title + " " + mActiveManga.chapters[mChapterIndex].id + " page " + mActiveManga.chapters[mChapterIndex].pages[index].id + " (" + (mPageIndex + 1) + "/"
//                    + mActiveManga.chapters[mChapterIndex].pages.length + ")", false);
//        }
//        catch (NullPointerException e)
//        {
//            Mango.log("oops! Nullpointerexception at setTitlebarText in displayImage. Killing activity.");
//            cleanup();
//            finish();
//            return;
//        }
//
//        if (!Mango.getSharedPreferences().getBoolean("disableAnimation", false))
//        {
//            try
//            {
//                if (mPageView.getImageBitmap() != null)
//                {
//                    Bitmap bm = Bitmap.createBitmap(mPageView.getWidth(), mPageView.getHeight(), Config.ARGB_8888);
//                    Canvas c = new Canvas(bm);
//                    mPageView.onDraw(c);
//                    if (mAnimateSlideIn)
//                        mAnimatorStaticView.setImageBitmap(bm);
//                    else
//                        mAnimatorSlideView.setImageBitmap(bm);
//                    mAnimatorStaticView.setVisibility(View.VISIBLE);
//                    mAnimatePage = true;
//                    mAnimatorStaticView.invalidate();
//                    mPageView.invalidate();
//                }
//            }
//            catch (OutOfMemoryError oom)
//            {
//                Mango.log("Skipping animation because we're out of memory. :>");
//            }
//            catch (NullPointerException ex)
//            {
//                Mango.log("Pagefield is null, probably due to orientation change. Returning.");
//                return;
//            }
//        }
//
//        float scale = mPageView.getScale();
//
//        try
//        {
//            mPageView.setImageBitmap(bitmap);
//
//            if (Mango.getSharedPreferences().getBoolean("stickyZoom", false))
//                mPageView.zoomTo(scale, 0, 0);
//
//            if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                mPageView.scrollToTopLeftCorner();
//            else
//                mPageView.scrollToTopRightCorner();
//
//            newPageCallback();
//
//            if (mActiveManga != null && !fail)
//                preloadPage(index + 1);
//        }
//        catch (NullPointerException ex)
//        {
//            Mango.log("Oops! NullPointerException around mPageView.setImage() in displayImage. Killing activity.");
//            cleanup();
//            finish();
//            return;
//        }
//        catch (OutOfMemoryError oom)
//        {
//            Mango.log("PagereaderActivity ran out of memory.");
//            restartApp();
//        }
//    }
//
//    public String generateFileName(String pageId)
//    {
//        try
//        {
//            return Mango.getSiteId() + mActiveManga.id + mActiveManga.chapters[mChapterIndex].id + pageId;
//        }
//        catch (NullPointerException e)
//        {
//            String errorData = "NullPointerException in generateFileName method.\nmActiveManga: " + String.valueOf(mActiveManga) + "\ncurrentChapter: "
//                    + (mActiveManga == null ? "null" : String.valueOf(mActiveManga.chapters)) + "\npageId: " + pageId;
//            Mango.alert("An unexpected error occurred. Mango will try to keep going, but you should probably close the chapter and re-open it.\n\nTechnical data:\n" + errorData,
//                    NewPagereaderActivity.this);
//            Mango.log("generateFileName error  " + errorData);
//            return pageId;
//        }
//    }
//
//    public void setTitlebarText(CharSequence title, boolean override)
//    {
//        if (Mango.getSharedPreferences().getBoolean("disablePageBar", false) && !override)
//            return;
//
//        try
//        {
//            mTitlebarContainer.bringToFront();
//            mLastSetTitle = System.currentTimeMillis();
//            mTitlebarText = (String) title;
//
//            if (Mango.getSharedPreferences().getBoolean("fullscreenReading", false))
//            {
//                Intent bat = NewPagereaderActivity.this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//                int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//                int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
//                String time = DateFormat.getTimeFormat(getApplicationContext()).format(new Date());
//                String battery = String.valueOf((level * 100 / scale));
//                mTitlebarClockView.setText(battery + "%  " + time);
//            }
//
//            mTitlebarContainer.bringToFront();
//            mTitlebarTextView.setText(title);
//            if (mTitlebarContainer.getVisibility() != View.VISIBLE)
//            {
//                mAnimatorSlideView.postDelayed(mTitlebarHideRunnable, 4000);
//                mTitlebarContainer.clearAnimation();
//                mTitlebarContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.titlebarin));
//                mTitlebarContainer.setVisibility(View.VISIBLE);
//            }
//        }
//        catch (Exception e)
//        {
//            String techdata = String.valueOf(mPageView) + ", " + String.valueOf(mTitlebarHideRunnable);
//            Mango.log(e.toString() + " at setTitlebarText (" + techdata + ")");
//        }
//    }
//
//    private Runnable mTitlebarHideRunnable = new Runnable()
//    {
//        @Override
//        public void run()
//        {
//            try
//            {
//                long elapsed = System.currentTimeMillis() - mLastSetTitle;
//                if (elapsed < 4000)
//                {
//                    mAnimatorSlideView.postDelayed(mTitlebarHideRunnable, 4000 - elapsed);
//                    return;
//                }
//                mTitlebarContainer.bringToFront();
//                mTitlebarContainer.clearAnimation();
//                mTitlebarContainer.startAnimation(AnimationUtils.loadAnimation(NewPagereaderActivity.this, R.anim.titlebarout));
//                mTitlebarContainer.setVisibility(View.INVISIBLE);
//            }
//            catch (NullPointerException e)
//            {
//                // activity closed
//            }
//        }
//    };
//
//    private void setStatus(String text)
//    {
//        mStatusText.setText(text);
//        if (mStatusbarContainer.getVisibility() == View.INVISIBLE)
//        {
//            mStatusText.startAnimation(mPulseAnimation);
//        }
//        mStatusbarContainer.setVisibility(View.VISIBLE);
//    }
//
//    private void clearStatus()
//    {
//        mStatusText.setText("");
//        mStatusText.clearAnimation();
//        mStatusbarContainer.setVisibility(View.INVISIBLE);
//    }
//
//    private void cancelPageLoad(int index)
//    {
//        mPendingPage = -1;
//        mInstantiated = true;
//        mBusy = false;
//        clearStatus();
//    }
//
//    private void checkReadingProgress()
//    {
//        if (mTrackReadingProgress > 1)
//            return;
//
//        try
//        {
//            MangoSqlite db = new MangoSqlite(this);
//            db.open();
//            final Favorite f = db.getFavoriteForManga(mActiveManga);
//            db.close();
//            if (f.siteId == Mango.getSiteId() && f.progressChapterId != null)
//            {
//                if (f.progressChapterIndex < mChapterIndex - 1)
//                {
//                    AlertDialog alert = new AlertDialog.Builder(NewPagereaderActivity.this).create();
//                    alert.setTitle("Set Reading Progress Ahead?");
//                    alert.setMessage("Your reading progress for this manga is Chapter " + f.progressChapterId
//                            + ", which is pretty far behind this chapter.\n\nDo you want to set your reading progress this far ahead?");
//                    alert.setButton(DialogInterface.BUTTON_POSITIVE, "Yep", new DialogInterface.OnClickListener()
//                    {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which)
//                        {
//                            mTrackReadingProgress = 1;
//                            updateReadingProgress(true);
//                            Mango.alert("Okay. Mango has set your reading progress to this chapter.", NewPagereaderActivity.this);
//                        }
//                    });
//                    alert.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener()
//                    {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which)
//                        {
//                            mTrackReadingProgress = 2;
//                            Mango.alert("Okay. Mango won't update your progress while you read ahead.", NewPagereaderActivity.this);
//                        }
//                    });
//                    alert.show();
//                }
//                else
//                    mTrackReadingProgress = 1;
//            }
//            else if (f.siteId == Mango.getSiteId() && f.progressChapterId == null)
//            {
//                mTrackReadingProgress = 1;
//            }
//            else
//            {
//                AlertDialog alert = new AlertDialog.Builder(NewPagereaderActivity.this).create();
//                alert.setTitle("Track Reading Progress?");
//                alert.setMessage("You're already reading this manga on " + Mango.getSiteName(f.siteId)
//                        + ". Mango can only track your progress for one manga source at a time. Do you want to start tracking your reading progress for this manga on "
//                        + Mango.getSiteName(Mango.getSiteId()) + " instead?");
//                alert.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {
//                        mTrackReadingProgress = 1;
//                        MangoSqlite db = new MangoSqlite(NewPagereaderActivity.this);
//                        db.open();
//                        db.clearFavoriteProgress(f.rowId);
//                        db.close();
//                        updateReadingProgress();
//                        Mango.alert("Okay. Mango is now tracking your reading progress for this manga on " + Mango.getSiteName(Mango.getSiteId()) + ".", NewPagereaderActivity.this);
//                    }
//                });
//                alert.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener()
//                {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which)
//                    {
//                        mTrackReadingProgress = 2;
//                        Mango.alert("Okay. Mango won't track your progress for this manga while you read on " + Mango.getSiteName(Mango.getSiteId()), NewPagereaderActivity.this);
//                    }
//                });
//                alert.show();
//            }
//        }
//        catch (Exception e)
//        {
//            mTrackReadingProgress = 3;
//        }
//    }
//
//    private boolean isReadingProgressBehind(Favorite f)
//    {
//        try
//        {
//            if (mChapterIndex >= f.progressChapterIndex)
//            {
//                if ((mChapterIndex == f.progressChapterIndex && mPageIndex >= f.progressPageIndex) || mChapterIndex > f.progressChapterIndex)
//                {
//                    return true;
//                }
//            }
//        }
//        catch (Exception e)
//        {
//        }
//        return false;
//    }
//
//    private void updateReadingProgress()
//    {
//        updateReadingProgress(false);
//    }
//
//    private void updateReadingProgress(boolean forceUpdate)
//    {
//        if (mTrackReadingProgress != 1 && !forceUpdate)
//            return;
//
//        MangoSqlite db = new MangoSqlite(NewPagereaderActivity.this);
//        try
//        {
//            db.open();
//            Favorite f = db.getFavoriteForManga(mActiveManga);
//            f.mangaId = mActiveManga.id;
//            f.siteId = Mango.getSiteId();
//
//            if (mChapterIndex == (mActiveManga.chapters.length - 1)) // we're reading the latest chapter
//            {
//                f.lastChapterId = mChapterId;
//                f.lastChapterIndex = mChapterIndex;
//                f.lastChapterName = mActiveManga.chapters[mChapterIndex].title;
//                f.lastChapterUrl = mActiveManga.chapters[mChapterIndex].url;
//                f.lastChapterTime = -1;
//            }
//
//            if (isReadingProgressBehind(f) || forceUpdate)
//            {
//                f.progressChapterId = mActiveManga.chapters[mChapterIndex].id;
//                f.progressChapterIndex = mChapterIndex;
//                f.progressChapterName = mActiveManga.chapters[mChapterIndex].title;
//                f.progressChapterUrl = mActiveManga.chapters[mChapterIndex].url;
//                f.progressPageIndex = mPageIndex;
//                f.readDate = System.currentTimeMillis();
//            }
//            db.updateFavorite(f);
//        }
//        catch (Exception e)
//        {
//            return;
//        }
//        finally
//        {
//            db.close();
//        }
//        refreshMenu();
//    }
//
//    private void addRecent()
//    {
//        if (Mango.getSharedPreferences().getBoolean("disableHistory", false))
//            return;
//        ArrayList<Bookmark> recentArrayList = new ArrayList<Bookmark>();
//        MangoSqlite db = new MangoSqlite(NewPagereaderActivity.this);
//        long updateRecentRowId = -1;
//        try
//        {
//            db.open();
//            Bookmark[] bmarray = db.getAllHistoryArray(MangoSqlite.KEY_UPDATETIME + " ASC", MangoSqlite.KEY_MANGAID + " = '" + mActiveManga.id + "'", true);
//            for (int i = 0; i < bmarray.length; i++)
//            {
//                Bookmark bm = bmarray[i];
//                if (bm.bookmarkType == Bookmark.RECENT)
//                {
//                    recentArrayList.add(bm);
//                    if (bm.mangaId.equals(mActiveManga.id) && bm.chapterIndex == mChapterIndex)
//                        updateRecentRowId = bm.rowId;
//                }
//                if (recentArrayList.size() >= 10000 && updateRecentRowId == -1)
//                {
//                    db.deleteBookmark(recentArrayList.get(0).rowId);
//                    recentArrayList.remove(0);
//                }
//            }
//            if (updateRecentRowId != -1)
//                db.deleteBookmark(updateRecentRowId);
//            db.insertRecentBookmark(mActiveManga.id, mActiveManga.title, mChapterIndex, mActiveManga.chapters[mChapterIndex].title, mActiveManga.chapters[mChapterIndex].id,
//                    mActiveManga.chapters.length, Mango.getSiteId(), false);
//        }
//        catch (SQLException e)
//        {
//            Mango.alert("Mango encountered an error while trying to create a bookmark.\n\n" + e.toString(), NewPagereaderActivity.this);
//        }
//        finally
//        {
//            db.close();
//        }
//    }
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        if (!mInstantiated)
//            return super.onKeyDown(keyCode, event);
//        if (keyCode == KeyEvent.KEYCODE_BACK && mAdVisible)
//        {
//            adContinueClicked();
//            return true;
//        }
//        if (keyCode == KeyEvent.KEYCODE_BACK && mPendingPage != -1)
//        {
//            cancelPageLoad(mPendingPage);
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event)
//    {
//        if (!mInstantiated)
//            return super.dispatchKeyEvent(event);
//
//        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && mPendingPage != -1)
//        {
//
//        }
//        // Sony Xperia PLAY L1 shoulder button
//        if (event.getKeyCode() == 102 && event.getAction() == KeyEvent.ACTION_UP)
//        {
//            if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                showPreviousPage();
//            else
//                showNextPage();
//            return true;
//        }
//        // Sony Xperia PLAY R1 shoulder button
//        if (event.getKeyCode() == 103 && event.getAction() == KeyEvent.ACTION_UP)
//        {
//            if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                showNextPage();
//            else
//                showPreviousPage();
//            return true;
//        }
//        // For volume rocker control, we need to consume both the up and down key events.
//        // But we only want to change pages once.
//        // Volume Up
//        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
//        {
//            if (Mango.getSharedPreferences().getBoolean("volumeRockerControls", false))
//            {
//                if (event.getAction() == KeyEvent.ACTION_UP)
//                {
//                    if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                        showPreviousPage();
//                    else
//                        showNextPage();
//                }
//                return true;
//            }
//        }
//        // Volume down
//        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
//        {
//            if (Mango.getSharedPreferences().getBoolean("volumeRockerControls", false))
//            {
//                if (event.getAction() == KeyEvent.ACTION_UP)
//                {
//                    if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                        showNextPage();
//                    else
//                        showPreviousPage();
//                }
//                return true;
//            }
//        }
//        // Enter or Dpad Center
//        if ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) && event.getAction() == KeyEvent.ACTION_UP)
//        {
//            showNextPage();
//            return true;
//        }
//        // Dpad down or S
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_S ||
//                event.getKeyCode() == KeyEvent.KEYCODE_SPACE
//                        && (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_MULTIPLE))
//        {
//            mPageView.scrollDown();
//        }
//        // Dpad up or W
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_W &&
//                (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_MULTIPLE))
//        {
//            mPageView.scrollUp();
//        }
//        // Dpad left or A
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_A
//                && (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_MULTIPLE))
//        {
//            mPageView.scrollLeft();
//        }
//        // Dpad right or D
//        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT || event.getKeyCode() == KeyEvent.KEYCODE_D
//                && (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_MULTIPLE))
//        {
//            mPageView.scrollRight();
//        }
//        return super.dispatchKeyEvent(event);
//    }
//
//    private class XmlDownloader extends AsyncTask<String, Void, String>
//    {
//        WeakReference<NewPagereaderActivity> activity = null;
//
//        public XmlDownloader(NewPagereaderActivity activity)
//        {
//            attach(activity);
//        }
//
//        @Override
//        protected String doInBackground(String... params)
//        {
//            if (activity == null || activity.get() == null)
//                return "Exception: loader's activity reference was null. (screen was rotated?)";
//            return MangoHttp.downloadHtml(params[0], activity.get());
//        }
//
//        @Override
//        protected void onPostExecute(String data)
//        {
//            if (activity == null || activity.get() == null)
//            {
//                Mango.log("AsyncTask skipped onPostExecute because no activity is attached!");
//            }
//            else
//            {
//                activity.get().callback(data);
//            }
//        }
//
//        void detach()
//        {
//            activity = null;
//        }
//
//        void attach(NewPagereaderActivity activity)
//        {
//            this.activity = new WeakReference<NewPagereaderActivity>(activity);
//        }
//    }
//
//    protected void newPageCallback()
//    {
//        // if (Mango.getSharedPreferences().getBoolean("stickyZoom", false))
//        // mZoomControl.getZoomState().setPanX(mZoomControl.getMaxPanX());
//        // else
//        // mZoomControl.getZoomState().setPanX(0.5f);
//
//        if (!mAnimatePage)
//            return;
//
//        mAnimatePage = false;
//        mAnimatorSlideView.setVisibility(View.VISIBLE);
//        Bitmap bm = Bitmap.createBitmap(mPageView.getWidth(), mPageView.getHeight(), Config.ARGB_8888);
//        Canvas c = new Canvas(bm);
//        mPageView.onDraw(c);
//
//        if (mAnimateSlideIn)
//            mAnimatorSlideView.setImageBitmap(bm);
//        else
//            mAnimatorStaticView.setImageBitmap(bm);
//        mAnimatorStaticView.bringToFront();
//        mAnimatorSlideView.bringToFront();
//        mTitlebarContainer.bringToFront();
//        mAnimatorSlideView.clearAnimation();
//        if (mAnimateSlideIn)
//        {
//            mAnimatorSlideView.setAnimation(mSlideInAnimation);
//            mSlideInAnimation.reset();
//            mSlideInAnimation.startNow();
//        }
//        else
//        {
//            mAnimatorSlideView.setAnimation(mSlideOffAnimation);
//            mSlideOffAnimation.reset();
//            mSlideOffAnimation.startNow();
//        }
//        mAnimatorSlideView.postDelayed(new Runnable()
//        {
//
//            @Override
//            public void run()
//            {
//                // clean up and free resources used by animator
//                try
//                {
//                    mPageView.bringToFront();
//                    mAnimatorSlideView.setVisibility(View.INVISIBLE);
//                    mAnimatorStaticView.setVisibility(View.INVISIBLE);
//                    mAnimatorSlideView.postDelayed(new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
//                            if (mAnimateSlideIn)
//                            {
//                                if (mSlideInAnimation.hasEnded())
//                                {
//                                    mAnimatorSlideView.setImageBitmap(null);
//                                    mAnimatorStaticView.setImageBitmap(null);
//                                }
//                            }
//                            else
//                            {
//                                if (mSlideOffAnimation.hasEnded())
//                                {
//                                    mAnimatorSlideView.setImageBitmap(null);
//                                    mAnimatorStaticView.setImageBitmap(null);
//                                }
//                            }
//                        }
//                    }, 500);
//                }
//                catch (NullPointerException e)
//                {
//                    // cleanup() has already been called
//                }
//            }
//        }, 350);
//    }
//
//    protected void setTouchControlCallbacks()
//    {
//
//        mPageView.setOnTapCallback(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                if (Mango.getSharedPreferences().getBoolean("disableTapToAdvance", false))
//                    return;
//                showNextPage();
//            }
//
//        });
//        mPageView.setOnBackTapCallback(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                if (Mango.getSharedPreferences().getBoolean("disableTapToAdvance", false))
//                    return;
//                showPreviousPage();
//            }
//
//        });
//        mPageView.setOnLeftFlingCallback(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                if (Mango.getSharedPreferences().getBoolean("disableSwipeControls", false))
//                    return;
//                if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                    showNextPage();
//                else
//                    showPreviousPage();
//            }
//
//        });
//        mPageView.setOnRightFlingCallback(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                if (Mango.getSharedPreferences().getBoolean("disableSwipeControls", false))
//                    return;
//                if (Mango.getSharedPreferences().getBoolean("leftRightReading", false))
//                    showPreviousPage();
//                else
//                    showNextPage();
//            }
//
//        });
//    }
//
//    private void launchShareIntent()
//    {
//        final Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_TEXT, "I'm reading " + mActiveManga.title + " on my Android with Mango! @MangoApp" + (mCurrentPageImageUrl != null ? "\n" + mCurrentPageImageUrl : ""));
//        startActivity(Intent.createChooser(intent, "Share via..."));
//    }
//}
