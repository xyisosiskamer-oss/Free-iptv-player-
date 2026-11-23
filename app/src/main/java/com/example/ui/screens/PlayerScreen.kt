package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgramUi
import com.example.data.preferences.PlayerResizeMode
import com.example.i18n.LocalAppStrings
import com.example.ui.components.ChannelListRow
import com.example.ui.components.ChannelLogo
import com.example.ui.components.formatEpgTimeRange
import com.example.ui.components.tvFocusHighlight
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldLive
import com.example.ui.viewmodel.PlayerSession
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface PlayerErrorState {
    data class HttpCode(val code: Int) : PlayerErrorState
    data object Timeout : PlayerErrorState
    data class Generic(val message: String) : PlayerErrorState
}

private enum class PlayerOrientationLock {
    SYSTEM_DEFAULT,
    LOCKED_LANDSCAPE,
    LOCKED_PORTRAIT
}

private sealed interface PlayerHudFeedback {
    data class SeekDelta(val seconds: Int) : PlayerHudFeedback
    data class Brightness(val percent: Int) : PlayerHudFeedback
    data class Volume(val percent: Int) : PlayerHudFeedback
    data class Message(val text: String) : PlayerHudFeedback
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    session: PlayerSession,
    favoriteChannels: List<ChannelItem>,
    resizeMode: PlayerResizeMode,
    isTv: Boolean,
    httpClient: OkHttpClient,
    isPlaylistRemote: Boolean = false,
    isRefreshingPlaylist: Boolean = false,
    onRefreshPlaylist: (isAutoRetry: Boolean) -> Unit = {},
    onBack: () -> Unit,
    onSelectChannel: (ChannelItem) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit,
    onCycleResizeMode: () -> Unit,
    onPlayArchiveProgram: (EpgProgramUi) -> Unit,
    onReturnToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    var orientationLock by remember { mutableStateOf(PlayerOrientationLock.SYSTEM_DEFAULT) }
    var showControls by remember { mutableStateOf(true) }
    var showDrawer by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPill by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<PlayerErrorState?>(null) }
    val activeArchive = session.archivePlayback
    val playbackUrl = activeArchive?.archiveUrl ?: session.currentChannel.streamUrl

    var retryCounter by remember { mutableIntStateOf(0) }
    var autoRefreshAttempted by remember(playbackUrl) {
        mutableStateOf(false)
    }
    var awaitingManualRefresh by remember { mutableStateOf(false) }
    var urlBeforeManualRefresh by remember { mutableStateOf<String?>(null) }
    var quickListTab by remember { mutableIntStateOf(0) } // 0 = Queue, 1 = Favorites, 2 = EPG / Archive
    var playerSearchQuery by remember { mutableStateOf("") }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeekable by remember { mutableStateOf(false) }
    var isLiveStream by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerEndAtMs by remember { mutableLongStateOf(0L) }
    var sleepTimerMinutesLeft by remember { mutableIntStateOf(0) }

    var hudFeedback by remember { mutableStateOf<PlayerHudFeedback?>(null) }
    var hudFeedbackToken by remember { mutableIntStateOf(0) }
    var videoBoxSize by remember { mutableStateOf(IntSize.Zero) }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val initialBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    var sessionBrightness by remember {
        mutableFloatStateOf(
            if (initialBrightness in 0f..1f) initialBrightness else 0.6f
        )
    }

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(orientationLock, isTv, activity) {
        if (!isTv && activity != null) {
            activity.requestedOrientation = when (orientationLock) {
                PlayerOrientationLock.SYSTEM_DEFAULT -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                PlayerOrientationLock.LOCKED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayerOrientationLock.LOCKED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    DisposableEffect(isTv, activity) {
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                val lp = w.attributes
                if (lp.screenBrightness != initialBrightness) {
                    lp.screenBrightness = initialBrightness
                    w.attributes = lp
                }
            }
            if (!isTv) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = classifyPlaybackError(error)
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (playbackError == null) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(playbackUrl, retryCounter) {
        val channel = session.currentChannel
        playbackError = null
        isBuffering = true
        currentPositionMs = 0L
        durationMs = 0L
        isSeekable = false
        isScrubbing = false

        val headers = buildMap {
            channel.referrer?.let { put("Referer", it) }
        }
        val userAgent = channel.userAgent
            ?: "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 KobaidzeIPTV/1.0"

        val dataSourceFactory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent(userAgent)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        val loadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(3) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val err = loadErrorInfo.exception
                if (err is HttpDataSource.InvalidResponseCodeException) {
                    val code = err.responseCode
                    if (code == 401 || code == 403 || code == 404 || code == 410) {
                        return if (loadErrorInfo.errorCount <= 1) 900L else C.TIME_UNSET
                    }
                }
                return if (loadErrorInfo.errorCount <= 3) {
                    (loadErrorInfo.errorCount * 800L).coerceAtMost(3000L)
                } else {
                    C.TIME_UNSET
                }
            }
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorPolicy)
            .createMediaSource(MediaItem.fromUri(playbackUrl))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        showControls = true
    }

    LaunchedEffect(playbackError, playbackUrl, isPlaylistRemote, isRefreshingPlaylist) {
        val err = playbackError
        if (err is PlayerErrorState.HttpCode &&
            err.code in listOf(401, 403, 404, 410) &&
            isPlaylistRemote &&
            !autoRefreshAttempted &&
            !isRefreshingPlaylist
        ) {
            autoRefreshAttempted = true
            onRefreshPlaylist(true)
        }
    }

    LaunchedEffect(isRefreshingPlaylist) {
        if (!isRefreshingPlaylist && awaitingManualRefresh) {
            val unchanged = urlBeforeManualRefresh == playbackUrl
            awaitingManualRefresh = false
            urlBeforeManualRefresh = null
            if (unchanged) {
                retryCounter++
            }
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(activeArchive) {
        if (activeArchive == null && playbackSpeed != 1.0f) {
            playbackSpeed = 1.0f
        }
    }

    LaunchedEffect(exoPlayer, playbackUrl) {
        while (true) {
            if (!isScrubbing) {
                val dur = exoPlayer.duration
                val validDuration = if (dur != C.TIME_UNSET && dur > 0L) dur else 0L
                durationMs = validDuration
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                isSeekable = exoPlayer.isCurrentMediaItemSeekable && validDuration > 0L
                isLiveStream = exoPlayer.isCurrentMediaItemLive
            }
            delay(400L)
        }
    }

    LaunchedEffect(showControls, isPlaying, showDrawer, playbackError, isScrubbing, isScreenLocked) {
        if (showControls && isPlaying && !showDrawer && playbackError == null && !isScrubbing && !isScreenLocked) {
            delay(4500)
            showControls = false
        }
    }

    LaunchedEffect(showUnlockPill) {
        if (showUnlockPill) {
            delay(2500)
            showUnlockPill = false
        }
    }

    LaunchedEffect(hudFeedbackToken) {
        if (hudFeedbackToken > 0) {
            delay(1100)
            hudFeedback = null
        }
    }

    LaunchedEffect(sleepTimerEndAtMs) {
        if (sleepTimerEndAtMs <= 0L) {
            sleepTimerMinutesLeft = 0
            return@LaunchedEffect
        }
        while (sleepTimerEndAtMs > 0L) {
            val remainingMs = sleepTimerEndAtMs - System.currentTimeMillis()
            if (remainingMs <= 0L) {
                sleepTimerEndAtMs = 0L
                sleepTimerMinutesLeft = 0
                exoPlayer.pause()
                hudFeedback = PlayerHudFeedback.Message(strings.sleepTimerTriggeredMsg)
                hudFeedbackToken++
                break
            }
            sleepTimerMinutesLeft = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            delay(5000L)
        }
    }

    val media3ResizeMode = when (resizeMode) {
        PlayerResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        PlayerResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        PlayerResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    val resizeLabel = when (resizeMode) {
        PlayerResizeMode.FIT -> strings.resizeFit
        PlayerResizeMode.FILL -> strings.resizeFill
        PlayerResizeMode.ZOOM -> strings.resizeZoom
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.currentChannel) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !showDrawer) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            onNextChannel()
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            onPreviousChannel()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!showControls) {
                                onNextChannel()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!showControls) {
                                onPreviousChannel()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MENU -> {
                            if (!showControls) {
                                showDrawer = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val isFullscreen = isLandscape || isTv
        val useSideBySideOrFullscreen = isFullscreen || maxWidth >= 700.dp

        BackHandler {
            when {
                isScreenLocked -> {
                    showUnlockPill = true
                }
                showDrawer -> showDrawer = false
                orientationLock == PlayerOrientationLock.LOCKED_LANDSCAPE && !isTv -> {
                    orientationLock = PlayerOrientationLock.SYSTEM_DEFAULT
                }
                else -> onBack()
            }
        }

        DisposableEffect(isFullscreen, activity) {
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isFullscreen) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val videoContainerModifier = if (useSideBySideOrFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .aspectRatio(16f / 9f)
            }

            Box(
                modifier = videoContainerModifier
                    .background(Color.Black)
                    .onSizeChanged { videoBoxSize = it }
                    .focusable()
                    .pointerInput(isTv, isScreenLocked, isSeekable, activeArchive, durationMs) {
                        detectTapGestures(
                            onTap = {
                                if (isScreenLocked) {
                                    showUnlockPill = !showUnlockPill
                                } else {
                                    showControls = !showControls
                                }
                            },
                            onDoubleTap = { offset ->
                                if (isScreenLocked || isTv) return@detectTapGestures
                                val width = videoBoxSize.width.toFloat().coerceAtLeast(1f)
                                val canSeek = isSeekable || activeArchive != null
                                if (canSeek && offset.x < width * 0.4f) {
                                    val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    hudFeedback = PlayerHudFeedback.SeekDelta(-10)
                                    hudFeedbackToken++
                                } else if (canSeek && offset.x > width * 0.6f) {
                                    val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                    val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(maxPos)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    hudFeedback = PlayerHudFeedback.SeekDelta(10)
                                    hudFeedbackToken++
                                } else {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            }
                        )
                    }
                    .then(
                        if (!isTv && !isScreenLocked) {
                            Modifier.pointerInput(videoBoxSize, activity, audioManager) {
                                var dragIsBrightness = false
                                var startX = 0f
                                var accumulatedVolDelta = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { startOffset ->
                                        startX = startOffset.x
                                        val w = videoBoxSize.width.toFloat().coerceAtLeast(1f)
                                        dragIsBrightness = startX < w * 0.5f
                                        accumulatedVolDelta = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        val h = videoBoxSize.height.toFloat().coerceAtLeast(200f)
                                        val deltaRatio = -dragAmount / (h * 0.75f)
                                        if (dragIsBrightness) {
                                            val next = (sessionBrightness + deltaRatio).coerceIn(0.05f, 1.0f)
                                            sessionBrightness = next
                                            val w = activity?.window
                                            if (w != null) {
                                                val lp = w.attributes
                                                lp.screenBrightness = next
                                                w.attributes = lp
                                            }
                                            hudFeedback = PlayerHudFeedback.Brightness((next * 100f).toInt())
                                            hudFeedbackToken++
                                        } else if (audioManager != null) {
                                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                            accumulatedVolDelta += deltaRatio * maxVol
                                            if (kotlin.math.abs(accumulatedVolDelta) >= 1f) {
                                                val step = accumulatedVolDelta.toInt()
                                                accumulatedVolDelta -= step
                                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val nextVol = (currentVol + step).coerceIn(0, maxVol)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                                val pct = ((nextVol.toFloat() / maxVol) * 100f).toInt()
                                                hudFeedback = PlayerHudFeedback.Volume(pct)
                                                hudFeedbackToken++
                                            }
                                        }
                                        change.consume()
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = exoPlayer
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                        playerView.resizeMode = media3ResizeMode
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering && playbackError == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = strings.buffering,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                playbackError?.let { errState ->
                    val isAuthOrNotFound = errState is PlayerErrorState.HttpCode &&
                        errState.code in listOf(401, 403, 404, 410)
                    val isCompactHeight = !useSideBySideOrFullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xE8070B14))
                            .then(
                                if (useSideBySideOrFullscreen) {
                                    Modifier
                                        .statusBarsPadding()
                                        .navigationBarsPadding()
                                        .displayCutoutPadding()
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = if (isCompactHeight) 8.dp else 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                OsdIconButton(
                                    onClick = onBack,
                                    testTag = "player_error_back_button"
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.back,
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = session.currentChannel.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OsdIconButton(
                                    onClick = { onToggleFavorite(session.currentChannel) },
                                    testTag = "player_error_favorite_button"
                                ) {
                                    Icon(
                                        imageVector = if (session.currentChannel.isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = if (session.currentChannel.isFavorite) {
                                            strings.removeFromFavorites
                                        } else {
                                            strings.addToFavorites
                                        },
                                        tint = if (session.currentChannel.isFavorite) CoralAccent else Color.White
                                    )
                                }
                                if (!isTv) {
                                    OsdIconButton(
                                        onClick = {
                                            orientationLock = if (isLandscape) {
                                                PlayerOrientationLock.LOCKED_PORTRAIT
                                            } else {
                                                PlayerOrientationLock.LOCKED_LANDSCAPE
                                            }
                                        },
                                        testTag = "player_error_fullscreen_button"
                                    ) {
                                        Icon(
                                            imageVector = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                            contentDescription = if (isLandscape) strings.fullscreenExit else strings.fullscreenEnter,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 10.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (isRefreshingPlaylist) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = strings.refreshingStreamTokens,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = CoralAccent,
                                            modifier = Modifier.size(if (isCompactHeight) 22.dp else 32.dp)
                                        )
                                        Text(
                                            text = strings.streamErrorTitle,
                                            style = if (isCompactHeight) {
                                                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            } else {
                                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            },
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val baseSubtitle = when (errState) {
                                        is PlayerErrorState.HttpCode -> strings.streamHttpErrorSubtitle(errState.code)
                                        is PlayerErrorState.Timeout -> strings.streamTimeoutErrorSubtitle
                                        is PlayerErrorState.Generic -> strings.streamErrorSubtitle
                                    }
                                    val fullSubtitle = if (isAuthOrNotFound && isPlaylistRemote) {
                                        "$baseSubtitle ${strings.streamExpiredHint}"
                                    } else {
                                        baseSubtitle
                                    }
                                    Text(
                                        text = fullSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        maxLines = if (isCompactHeight) 2 else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { retryCounter++ },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("player_retry_button")
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.tryAgain, style = MaterialTheme.typography.labelMedium)
                                        }
                                        if (isPlaylistRemote) {
                                            OutlinedButton(
                                                onClick = {
                                                    urlBeforeManualRefresh = playbackUrl
                                                    awaitingManualRefresh = true
                                                    onRefreshPlaylist(false)
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("player_refresh_playlist_button")
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(strings.refreshPlaylistAndRetry, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        if (session.queue.size > 1) {
                                            Button(
                                                onClick = onNextChannel,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("player_error_next_channel_button")
                                            ) {
                                                Text(strings.nextWorkingChannel, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Gesture / Sleep HUD pill
                androidx.compose.animation.AnimatedVisibility(
                    visible = hudFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    hudFeedback?.let { feedback ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (feedback) {
                                    is PlayerHudFeedback.SeekDelta -> {
                                        Icon(
                                            imageVector = if (feedback.seconds < 0) Icons.Filled.Replay10 else Icons.Filled.Forward10,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (feedback.seconds > 0) "+${feedback.seconds}s" else "${feedback.seconds}s",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    is PlayerHudFeedback.Brightness -> {
                                        Icon(
                                            imageVector = Icons.Filled.BrightnessHigh,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24)
                                        )
                                        Text(
                                            text = strings.brightnessLabel(feedback.percent),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    is PlayerHudFeedback.Volume -> {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = strings.volumeLabel(feedback.percent),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    is PlayerHudFeedback.Message -> {
                                        Icon(
                                            imageVector = Icons.Filled.Bedtime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = feedback.text,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Unlock Screen Pill when Screen Lock is active
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScreenLocked && showUnlockPill,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                isScreenLocked = false
                                showUnlockPill = false
                                showControls = true
                            }
                            .testTag("player_unlock_button"),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = strings.unlockScreen,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${strings.screenLockedMsg} • ${strings.unlockScreen}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                // Persistent Portrait Fullscreen button when OSD auto-hides
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLandscape && !isTv && !isScreenLocked && playbackError == null && !showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    OsdIconButton(
                        onClick = { orientationLock = PlayerOrientationLock.LOCKED_LANDSCAPE },
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        testTag = "player_persistent_fullscreen_button"
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = strings.fullscreenEnter,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // OSD Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls && !isScreenLocked && playbackError == null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.78f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .then(
                                if (useSideBySideOrFullscreen) {
                                    Modifier
                                        .statusBarsPadding()
                                        .navigationBarsPadding()
                                        .displayCutoutPadding()
                                } else {
                                    Modifier
                                }
                            )
                            .padding(12.dp)
                    ) {
                        // Top OSD Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                OsdIconButton(
                                    onClick = onBack,
                                    testTag = "player_back_button"
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.back,
                                        tint = Color.White
                                    )
                                }

                                ChannelLogo(
                                    name = session.currentChannel.name,
                                    logoUrl = session.currentChannel.logoUrl,
                                    size = 38.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.currentChannel.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isArchiveMode = activeArchive != null
                                        val badgeColor = if (isArchiveMode) Color(0xFFF59E0B) else EmeraldLive
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = badgeColor.copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = if (isArchiveMode) strings.archiveBadge else strings.liveBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        val subtitleText = if (activeArchive != null) {
                                            "${formatEpgTimeRange(activeArchive.program)} • ${activeArchive.program.title}"
                                        } else {
                                            session.currentChannel.currentProgram?.let {
                                                "${formatEpgTimeRange(it)} • ${it.title}"
                                            } ?: session.currentChannel.groupTitle.ifBlank { strings.generalCategory }
                                        }
                                        Text(
                                            text = subtitleText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val behindLive = isLiveStream && isSeekable && durationMs > 0L && (durationMs - currentPositionMs) > 15_000L
                                if (activeArchive != null || behindLive) {
                                    Surface(
                                        modifier = Modifier
                                            .tvFocusHighlight(shape = RoundedCornerShape(50))
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                if (activeArchive != null) {
                                                    onReturnToLive()
                                                } else {
                                                    exoPlayer.seekToDefaultPosition()
                                                }
                                            }
                                            .testTag("player_jump_live_button"),
                                        shape = RoundedCornerShape(50),
                                        color = EmeraldLive.copy(alpha = 0.24f),
                                        border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.7f))
                                    ) {
                                        Text(
                                            text = strings.jumpToLive,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldLive,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                OsButtonSleepTimer(
                                    minutesLeft = sleepTimerMinutesLeft,
                                    onClick = { showSleepTimerDialog = true }
                                )

                                OsdIconButton(
                                    onClick = { onToggleFavorite(session.currentChannel) },
                                    testTag = "player_favorite_button"
                                ) {
                                    Icon(
                                        imageVector = if (session.currentChannel.isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = if (session.currentChannel.isFavorite) {
                                            strings.removeFromFavorites
                                        } else {
                                            strings.addToFavorites
                                        },
                                        tint = if (session.currentChannel.isFavorite) CoralAccent else Color.White
                                    )
                                }

                                if (useSideBySideOrFullscreen) {
                                    OsdIconButton(
                                        onClick = { showDrawer = !showDrawer },
                                        testTag = "player_channel_drawer_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = strings.channelListDrawer,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom OSD Controls + Seek Bar + EPG Progress
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeArchive == null) {
                                session.currentChannel.currentProgram?.let { currentProg ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${strings.epgNowLabel}: ${currentProg.title}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            session.currentChannel.nextProgram?.let { nextProg ->
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = "${strings.epgNextLabel}: ${nextProg.title}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (!isSeekable) {
                                            LinearProgressIndicator(
                                                progress = { currentProg.progressAt(nowMillis) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(50)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = Color.White.copy(alpha = 0.24f)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isSeekable && durationMs > 0L) {
                                val sliderPos = if (isScrubbing) {
                                    scrubPositionMs.toFloat()
                                } else {
                                    currentPositionMs.coerceIn(0L, durationMs).toFloat()
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Slider(
                                        value = sliderPos.coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                                        onValueChange = { newVal ->
                                            isScrubbing = true
                                            scrubPositionMs = newVal.toLong()
                                        },
                                        onValueChangeFinished = {
                                            exoPlayer.seekTo(scrubPositionMs)
                                            currentPositionMs = scrubPositionMs
                                            isScrubbing = false
                                        },
                                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .testTag("player_seek_slider")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatPlaybackDuration(sliderPos.toLong()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = formatPlaybackDuration(durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OsdIconButton(
                                        onClick = onPreviousChannel,
                                        testTag = "player_prev_channel_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SkipPrevious,
                                            contentDescription = strings.previousChannel,
                                            tint = Color.White
                                        )
                                    }

                                    if (isSeekable || activeArchive != null) {
                                        OsdIconButton(
                                            onClick = {
                                                val target = (exoPlayer.currentPosition - 15_000L).coerceAtLeast(0L)
                                                exoPlayer.seekTo(target)
                                                currentPositionMs = target
                                            },
                                            testTag = "player_seek_back_button"
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Replay10,
                                                contentDescription = strings.seekBack15,
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    OsdIconButton(
                                        onClick = {
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.play()
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        testTag = "player_play_pause_button"
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) strings.pause else strings.play,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    if (isSeekable || activeArchive != null) {
                                        OsdIconButton(
                                            onClick = {
                                                val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                                val target = (exoPlayer.currentPosition + 15_000L).coerceAtMost(maxPos)
                                                exoPlayer.seekTo(target)
                                                currentPositionMs = target
                                            },
                                            testTag = "player_seek_forward_button"
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Forward10,
                                                contentDescription = strings.seekForward15,
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    OsdIconButton(
                                        onClick = onNextChannel,
                                        testTag = "player_next_channel_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SkipNext,
                                            contentDescription = strings.nextChannel,
                                            tint = Color.White
                                        )
                                    }

                                    if (isTv) {
                                        Text(
                                            text = strings.tvControlsHint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                    }

                                    if (isSeekable || activeArchive != null) {
                                        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        Surface(
                                            modifier = Modifier
                                                .tvFocusHighlight(shape = RoundedCornerShape(50))
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    val idx = speeds.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.05f }
                                                    playbackSpeed = speeds[(idx + 1) % speeds.size]
                                                }
                                                .testTag("player_speed_button"),
                                            shape = RoundedCornerShape(50),
                                            color = Color.Black.copy(alpha = 0.58f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Speed,
                                                    contentDescription = strings.playbackSpeed,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .tvFocusHighlight(shape = RoundedCornerShape(50))
                                            .clip(RoundedCornerShape(50))
                                            .clickable(onClick = onCycleResizeMode)
                                            .testTag("player_resize_button"),
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.58f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AspectRatio,
                                                contentDescription = resizeLabel,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = resizeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (!isTv) {
                                        OsdIconButton(
                                            onClick = {
                                                isScreenLocked = true
                                                showControls = false
                                                showUnlockPill = true
                                            },
                                            testTag = "player_lock_button"
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = strings.lockScreen,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                if (!isTv) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OsdIconButton(
                                        onClick = {
                                            orientationLock = if (isLandscape) {
                                                PlayerOrientationLock.LOCKED_PORTRAIT
                                            } else {
                                                PlayerOrientationLock.LOCKED_LANDSCAPE
                                            }
                                        },
                                        testTag = "player_fullscreen_button"
                                    ) {
                                        Icon(
                                            imageVector = if (isLandscape) {
                                                Icons.Filled.FullscreenExit
                                            } else {
                                                Icons.Filled.Fullscreen
                                            },
                                            contentDescription = if (isLandscape) {
                                                strings.fullscreenExit
                                            } else {
                                                strings.fullscreenEnter
                                            },
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Portrait Phone Panel below Video (Channels / Favorites / EPG + Search)
            if (!useSideBySideOrFullscreen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerPanelContent(
                        selectedTab = quickListTab,
                        onSelectTab = { quickListTab = it },
                        searchQuery = playerSearchQuery,
                        onSearchQueryChange = { playerSearchQuery = it },
                        currentChannel = session.currentChannel,
                        activeArchiveProgram = activeArchive?.program,
                        queue = session.queue,
                        favorites = favoriteChannels,
                        nowMillis = nowMillis,
                        onSelectChannel = onSelectChannel,
                        onToggleFavorite = onToggleFavorite,
                        onPlayArchiveProgram = onPlayArchiveProgram,
                        onReturnToLive = onReturnToLive
                    )
                }
            }
        }

        // Slide-over Side Drawer for Fullscreen / TV
        AnimatedVisibility(
            visible = showDrawer && useSideBySideOrFullscreen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (maxWidth < 700.dp) 310.dp else 380.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .displayCutoutPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.channelListDrawer,
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showDrawer = false }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.back)
                        }
                    }

                    PlayerPanelContent(
                        selectedTab = quickListTab,
                        onSelectTab = { quickListTab = it },
                        searchQuery = playerSearchQuery,
                        onSearchQueryChange = { playerSearchQuery = it },
                        currentChannel = session.currentChannel,
                        activeArchiveProgram = activeArchive?.program,
                        queue = session.queue,
                        favorites = favoriteChannels,
                        nowMillis = nowMillis,
                        onSelectChannel = { ch ->
                            onSelectChannel(ch)
                            showDrawer = false
                        },
                        onToggleFavorite = onToggleFavorite,
                        onPlayArchiveProgram = { prog ->
                            onPlayArchiveProgram(prog)
                            showDrawer = false
                        },
                        onReturnToLive = {
                            onReturnToLive()
                            showDrawer = false
                        }
                    )
                }
            }
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentMinutesLeft = sleepTimerMinutesLeft,
                onSelectMinutes = { minutes ->
                    sleepTimerEndAtMs = if (minutes <= 0) {
                        0L
                    } else {
                        System.currentTimeMillis() + minutes * 60_000L
                    }
                    showSleepTimerDialog = false
                },
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

@Composable
private fun PlayerPanelContent(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentChannel: ChannelItem,
    activeArchiveProgram: EpgProgramUi?,
    queue: List<ChannelItem>,
    favorites: List<ChannelItem>,
    nowMillis: Long,
    onSelectChannel: (ChannelItem) -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit,
    onPlayArchiveProgram: (EpgProgramUi) -> Unit,
    onReturnToLive: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showArchiveSubTab by remember(currentChannel.streamUrl) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                text = {
                    Text(
                        "${strings.navChannels} (${queue.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                text = {
                    Text(
                        "${strings.navFavorites} (${favorites.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                text = {
                    Text(
                        strings.epgTabTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }

        if (selectedTab != 2) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = strings.searchChannelsHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearFilters)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("player_channel_search_input")
            )

            val rawList = if (selectedTab == 0) queue else favorites
            val trimmed = searchQuery.trim()
            val displayList = if (trimmed.isEmpty()) {
                rawList
            } else {
                rawList.filter { ch ->
                    ch.name.contains(trimmed, ignoreCase = true) ||
                        ch.groupTitle.contains(trimmed, ignoreCase = true) ||
                        (ch.currentProgram?.title?.contains(trimmed, ignoreCase = true) == true)
                }
            }

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 1 && favorites.isEmpty()) {
                            strings.emptyFavoritesTitle
                        } else {
                            strings.noChannelsTitle
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = displayList,
                        key = { idx, item -> "player_list_${selectedTab}_${idx}_${item.streamUrl}" }
                    ) { idx, ch ->
                        ChannelListRow(
                            channel = ch,
                            indexNumber = idx + 1,
                            isPlaying = ch.streamUrl == currentChannel.streamUrl,
                            onPlay = { onSelectChannel(ch) },
                            onToggleFavorite = { onToggleFavorite(ch) }
                        )
                    }
                }
            }
        } else {
            // EPG Schedule & Catchup Archive Tab
            val schedule = currentChannel.schedule
            val archivePrograms = currentChannel.archivePrograms

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showArchiveSubTab,
                        onClick = { showArchiveSubTab = false },
                        label = { Text("${strings.upcomingTab} (${schedule.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("epg_upcoming_chip")
                    )
                    FilterChip(
                        selected = showArchiveSubTab,
                        onClick = { showArchiveSubTab = true },
                        label = { Text("${strings.archiveTab} (${archivePrograms.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("epg_archive_chip")
                    )
                    if (activeArchiveProgram != null) {
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onReturnToLive,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(strings.jumpToLive, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (!showArchiveSubTab) {
                    if (schedule.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = strings.epgNoSchedule,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = schedule,
                                key = { idx, prog -> "epg_${idx}_${prog.startTimeMillis}" }
                            ) { _, prog ->
                                val isLiveNow = nowMillis in prog.startTimeMillis..prog.endTimeMillis
                                EpgScheduleCard(
                                    program = prog,
                                    isLiveNow = isLiveNow,
                                    nowMillis = nowMillis,
                                    liveLabel = strings.epgNowLabel
                                )
                            }
                        }
                    }
                } else {
                    if (archivePrograms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = strings.noArchivePrograms,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!currentChannel.hasCatchup) {
                                item(key = "archive_notice") {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = strings.archiveNotDeclaredHint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        )
                                    }
                                }
                            }
                            itemsIndexed(
                                items = archivePrograms,
                                key = { idx, prog -> "archive_${idx}_${prog.startTimeMillis}" }
                            ) { _, prog ->
                                val isPlayingThisArchive = activeArchiveProgram?.startTimeMillis == prog.startTimeMillis &&
                                    activeArchiveProgram.title == prog.title
                                ArchiveProgramCard(
                                    program = prog,
                                    isPlaying = isPlayingThisArchive,
                                    watchLabel = strings.watchArchive,
                                    playingLabel = strings.nowPlaying,
                                    onPlayArchive = { onPlayArchiveProgram(prog) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveProgramCard(
    program: EpgProgramUi,
    isPlaying: Boolean,
    watchLabel: String,
    playingLabel: String,
    onPlayArchive: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlayArchive)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatArchiveDateTime(program),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!program.description.isNullOrBlank()) {
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isPlaying) Color(0xFFF59E0B).copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = watchLabel,
                        tint = if (isPlaying) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPlaying) playingLabel else watchLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isPlaying) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatArchiveDateTime(program: EpgProgramUi): String {
    val dateFmt = SimpleDateFormat("dd.MM", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${dateFmt.format(Date(program.startTimeMillis))} • ${timeFmt.format(Date(program.startTimeMillis))}–${timeFmt.format(Date(program.endTimeMillis))}"
}

private fun formatPlaybackDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun EpgScheduleCard(
    program: EpgProgramUi,
    isLiveNow: Boolean,
    nowMillis: Long,
    liveLabel: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isLiveNow) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (isLiveNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatEpgTimeRange(program),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (isLiveNow) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldLive.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = liveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLive,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (!program.category.isNullOrBlank()) {
                    Text(
                        text = program.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = program.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!program.description.isNullOrBlank()) {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLiveNow) {
                LinearProgressIndicator(
                    progress = { program.progressAt(nowMillis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

private fun classifyPlaybackError(error: Throwable): PlayerErrorState {
    var current: Throwable? = error
    var depth = 0
    while (current != null && depth < 16) {
        when (current) {
            is HttpDataSource.InvalidResponseCodeException -> {
                return PlayerErrorState.HttpCode(current.responseCode)
            }
            is SocketTimeoutException, is ConnectException, is UnknownHostException -> {
                return PlayerErrorState.Timeout
            }
        }
        current = current.cause
        depth++
    }
    return PlayerErrorState.Generic(error.localizedMessage ?: error.message ?: "Playback error")
}

@Composable
private fun OsButtonSleepTimer(
    minutesLeft: Int,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    if (minutesLeft > 0) {
        Surface(
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .testTag("player_sleep_timer_button"),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = strings.sleepTimerTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.sleepTimerRemaining(minutesLeft),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    } else {
        OsdIconButton(
            onClick = onClick,
            testTag = "player_sleep_timer_button"
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = strings.sleepTimerTitle,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SleepTimerDialog(
    currentMinutesLeft: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val options = listOf(0, 15, 30, 60, 90)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = strings.sleepTimerTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { min ->
                    val label = if (min == 0) strings.sleepTimerOff else strings.sleepTimerMinutes(min)
                    val isSelected = (min == 0 && currentMinutesLeft == 0) ||
                        (min > 0 && currentMinutesLeft in (min - 1)..min)
                    val rowShape = RoundedCornerShape(14.dp)
                    Surface(
                        shape = rowShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .clickable { onSelectMinutes(min) }
                            .testTag("sleep_timer_option_$min")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun OsdIconButton(
    onClick: () -> Unit,
    containerColor: Color = Color.Black.copy(alpha = 0.56f),
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .tvFocusHighlight(shape = CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
