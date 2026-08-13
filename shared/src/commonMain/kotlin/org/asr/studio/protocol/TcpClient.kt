package org.asr.studio.protocol

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.readByte
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectMode { WIFI, ADB }

class TcpClient(
    private val scope: CoroutineScope
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _activeMode = MutableStateFlow<ConnectMode?>(null)
    val activeMode = _activeMode.asStateFlow()

    private val _frames = MutableSharedFlow<AsrFrame>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val frames = _frames.asSharedFlow()

    private var currentConnection: TcpConnection? = null
    private var connectionJob: Job? = null

    // Tracks the live ServerSocket when in Listen mode.
    // Stored as a field so disconnect() can close it synchronously, eliminating
    // the race condition where rapid Disconnect→Listen gets "Address already in use".
    @Volatile private var activeServerSocket: io.ktor.network.sockets.ServerSocket? = null

    fun connect(host: String, port: Int) {
        println("[TcpClient] connect() called with $host:$port")
        disconnect()
        _activeMode.value = if (host == "127.0.0.1" || host == "localhost") ConnectMode.ADB else ConnectMode.WIFI
        connectionJob = scope.launch(Dispatchers.IO) {
            println("[TcpClient] Entering connect loop for $host:$port")
            while (isActive) {
                if (_connectionState.value != ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Connecting
                }
                try {
                    println("[TcpClient] Attempting to connect to $host:$port ...")
                    val socket = aSocket(selectorManager).tcp().connect(host, port)
                    println("[TcpClient] Socket connected to $host:$port")
                    handleSocket(socket)
                } catch (e: Exception) {
                    println("[TcpClient] Connection failed: ${e.message}")
                    _connectionState.value = ConnectionState.Connecting
                    delay(2000)
                }
            }
            println("[TcpClient] Exiting connect loop")
        }
    }

    fun listen(host: String = "0.0.0.0", port: Int = 4242) {
        println("[TcpClient] listen() requested on $host:$port")
        disconnect()
        _activeMode.value = if (host == "127.0.0.1" || host == "localhost") ConnectMode.ADB else ConnectMode.WIFI
        connectionJob = scope.launch(Dispatchers.IO) {
            println("[TcpClient] Binding ServerSocket to $host:$port ...")
            val serverSocket = try {
                aSocket(selectorManager).tcp().bind(hostname = host, port = port)
            } catch (e: Exception) {
                println("[TcpClient] BIND FAILURE on port $port: ${e.message}")
                _connectionState.value = ConnectionState.Disconnected
                return@launch
            }
            // Register the socket so disconnect() can close it immediately
            activeServerSocket = serverSocket
            println("[TcpClient] Server socket bound to ${serverSocket.localAddress}")

            try {
                while (isActive) {
                    if (_connectionState.value != ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Connecting
                    }
                    println("[TcpClient] Server state: ${_connectionState.value} (Waiting for client on $port...)")
                    val socket = serverSocket.accept()
                    println("[TcpClient] Incoming connection accepted from ${socket.remoteAddress}")
                    handleSocket(socket)
                }
            } catch (e: Exception) {
                if (isActive) {
                    println("[TcpClient] Accept loop error: ${e.message}")
                    e.printStackTrace()
                }
            } finally {
                activeServerSocket = null
                println("[TcpClient] Closing server socket")
                runCatching { serverSocket.close() }
            }
            println("[TcpClient] Exiting listen loop coroutine")
        }
    }

    private suspend fun handleSocket(socket: io.ktor.network.sockets.Socket) {
        println("[TcpClient] handleSocket() started for remote=${socket.remoteAddress} local=${socket.localAddress}")
        val connection = TcpConnection(socket, scope)
        currentConnection = connection
        
        _connectionState.value = ConnectionState.Connected
        println("[TcpClient] State transition: Connected (Source of truth updated)")
        
        val frameCollector = scope.launch {
            try {
                connection.frames.collect { frame ->
                    // println("[TcpClient] Emitting frame from Topic=${TopicId.fromValue(frame.topicId)}")
                    _frames.emit(frame)
                }
            } catch (e: Exception) {
                println("[TcpClient] frameCollector collector error: ${e.message}")
            }
        }
        
        println("[TcpClient] Starting connection reader...")
        connection.start()
        
        // Wait for connection to end by joining the read job
        println("[TcpClient] Blocking on awaitCompletion until socket closes...")
        connection.awaitCompletion()
        
        println("[TcpClient] Connection session ended. Cleaning up.")
        frameCollector.cancel()
        currentConnection = null
        _connectionState.value = ConnectionState.Disconnected
        println("[TcpClient] State transition: Disconnected")
    }

    fun disconnect() {
        println("[TcpClient] disconnect() called")
        connectionJob?.cancel()
        connectionJob = null
        // Explicitly close the server socket BEFORE the new listen() can attempt to bind.
        // Without this, the old coroutine's finally block closes it asynchronously after
        // the new bind attempt, causing "Address already in use".
        runCatching { activeServerSocket?.close() }
        activeServerSocket = null
        currentConnection?.stop()
        currentConnection = null
        _connectionState.value = ConnectionState.Disconnected
        _activeMode.value = null
        println("[TcpClient] State changed to Disconnected (via disconnect)")
    }

    suspend fun send(packet: AsrFrame) {
        currentConnection?.send(packet)
    }
}

// Removed awaitClosed extension as it was buggy and consumed bytes
