package com.garemat.moonstone_companion

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NearbyManager(private val context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val STRATEGY = Strategy.P2P_STAR
    private val SERVICE_ID = "com.garemat.moonstone_companion.NEARBY_SESSION"

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints = _connectedEndpoints.asStateFlow()

    private val _discoveredEndpoints = MutableStateFlow<Map<String, String>>(emptyMap()) // endpointId -> name
    val discoveredEndpoints = _discoveredEndpoints.asStateFlow()

    private var onPayloadReceived: ((String, String) -> Unit)? = null
    private var onConnectionEstablished: ((String) -> Unit)? = null

    fun setPayloadListener(listener: (String, String) -> Unit) {
        onPayloadReceived = listener
    }

    fun setConnectionListener(listener: (String) -> Unit) {
        onConnectionEstablished = listener
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _connectedEndpoints.update { it + endpointId }
                    _discoveredEndpoints.update { it - endpointId }
                    onConnectionEstablished?.invoke(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w("Nearby", "Connection rejected: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e("Nearby", "Connection error: $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.update { it - endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val message = String(bytes, Charsets.UTF_8)
                    onPayloadReceived?.invoke(endpointId, message)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    fun startAdvertising(localName: String) {
        connectionsClient.stopAdvertising()
        val options = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .setDisruptiveUpgrade(false) // Prevents Wi-Fi Hotspot switching
            .build()
            
        connectionsClient.startAdvertising(localName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnFailureListener { Log.e("Nearby", "Advertising failed", it) }
    }

    fun startDiscovery() {
        connectionsClient.stopDiscovery()
        _discoveredEndpoints.value = emptyMap()
        // Focus on discovery and maintain low-bandwidth reliable connection
        val options = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                _discoveredEndpoints.update { it + (endpointId to info.endpointName) }
            }

            override fun onEndpointLost(endpointId: String) {
                _discoveredEndpoints.update { it - endpointId }
            }
        }, options)
            .addOnFailureListener { Log.e("Nearby", "Discovery failed", it) }
    }

    fun requestConnection(localName: String, endpointId: String) {
        connectionsClient.requestConnection(localName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { Log.e("Nearby", "Connection request failed", it) }
    }

    fun sendPayload(endpointId: String, message: String) {
        val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun sendPayloadToAll(message: String) {
        val endpoints = _connectedEndpoints.value.toList()
        if (endpoints.isNotEmpty()) {
            val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
            connectionsClient.sendPayload(endpoints, payload)
        }
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        _discoveredEndpoints.value = emptyMap()
    }
}
