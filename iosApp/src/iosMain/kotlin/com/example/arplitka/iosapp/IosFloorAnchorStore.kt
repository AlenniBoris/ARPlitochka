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
    private val cachedPositions = mutableMapOf<String, ArPoint3D>()

    val logicalIds: List<String>
        get() = entries.map { it.logicalId }

    fun register(logicalId: String, anchor: ARAnchor, initialPosition: ArPoint3D) {
        entries += Entry(logicalId = logicalId, platformId = anchor.identifier)
        cachedPositions[logicalId] = initialPosition
    }

    fun detachLast(session: ARSession, frame: ARFrame?) {
        val entry = entries.removeLastOrNull() ?: return
        cachedPositions.remove(entry.logicalId)
        removePlatformAnchor(session, frame, entry.platformId)
    }

    fun detachAll(session: ARSession, frame: ARFrame?) {
        val ids = entries.map { it.platformId }
        entries.clear()
        cachedPositions.clear()
        ids.forEach { platformId -> removePlatformAnchor(session, frame, platformId) }
    }

    /** Tap-time cached positions are the source of truth (anchors are session ownership only). */
    fun placedPoints(sectionFloorY: Float?): List<PlacedContourPoint> =
        entries.mapNotNull { entry ->
            val raw = cachedPositions[entry.logicalId] ?: return@mapNotNull null
            val position = com.example.arplitka.shared.ar.domain.logic.FloorGeometry.projectToSectionFloor(
                raw,
                sectionFloorY
            )
            cachedPositions[entry.logicalId] = position
            PlacedContourPoint(id = entry.logicalId, position = position)
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
