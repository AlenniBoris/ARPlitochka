package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.bridge.pg_create_contour_fill_geometry
import com.example.arplitka.iosapp.bridge.pg_create_contour_lines_geometry
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.SceneKit.SCNGeometry
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

internal data class ContourFillBuildRequest(
    val pointBuffer: FloatArray,
    val uvBuffer: FloatArray,
    val fillPointCount: Int,
    val centroidX: Float,
    val centroidZ: Float,
    val centroidU: Float,
    val centroidV: Float,
    val yM: Float
)

internal data class ContourLinesBuildRequest(
    val segmentBuffer: FloatArray,
    val segmentCount: Int,
    val yM: Float,
    val halfWidthM: Float
)

/**
 * Build contour fill/lines off the main thread.
 * Stale in-flight builds are dropped via a generation counter (same pattern as scan grid).
 */
@OptIn(ExperimentalForeignApi::class)
internal class AsyncContourGeometryBuilder {
    private var fillGeneration = 0
    private var linesGeneration = 0

    fun cancelAll() {
        fillGeneration++
        linesGeneration++
    }

    fun requestFillBuild(
        batchKey: Int,
        request: ContourFillBuildRequest,
        onReady: (Int, SCNGeometry?) -> Unit
    ) {
        val generation = fillGeneration + 1
        fillGeneration = generation

        val pointBuffer = request.pointBuffer.copyOf()
        val uvBuffer = request.uvBuffer.copyOf()
        val fillPointCount = request.fillPointCount
        val centroidX = request.centroidX
        val centroidZ = request.centroidZ
        val centroidU = request.centroidU
        val centroidV = request.centroidV
        val yM = request.yM

        dispatch_async(backgroundQueue()) {
            val geometry = pointBuffer.usePinned { pointsPinned ->
                uvBuffer.usePinned { uvsPinned ->
                    pg_create_contour_fill_geometry(
                        pointCount = fillPointCount,
                        pointsXZ = pointsPinned.addressOf(0),
                        uvs = uvsPinned.addressOf(0),
                        centroidX = centroidX,
                        centroidZ = centroidZ,
                        centroidU = centroidU,
                        centroidV = centroidV,
                        yM = yM
                    )
                }
            }
            dispatch_async(dispatch_get_main_queue()) {
                if (fillGeneration != generation) return@dispatch_async
                onReady(batchKey, geometry)
            }
        }
    }

    fun requestLinesBuild(
        batchKey: Int,
        request: ContourLinesBuildRequest,
        onReady: (Int, SCNGeometry?) -> Unit
    ) {
        val generation = linesGeneration + 1
        linesGeneration = generation

        val segmentBuffer = request.segmentBuffer.copyOf()
        val segmentCount = request.segmentCount
        val yM = request.yM
        val halfWidthM = request.halfWidthM

        dispatch_async(backgroundQueue()) {
            val geometry = segmentBuffer.usePinned { pinned ->
                pg_create_contour_lines_geometry(
                    segmentCount = segmentCount,
                    segmentPairsXZ = pinned.addressOf(0),
                    yM = yM,
                    halfWidthM = halfWidthM
                )
            }
            dispatch_async(dispatch_get_main_queue()) {
                if (linesGeneration != generation) return@dispatch_async
                onReady(batchKey, geometry)
            }
        }
    }
}

private fun backgroundQueue() = dispatch_get_global_queue(0L, 0u)
