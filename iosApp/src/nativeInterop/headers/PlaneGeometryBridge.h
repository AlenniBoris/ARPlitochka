#pragma once

#include <stdbool.h>
#include <stdint.h>
#import <SceneKit/SceneKit.h>

#ifdef __cplusplus
extern "C" {
#endif

bool pg_local_point_in_polygon(void *planeAnchor, float localX, float localZ);
bool pg_local_point_in_render_boundary(void *planeAnchor, int boundaryMode, float localX, float localZ);
bool pg_anchor_has_polygon_boundary(void *planeAnchor);
uint32_t pg_geometry_signature(void *planeAnchor);
uint32_t pg_geometry_signature_extent(void *planeAnchor);
int pg_generate_dot_points(
    void *planeAnchor,
    float stepM,
    float refX,
    float refZ,
    float maxRadiusM,
    float *outLocalX,
    float *outLocalZ,
    int maxPoints
);
int pg_collect_window_dot_points(
    void *planeAnchor,
    int boundaryMode,
    float stepM,
    float refX,
    float refZ,
    float maxRadiusM,
    float *outLocalX,
    float *outLocalZ,
    int maxPoints
);
float pg_polygon_area(void *planeAnchor);
#define PG_DOT_BOUNDARY_EXTENT 0
#define PG_DOT_BOUNDARY_POLYGON 1

SCNGeometry *pg_create_dot_mesh_geometry(
    void *planeAnchor,
    int boundaryMode,
    float stepM,
    float refX,
    float refZ,
    float maxRadiusM,
    float dotRadiusM,
    float yOffsetM,
    int maxPoints,
    int *outDotCount
);
SCNGeometry *pg_create_dot_mesh_local_disc(
    float centerX,
    float centerZ,
    float radiusM,
    float stepM,
    float dotRadiusM,
    float yOffsetM,
    int *outDotCount
);
SCNGeometry *pg_create_preview_dot_mesh_geometry(
    float radiusM,
    float stepM,
    float dotRadiusM,
    float yOffsetM,
    int *outDotCount
);
SCNGeometry *pg_create_dot_mesh_from_points(
    const float *localX,
    const float *localZ,
    int pointCount,
    float dotRadiusM,
    float yOffsetM,
    int *outDotCount
);

#ifdef __cplusplus
}
#endif
