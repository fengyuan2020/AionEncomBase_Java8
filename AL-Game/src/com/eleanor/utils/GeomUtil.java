package com.eleanor.utils;

import com.aionemu.gameserver.geoEngine.math.FastMath;
import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * Utility class providing geometric calculations.
 */
public class GeomUtil {

    /**
     * Calculates the next point in 2D space given a source point, angle, and distance.
     *
     * @param source   The source point.
     * @param angle    The angle in degrees.
     * @param distance The distance to the next point.
     * @return The next point in 2D space.
     */
    public static Vector2f getNextPoint2D(Vector2f source, float angle, float distance) {
        // Convert angle to radians
        double angleInRadians = Math.toRadians(angle);

        // Calculate x and y coordinates of the next point
        double x = source.x + distance * Math.cos(angleInRadians);
        double y = source.y + distance * Math.sin(angleInRadians);

        return new Vector2f((float) x, (float) y);
    }

    /**
     * Calculates the next point in 2D space given a source point, direction vector, and distance.
     *
     * @param sX       The x-coordinate of the source point.
     * @param sY       The y-coordinate of the source point.
     * @param vecX     The x-component of the direction vector.
     * @param vecY     The y-component of the direction vector.
     * @param distance The distance to the next point.
     * @return The next point in 2D space.
     */
    public static Vector2f getNextPoint2D(float sX, float sY, float vecX, float vecY, float distance) {
        return new Vector2f(sX + vecX * distance, sY + vecY * distance);
    }

    /**
     * Calculates the direction vector from one 3D point to another.
     *
     * @param from The starting point.
     * @param to   The destination point.
     * @return The normalized direction vector.
     */
    public static Vector3f getDirection3D(Vector3f from, Vector3f to) {
        // Calculate the direction vector by subtracting the starting point from the destination point
        Vector3f direction = to.subtract(from);

        // Normalize the direction vector to have a length of 1
        return direction.normalizeLocal();
    }

    /**
     * Calculates the next point in 3D space given a source point, direction vector, and distance.
     *
     * @param source    The source point.
     * @param direction The direction vector.
     * @param distance  The distance to the next point.
     * @return The next point in 3D space.
     */
    public static Vector3f getNextPoint3D(Vector3f source, Vector3f direction, float distance) {
        // Calculate the next point by adding the scaled direction vector to the source point
        return source.add(direction.mult(distance));
    }

    /**
     * Calculates the distance between two 3D points.
     *
     * @param source The source point.
     * @param x2     The x-coordinate of the second point.
     * @param y2     The y-coordinate of the second point.
     * @param z2     The z-coordinate of the second point.
     * @return The distance between the two points.
     */
    public static float getDistance3D(Vector3f source, float x2, float y2, float z2) {
        return getDistance3D(source.x, source.y, source.z, x2, y2, z2);
    }

    /**
     * Calculates the distance between two 3D points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param z1 The z-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @param z2 The z-coordinate of the second point.
     * @return The distance between the two points.
     */
    public static float getDistance3D(float x1, float y1, float z1, float x2, float y2, float z2) {
        // Calculate the differences in x, y, and z coordinates
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;

        // Calculate the distance using the Pythagorean theorem
        return FastMath.sqrt((float) (dx * dx + dy * dy + dz * dz));
    }
}