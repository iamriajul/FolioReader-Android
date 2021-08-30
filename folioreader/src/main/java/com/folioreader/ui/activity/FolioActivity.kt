/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.folioreader.Config
import com.folioreader.Constants
import com.folioreader.Constants.*
import com.folioreader.FolioReader
import com.folioreader.R
import com.folioreader.databinding.FolioActivityBinding
import com.folioreader.model.DisplayUnit
import com.folioreader.model.HighlightImpl
import com.folioreader.model.TOCLinkWrapper
import com.folioreader.model.event.MediaOverlayPlayPauseEvent
import com.folioreader.model.event.ReloadDataEvent
import com.folioreader.model.locators.ReadLocator
import com.folioreader.model.locators.SearchLocator
import com.folioreader.ui.adapter.FolioPageFragmentAdapter
import com.folioreader.ui.adapter.SearchAdapter
import com.folioreader.ui.adapter.TOCAdapter
import com.folioreader.ui.adapter.TOCAdapter.TOCCallback
import com.folioreader.ui.fragment.FolioPageFragment
import com.folioreader.ui.fragment.MediaControllerFragment
import com.folioreader.ui.fragment.TableOfContentFragment
import com.folioreader.ui.fragment.bookmark.BookmarkHighlightAndNoteFragment
import com.folioreader.ui.view.*
import com.folioreader.ui.view.ConfigBottomSheetDialogFragment
import com.folioreader.ui.view.DirectionalViewpager
import com.folioreader.ui.view.MediaControllerCallback
import com.folioreader.util.AppUtil
import com.folioreader.util.FileUtil
import com.folioreader.util.UiUtil
import org.greenrobot.eventbus.EventBus
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.parser.CbzParser
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.server.Server
import java.lang.ref.WeakReference
import java.util.ArrayList
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import androidx.fragment.app.Fragment

import kotlin.math.log

class FolioActivity : AppCompatActivity(), FolioActivityCallback, MediaControllerCallback,
    View.OnSystemUiVisibilityChangeListener, TOCCallback {

    private var bookFileName: String? = null

    private lateinit var binding: FolioActivityBinding

    //    private var distractionFreeMode: Boolean = false
    private var hasFullScreen: Boolean = false
    private var handler: Handler? = null

    private var currentChapterIndex: Int = 0
    private var mFolioPageFragmentAdapter: FolioPageFragmentAdapter? = null
    private var entryReadLocator: ReadLocator? = null
    private var lastReadLocator: ReadLocator? = null
    private var outState: Bundle? = null
    private var savedInstanceState: Bundle? = null

    private var r2StreamerServer: Server? = null
    private var pubBox: PubBox? = null
    private var spine: List<Link>? = null

    private var mBookId: String? = null
    private var mEpubFilePath: String? = null
    private var mEpubSourceType: EpubSourceType? = null
    private var mEpubRawId = 0
    private var mediaControllerFragment: MediaControllerFragment? = null
    private var direction: Config.Direction = Config.Direction.VERTICAL
    private var portNumber: Int = Constants.DEFAULT_PORT_NUMBER
    private var streamerUri: Uri? = null

    private var searchUri: Uri? = null
    private var searchAdapterDataBundle: Bundle? = null
    private var searchQuery: CharSequence? = null
    private var searchLocator: SearchLocator? = null

    private var displayMetrics: DisplayMetrics? = null
    private var density: Float = 0.toFloat()
    private var topActivity: Boolean? = null
    private var taskImportance: Int = 0

    private var isNightMode = false

    private lateinit var mTOCAdapter: TOCAdapter


    companion object {

        @JvmField
        val LOG_TAG: String = FolioActivity::class.java.simpleName

        const val INTENT_EPUB_SOURCE_PATH = "com.folioreader.epub_asset_path"
        const val INTENT_EPUB_SOURCE_TYPE = "epub_source_type"
        const val EXTRA_READ_LOCATOR = "com.folioreader.extra.READ_LOCATOR"
        private const val BUNDLE_READ_LOCATOR_CONFIG_CHANGE = "BUNDLE_READ_LOCATOR_CONFIG_CHANGE"
        private const val BUNDLE_DISTRACTION_FREE_MODE = "BUNDLE_DISTRACTION_FREE_MODE"
        const val EXTRA_SEARCH_ITEM = "EXTRA_SEARCH_ITEM"
        const val ACTION_SEARCH_CLEAR = "ACTION_SEARCH_CLEAR"
        private const val HIGHLIGHT_ITEM = "highlight_item"


        /**
         * [RECURSIVE]
         *
         *
         * function generates list of [TOCLinkWrapper] of TOC list from publication manifest
         *
         * @param tocLink     table of content elements
         * @param indentation level of hierarchy of the child elements
         * @return generated [TOCLinkWrapper] list
         */
        private fun createTocLinkWrapper(tocLink: Link, indentation: Int): TOCLinkWrapper {
            val tocLinkWrapper = TOCLinkWrapper(tocLink, indentation)

            for (tocLink1 in tocLink.children) {
                val tocLinkWrapper1 = createTocLinkWrapper(tocLink1, indentation + 1)
                if (tocLinkWrapper1.indentation != 3) {
                    tocLinkWrapper.addChild(tocLinkWrapper1)
                }
            }
            return tocLinkWrapper
        }


        private fun createTOCFromSpine(spine: List<Link>): ArrayList<TOCLinkWrapper>? {
            val tocLinkWrappers = ArrayList<TOCLinkWrapper>()
            for (link in spine) {
                val tocLink = Link()
                tocLink.title = link.title
                tocLink.href = link.href
                tocLinkWrappers.add(TOCLinkWrapper(tocLink, 0))
            }
            return tocLinkWrappers
        }


    }

    private val closeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(LOG_TAG, "-> closeBroadcastReceiver -> onReceive -> " + intent.action!!)

            val action = intent.action
            if (action != null && action == FolioReader.ACTION_CLOSE_FOLIOREADER) {

                try {
                    val activityManager =
                        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val tasks = activityManager.runningAppProcesses
                    taskImportance = tasks[0].importance
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "-> ", e)
                }

                val closeIntent = Intent(applicationContext, FolioActivity::class.java)
                closeIntent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                closeIntent.action = FolioReader.ACTION_CLOSE_FOLIOREADER
                this@FolioActivity.startActivity(closeIntent)
            }
        }
    }

    val statusBarHeight: Int
        get() {
            var result = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0)
                result = resources.getDimensionPixelSize(resourceId)
            return result
        }

    private val searchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(LOG_TAG, "-> searchReceiver -> onReceive -> " + intent.action!!)

            val action = intent.action ?: return
            when (action) {
                ACTION_SEARCH_CLEAR -> clearSearchLocator()
            }
        }
    }

    private val currentFragment: FolioPageFragment?
        get() = if (mFolioPageFragmentAdapter != null) {
            mFolioPageFragmentAdapter!!.getItem(binding.folioPageViewPager.currentItem) as FolioPageFragment
        } else {
            null
        }

    enum class EpubSourceType {
        RAW,
        ASSETS,
        SD_CARD
    }

    private enum class RequestCode private constructor(internal val value: Int) {
        CONTENT_HIGHLIGHT(77),
        SEARCH(101)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.v(LOG_TAG, "-> onNewIntent")

        val action = getIntent().action
        if (action != null && action == FolioReader.ACTION_CLOSE_FOLIOREADER) {

            if (topActivity == null || topActivity == false) {
                // FolioActivity was already left, so no need to broadcast ReadLocator again.
                // Finish activity without going through onPause() and onStop()
                finish()

                // To determine if app in background or foreground
                var appInBackground = false
                if (Build.VERSION.SDK_INT < 26) {
                    if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND == taskImportance)
                        appInBackground = true
                } else {
                    if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED == taskImportance)
                        appInBackground = true
                }
                if (appInBackground)
                    moveTaskToBack(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.v(LOG_TAG, "-> onResume")
        topActivity = true

        val action = intent.action
        if (action != null && action == FolioReader.ACTION_CLOSE_FOLIOREADER) {
            // FolioActivity is topActivity, so need to broadcast ReadLocator.
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.v(LOG_TAG, "-> onStop")
        topActivity = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Need to add when vector drawables support library is used.
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        handler = Handler(Looper.getMainLooper())
        val display = windowManager.defaultDisplay
        displayMetrics = resources.displayMetrics
        display.getRealMetrics(displayMetrics)
        density = displayMetrics!!.density
        LocalBroadcastManager.getInstance(this).registerReceiver(
            closeBroadcastReceiver,
            IntentFilter(FolioReader.ACTION_CLOSE_FOLIOREADER)
        )

        // Fix for screen get turned off while reading
        // TODO -> Make this configurable
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setConfig(savedInstanceState)
        initDistractionFreeMode(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.folio_activity)
        this.savedInstanceState = savedInstanceState

        Log.d(LOG_TAG, "onCreate: ${binding.llPages.height}")

        if (savedInstanceState != null) {
            searchAdapterDataBundle = savedInstanceState.getBundle(SearchAdapter.DATA_BUNDLE)
            searchQuery =
                savedInstanceState.getCharSequence(SearchActivity.BUNDLE_SAVE_SEARCH_QUERY)
        }

        mBookId = intent.getStringExtra(FolioReader.EXTRA_BOOK_ID)
        mEpubSourceType =
            intent.extras!!.getSerializable(FolioActivity.INTENT_EPUB_SOURCE_TYPE) as EpubSourceType
        if (mEpubSourceType == EpubSourceType.RAW) {
            mEpubRawId = intent.extras!!.getInt(FolioActivity.INTENT_EPUB_SOURCE_PATH)
        } else {
            mEpubFilePath = intent.extras!!
                .getString(FolioActivity.INTENT_EPUB_SOURCE_PATH)
        }

        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        initActionBar()
        initMediaController()
        initSettingUI()
        if (ContextCompat.checkSelfPermission(
                this@FolioActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@FolioActivity,
                Constants.getWriteExternalStoragePerms(),
                Constants.WRITE_EXTERNAL_STORAGE_REQUEST
            )
        } else {
            setupBook()
        }

        observeAutoScrollingSetting()
        setupRecyclerViews()

        setupListeners()
        showPageInfoOrOthers(hasPageInfo = true)

        updateFontSize()

        configFonts()
        setupSwipeObserver()
        setupBrightnessAndThemes()
        setupDimInterval()
        Log.d(LOG_TAG, "onCreate: screenBrightNess ${window.attributes.screenBrightness}")

        updatePageInfo()
    }

    private fun updatePageInfo() {
        Log.d(LOG_TAG, "updatePageInfo: ${mFolioPageFragmentAdapter?.count}")
        Log.d(LOG_TAG, "updatePageInfo: ${spine?.size}")
    }

    private fun setupBrightnessAndThemes() {
        isNightMode = config.isNightMode

        val layoutParams = window.attributes // Get Params
        layoutParams.screenBrightness =
            (binding.brightness.sbBrightness.progress / 255f) // Set Value
        window.attributes = layoutParams
    }


    private fun configFonts() {
        selectFont(config.font, false)
        binding.fontFamily.bgFontFamily.check(getCheckedButtonId(config.font))

        binding.fontFamily.sbFontSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                config.fontSize = progress
                AppUtil.saveConfig(applicationContext, config)
                EventBus.getDefault().post(ReloadDataEvent())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })


        binding.fontFamily.bgFontFamily.addOnButtonCheckedListener { _, checkedId, checked ->
            if (checked) {
                val fontPosition = when (checkedId) {
                    R.id.btnTimes -> {
                        FONT_TIMES
                    }
                    R.id.btnArial -> {
                        FONT_ARIAL
                    }
                    R.id.btnVerdana -> {
                        FONT_VERDANA
                    }
                    R.id.btnCastleT -> {
                        FONT_CASTLE_T
                    }
                    else -> {
                        FONT_TIMES
                    }
                }
                selectFont(fontPosition, true)
            }
        }
    }


    private fun toggleBlackTheme() {

        val day = ContextCompat.getColor(applicationContext, R.color.white)
        val night = ContextCompat.getColor(applicationContext, R.color.night)

        val colorAnimation = ValueAnimator.ofObject(
            ArgbEvaluator(),
            if (isNightMode) night else day, if (isNightMode) day else night
        )
        colorAnimation.duration = ConfigBottomSheetDialogFragment.FADE_DAY_NIGHT_MODE.toLong()

        colorAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
        }

        colorAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}

            override fun onAnimationEnd(animator: Animator) {
                isNightMode = !isNightMode
                updateThemes()
                config.isNightMode = isNightMode
                AppUtil.saveConfig(applicationContext, config)
                EventBus.getDefault().post(ReloadDataEvent())
            }

            override fun onAnimationCancel(animator: Animator) {}

            override fun onAnimationRepeat(animator: Animator) {}
        })

        colorAnimation.duration = ConfigBottomSheetDialogFragment.FADE_DAY_NIGHT_MODE.toLong()

        val attrs = intArrayOf(android.R.attr.navigationBarColor)
        val typedArray = theme?.obtainStyledAttributes(attrs)
        val defaultNavigationBarColor = typedArray?.getColor(
            0,
            ContextCompat.getColor(applicationContext, R.color.white)
        )
        val black = ContextCompat.getColor(applicationContext, R.color.black)

        val navigationColorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            if (isNightMode) black else defaultNavigationBarColor,
            if (isNightMode) defaultNavigationBarColor else black
        )

        navigationColorAnim.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            window?.navigationBarColor = value
        }

        navigationColorAnim.duration = ConfigBottomSheetDialogFragment.FADE_DAY_NIGHT_MODE.toLong()
        navigationColorAnim.start()

        colorAnimation.start()
    }


    private fun getCheckedButtonId(font: Int): Int {
        return when (font) {
            FONT_TIMES -> binding.fontFamily.btnTimes.id
            FONT_ARIAL -> binding.fontFamily.btnArial.id
            FONT_VERDANA -> binding.fontFamily.btnVerdana.id
            FONT_CASTLE_T -> binding.fontFamily.btnCastleT.id
            else -> binding.fontFamily.btnDefault.id
        }
    }

    private fun selectFont(selectedFont: Int, isReloadNeeded: Boolean) {
        config.font = selectedFont
        if (isReloadNeeded) {
            AppUtil.saveConfig(this, config)
            EventBus.getDefault().post(ReloadDataEvent())
        }
    }


    private fun updateFontSize() {
        binding.fontFamily.sbFontSize.progress = config.fontSize
    }

    private fun setAudioPlayerBackground() {
        if (isNightMode) {
            mediaControllerFragment?.setDayMode()
        } else {
            mediaControllerFragment?.setNightMode()
        }
    }

    private fun setupListeners() {

        binding.brightness.cvWhite.setOnClickListener {
            isNightMode = true
            toggleBlackTheme()
            UiUtil.setColorResToDrawable(
                R.color.white,
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_dark_mode)
            )
            UiUtil.setColorIntToDrawable(
                config.themeColor,
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_light_mode)
            )
//            updateThemes()
            setAudioPlayerBackground()
        }

        binding.brightness.cvBlack.setOnClickListener {
            isNightMode = false
            toggleBlackTheme()
            UiUtil.setColorResToDrawable(
                R.color.black,
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_light_mode)
            )
            UiUtil.setColorIntToDrawable(
                config.themeColor,
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_dark_mode)
            )
            setAudioPlayerBackground()
        }

        binding.bottomMenus.buttonMenuGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnChapters -> {
                        clearMenuItemChecked()
                        toggleStartDrawer()
                    }
                    R.id.btnBookmark -> {
                        clearMenuItemChecked()
                        showBookmarkHighlightAndNote()
                    }
                    R.id.btnFullScreen -> {
                        hasFullScreen = !hasFullScreen
                        fullScreenMode()
                    }
                    R.id.btnBrightness -> {
                        showPageInfoOrOthers(hasBrightNess = true)
                    }
                    R.id.btnRotate -> {
                        showPageInfoOrOthers(hasPageInfo = true)
                    }
                    R.id.btnFontFamily -> {
                        showPageInfoOrOthers(hasFontFamily = true)
                    }
                }
            } else {
                showPageInfoOrOthers(hasPageInfo = true)
            }
        }


        binding.brightness.sbBrightness.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                config.currentBrightness = progress
//                AppUtil.saveConfig(applicationContext, config)
//                EventBus.getDefault().post(ReloadDataEvent())
                val layoutParams = window.attributes // Get Params
                layoutParams.screenBrightness = (progress / 255f) // Set Value
                window.attributes = layoutParams

                binding.brightness.tvCurrentBrightness.text = progress.toString()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

    }

    private fun clearMenuItemChecked() {
        binding.bottomMenus.buttonMenuGroup.clearChecked()
    }

    private fun showPageInfoOrOthers(
        hasPageInfo: Boolean = false,
        hasBrightNess: Boolean = false,
        hasFontFamily: Boolean = false
    ) {
        if (hasPageInfo) {
            binding.pageInfo.root.show()
        } else {
            binding.pageInfo.root.hide()
        }
        if (hasBrightNess) {
            binding.brightness.root.show()
        } else {
            binding.brightness.root.hide()
        }
        if (hasFontFamily) {
            binding.fontFamily.root.show()
        } else {
            binding.fontFamily.root.hide()
        }
    }

    private fun setupRecyclerViews() {
        binding.chaptersRecyclerView.setHasFixedSize(true)
        binding.chaptersRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun initAdapter() {
        val publication = pubBox?.publication
        if (publication != null) {
            if (publication.tableOfContents.isNotEmpty()) {

                val tocLinkWrappers = ArrayList<TOCLinkWrapper>()
                for (tocLink in publication.tableOfContents) {
                    val tocLinkWrapper = createTocLinkWrapper(tocLink, 0)
                    tocLinkWrappers.add(tocLinkWrapper)
                }
                onLoadTOC(tocLinkWrappers)
            } else {
                onLoadTOC(createTOCFromSpine(publication.readingOrder) ?: ArrayList())
            }
        } else {
            onError()
        }
    }


    private fun onLoadTOC(tocLinkWrapperList: ArrayList<TOCLinkWrapper>) {
        mTOCAdapter = TOCAdapter(
            this, tocLinkWrapperList,
            spine!![currentChapterIndex].href, config
        )
        mTOCAdapter.setCallback(this)
        binding.chaptersRecyclerView.adapter = mTOCAdapter
    }

    private fun onError() {
        binding.chaptersRecyclerView.visibility = View.GONE
    }


    private lateinit var config: Config
    private fun initActionBar() {

        setSupportActionBar(binding.toolbar)

        config = AppUtil.getSavedConfig(applicationContext)
        isNightMode = config.isNightMode

//        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_drawer)
//        UiUtil.setColorIntToDrawable(config.themeColor, drawable!!)
//        binding.toolbar.navigationIcon = drawable

        updateThemes()

    }

    private fun updateThemes() {
        val color = if (isNightMode) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            val attrs = intArrayOf(android.R.attr.navigationBarColor)
            val typedArray = theme.obtainStyledAttributes(attrs)
            typedArray.getColor(0, ContextCompat.getColor(this, R.color.white))
        }
        window.navigationBarColor = color

        if (isNightMode) {
            setNightMode()
        } else {
            setDayMode()
        }
    }


    override fun setDayMode() {
        Log.v(LOG_TAG, "-> setDayMode")

        binding.toolbar.background = ColorDrawable(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))

        binding.llChapters.background = ColorDrawable(ContextCompat.getColor(this, R.color.white))
        binding.chapterTitleTextView.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    override fun setNightMode() {
        binding.toolbar.background = ColorDrawable(ContextCompat.getColor(this, R.color.black))
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(
                this,
                R.color.night_title_text_color
            )
        )

        binding.llChapters.background = ColorDrawable(ContextCompat.getColor(this, R.color.black))
        binding.chapterTitleTextView.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    override fun updatePages(currentPages: Int, totalPages: Int) {
        binding.pageInfo.tvPages.text = getString(R.string.pages_format, currentPages, totalPages)


//        val chapterName = pubBox?.publication?.tableOfContents?.get(currentChapterIndex)?.title ?: return
//        binding.pageInfo.tvChapterName.text = getString(R.string.chapter_name_format, chapterName)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    private fun initMediaController() {
        mediaControllerFragment = MediaControllerFragment.getInstance(supportFragmentManager, this)
    }

    private fun initSettingUI() {
        binding.settingLayout.closeImageView.setOnClickListener {
            toggleEndDrawer()
        }
        val config = AppUtil.getSavedConfig(this)
        binding.settingLayout.config = config
        binding.settingLayout.autoScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.isAutoScroll = isChecked
            saveConfig(config)
        }
        binding.settingLayout.autoScrollIntervalSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                config.intervalAutoScroll = (value * 15).toInt()
                saveConfig(config)
            }
        }
        binding.settingLayout.increaseAutoScrollIntervalImageView.setOnClickListener {
            if (config.intervalAutoScroll < 120) {
                config.intervalAutoScroll = config.intervalAutoScroll + 5
                saveConfig(config)
            }
        }
        binding.settingLayout.reduceAutoScrollIntervalImageView.setOnClickListener {
            if (config.intervalAutoScroll > 5) {
                config.intervalAutoScroll = config.intervalAutoScroll - 5
            } else if (config.intervalAutoScroll > 0) {
                config.intervalAutoScroll = 0
            }
            saveConfig(config)
        }
        binding.settingLayout.continuousAutoScrollSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isContinuousAutoScroll = isChecked
            AppUtil.saveConfig(this, config)
        }
        binding.settingLayout.contiguousAutoScrollIntervalSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                config.intervalContinuousAutoScroll = value.toInt()
                saveConfig(config)
            }
        }
        binding.settingLayout.increaseContiguousAutoScrollIntervalImageView.setOnClickListener {
            if (config.intervalContinuousAutoScroll < 100) {
                config.intervalContinuousAutoScroll = config.intervalContinuousAutoScroll + 5
                saveConfig(config)
            }
        }
        binding.settingLayout.reduceContiguousAutoScrollIntervalImageView.setOnClickListener {
            if (config.intervalContinuousAutoScroll > 5) {
                config.intervalContinuousAutoScroll = config.intervalContinuousAutoScroll - 5
            } else if (config.intervalContinuousAutoScroll > 0) {
                config.intervalContinuousAutoScroll = 0
            }
            saveConfig(config)
        }

        binding.settingLayout.wholePageSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isWholePageAtATime = isChecked
            saveConfig(config)
        }
        binding.settingLayout.volumeToControlSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isUseVolumeForControlNavigation = isChecked
            saveConfig(config)
        }
        binding.settingLayout.swipeBrightnessSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isSwipeForControlBrightness = isChecked
            saveConfig(config)
        }
        binding.settingLayout.blueLightSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                config.lightFilter = value.toInt()
                saveConfig(config)
            }
        }
        binding.settingLayout.increaseBlueLightImageView.setOnClickListener {
            if (config.lightFilter < 100) {
                config.lightFilter = config.lightFilter + 5
                saveConfig(config)
            }
        }
        binding.settingLayout.reduceBlueLightImageView.setOnClickListener {
            if (config.lightFilter > 5) {
                config.lightFilter = config.lightFilter - 5
            } else if (config.lightFilter > 0) {
                config.lightFilter = 0
            }
            saveConfig(config)
        }
        binding.settingLayout.inactiveDimSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isDimOnInactive = isChecked
            saveConfig(config)
        }
        binding.settingLayout.shakeSsSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isShakeToTakeScreenShort = isChecked
            saveConfig(config)
        }
        binding.settingLayout.alignmentJustifySwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isJustifiedAlignment = isChecked
            saveConfig(config)
        }
        binding.settingLayout.lineHeightSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                config.lineHeight = value.toInt()
                saveConfig(config)
            }
        }
        binding.settingLayout.increaseLineHeightImageView.setOnClickListener {
            if (config.lineHeight < 100) {
                config.lineHeight = config.lineHeight + 5
                saveConfig(config)
            }
        }
        binding.settingLayout.reduceLineHeightImageView.setOnClickListener {
            if (config.lineHeight > 5) {
                config.lineHeight = config.lineHeight - 5
            } else if (config.lineHeight > 0) {
                config.lineHeight = 0
            }
            saveConfig(config)
        }
        binding.settingLayout.hyphenationSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            config.isHyphenation = isChecked
            saveConfig(config)
        }
        binding.settingLayout.inactiveDimTimesRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->
            when (checkedId) {
                R.id.five_min_radio_btn -> {
                    config.dimOnInactiveTime = 5
                }
                R.id.ten_min_radio_btn -> {
                    config.dimOnInactiveTime = 10
                }
                R.id.fifteen_min_radio_btn -> {
                    config.dimOnInactiveTime = 15
                }
            }
            saveConfig(config)
        }
    }

    private fun saveConfig(config: Config) {
        AppUtil.saveConfig(this, config)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val config = AppUtil.getSavedConfig(applicationContext)!!
        UiUtil.setColorIntToDrawable(config.themeColor, menu.findItem(R.id.itemSearch).icon)
        UiUtil.setColorIntToDrawable(config.themeColor, menu.findItem(R.id.itemConfig).icon)
        UiUtil.setColorIntToDrawable(config.themeColor, menu.findItem(R.id.itemTts).icon)
        UiUtil.setColorIntToDrawable(config.themeColor, menu.findItem(R.id.itemSetting).icon)

        if (!config.isShowTts)
            menu.findItem(R.id.itemTts).isVisible = false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //Log.d(LOG_TAG, "-> onOptionsItemSelected -> " + item.getItemId());

        when (item.itemId) {
            android.R.id.home -> {
                Log.v(LOG_TAG, "-> onOptionsItemSelected -> drawer")
//                startContentHighlightActivity()
                toggleStartDrawer()
                return true

            }
            R.id.itemSearch -> {
                Log.v(LOG_TAG, "-> onOptionsItemSelected -> " + item.title)
                if (searchUri == null)
                    return true
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra(SearchActivity.BUNDLE_SPINE_SIZE, spine?.size ?: 0)
                intent.putExtra(SearchActivity.BUNDLE_SEARCH_URI, searchUri)
                intent.putExtra(SearchAdapter.DATA_BUNDLE, searchAdapterDataBundle)
                intent.putExtra(SearchActivity.BUNDLE_SAVE_SEARCH_QUERY, searchQuery)
                startActivityForResult(intent, RequestCode.SEARCH.value)
                return true

            }
            R.id.itemConfig -> {
                Log.v(LOG_TAG, "-> onOptionsItemSelected -> " + item.title)
                showConfigBottomSheetDialogFragment()
                return true

            }
            R.id.itemTts -> {
                Log.v(LOG_TAG, "-> onOptionsItemSelected -> " + item.title)
                showMediaController()
                return true
            }
            R.id.itemSetting -> {
                toggleEndDrawer()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }

    }

    private fun toggleEndDrawer() {
        if (binding.mainDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.mainDrawer.closeDrawer(GravityCompat.END)
        } else {
            binding.mainDrawer.openDrawer(GravityCompat.END)
        }
    }

    private fun toggleStartDrawer() {
        if (binding.mainDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.mainDrawer.closeDrawer(GravityCompat.START)
        } else {
            binding.mainDrawer.openDrawer(GravityCompat.START)
            initAdapter()
        }
    }

    fun startContentHighlightActivity() {

        val intent = Intent(this@FolioActivity, ContentHighlightActivity::class.java)

        intent.putExtra(Constants.PUBLICATION, pubBox!!.publication)
        try {
            intent.putExtra(CHAPTER_SELECTED, spine!![currentChapterIndex].href)
        } catch (e: NullPointerException) {
            Log.w(LOG_TAG, "-> ", e)
            intent.putExtra(CHAPTER_SELECTED, "")
        } catch (e: IndexOutOfBoundsException) {
            Log.w(LOG_TAG, "-> ", e)
            intent.putExtra(CHAPTER_SELECTED, "")
        }

        intent.putExtra(FolioReader.EXTRA_BOOK_ID, mBookId)
        intent.putExtra(Constants.BOOK_TITLE, bookFileName)

        startActivityForResult(intent, RequestCode.CONTENT_HIGHLIGHT.value)
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up)
    }

    fun showConfigBottomSheetDialogFragment() {
        ConfigBottomSheetDialogFragment().show(
            supportFragmentManager,
            ConfigBottomSheetDialogFragment.LOG_TAG
        )
    }

    private fun showBookmarkHighlightAndNote() {
        BookmarkHighlightAndNoteFragment().show(
            supportFragmentManager,
            BookmarkHighlightAndNoteFragment.TAG
        )
    }

    fun showMediaController() {
        mediaControllerFragment!!.show(supportFragmentManager)
    }

    private fun setupBook() {
        Log.v(LOG_TAG, "-> setupBook")
        try {
            initBook()
            onBookInitSuccess()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "-> Failed to initialize book", e)
            onBookInitFailure()
        }

    }

    @Throws(Exception::class)
    private fun initBook() {
        Log.v(LOG_TAG, "-> initBook")

        bookFileName = FileUtil.getEpubFilename(this, mEpubSourceType!!, mEpubFilePath, mEpubRawId)
        val path = FileUtil.saveEpubFileAndLoadLazyBook(
            this, mEpubSourceType, mEpubFilePath,
            mEpubRawId, bookFileName
        )
        val extension: Publication.EXTENSION
        var extensionString: String? = null
        try {
            extensionString = FileUtil.getExtensionUppercase(path)
            extension = Publication.EXTENSION.valueOf(extensionString)
        } catch (e: IllegalArgumentException) {
            throw Exception("-> Unknown book file extension `$extensionString`", e)
        }

        pubBox = when (extension) {
            Publication.EXTENSION.EPUB -> {
                val epubParser = EpubParser()
                epubParser.parse(path!!, "")
            }
            Publication.EXTENSION.CBZ -> {
                val cbzParser = CbzParser()
                cbzParser.parse(path!!, "")
            }
            else -> {
                null
            }
        }



        portNumber =
            intent.getIntExtra(FolioReader.EXTRA_PORT_NUMBER, Constants.DEFAULT_PORT_NUMBER)
        portNumber = AppUtil.getAvailablePortNumber(portNumber)

        r2StreamerServer = Server(portNumber)
        r2StreamerServer!!.addEpub(
            pubBox!!.publication, pubBox!!.container,
            "/" + bookFileName!!, null
        )

        r2StreamerServer!!.start()

        FolioReader.initRetrofit(streamerUrl)
    }

    private fun onBookInitFailure() {
        //TODO -> Fail gracefully
    }

    private fun onBookInitSuccess() {

        val publication = pubBox!!.publication
        spine = publication.readingOrder
        title = publication.metadata.title

        if (mBookId == null) {
            if (!publication.metadata.identifier.isEmpty()) {
                mBookId = publication.metadata.identifier
            } else {
                if (!publication.metadata.title.isEmpty()) {
                    mBookId = publication.metadata.title.hashCode().toString()
                } else {
                    mBookId = bookFileName!!.hashCode().toString()
                }
            }
        }

        // searchUri currently not in use as it's uri is constructed through Retrofit,
        // code kept just in case if required in future.
        for (link in publication.links) {
            if (link.rel.contains("search")) {
                searchUri = Uri.parse("http://" + link.href!!)
                break
            }
        }
        if (searchUri == null)
            searchUri = Uri.parse(streamerUrl + "search")

        configFolio()
    }

    override fun onBackPressed() {
        when {
            binding.mainDrawer.isDrawerOpen(GravityCompat.START) -> {
                binding.mainDrawer.closeDrawer(GravityCompat.START)
            }
            binding.mainDrawer.isDrawerOpen(GravityCompat.END) -> {
                binding.mainDrawer.closeDrawer(GravityCompat.END)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun getStreamerUrl(): String {

        if (streamerUri == null) {
            streamerUri =
                Uri.parse(String.format(STREAMER_URL_TEMPLATE, LOCALHOST, portNumber, bookFileName))
        }
        return streamerUri.toString()
    }

    override fun onDirectionChange(newDirection: Config.Direction) {
        Log.v(LOG_TAG, "-> onDirectionChange")

        var folioPageFragment: FolioPageFragment? = currentFragment ?: return
        entryReadLocator = folioPageFragment!!.getLastReadLocator()
        val searchLocatorVisible = folioPageFragment.searchLocatorVisible

        direction = newDirection

        binding.folioPageViewPager.setDirection(newDirection)
        mFolioPageFragmentAdapter = FolioPageFragmentAdapter(
            supportFragmentManager,
            spine, bookFileName, mBookId
        )
        binding.folioPageViewPager.adapter = mFolioPageFragmentAdapter
        binding.folioPageViewPager.currentItem = currentChapterIndex

        folioPageFragment = currentFragment ?: return
        searchLocatorVisible?.let {
            folioPageFragment.highlightSearchLocator(searchLocatorVisible)
        }
    }

    private fun initDistractionFreeMode(savedInstanceState: Bundle?) {
        Log.v(LOG_TAG, "-> initDistractionFreeMode")

        window.decorView.setOnSystemUiVisibilityChangeListener(this)

        // Deliberately Hidden and shown to make activity contents lay out behind SystemUI
//        hideSystemUI()
//        showSystemUI()

//        distractionFreeMode =
//            savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_DISTRACTION_FREE_MODE)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Log.v(LOG_TAG, "-> onPostCreate")

//        if (distractionFreeMode) {
//            handler!!.post { hideSystemUI() }
//        }
    }

    /**
     * @return returns height of status bar + app bar as requested by param [DisplayUnit]
     */
    override fun getTopDistraction(unit: DisplayUnit): Int {

        var topDistraction = 0
        if (hasFullScreen /*!distractionFreeMode*/) {
            topDistraction = statusBarHeight
            if (actionBar != null)
                topDistraction += actionBar!!.height
        }

        when (unit) {
            DisplayUnit.PX -> return topDistraction

            DisplayUnit.DP -> {
                topDistraction /= density.toInt()
                return topDistraction
            }

            else -> throw IllegalArgumentException("-> Illegal argument -> unit = $unit")
        }
    }

    /**
     * Calculates the bottom distraction which can cause due to navigation bar.
     * In mobile landscape mode, navigation bar is either to left or right of the screen.
     * In tablet, navigation bar is always at bottom of the screen.
     *
     * @return returns height of navigation bar as requested by param [DisplayUnit]
     */
    override fun getBottomDistraction(unit: DisplayUnit): Int {

        var bottomDistraction = 0
        if (hasFullScreen /*!distractionFreeMode*/)
            bottomDistraction = binding.appBarLayout.navigationBarHeight

        when (unit) {
            DisplayUnit.PX -> return bottomDistraction

            DisplayUnit.DP -> {
                bottomDistraction /= density.toInt()
                return bottomDistraction
            }

            else -> throw IllegalArgumentException("-> Illegal argument -> unit = $unit")
        }
    }

    /**
     * Calculates the Rect for visible viewport of the webview in PX.
     * Visible viewport changes in following cases -
     * 1. In distraction free mode,
     * 2. In mobile landscape mode as navigation bar is placed either on left or right side,
     * 3. In tablets, navigation bar is always placed at bottom of the screen.
     */
    private fun computeViewportRect(): Rect {
        //Log.v(LOG_TAG, "-> computeViewportRect");

        val viewportRect = Rect(binding.appBarLayout.insets)
        if (!hasFullScreen/*distractionFreeMode*/)
            viewportRect.left = 0
        viewportRect.top = getTopDistraction(DisplayUnit.PX)
        if (!hasFullScreen/*distractionFreeMode*/) {
            viewportRect.right = displayMetrics!!.widthPixels
        } else {
            viewportRect.right = displayMetrics!!.widthPixels - viewportRect.right
        }
        viewportRect.bottom = displayMetrics!!.heightPixels - getBottomDistraction(DisplayUnit.PX)

        return viewportRect
    }

    override fun getViewportRect(unit: DisplayUnit): Rect {

        val viewportRect = computeViewportRect()
        when (unit) {
            DisplayUnit.PX -> return viewportRect

            DisplayUnit.DP -> {
                viewportRect.left /= density.toInt()
                viewportRect.top /= density.toInt()
                viewportRect.right /= density.toInt()
                viewportRect.bottom /= density.toInt()
                return viewportRect
            }

            DisplayUnit.CSS_PX -> {
                viewportRect.left = Math.ceil((viewportRect.left / density).toDouble()).toInt()
                viewportRect.top = Math.ceil((viewportRect.top / density).toDouble()).toInt()
                viewportRect.right = Math.ceil((viewportRect.right / density).toDouble()).toInt()
                viewportRect.bottom = Math.ceil((viewportRect.bottom / density).toDouble()).toInt()
                return viewportRect
            }

            else -> throw IllegalArgumentException("-> Illegal argument -> unit = $unit")
        }
    }

    override fun getActivity(): WeakReference<FolioActivity> {
        return WeakReference(this)
    }

    override fun onSystemUiVisibilityChange(visibility: Int) {
        Log.v(LOG_TAG, "-> onSystemUiVisibilityChange -> visibility = $visibility")

//        distractionFreeMode = visibility != View.SYSTEM_UI_FLAG_VISIBLE
//        Log.v(LOG_TAG, "-> distractionFreeMode = $distractionFreeMode")
//
//        if (actionBar != null) {
//            if (distractionFreeMode) {
//                actionBar!!.hide()
//            } else {
//                actionBar!!.show()
//            }
//        }
    }

    override fun toggleSystemUI() {

//        hasFullScreen = distractionFreeMode
//        if (distractionFreeMode) {
//            showSystemUI()
//        } else {
//            hideSystemUI()
//        }
        binding.appBarLayout.visibility = View.VISIBLE
        binding.llPages.visibility = View.VISIBLE
    }

    private fun fullScreenMode() {
        if (hasFullScreen) {
            binding.appBarLayout.visibility = View.GONE
            binding.llPages.visibility = View.GONE
        } else {
            binding.appBarLayout.visibility = View.VISIBLE
            binding.llPages.visibility = View.VISIBLE
        }
    }


    private fun showSystemUI() {
        Log.v(LOG_TAG, "-> showSystemUI")

        if (Build.VERSION.SDK_INT >= 16) {
            val decorView = window.decorView
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            binding.appBarLayout.setTopMargin(statusBarHeight)
            onSystemUiVisibilityChange(View.SYSTEM_UI_FLAG_VISIBLE)
        }
    }

    private fun hideSystemUI() {
        Log.v(LOG_TAG, "-> hideSystemUI")

        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun getEntryReadLocator(): ReadLocator? {
        if (entryReadLocator != null) {
            val tempReadLocator = entryReadLocator
            entryReadLocator = null
            return tempReadLocator
        }
        return null
    }

    /**
     * Go to chapter specified by href
     *
     * @param href http link or relative link to the page or to the anchor
     * @return true if href is of EPUB or false if other link
     */
    override fun goToChapter(href: String): Boolean {

        for (link in spine!!) {
            if (href.contains(link.href!!)) {
                currentChapterIndex = spine!!.indexOf(link)
                binding.folioPageViewPager.currentItem = currentChapterIndex
                val folioPageFragment = currentFragment
                folioPageFragment!!.scrollToFirst()
                folioPageFragment.scrollToAnchorId(href)
                return true
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.SEARCH.value) {
            Log.v(LOG_TAG, "-> onActivityResult -> " + RequestCode.SEARCH)

            if (resultCode == Activity.RESULT_CANCELED)
                return

            searchAdapterDataBundle = data!!.getBundleExtra(SearchAdapter.DATA_BUNDLE)
            searchQuery = data.getCharSequenceExtra(SearchActivity.BUNDLE_SAVE_SEARCH_QUERY)

            if (resultCode == SearchActivity.ResultCode.ITEM_SELECTED.value) {

                searchLocator = data.getParcelableExtra(EXTRA_SEARCH_ITEM)
                // In case if SearchActivity is recreated due to screen rotation then FolioActivity
                // will also be recreated, so mFolioPageViewPager might be null.
                currentChapterIndex = getChapterIndex(Constants.HREF, searchLocator!!.href)
                binding.folioPageViewPager.currentItem = currentChapterIndex
                val folioPageFragment = currentFragment ?: return
                folioPageFragment.highlightSearchLocator(searchLocator!!)
                searchLocator = null
            }

        } else if (requestCode == RequestCode.CONTENT_HIGHLIGHT.value && resultCode == Activity.RESULT_OK &&
            data!!.hasExtra(TYPE)
        ) {

            val type = data.getStringExtra(TYPE)

            if (type == CHAPTER_SELECTED) {
                goToChapter(data.getStringExtra(SELECTED_CHAPTER_POSITION) ?: "")

            } else if (type == HIGHLIGHT_SELECTED) {
                val highlightImpl = data.getParcelableExtra<HighlightImpl>(HIGHLIGHT_ITEM)
                currentChapterIndex = highlightImpl!!.pageNumber
                binding.folioPageViewPager.currentItem = currentChapterIndex
                val folioPageFragment = currentFragment ?: return
                folioPageFragment.scrollToHighlightId(highlightImpl.rangy)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (outState != null)
            outState!!.putSerializable(BUNDLE_READ_LOCATOR_CONFIG_CHANGE, lastReadLocator)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.unregisterReceiver(searchReceiver)
        localBroadcastManager.unregisterReceiver(closeBroadcastReceiver)

        if (r2StreamerServer != null)
            r2StreamerServer!!.stop()

        if (isFinishing) {
            localBroadcastManager.sendBroadcast(Intent(FolioReader.ACTION_FOLIOREADER_CLOSED))
            FolioReader.get().retrofit = null
            FolioReader.get().r2StreamerApi = null
        }
    }

    override fun getCurrentChapterIndex(): Int {
        return currentChapterIndex
    }

    private var chapterCounter = 0

    private fun configFolio() {

        // Replacing with addOnPageChangeListener(), onPageSelected() is not invoked
        binding.folioPageViewPager.setOnPageChangeListener(object :
            DirectionalViewpager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                Log.d(LOG_TAG, "-> onPageSelected -> position = $position")
                Log.d(LOG_TAG, "-> onPageSelected -> href = ${spine!![currentChapterIndex].href}")

                EventBus.getDefault().post(
                    MediaOverlayPlayPauseEvent(
                        spine!![currentChapterIndex].href, false, true
                    )
                )
                mediaControllerFragment!!.setPlayButtonDrawable()
                currentChapterIndex = position

                val chapters = pubBox?.publication?.tableOfContents
                if (chapters != null && chapterCounter < chapters.size) {
                    var chapterName = chapters[chapterCounter].title
                    val href = spine!![currentChapterIndex].href
                    val childrenLink = chapters[chapterCounter].children.firstOrNull {
                        it.href == href
                    }

                    if (chapters[chapterCounter].href == href) {
                        chapterName = chapters[chapterCounter].title
                    }
                    if (childrenLink != null) {
                        chapterName = childrenLink.title
                    }
                    if (chapters[chapterCounter].href != href && childrenLink == null) {
                        chapterCounter++
                    }

                    binding.pageInfo.tvChapterName.text =
                        getString(R.string.chapter_name_format, chapterName)
                }

            }

            override fun onPageScrollStateChanged(state: Int) {

                if (state == DirectionalViewpager.SCROLL_STATE_IDLE) {
                    val position = binding.folioPageViewPager.currentItem
                    Log.v(
                        LOG_TAG,
                        "->  onPageScrollStateChanged -> DirectionalViewpager -> " +
                                "position = " + position
                    )

                    var folioPageFragment =
                        mFolioPageFragmentAdapter!!.getItem(position - 1) as FolioPageFragment?
                    if (folioPageFragment != null) {
                        folioPageFragment.scrollToLast()
                        if (folioPageFragment.mWebview != null)
                            folioPageFragment.mWebview!!.dismissPopupWindow()
                    }

                    folioPageFragment =
                        mFolioPageFragmentAdapter!!.getItem(position + 1) as FolioPageFragment?
                    if (folioPageFragment != null) {
                        folioPageFragment.scrollToFirst()
                        if (folioPageFragment.mWebview != null)
                            folioPageFragment.mWebview!!.dismissPopupWindow()

//                        val chapters = pubBox?.publication?.tableOfContents
//                        if (chapters != null && chapterCounter < chapters.size) {
//                            val chapterName = chapters[chapterCounter++].title
//                            binding.pageInfo.tvChapterName.text = getString(R.string.chapter_name_format, chapterName)
//                        }
                    }
                }
            }
        })

        binding.folioPageViewPager.setDirection(direction)
        mFolioPageFragmentAdapter = FolioPageFragmentAdapter(
            supportFragmentManager,
            spine, bookFileName, mBookId
        )
        binding.folioPageViewPager.adapter = mFolioPageFragmentAdapter

        // In case if SearchActivity is recreated due to screen rotation then FolioActivity
        // will also be recreated, so searchLocator is checked here.
        if (searchLocator != null) {

            currentChapterIndex = getChapterIndex(Constants.HREF, searchLocator!!.href)
            binding.folioPageViewPager.currentItem = currentChapterIndex
            val folioPageFragment = currentFragment ?: return
            folioPageFragment.highlightSearchLocator(searchLocator!!)
            searchLocator = null

        } else {

            val readLocator: ReadLocator?
            if (savedInstanceState == null) {
                readLocator = intent.getParcelableExtra(FolioActivity.EXTRA_READ_LOCATOR)
                entryReadLocator = readLocator
            } else {
                readLocator = savedInstanceState!!.getParcelable(BUNDLE_READ_LOCATOR_CONFIG_CHANGE)
                lastReadLocator = readLocator
            }
            currentChapterIndex = getChapterIndex(readLocator)
            binding.folioPageViewPager.currentItem = currentChapterIndex
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            searchReceiver,
            IntentFilter(ACTION_SEARCH_CLEAR)
        )


        val chapters = pubBox?.publication?.tableOfContents ?: return
        val chapterName = chapters[chapterCounter++].title
        binding.pageInfo.tvChapterName.text =
            getString(R.string.chapter_name_format, chapterName)
    }

    private fun getChapterIndex(readLocator: ReadLocator?): Int {

        if (readLocator == null) {
            return 0
        } else if (!TextUtils.isEmpty(readLocator.href)) {
            return getChapterIndex(Constants.HREF, readLocator.href)
        }

        return 0
    }

    private fun getChapterIndex(caseString: String, value: String): Int {
        for (i in spine!!.indices) {
            when (caseString) {
                Constants.HREF -> if (spine!![i].href == value)
                    return i
            }
        }
        return 0
    }

    /**
     * If called, this method will occur after onStop() for applications targeting platforms
     * starting with Build.VERSION_CODES.P. For applications targeting earlier platform versions
     * this method will occur before onStop() and there are no guarantees about whether it will
     * occur before or after onPause()
     *
     * @see Activity.onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.v(LOG_TAG, "-> onSaveInstanceState")
        this.outState = outState

//        outState.putBoolean(BUNDLE_DISTRACTION_FREE_MODE, distractionFreeMode)
        outState.putBundle(SearchAdapter.DATA_BUNDLE, searchAdapterDataBundle)
        outState.putCharSequence(SearchActivity.BUNDLE_SAVE_SEARCH_QUERY, searchQuery)
    }

    override fun storeLastReadLocator(lastReadLocator: ReadLocator) {
        Log.v(LOG_TAG, "-> storeLastReadLocator")
        this.lastReadLocator = lastReadLocator
    }

    private fun setConfig(savedInstanceState: Bundle?) {

        val intentConfig = intent.getParcelableExtra<Config>(Config.INTENT_CONFIG)
        val overrideConfig = intent.getBooleanExtra(Config.EXTRA_OVERRIDE_CONFIG, false)
        val savedConfig = AppUtil.getSavedConfig(this)

        config = if (savedInstanceState != null) {
            savedConfig
        } else if (savedConfig == null) {
            intentConfig ?: Config()
        } else {
            if (intentConfig != null && overrideConfig) {
                intentConfig
            } else {
                savedConfig
            }
        }

        AppUtil.saveConfig(this, config)
        direction = savedConfig?.direction ?: Config.Direction.VERTICAL
    }

    override fun play() {
        EventBus.getDefault().post(
            MediaOverlayPlayPauseEvent(
                spine!![currentChapterIndex].href, true, false
            )
        )
    }

    override fun pause() {
        EventBus.getDefault().post(
            MediaOverlayPlayPauseEvent(
                spine!![currentChapterIndex].href, false, false
            )
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.WRITE_EXTERNAL_STORAGE_REQUEST -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBook()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.cannot_access_epub_message),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun getDirection(): Config.Direction {
        return direction
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.e(LOG_TAG, "onKeyDown: $keyCode")
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            useVolumeControlForNavigation(false)
            return false
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            useVolumeControlForNavigation(true)
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun clearSearchLocator() {
        Log.v(LOG_TAG, "-> clearSearchLocator")

        val fragments = mFolioPageFragmentAdapter!!.fragments
        for (i in fragments.indices) {
            val folioPageFragment = fragments[i] as FolioPageFragment?
            folioPageFragment?.clearSearchLocator()
        }

        val savedStateList = mFolioPageFragmentAdapter!!.savedStateList
        if (savedStateList != null) {
            for (i in savedStateList.indices) {
                val savedState = savedStateList[i]
                val bundle = FolioPageFragmentAdapter.getBundleFromSavedState(savedState)
                bundle?.putParcelable(FolioPageFragment.BUNDLE_SEARCH_LOCATOR, null)
            }
        }
    }

    override fun onTocClicked(position: Int) {
        val tocLinkWrapper = mTOCAdapter.getItemAt(position) as TOCLinkWrapper
        goToChapter(tocLinkWrapper.tocLink.href ?: "")
        toggleStartDrawer()
    }

    override fun onExpanded(position: Int) {
        val tocLinkWrapper = mTOCAdapter.getItemAt(position) as TOCLinkWrapper
        if (tocLinkWrapper.children != null && tocLinkWrapper.children.size > 0) {
            mTOCAdapter.toggleGroup(position)
        }
    }


    private fun useVolumeControlForNavigation(isNext: Boolean) {
        val config = AppUtil.getSavedConfig(this)
        if (!config.isUseVolumeForControlNavigation) return
        val currentPosition = binding.folioPageViewPager.currentItem
        Log.d(LOG_TAG, "useVolumeControlForNavigation: $currentPosition")
        if (isNext) {
            if (currentPosition >= 0 && currentPosition < mFolioPageFragmentAdapter?.count ?: 0) {
                binding.folioPageViewPager.currentItem = currentPosition + 1
            }
        } else {
            if (currentPosition > 0) {
                binding.folioPageViewPager.currentItem = currentPosition - 1
            }
        }
    }

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private fun observeAutoScrollingSetting() {
        val config = AppUtil.getSavedConfig(this)
        if (config.isAutoScroll) {
            autoScrollHandler.postDelayed({
                val folioPageFragment =
                    mFolioPageFragmentAdapter!!.getItem(binding.folioPageViewPager.currentItem) as FolioPageFragment?
                if (folioPageFragment != null) {
                    Log.d(LOG_TAG, "observeAutoScrollingSetting: ")
                    if (!folioPageFragment.scrollToNext()) {
                        binding.folioPageViewPager.currentItem =
                            binding.folioPageViewPager.currentItem + 1
                    }
                    observeAutoScrollingSetting()
                }
            }, config.intervalAutoScroll * 1000L)
        }
    }

    private fun setupSwipeObserver() {

    }

    private val runnable = {
        toggleDim()
    }
    private val dimObserverHandler = Handler(Looper.getMainLooper())
    private var dimEnable = false
    override fun onUserInteraction() {
        super.onUserInteraction()
        val config = AppUtil.getSavedConfig(this)
        if (config.isDimOnInactive) {
            if (dimEnable) {
                toggleDim()
            }
            dimObserverHandler.removeCallbacks(runnable)
            dimObserverHandler.postDelayed(
                runnable, config.dimOnInactiveTime
                        * 60
                        * 1000L
            )
            Log.d(LOG_TAG, "onUserInteraction: ")
        }
    }

    private fun setupDimInterval() {
        val config = AppUtil.getSavedConfig(this)
        if (config.isDimOnInactive) {
            dimObserverHandler.postDelayed(
                runnable, config.dimOnInactiveTime
                        * 60
                        * 1000L
            )
        }
    }

    private fun toggleDim() {
        val config = AppUtil.getSavedConfig(this)
        if (config.isDimOnInactive) {
            val layoutParams = window.attributes
            val alpha = layoutParams.alpha
            if (alpha == 1.0f) {
                layoutParams.alpha = 0.2f
                window.attributes = layoutParams
                dimEnable = true
                Log.d(LOG_TAG, "toggleDim: true")
            } else {
                layoutParams.alpha = 1.0f
                window.attributes = layoutParams
                dimEnable = false
                Log.d(LOG_TAG, "toggleDim: false")
            }
        }
    }
}