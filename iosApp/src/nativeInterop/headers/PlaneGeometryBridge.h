#pragma once

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

bool pg_local_point_in_polygon(void *planeAnchor, float localX, float localZ);
uint32_t pg_geometry_signature(void *planeAnchor);
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
float pg_polygon_area(void *planeAnchor);

#ifdef __cplusplus
}
#endif
