package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.ViewGroup
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    AUTO_SENSOR,
    LOCKED_LANDSCAPE,
    LOCKED_PORTRAIT
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    session: PlayerSession,
    favoriteChannels: List<ChannelItem>,
    resizeMode: PlayerResizeMode,
    isTv: Boolean,
    httpClient: OkHttpClient,
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

    var orientationLock by remember { mutableStateOf(PlayerOrientationLock.AUTO_SENSOR) }
    var showControls by remember { mutableStateOf(true) }
    var showDrawer by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<PlayerErrorState?>(null) }
    var retryCounter by remember { mutableIntStateOf(0) }
    var quickListTab by remember { mutableIntStateOf(0) } // 0 = Queue, 1 = Favorites, 2 = EPG / Archive
    var playerSearchQuery by remember { mutableStateOf("") }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeekable by remember { mutableStateOf(false) }
    var isLiveStream by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    val activeArchive = session.archivePlayback
    val playbackUrl = activeArchive?.archiveUrl ?: session.currentChannel.streamUrl

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
                PlayerOrientationLock.AUTO_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                PlayerOrientationLock.LOCKED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayerOrientationLock.LOCKED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    DisposableEffect(isTv, context, activity) {
        val orientationListener = if (!isTv) {
            object : OrientationEventListener(context.applicationContext) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return
                    val isUpright = orientation <= 25 || orientation >= 335
                    val isSideways = orientation in 65..115 || orientation in 245..295
                    when (orientationLock) {
                        PlayerOrientationLock.LOCKED_PORTRAIT -> {
                            if (isUpright) {
                                orientationLock = PlayerOrientationLock.AUTO_SENSOR
                            }
                        }
                        PlayerOrientationLock.LOCKED_LANDSCAPE -> {
                            if (isSideways) {
                                orientationLock = PlayerOrientationLock.AUTO_SENSOR
                            }
                        }
                        PlayerOrientationLock.AUTO_SENSOR -> Unit
                    }
                }
            }.apply {
                if (canDetectOrientation()) {
                    enable()
                }
            }
        } else {
            null
        }

        onDispose {
            orientationListener?.disable()
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
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

        val loadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(1) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val err = loadErrorInfo.exception
                if (err is HttpDataSource.InvalidResponseCodeException &&
                    (err.responseCode == 404 || err.responseCode == 403 || err.responseCode == 410)
                ) {
                    return C.TIME_UNSET
                }
                return if (loadErrorInfo.errorCount <= 1) 600L else C.TIME_UNSET
            }
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorPolicy)
            .createMediaSource(MediaItem.fromUri(playbackUrl))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        showControls = true
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

    LaunchedEffect(showControls, isPlaying, showDrawer, playbackError, isScrubbing) {
        if (showControls && isPlaying && !showDrawer && playbackError == null && !isScrubbing) {
            delay(4500)
            showControls = false
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

    val nowMillis = remember(session.currentChannel) { System.currentTimeMillis() }

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
                showDrawer -> showDrawer = false
                isLandscape && !isTv -> orientationLock = PlayerOrientationLock.LOCKED_PORTRAIT
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
                    .focusable()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = !showControls
                    }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xD9070B14))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = CoralAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = strings.streamErrorTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = when (errState) {
                                    is PlayerErrorState.HttpCode -> strings.streamHttpErrorSubtitle(errState.code)
                                    is PlayerErrorState.Timeout -> strings.streamTimeoutErrorSubtitle
                                    is PlayerErrorState.Generic -> strings.streamErrorSubtitle
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { retryCounter++ },
                                    modifier = Modifier.testTag("player_retry_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.tryAgain)
                                }
                                if (session.queue.size > 1) {
                                    Button(onClick = onNextChannel) {
                                        Text(strings.nextWorkingChannel)
                                    }
                                }
                            }
                        }
                    }
                }

                // OSD Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
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
                                }

                                if (isTv) {
                                    Text(
                                        text = strings.tvControlsHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(horizontal = 8.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
