package com.shinku.reader.ui.reader.viewer

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import coil3.BitmapImage
import coil3.asDrawable
import coil3.dispose
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.ViewSizeResolver
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_IN_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import com.github.chrisbanes.photoview.PhotoView
import com.shinku.reader.core.common.util.system.ImageUtil
import com.shinku.reader.data.coil.TachiyomiImageDecoder
import com.shinku.reader.data.coil.allowUpscaling
import com.shinku.reader.data.coil.cropBorders
import com.shinku.reader.data.coil.customDecoder
import com.shinku.reader.data.coil.webtoonStrips
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.ui.reader.model.ReaderPage
import com.shinku.reader.ui.reader.viewer.webtoon.WebtoonStripImageView
import com.shinku.reader.ui.reader.viewer.webtoon.WebtoonSubsamplingImageView
import com.shinku.reader.util.system.animatorDurationScale
import com.shinku.reader.util.view.isVisibleOnScreen
import okio.BufferedSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max

/**
 * A wrapper view for showing page image.
 *
 * Animated image will be drawn by [PhotoView] while [SubsamplingScaleImageView] will take non-animated image.
 *
 * @param isWebtoon if true, [WebtoonStripImageView] will be used instead of [SubsamplingScaleImageView]
 * and [AppCompatImageView] will be used instead of [PhotoView]
 */
open class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttrs: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    private val isWebtoon: Boolean = false,
) : FrameLayout(context, attrs, defStyleAttrs, defStyleRes) {

    private val alwaysDecodeLongStripWithSSIV by lazy {
        Injekt.get<BasePreferences>().alwaysDecodeLongStripWithSSIV().get()
    }

    var pageView: View? = null
        private set

    fun mapScreenPointToSource(screenX: Float, screenY: Float): PointF? {
        val view = pageView ?: return null
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val localX = screenX - loc[0]
        val localY = screenY - loc[1]

        return when (view) {
            is SubsamplingScaleImageView -> {
                if (!view.isReady) return null
                val pt = PointF()
                view.viewToSourceCoord(PointF(localX, localY), pt)
            }
            is WebtoonStripImageView -> {
                val imgWidth = view.sourceWidth
                if (imgWidth <= 0 || view.width <= 0) return null
                val scale = view.width.toFloat() / imgWidth.toFloat()
                PointF(localX / scale, localY / scale)
            }
            is android.widget.ImageView -> {
                val drawable = view.drawable ?: return null
                val dWidth = drawable.intrinsicWidth
                val dHeight = drawable.intrinsicHeight
                if (dWidth <= 0 || dHeight <= 0 || view.width <= 0 || view.height <= 0) return null
                val scaleX = view.width.toFloat() / dWidth.toFloat()
                val scaleY = view.height.toFloat() / dHeight.toFloat()
                PointF(localX / scaleX, localY / scaleY)
            }
            else -> null
        }
    }

    fun mapScreenRectToSource(screenRect: android.graphics.Rect): android.graphics.Rect? {
        val topLeft = mapScreenPointToSource(screenRect.left.toFloat(), screenRect.top.toFloat()) ?: return null
        val bottomRight = mapScreenPointToSource(screenRect.right.toFloat(), screenRect.bottom.toFloat()) ?: return null
        return android.graphics.Rect(
            minOf(topLeft.x, bottomRight.x).toInt().coerceAtLeast(0),
            minOf(topLeft.y, bottomRight.y).toInt().coerceAtLeast(0),
            maxOf(topLeft.x, bottomRight.x).toInt(),
            maxOf(topLeft.y, bottomRight.y).toInt(),
        )
    }

    private var stripRequest: Disposable? = null

    private var config: Config? = null

    var currentPage: ReaderPage? = null
        set(value) {
            field = value
            translationOverlay?.page = value
        }

    private var translationOverlay: TranslationOverlay? = null

    private var panels: List<RectF> = emptyList()
    private var currentPanelIndex: Int = -1

    var onImageLoaded: (() -> Unit)? = null
    var onImageDimensionsLoaded: ((width: Int, height: Int) -> Unit)? = null
    var onImageLoadError: ((Throwable?) -> Unit)? = null
    var onScaleChanged: ((newScale: Float) -> Unit)? = null
    var onViewClicked: (() -> Unit)? = null

    /**
     * For automatic background. Will be set as background color when [onImageLoaded] is called.
     */
    var pageBackground: Drawable? = null

    @CallSuper
    open fun onImageLoaded() {
        onImageLoaded?.invoke()
        background = pageBackground

        val isLiveTranslation = Injekt.get<ShinKuPreferences>().liveTranslation().get()
        val page = currentPage
        if (page != null) {
            val language = Injekt.get<ShinKuPreferences>().translationTargetLanguage().get()
            if (page.translationLanguage != language) {
                page.translationBlocks = null
                page.showTranslation = false
            }
            val isChapterShowing = page.chapter.pages?.any { it.showTranslation } == true
            val shouldShow = isLiveTranslation || isChapterShowing
            if (page.translationBlocks == null) {
                try {
                    val chapterId = page.chapter.chapter.id ?: 0L
                    if (chapterId > 0L) {
                        val targetLang = Injekt.get<ShinKuPreferences>().translationTargetLanguage().get()
                        val cached = com.shinku.reader.ui.reader.model.TranslationCache.getMemory(chapterId, page.index, targetLang)
                        if (cached != null) {
                            page.translationBlocks = cached
                            page.translationLanguage = targetLang
                            page.showTranslation = shouldShow
                        }
                    }
                } catch (_: Throwable) {
                }
            } else if (shouldShow) {
                page.showTranslation = true
            }
        }
        if (currentPage?.showTranslation == true) {
            updateTranslationOverlay()
        }
    }

    @CallSuper
    open fun onImageLoadError(error: Throwable?) {
        onImageLoadError?.invoke(error)
    }

    @CallSuper
    open fun onScaleChanged(newScale: Float) {
        onScaleChanged?.invoke(newScale)
    }

    @CallSuper
    open fun onViewClicked() {
        onViewClicked?.invoke()
    }

    open fun onPageSelected(forward: Boolean) {
        with(pageView as? SubsamplingScaleImageView) {
            if (this == null) return
            if (isReady) {
                landscapeZoom(forward)
            } else {
                setOnImageEventListener(
                    object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        override fun onReady() {
                            setupZoom(config)
                            landscapeZoom(forward)
                            this@ReaderPageImageView.onImageLoaded()
                        }

                        override fun onImageLoadError(e: Exception) {
                            onImageLoadError(e)
                        }
                    },
                )
            }
        }
    }

    private fun SubsamplingScaleImageView.landscapeZoom(forward: Boolean) {
        val config = config
        if (config != null &&
            config.landscapeZoom &&
            config.minimumScaleType == SCALE_TYPE_CENTER_INSIDE &&
            sWidth > sHeight &&
            scale == minScale
        ) {
            handler?.postDelayed(500) {
                val point = when (config.zoomStartPosition) {
                    ZoomStartPosition.LEFT -> if (forward) PointF(0F, 0F) else PointF(sWidth.toFloat(), 0F)
                    ZoomStartPosition.RIGHT -> if (forward) PointF(sWidth.toFloat(), 0F) else PointF(0F, 0F)
                    ZoomStartPosition.CENTER -> center
                }

                val targetScale = height.toFloat() / sHeight.toFloat()
                (animateScaleAndCenter(targetScale, point) ?: return@postDelayed)
                    .withDuration(500)
                    .withEasing(EASE_IN_OUT_QUAD)
                    .withInterruptible(true)
                    .start()
            }
        }
    }

    fun setImage(drawable: Drawable, config: Config) {
        this.config = config
        if (drawable is Animatable) {
            prepareAnimatedImageView()
            setAnimatedImage(drawable, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(drawable, config)
        }
    }

    fun setImage(source: BufferedSource, isAnimated: Boolean, config: Config) {
        this.config = config
        if (isAnimated) {
            prepareAnimatedImageView()
            setAnimatedImage(source, config)
        } else if (isWebtoon && !alwaysDecodeLongStripWithSSIV) {
            prepareStripImageView()
            setStripImage(source, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(source, config)
        }
    }

    /**
     * Set panels for guided view.
     */
    fun setPanels(panels: List<RectF>) {
        this.panels = panels
        this.currentPanelIndex = -1
    }

    /**
     * Move to the next panel. Returns true if moved, false if no more panels.
     */
    fun moveToNextPanel(): Boolean {
        val view = pageView as? SubsamplingScaleImageView ?: return false
        if (panels.isEmpty()) return false

        if (currentPanelIndex < panels.size - 1) {
            currentPanelIndex++
            animateToPanel(panels[currentPanelIndex])
            return true
        }
        return false
    }

    /**
     * Move to the previous panel. Returns true if moved, false if at the beginning.
     */
    fun moveToPreviousPanel(): Boolean {
        val view = pageView as? SubsamplingScaleImageView ?: return false
        if (panels.isEmpty()) return false

        if (currentPanelIndex > 0) {
            currentPanelIndex--
            animateToPanel(panels[currentPanelIndex])
            return true
        } else if (currentPanelIndex == 0) {
            currentPanelIndex = -1
            view.animateScaleAndCenter(view.minScale, view.center)
                ?.withDuration(250)
                ?.start()
            return true
        }
        return false
    }

    private fun animateToPanel(panel: RectF) {
        val view = pageView as? SubsamplingScaleImageView ?: return

        // Calculate scale to fit panel
        val scaleX = width.toFloat() / panel.width()
        val scaleY = height.toFloat() / panel.height()
        val targetScale = minOf(scaleX, scaleY, view.maxScale)

        val targetCenter = PointF(panel.centerX(), panel.centerY())

        view.animateScaleAndCenter(targetScale, targetCenter)
            ?.withDuration(400)
            ?.withEasing(EASE_IN_OUT_QUAD)
            ?.start()
    }

    fun recycle() {
        stripRequest?.dispose()
        stripRequest = null
        pageView?.let {
            when (it) {
                is SubsamplingScaleImageView -> it.recycle()
                is AppCompatImageView -> {
                    it.dispose()
                    it.setImageDrawable(null)
                }
                is WebtoonStripImageView -> it.recycle()
            }
            it.isVisible = false
        }
    }

    /**
     * Check if the image can be panned to the left
     */
    fun canPanLeft(): Boolean = canPan { it.left }

    /**
     * Check if the image can be panned to the right
     */
    fun canPanRight(): Boolean = canPan { it.right }

    /**
     * Check whether the image can be panned.
     * @param fn a function that returns the direction to check for
     */
    private fun canPan(fn: (RectF) -> Float): Boolean {
        (pageView as? SubsamplingScaleImageView)?.let { view ->
            RectF().let {
                view.getPanRemaining(it)
                return fn(it) > 1
            }
        }
        return false
    }

    /**
     * Pans the image to the left by a screen's width worth.
     */
    fun panLeft() {
        pan { center, view -> center.also { it.x -= view.width / view.scale } }
    }

    /**
     * Pans the image to the right by a screen's width worth.
     */
    fun panRight() {
        pan { center, view -> center.also { it.x += view.width / view.scale } }
    }

    /**
     * Pans the image.
     * @param fn a function that computes the new center of the image
     */
    private fun pan(fn: (PointF, SubsamplingScaleImageView) -> PointF) {
        (pageView as? SubsamplingScaleImageView)?.let { view ->

            val target = fn(view.center ?: return, view)
            view.animateCenter(target)!!
                .withEasing(EASE_OUT_QUAD)
                .withDuration(250)
                .withInterruptible(true)
                .start()
        }
    }

    private fun prepareNonAnimatedImageView() {
        if (pageView is SubsamplingScaleImageView) return
        removeView(pageView)

        pageView = if (isWebtoon) {
            WebtoonSubsamplingImageView(context)
        } else {
            SubsamplingScaleImageView(context)
        }.apply {
            setMaxTileSize(ImageUtil.hardwareBitmapThreshold)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumTileDpi(180)
            setOnStateChangedListener(
                object : SubsamplingScaleImageView.OnStateChangedListener {
                    override fun onScaleChanged(newScale: Float, origin: Int) {
                        this@ReaderPageImageView.onScaleChanged(newScale)
                        translationOverlay?.invalidate()
                    }

                    override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                        translationOverlay?.invalidate()
                    }
                },
            )
            setOnClickListener { this@ReaderPageImageView.onViewClicked() }
        }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
        ensureTranslationOverlay()
    }

    private fun SubsamplingScaleImageView.setupZoom(config: Config?) {
        // 5x zoom
        maxScale = scale * MAX_ZOOM_SCALE
        setDoubleTapZoomScale(scale * 2)

        when (config?.zoomStartPosition) {
            ZoomStartPosition.LEFT -> setScaleAndCenter(scale, PointF(0F, 0F))
            ZoomStartPosition.RIGHT -> setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0F))
            ZoomStartPosition.CENTER -> setScaleAndCenter(scale, center)
            null -> {}
        }
    }

    private fun setNonAnimatedImage(
        data: Any,
        config: Config,
    ) = (pageView as? SubsamplingScaleImageView)?.apply {
        setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
        setMinimumScaleType(config.minimumScaleType)
        setMinimumDpi(1) // Just so that very small image will be fit for initial load
        setCropBorders(config.cropBorders)
        setOnImageEventListener(
            object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    setupZoom(config)
                    if (isVisibleOnScreen()) landscapeZoom(true)
                    this@ReaderPageImageView.onImageDimensionsLoaded?.invoke(sWidth, sHeight)
                    this@ReaderPageImageView.onImageLoaded()
                }

                override fun onImageLoadError(e: Exception) {
                    this@ReaderPageImageView.onImageLoadError(e)
                }
            },
        )

        when (data) {
            is BitmapDrawable -> {
                setImage(ImageSource.bitmap(data.bitmap))
                isVisible = true
            }
            is BufferedSource -> {
                val aiUpscaling = Injekt.get<ShinKuPreferences>().aiUpscaling().get()
                val isLowRes = if (aiUpscaling) {
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(data.peek().inputStream(), null, options)
                        options.outWidth in 1..<1500 && options.outHeight in 1..<1500
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    false
                }

                if (!isLowRes && (!isWebtoon || alwaysDecodeLongStripWithSSIV)) {
                    setHardwareConfig(ImageUtil.canUseHardwareBitmap(data))
                    setImage(ImageSource.inputStream(data.inputStream()))
                    isVisible = true
                    return@apply
                }

                ImageRequest.Builder(context)
                    .data(data)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .target(
                        onSuccess = { result ->
                            val image = result as BitmapImage
                            setImage(ImageSource.bitmap(image.bitmap))
                            isVisible = true
                        },
                    )
                    .listener(
                        onError = { _, result ->
                            onImageLoadError(result.throwable)
                        },
                    )
                    .size(ViewSizeResolver(this@ReaderPageImageView))
                    .precision(Precision.INEXACT)
                    .cropBorders(config.cropBorders)
                    .customDecoder(true)
                    .allowUpscaling(aiUpscaling)
                    .crossfade(false)
                    .build()
                    .let(context.imageLoader::enqueue)
            }
            else -> {
                throw IllegalArgumentException("Not implemented for class ${data::class.simpleName}")
            }
        }
    }

    private fun prepareStripImageView() {
        if (pageView is WebtoonStripImageView) return
        removeView(pageView)

        pageView = WebtoonStripImageView(context)
        addView(pageView, MATCH_PARENT, WRAP_CONTENT)
        ensureTranslationOverlay()
    }

    private fun setStripImage(data: BufferedSource, config: Config) {
        val view = pageView as? WebtoonStripImageView ?: return
        view.onReload = { requestStrips(view, data, config, isReload = true) }
        requestStrips(view, data, config, isReload = false)
    }

    private fun requestStrips(view: WebtoonStripImageView, data: BufferedSource, config: Config, isReload: Boolean) {
        stripRequest?.dispose()
        stripRequest = ImageRequest.Builder(context)
            .data(data.peek())
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { result ->
                    view.setImage(result as TachiyomiImageDecoder.StripsImage)
                    if (!isReload) {
                        view.isVisible = true
                        onImageLoaded()
                    }
                },
            )
            .listener(
                onError = { _, result ->
                    onImageLoadError(result.throwable)
                },
            )
            .size(ViewSizeResolver(this))
            .precision(Precision.INEXACT)
            .cropBorders(config.cropBorders)
            .webtoonStrips(true)
            .crossfade(false)
            .build()
            .let(context.imageLoader::enqueue)
    }

    private fun prepareAnimatedImageView() {
        if (pageView is AppCompatImageView) return
        removeView(pageView)

        pageView = if (isWebtoon) {
            AppCompatImageView(context)
        } else {
            PhotoView(context)
        }.apply {
            adjustViewBounds = true

            if (this is PhotoView) {
                setScaleLevels(1F, 2F, MAX_ZOOM_SCALE)
                // Force 2 scale levels on double tap
                setOnDoubleTapListener(
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            if (scale > 1F) {
                                setScale(1F, e.x, e.y, true)
                            } else {
                                setScale(2F, e.x, e.y, true)
                            }
                            return true
                        }

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            this@ReaderPageImageView.onViewClicked()
                            return super.onSingleTapConfirmed(e)
                        }
                    },
                )
                setOnScaleChangeListener { _, _, _ ->
                    this@ReaderPageImageView.onScaleChanged(scale)
                }
            }
        }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
        ensureTranslationOverlay()
    }

    private fun setAnimatedImage(
        data: Any,
        config: Config,
    ) = (pageView as? AppCompatImageView)?.apply {
        if (this is PhotoView) {
            setZoomTransitionDuration(config.zoomDuration.getSystemScaledDuration())
        }

        val request = ImageRequest.Builder(context)
            .data(data)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { result ->
                    val drawable = result.asDrawable(context.resources)
                    setImageDrawable(drawable)
                    (drawable as? Animatable)?.start()
                    isVisible = true
                    this@ReaderPageImageView.onImageDimensionsLoaded?.invoke(drawable.intrinsicWidth, drawable.intrinsicHeight)
                    this@ReaderPageImageView.onImageLoaded()
                },
            )
            .listener(
                onError = { _, result ->
                    onImageLoadError(result.throwable)
                },
            )
            .crossfade(false)
            .build()
        context.imageLoader.enqueue(request)
    }

    private fun Int.getSystemScaledDuration(): Int {
        return (this * context.animatorDurationScale).toInt().coerceAtLeast(1)
    }

    /**
     * All of the config except [zoomDuration] will only be used for non-animated image.
     */
    data class Config(
        val zoomDuration: Int,
        val minimumScaleType: Int = SCALE_TYPE_CENTER_INSIDE,
        val cropBorders: Boolean = false,
        val zoomStartPosition: ZoomStartPosition = ZoomStartPosition.CENTER,
        val landscapeZoom: Boolean = false,
    )

    enum class ZoomStartPosition {
        LEFT,
        CENTER,
        RIGHT,
    }

    private fun ensureTranslationOverlay(targetPage: ReaderPage? = null) {
        val pageToUse = targetPage ?: this.currentPage
        // Ensure strictly 1 TranslationOverlay view exists in this container
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is TranslationOverlay && child !== translationOverlay) {
                removeViewAt(i)
            }
        }
        if (translationOverlay == null) {
            translationOverlay = TranslationOverlay(context).apply {
                this.pageView = this@ReaderPageImageView.pageView
                this.page = pageToUse
                this.elevation = 20f
                this.isClickable = false
                this.isFocusable = false
            }
            addView(translationOverlay, android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        } else {
            if (translationOverlay?.parent == null) {
                addView(translationOverlay, android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }
            translationOverlay?.pageView = this.pageView
            translationOverlay?.page = pageToUse
            translationOverlay?.elevation = 20f
            translationOverlay?.bringToFront()
        }
    }

    fun updateTranslationOverlay(targetPage: ReaderPage? = null) {
        val pageToUse = targetPage ?: this.currentPage ?: return
        if (pageToUse.translationLanguage != Injekt.get<ShinKuPreferences>().translationTargetLanguage().get()) {
            pageToUse.translationBlocks = null
            pageToUse.showTranslation = false
        }
        if (pageToUse.translationBlocks == null) {
            try {
                val chapterId = pageToUse.chapter.chapter.id ?: 0L
                if (chapterId > 0L) {
                    val targetLang = Injekt.get<ShinKuPreferences>().translationTargetLanguage().get()
                    val cached = com.shinku.reader.ui.reader.model.TranslationCache.getMemory(chapterId, pageToUse.index, targetLang)
                    if (cached != null) {
                        pageToUse.translationBlocks = cached
                        pageToUse.translationLanguage = targetLang
                        pageToUse.showTranslation = true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        ensureTranslationOverlay(pageToUse)
        translationOverlay?.page = pageToUse
        translationOverlay?.pageView = this.pageView
        translationOverlay?.elevation = 20f
        translationOverlay?.bringToFront()
        translationOverlay?.visibility = if (pageToUse.showTranslation) VISIBLE else GONE
        translationOverlay?.invalidate()
        postInvalidate()
    }
}

class TranslationOverlay(context: Context) : View(context) {
    var page: ReaderPage? = null
    var pageView: View? = null

    init {
        isClickable = false
        isFocusable = false
    }

    private val maskPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val maskBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        isAntiAlias = true
    }

    private val boldTypeface: android.graphics.Typeface by lazy {
        android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    private val drawnBounds = ArrayList<RectF>(24)
    private val sTopLeft = PointF()
    private val vTopLeft = PointF()
    private val sBottomRight = PointF()
    private val vBottomRight = PointF()

    private data class LayoutCacheKey(
        val text: String,
        val widthBucket: Int,
        val heightBucket: Int,
        val textColor: Int,
        val bgColor: Int,
    )

    private data class CachedBubbleLayout(
        val fillLayout: StaticLayout,
        val strokeLayout: StaticLayout,
        val contentWidth: Int,
    )

    private val layoutCache = object : LinkedHashMap<LayoutCacheKey, CachedBubbleLayout>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LayoutCacheKey, CachedBubbleLayout>?): Boolean = size > 80
    }

    private fun getOrBuildBubbleLayout(
        text: String,
        width: Int,
        height: Int,
        textColor: Int,
        bgColor: Int,
    ): CachedBubbleLayout {
        // Quantize dimensions to 2px buckets so minor pan/zoom subpixel jitter hits cache 100% of the time
        val widthBucket = (width / 2).coerceAtLeast(1) * 2
        val heightBucket = (height / 2).coerceAtLeast(1) * 2
        val key = LayoutCacheKey(text, widthBucket, heightBucket, textColor, bgColor)
        layoutCache[key]?.let { return it }

        val horizontalPadding = (widthBucket * 0.06f).coerceIn(4f, 16f)
        val verticalPadding = (heightBucket * 0.06f).coerceIn(4f, 16f)
        val contentWidth = (widthBucket - 2f * horizontalPadding).toInt().coerceAtLeast(1)
        val maxContentHeight = heightBucket - 2f * verticalPadding

        val fillPaint = TextPaint().apply {
            color = textColor
            isAntiAlias = true
            typeface = boldTypeface
            letterSpacing = 0.012f
        }

        // Start with an adaptive font size based on bubble area and character count
        val charCount = text.length.coerceAtLeast(1)
        val areaEstimate = kotlin.math.sqrt((contentWidth * maxContentHeight).toDouble() / (charCount * 0.85)).toFloat()
        var fontSizePx = minOf(heightBucket * 0.45f, areaEstimate).coerceIn(12f, 48f)
        val minFontSize = 8f
        var fillLayout: StaticLayout? = null

        while (fontSizePx >= minFontSize) {
            fillPaint.textSize = fontSizePx
            val candidate = StaticLayout.Builder.obtain(text, 0, text.length, fillPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.04f)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build()

            fillLayout = candidate
            if (candidate.height <= maxContentHeight) {
                break
            }
            fontSizePx -= 1.25f
        }

        val finalFillLayout = fillLayout ?: StaticLayout.Builder.obtain(text, 0, text.length, fillPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        // Build matching stroke layout for crisp scanlation halo around glyphs
        val strokePaint = TextPaint(fillPaint).apply {
            color = bgColor
            style = Paint.Style.STROKE
            strokeWidth = (fillPaint.textSize * 0.16f).coerceIn(2f, 5f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val strokeLayout = StaticLayout.Builder.obtain(text, 0, text.length, strokePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.04f)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
            .build()

        return CachedBubbleLayout(
            fillLayout = finalFillLayout,
            strokeLayout = strokeLayout,
            contentWidth = contentWidth,
        ).also { layoutCache[key] = it }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val page = page ?: return
        if (!page.showTranslation) return
        val blocks = page.translationBlocks ?: return
        val view = pageView ?: return

        drawnBounds.clear()

        for (block in blocks) {
            val box = block.bubbleRect ?: block.boundingBox ?: continue
            if (block.translatedText.isBlank()) continue

            val left: Float
            val top: Float
            val right: Float
            val bottom: Float

            when (view) {
                is com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView -> {
                    if (!view.isReady) continue
                    val scaleX = if (block.sourceWidth > 0 && view.sWidth > 0) view.sWidth.toFloat() / block.sourceWidth.toFloat() else 1f
                    val scaleY = if (block.sourceHeight > 0 && view.sHeight > 0) view.sHeight.toFloat() / block.sourceHeight.toFloat() else 1f
                    sTopLeft.set(box.left.toFloat() * scaleX, box.top.toFloat() * scaleY)
                    sBottomRight.set(box.right.toFloat() * scaleX, box.bottom.toFloat() * scaleY)
                    val p1 = view.sourceToViewCoord(sTopLeft, vTopLeft)
                    val p2 = view.sourceToViewCoord(sBottomRight, vBottomRight)
                    if (p1 == null || p2 == null) continue
                    left = vTopLeft.x
                    top = vTopLeft.y
                    right = vBottomRight.x
                    bottom = vBottomRight.y
                }
                is com.shinku.reader.ui.reader.viewer.webtoon.WebtoonStripImageView -> {
                    val imgWidth = view.sourceWidth
                    if (imgWidth <= 0) continue
                    val scale = view.width.toFloat() / imgWidth.toFloat()
                    left = box.left * scale
                    top = box.top * scale
                    right = box.right * scale
                    bottom = box.bottom * scale
                }
                is android.widget.ImageView -> {
                    val drawable = view.drawable ?: continue
                    val dWidth = drawable.intrinsicWidth
                    val dHeight = drawable.intrinsicHeight
                    if (dWidth <= 0 || dHeight <= 0) continue
                    val scaleX = view.width.toFloat() / dWidth.toFloat()
                    val scaleY = view.height.toFloat() / dHeight.toFloat()
                    left = box.left * scaleX
                    top = box.top * scaleY
                    right = box.right * scaleX
                    bottom = box.bottom * scaleY
                }
                else -> continue
            }

            val actualLeft = minOf(left, right)
            val actualRight = maxOf(left, right)
            val actualTop = minOf(top, bottom)
            val actualBottom = maxOf(top, bottom)

            val width = (actualRight - actualLeft).toInt()
            val height = (actualBottom - actualTop).toInt()
            if (width <= 6 || height <= 6) continue

            // Non-Maximum Suppression / Occlusion Suppression: avoid rendering colliding overlays
            val currentArea = (actualRight - actualLeft) * (actualBottom - actualTop)
            var collides = false
            for (drawn in drawnBounds) {
                val iLeft = maxOf(drawn.left, actualLeft)
                val iTop = maxOf(drawn.top, actualTop)
                val iRight = minOf(drawn.right, actualRight)
                val iBottom = minOf(drawn.bottom, actualBottom)
                if (iLeft < iRight && iTop < iBottom) {
                    val iArea = (iRight - iLeft) * (iBottom - iTop)
                    val drawnArea = (drawn.right - drawn.left) * (drawn.bottom - drawn.top)
                    val minArea = minOf(currentArea, drawnArea)
                    if (minArea > 0 && iArea / minArea > 0.40f) {
                        collides = true
                        break
                    }
                }
            }
            if (collides) continue
            drawnBounds.add(RectF(actualLeft, actualTop, actualRight, actualBottom))

            // 1. Draw soft-cornered speech-balloon mask so corners blend naturally inside oval comic bubbles
            maskPaint.color = block.bgColor
            maskPaint.alpha = 248
            val cornerRadius = (minOf(width, height) * 0.18f).coerceIn(4f, 18f)
            canvas.drawRoundRect(actualLeft, actualTop, actualRight, actualBottom, cornerRadius, cornerRadius, maskPaint)

            // 2. Draw clean subtle hairline border for comic panel / bubble definition
            maskBorderPaint.color = if (block.bgColor == android.graphics.Color.WHITE) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            maskBorderPaint.alpha = 36
            maskBorderPaint.strokeWidth = 1.5f
            canvas.drawRoundRect(actualLeft, actualTop, actualRight, actualBottom, cornerRadius, cornerRadius, maskBorderPaint)

            // 2. Typeset cached layout with crisp scanlation halo + foreground text (0 frame allocations)
            val cached = getOrBuildBubbleLayout(
                text = block.translatedText,
                width = width,
                height = height,
                textColor = block.textColor,
                bgColor = block.bgColor,
            )

            canvas.save()
            val textHeight = cached.fillLayout.height
            val textLeft = actualLeft + (width - cached.contentWidth) / 2f
            val dy = actualTop + (height - textHeight) / 2f
            canvas.translate(textLeft, dy)
            cached.strokeLayout.draw(canvas)
            cached.fillLayout.draw(canvas)
            canvas.restore()
        }
    }
}

private const val MAX_ZOOM_SCALE = 5F
