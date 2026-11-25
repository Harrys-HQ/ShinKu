package com.shinku.reader.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.reader.dialog.ReaderRemoteTranslationDialog
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

internal class RemoteAiConsent {
    internal class Request(val destination: String, val prompt: String, val title: StringResource) {
        val decision = CompletableDeferred<Boolean>()
    }

    internal val pending = MutableStateFlow<Request?>(null)

    suspend fun authorize(destination: String, prompt: String, title: StringResource): Boolean {
        val request = Request(destination, prompt, title)
        if (!pending.compareAndSet(null, request)) return false
        return try {
            request.decision.await()
        } finally {
            pending.compareAndSet(request, null)
        }
    }
}

@Composable
internal fun RemoteAiConsentDialog(consent: RemoteAiConsent) {
    val request by consent.pending.collectAsStateWithLifecycle()
    val current = request ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LifecycleResumeEffect(current) {
        onPauseOrDispose { current.decision.complete(false) }
    }
    ReaderRemoteTranslationDialog(
        destination = current.destination,
        prompt = current.prompt,
        onConfirm = { current.decision.complete(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) },
        onDismissRequest = { current.decision.complete(false) },
        title = stringResource(current.title),
    )
}
