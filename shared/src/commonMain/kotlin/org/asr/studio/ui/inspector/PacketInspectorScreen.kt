package org.asr.studio.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.PacketEntry
import org.asr.studio.protocol.TopicId

@Composable
fun PacketInspectorScreen(viewModel: PacketInspectorViewModel) {
    val packets by viewModel.packets.collectAsState()
    var selectedPacket by remember { mutableStateOf<PacketEntry?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Packet List
        Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Packet History (${packets.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { viewModel.clear() }) {
                    Text("Clear")
                }
            }
            
            PacketList(
                packets = packets,
                selectedPacket = selectedPacket,
                onPacketSelected = { selectedPacket = it }
            )
        }

        VerticalDivider()

        // Detail Panel
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
            PacketDetailPanel(selectedPacket)
        }
    }
}

@Composable
private fun PacketList(
    packets: List<PacketEntry>,
    selectedPacket: PacketEntry?,
    onPacketSelected: (PacketEntry) -> Unit
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom if at bottom
    LaunchedEffect(packets.size) {
        if (packets.isNotEmpty()) {
            listState.animateScrollToItem(packets.size - 1)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            HeaderRow()
        }
        items(packets) { entry ->
            PacketRow(
                entry = entry,
                isSelected = entry == selectedPacket,
                onClick = { onPacketSelected(entry) }
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Topic", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Seq", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Size", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Timestamp (ns)", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("CRC", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun PacketRow(
    entry: PacketEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val topic = TopicId.fromValue(entry.frame.topicId) ?: TopicId.RESERVED_1F
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(topic.name, Modifier.weight(1.2f), fontSize = 11.sp)
        Text(entry.frame.seq.toString(), Modifier.weight(0.8f), fontSize = 11.sp)
        Text(entry.frame.payloadLength.toString(), Modifier.weight(0.8f), fontSize = 11.sp)
        Text(entry.frame.timestampNs.toString(), Modifier.weight(1.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        
        Text(
            text = if (entry.frame.crcValid) "OK" else "FAIL",
            modifier = Modifier.weight(0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (entry.frame.crcValid) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

@Composable
private fun PacketDetailPanel(entry: PacketEntry?) {
    if (entry == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a packet to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Header Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow("Topic", TopicId.fromValue(entry.frame.topicId)?.name ?: "Unknown")
        DetailRow("Sequence", entry.frame.seq.toString())
        DetailRow("Timestamp", "${entry.frame.timestampNs} ns")
        DetailRow("Payload Size", "${entry.frame.payloadLength} bytes")
        DetailRow("Stored CRC", "0x${entry.frame.storedCrc.toString(16).uppercase()}")
        DetailRow("Computed CRC", "0x${entry.frame.computedCrc.toString(16).uppercase()}")
        DetailRow("CRC Valid", entry.frame.crcValid.toString())
        DetailRow("Flags", "0x${entry.frame.flags.toString(16).uppercase()}")

        Spacer(modifier = Modifier.height(24.dp))
        Text("Payload Hex Dump", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = hexDump(entry.frame.payload),
                modifier = Modifier.padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun hexDump(bytes: ByteArray): String {
    if (bytes.isEmpty()) return "(Empty)"
    val sb = StringBuilder()
    for (i in bytes.indices) {
        if (i > 0 && i % 16 == 0) sb.append("\n")
        sb.append("%02X ".format(bytes[i]))
    }
    return sb.toString()
}
