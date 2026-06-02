package com.example.arplitka.iosapp

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARAnchor
import platform.ARKit.ARFrame
import platform.ARKit.ARSession
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
internal class IosFloorAnchorStore {
    private data class Entry(
        val logicalId: String,
        val platformId: NSUUID
    )

    private val entries = mutableListOf<Entry>()

    val logicalIds: List<String>
        get() = entries.map { it.logicalId }

    fun register(logicalId: String, anchor: ARAnchor) {
        entries += Entry(logicalId = logicalId, platformId = anchor.identifier)
    }

    fun detachLast(session: ARSession, frame: ARFrame?) {
        val entry = entries.removeLastOrNull() ?: return
        removePlatformAnchor(session, frame, entry.platformId)
    }

    fun detachAll(session: ARSession, frame: ARFrame?) {
        val ids = entries.map { it.platformId }
        entries.clear()
        ids.forEach { platformId -> removePlatformAnchor(session, frame, platformId) }
    }

    fun readPositions(frame: ARFrame): List<PlacedContourPoint> =
        entries.mapNotNull { entry ->
            val anchor = frame.anchors
                .mapNotNull { it as? ARAnchor }
                .firstOrNull { it.identifier == entry.platformId }
                ?: return@mapNotNull null
            PlacedContourPoint(
                id = entry.logicalId,
                position = HitTransformReader.worldPointFromAnchor(anchor)
            )
        }

    private fun removePlatformAnchor(session: ARSession, frame: ARFrame?, platformId: NSUUID) {
        val anchor = frame?.anchors
            ?.mapNotNull { it as? ARAnchor }
            ?.firstOrNull { it.identifier == platformId }
        if (anchor != null) {
            session.removeAnchor(anchor)
        }
    }
}
