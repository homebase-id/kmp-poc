import id.homebase.homebasekmppoc.prototype.ui.driveFetch.BackendEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBusFlow {
    private val _events = MutableSharedFlow<BackendEvent>(replay = 1, extraBufferCapacity = 10)
    val events: SharedFlow<BackendEvent> = _events.asSharedFlow()

    suspend fun emit(event: BackendEvent) {
        _events.emit(event)
    }
}