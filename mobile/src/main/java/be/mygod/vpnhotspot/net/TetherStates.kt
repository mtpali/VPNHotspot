package be.mygod.vpnhotspot.net

import android.content.IntentFilter
import android.os.Build
import be.mygod.vpnhotspot.App.Companion.app
import be.mygod.vpnhotspot.util.Services
import be.mygod.vpnhotspot.util.broadcastReceiver
import be.mygod.vpnhotspot.util.ensureReceiverUnregistered
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Convenience class that reassembles TetherStates from [TetheringManagerCompat.eventFlow], backfilling
 * compat for API 29.
 */
data class TetherStates(
    val available: PersistentSet<String> = persistentSetOf(),
    val tethered: PersistentSet<String> = persistentSetOf(),
    val errored: PersistentMap<String, Int> = persistentMapOf(),
) {
    companion object {
        private const val ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED"

        /**
         * The reassembled tether states as a cold flow — one source registration per collector. API 30+
         * folds [TetheringManagerCompat.eventFlow]; API 29 parses the sticky
         * `ACTION_TETHER_STATE_CHANGED` broadcast.
         *
         * AOSP dispatches its startup callback burst from one `executor.execute { ... }` block, so the
         * folds are coalesced onto the main handler into a single emission per burst.
         */
        val flow: Flow<TetherStates> = channelFlow {
            var states = TetherStates()
            var dispatchPending = false
            val dispatch = Runnable {
                dispatchPending = false
                trySend(states)
            }
            fun scheduleDispatch() {
                if (dispatchPending) return
                dispatchPending = true
                Services.mainHandler.post(dispatch)
            }
            var broadcastRegistered = false
            val receiver = broadcastReceiver { _, intent ->
                val available = intent.getStringArrayListExtra("availableArray") ?: return@broadcastReceiver
                val tethered = intent.getStringArrayListExtra("tetherArray") ?: return@broadcastReceiver
                val errored = intent.getStringArrayListExtra("erroredArray") ?: return@broadcastReceiver
                val nextErrored = persistentMapOf<String, Int>().builder()
                for (iface in errored) nextErrored[iface] = states.errored[iface] ?: 0
                states = TetherStates(available.toPersistentIfaceSet(), tethered.toPersistentIfaceSet(),
                    nextErrored.build())
                scheduleDispatch()
            }
            if (Build.VERSION.SDK_INT < 30) {
                app.registerReceiver(receiver, IntentFilter(ACTION_TETHER_STATE_CHANGED))
                broadcastRegistered = true
            } else {
                launch(Dispatchers.Main.immediate) {
                    TetheringManagerCompat.eventFlow.collect { event ->
                        when (event) {
                            is TetheringManagerCompat.Event.ErrorChanged -> {
                                states = states.copy(errored = if (event.error == 0) {
                                    states.errored.removing(event.ifName)
                                } else {
                                    states.errored.putting(event.ifName, event.error)
                                })
                                scheduleDispatch()
                            }
                            is TetheringManagerCompat.Event.TetherableInterfacesChanged -> {
                                val available = event.interfaces.toPersistentIfaceSet()
                                states = states.copy(available = available,
                                    errored = states.errored.withoutKeysIn(available))
                                scheduleDispatch()
                            }
                            is TetheringManagerCompat.Event.TetheredInterfacesChanged -> {
                                val tethered = event.interfaces.toPersistentIfaceSet()
                                states = states.copy(tethered = tethered,
                                    errored = states.errored.withoutKeysIn(tethered))
                                scheduleDispatch()
                            }
                            else -> { }     // offload/supported/upstream/regexps/clients aren't tether states
                        }
                    }
                }
            }
            awaitClose {
                Services.mainHandler.removeCallbacks(dispatch)
                if (broadcastRegistered) app.ensureReceiverUnregistered(receiver)
            }
        }.buffer(Channel.UNLIMITED)

        private fun Iterable<String?>.toPersistentIfaceSet(): PersistentSet<String> {
            val builder = persistentSetOf<String>().builder()
            for (iface in this) if (iface != null) builder.add(iface)
            return builder.build()
        }

        private fun PersistentMap<String, Int>.withoutKeysIn(removed: PersistentSet<String>): PersistentMap<String, Int> {
            if (isEmpty() || removed.isEmpty()) return this
            var mapBuilder: PersistentMap.Builder<String, Int>? = null
            for (iface in keys) if (iface in removed) {
                (mapBuilder ?: builder().also { mapBuilder = it }).remove(iface)
            }
            return mapBuilder?.build() ?: this
        }
    }
}
