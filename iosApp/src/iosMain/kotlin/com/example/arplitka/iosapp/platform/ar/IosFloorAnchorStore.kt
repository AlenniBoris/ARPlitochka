package com.example.arplitka.iosapp.platform.ar

import com.example.arplitka.iosapp.bridge.pg_session_add_anchor_from_column_major
import com.example.arplitka.iosapp.platform.render.bridgePointer
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.ARKit.ARAnchor
import platform.ARKit.ARFrame
import platform.ARKit.ARSession
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
internal class IosFloorAnchorStore {
    private data class Entry(
        val logicalId: String,
        /** World position captured at tap time — not re-derived from plane anchor. */
        val tapWorldPosition: ArPoint3D
    )

    private val entries = mutableListOf<Entry>()
    private var contourRootAnchorId: NSUUID? = null

    val logicalIds: List<String>
        get() = entries.map { it.logicalId }

    fun setSectionPlaneAnchorId(anchorId: NSUUID?) {
        // Kept for coordinator compatibility.
    }

    fun register(
        session: ARSession,
        logicalId: String,
        worldPosition: ArPoint3D,
        @Suppress("UNUSED_PARAMETER") frame: ARFrame?
    ) {
        if (contourRootAnchorId == null) {
            contourRootAnchorId = createContourRootAnchor(session, worldPosition)
        }
        entries += Entry(
            logicalId = logicalId,
            tapWorldPosition = worldPosition
        )
    }

    fun detachLast(session: ARSession, frame: ARFrame?) {
        entries.removeLastOrNull()
        if (entries.isEmpty()) {
            removeContourRootAnchor(session, frame)
        }
    }

    fun detachAll(session: ARSession, frame: ARFrame?) {
        entries.clear()
        removeContourRootAnchor(session, frame)
    }

    fun placedPoints(@Suppress("UNUSED_PARAMETER") frame: ARFrame?, sectionFloorY: Float?): List<PlacedContourPoint> {
        if (entries.isEmpty()) return emptyList()
        return entries.map { entry ->
            val position = com.example.arplitka.shared.ar.domain.logic.FloorGeometry.projectToSectionFloor(
                entry.tapWorldPosition,
                sectionFloorY
            )
            PlacedContourPoint(id = entry.logicalId, position = position)
        }
    }

    private fun createContourRootAnchor(session: ARSession, worldPosition: ArPoint3D): NSUUID? {
        val transform = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            worldPosition.xMeters,
            worldPosition.yMeters,
            worldPosition.zMeters,
            1f
        )
        return transform.usePinned { pinned ->
            val anchorPtr = pg_session_add_anchor_from_column_major(
                sessionPtr = session.bridgePointer(),
                in16 = pinned.addressOf(0)
            )
            (anchorPtr as? ARAnchor)?.identifier
        }
    }

    private fun removeContourRootAnchor(session: ARSession, frame: ARFrame?) {
        val rootAnchorId = contourRootAnchorId ?: return
        frame?.findAnchor(rootAnchorId)?.let { session.removeAnchor(it) }
        contourRootAnchorId = null
    }

    private fun ARFrame.findAnchor(anchorId: NSUUID): ARAnchor? {
        val targetId = anchorId.UUIDString()
        return anchors
            .mapNotNull { it as? ARAnchor }
            .firstOrNull { it.identifier.UUIDString() == targetId }
    }
}
