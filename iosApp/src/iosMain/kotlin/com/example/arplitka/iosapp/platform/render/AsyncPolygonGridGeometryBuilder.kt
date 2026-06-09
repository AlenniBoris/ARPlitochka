package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.platform.render.bridgePointer
import com.example.arplitka.iosapp.bridge.PG_MAX_BOUNDARY_VERTICES
import com.example.arplitka.iosapp.bridge.pg_copy_plane_boundary_xz
import com.example.arplitka.iosapp.bridge.pg_create_polygon_grid_line_geometry_from_vertices
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.ARKit.ARPlaneAnchor
import platform.Foundation.NSUUID
import platform.SceneKit.SCNGeometry
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

internal data class PlaneBoundarySnapshot(
    val localX: FloatArray,
    val localZ: FloatArray,
    val vertexCount: Int
)

/**
 * Copy boundary on main, build polygon grid on a background queue, apply on main.
 * One in-flight build per anchor; stale results are dropped by generation counter.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AsyncPolygonGridGeometryBuilder {
    private val latestGeneration = mutableMapOf<NSUUID, Int>()

    fun cancel(anchorId: NSUUID) {
        latestGeneration.remove(anchorId)
    }

    fun cancelAll() {
        latestGeneration.clear()
    }

    fun requestBuild(
        anchorId: NSUUID,
        boundary: PlaneBoundarySnapshot,
        cellM: Float,
        lineWidthM: Float,
        boundaryLineWidthM: Float,
        yM: Float,
        onReady: (SCNGeometry?) -> Unit
    ) {
        val generation = (latestGeneration[anchorId] ?: 0) + 1
        latestGeneration[anchorId] = generation

        val localX = boundary.localX.copyOf()
        val localZ = boundary.localZ.copyOf()
        val vertexCount = boundary.vertexCount

        dispatch_async(backgroundQueue()) {
            val geometry = buildGeometry(
                localX,
                localZ,
                vertexCount,
                cellM,
                lineWidthM,
                boundaryLineWidthM,
                yM
            )
            dispatch_async(dispatch_get_main_queue()) {
                if (latestGeneration[anchorId] != generation) return@dispatch_async
                onReady(geometry)
            }
        }
    }

    private fun buildGeometry(
        localX: FloatArray,
        localZ: FloatArray,
        vertexCount: Int,
        cellM: Float,
        lineWidthM: Float,
        boundaryLineWidthM: Float,
        yM: Float
    ): SCNGeometry? =
        localX.usePinned { xPinned ->
            localZ.usePinned { zPinned ->
                pg_create_polygon_grid_line_geometry_from_vertices(
                    localX = xPinned.addressOf(0),
                    localZ = zPinned.addressOf(0),
                    vertexCount = vertexCount,
                    cellM = cellM,
                    lineWidthM = lineWidthM,
                    boundaryLineWidthM = boundaryLineWidthM,
                    yM = yM
                )
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
internal fun copyPlaneBoundarySnapshot(anchor: ARPlaneAnchor): PlaneBoundarySnapshot? =
    memScoped {
        val maxVertices = PG_MAX_BOUNDARY_VERTICES.toInt()
        val boundaryLocalX = allocArray<FloatVar>(maxVertices)
        val boundaryLocalZ = allocArray<FloatVar>(maxVertices)
        val count = pg_copy_plane_boundary_xz(
            planeAnchor = anchor.bridgePointer(),
            outLocalX = boundaryLocalX,
            outLocalZ = boundaryLocalZ,
            maxVertices = maxVertices
        )
        if (count < 3) return null
        val localX = FloatArray(count)
        val localZ = FloatArray(count)
        for (i in 0 until count) {
            localX[i] = boundaryLocalX[i]
            localZ[i] = boundaryLocalZ[i]
        }
        PlaneBoundarySnapshot(
            localX = localX,
            localZ = localZ,
            vertexCount = count
        )
    }

private fun backgroundQueue() = dispatch_get_global_queue(0L, 0u)
