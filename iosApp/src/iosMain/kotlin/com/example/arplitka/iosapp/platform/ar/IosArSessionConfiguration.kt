package com.example.arplitka.iosapp.platform.ar

import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARPlaneDetectionHorizontal
import platform.ARKit.ARSceneReconstructionMesh
import platform.ARKit.ARWorldTrackingConfiguration

internal enum class IosArSessionFeature {
    PLANE_DETECTION,
    LIDAR_MESH
}

@OptIn(ExperimentalForeignApi::class)
internal fun createWorldTrackingConfiguration(
    enableLidarMesh: Boolean
): Pair<ARWorldTrackingConfiguration, Set<IosArSessionFeature>> {
    val features = mutableSetOf(IosArSessionFeature.PLANE_DETECTION)
    val configuration = ARWorldTrackingConfiguration().apply {
        planeDetection = ARPlaneDetectionHorizontal
        if (enableLidarMesh && supportsSceneReconstructionMesh()) {
            sceneReconstruction = ARSceneReconstructionMesh
            features += IosArSessionFeature.LIDAR_MESH
        }
    }
    return configuration to features
}

@OptIn(ExperimentalForeignApi::class)
internal fun supportsSceneReconstructionMesh(): Boolean =
    ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstructionMesh)

internal fun Set<IosArSessionFeature>.debugLabel(): String =
    when {
        contains(IosArSessionFeature.LIDAR_MESH) -> "lidar-mesh+planes"
        else -> "planes"
    }
