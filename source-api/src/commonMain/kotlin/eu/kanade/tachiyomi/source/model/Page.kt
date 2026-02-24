package eu.kanade.tachiyomi.source.model

import android.net.Uri
import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open @SerialName("eu.kanade.tachiyomi.source.model.Page")
class Page(
    val index: Int,
    /* SY --> */
    var /* SY <-- */ url: String = "",
    var imageUrl: String? = null,
    @Transient var uri: Uri? = null, // Deprecated but can't be deleted due to extensions
) : ProgressListener {

    val number: Int
        get() = index + 1

    @Transient
    private val _statusFlow = MutableStateFlow<State>(State.Queue)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    @Transient
    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    sealed interface State {
        @SerialName("eu.kanade.tachiyomi.source.model.Queue")
data object Queue : State
        @SerialName("eu.kanade.tachiyomi.source.model.LoadPage")
data object LoadPage : State
        @SerialName("eu.kanade.tachiyomi.source.model.DownloadImage")
data object DownloadImage : State
        @SerialName("eu.kanade.tachiyomi.source.model.Ready")
data object Ready : State
        @SerialName("eu.kanade.tachiyomi.source.model.Error")
data class Error(val error: Throwable) : State
    }
}
