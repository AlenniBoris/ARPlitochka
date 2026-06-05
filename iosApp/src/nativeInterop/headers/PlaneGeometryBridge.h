#pragma once

#include <stdbool.h>
#include <stdint.h>
#import <SceneKit/SceneKit.h>
#import <UIKit/UIKit.h>

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
bool pg_plane_extent_dimensions(
    void *planeAnchor,
    float *outWidth,
    float *outHeight,
    float *outCenterX,
    float *outCenterZ
);
float pg_plane_extent_area(void *planeAnchor);
bool pg_world_xz_on_anchor(
    void *anchorPtr,
    float worldX,
    float worldY,
    float worldZ,
    float *outLocalX,
    float *outLocalZ
);
SCNGeometry *pg_create_grid_line_geometry(
    float widthM,
    float depthM,
    float centerX,
    float centerZ,
    float cellM,
    float lineWidthM,
    float yM
);
SCNGeometry *pg_create_polygon_grid_line_geometry(
    void *planeAnchor,
    float cellM,
    float lineWidthM,
    float yM
);
#define PG_RAYCAST_EXISTING_PLANE 0
#define PG_RAYCAST_ESTIMATED_PLANE 1
#define PG_RAYCAST_MESH 2
#define PG_PLANE_CLASSIFICATION_NONE (-1)
#define PG_PLANE_CLASSIFICATION_WALL 0
#define PG_PLANE_CLASSIFICATION_FLOOR 1
#define PG_PLANE_CLASSIFICATION_CEILING 2
#define PG_PLANE_CLASSIFICATION_TABLE 3
#define PG_PLANE_CLASSIFICATION_SEAT 4
#define PG_PLANE_CLASSIFICATION_UNKNOWN 7
bool pg_center_raycast(
    void *sessionPtr,
    void *framePtr,
    float screenX,
    float screenY,
    int targetKind,
    float *outDistance,
    float *outWorldTransform16,
    char *outAnchorUuid,
    int outAnchorUuidCapacity
);
bool pg_session_supports_mesh_raycast(void *sessionPtr);
int pg_plane_classification(void *planeAnchor);
bool pg_plane_is_floor_like(void *planeAnchor);
UIImage *pg_grid_pattern_image(void);
UIImage *pg_grid_pattern_image_outdoor(void);
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
