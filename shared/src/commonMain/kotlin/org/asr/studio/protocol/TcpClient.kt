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

class TcpClient(
    private val scope: CoroutineScope
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _frames = MutableSharedFlow<AsrFrame>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val frames = _frames.asSharedFlow()

    private var currentConnection: TcpConnection? = null
    private var connectionJob: Job? = null

    fun connect(host: String, port: Int) {
        println("[TcpClient] connect() called with $host:$port")
        disconnect()
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

    fun listen(port: Int) {
        println("[TcpClient] listen() requested on port $port")
        disconnect()
        connectionJob = scope.launch(Dispatchers.IO) {
            println("[TcpClient] Binding ServerSocket to 0.0.0.0:$port ...")
            try {
                // Explicitly binding to 0.0.0.0 to ensure both IPv4 and IPv6 can connect
                val serverSocket = aSocket(selectorManager).tcp().bind(hostname = "0.0.0.0", port = port)
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
                    println("[TcpClient] Closing server socket")
                    runCatching { serverSocket.close() }
                }
            } catch (e: Exception) {
                println("[TcpClient] BIND FAILURE on port $port: ${e.message}")
                _connectionState.value = ConnectionState.Disconnected
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
        currentConnection?.stop()
        currentConnection = null
        _connectionState.value = ConnectionState.Disconnected
        println("[TcpClient] State changed to Disconnected (via disconnect)")
    }

    suspend fun send(packet: AsrFrame) {
        currentConnection?.send(packet)
    }
}

// Removed awaitClosed extension as it was buggy and consumed bytes
