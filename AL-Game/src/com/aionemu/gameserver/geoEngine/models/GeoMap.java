/*
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.geoEngine.models;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Triangle;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.geoEngine.scene.mesh.DoorGeometry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr. Poke
 * @rework MATTY
 */
public class GeoMap extends Node {

  private static final Logger log = LoggerFactory.getLogger(GeoMap.class);
  private short[] terrainData;
  private final List<BoundingBox> tmpBox = new ArrayList<>();
  private final Map<String, DoorGeometry> doors = new FastMap<>();
  private int[] terrainCutoutData;

  /**
   * @param name
   */
  public GeoMap(String name, int worldSize) {
    super(name); // Call the superclass constructor with the name
    setCollisionFlags((short) (CollisionIntention.ALL.getId() << 8));
    for (int x = 0; x < worldSize; x += 256) {
      for (int y = 0; y < worldSize; y += 256) {
        Node geoNode = new Node("");
        geoNode.setCollisionFlags((short) (CollisionIntention.ALL.getId() << 8));
        tmpBox.add(new BoundingBox(new Vector3f(x, y, 0), new Vector3f(x + 256, y + 256, 4000)));
        super.attachChild(geoNode);
      }
    }
  }

  public String getDoorName(int worldId, String meshFile, float x, float y, float z) {
    if (!GeoDataConfig.GEO_DOORS_ENABLE) {
      return null;
    }
    String mesh = meshFile.toUpperCase();
    Vector3f templatePoint = new Vector3f(x, y, z);
    float distance = Float.MAX_VALUE;
    DoorGeometry foundDoor = null;
    for (Entry<String, DoorGeometry> doorEntry : doors.entrySet()) {
      String doorKey = doorEntry.getKey();
      DoorGeometry door = doorEntry.getValue();

      if (!doorKey.startsWith(Integer.toString(worldId)) || !doorKey.endsWith(mesh)) {
        continue;
      }

      float doorDistance = checkDistance(door, templatePoint, distance);
      if (doorDistance < distance) {
        distance = doorDistance;
        foundDoor = door;
      }
      if (checkDoorIntersection(door, templatePoint)) {
        foundDoor = door;
        break;
      }
    }
    if (foundDoor == null) {
      // log.warn("Could not find static door: " + worldId + " " + meshFile + " " +
      // templatePoint);
      return null;
    }
    foundDoor.setFoundTemplate(true);
    // log.info("Static door " + worldId + " " + meshFile + " " + templatePoint + "
    // matched " + foundDoor.getName() +
    // "; distance: " + distance);
    return foundDoor.getName();
  }

  private float checkDistance(DoorGeometry door, Vector3f templatePoint, float currentDistance) {
    float doorDistance = door.getWorldBound().distanceTo(templatePoint);
    return Math.min(currentDistance, doorDistance);
  }

  private boolean checkDoorIntersection(DoorGeometry door, Vector3f templatePoint) {
    return door.getWorldBound().intersects(templatePoint);
  }

  public boolean canPassWalker(
      float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
      int instanceId) {
    float distance = calculateDistance(x, y, targetX, targetY);
    if (distance > 50.0f) {
      return false;
    }
    Vector3f pos = new Vector3f(x, y, z);
    Vector3f dir = new Vector3f(targetX, targetY, targetZ).subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), true, instanceId);
    int collisions = this.collideWith(r, results);
    return results.size() == 0 && collisions == 0;  // Use size() instead of isEmpty()
  }

  public boolean canPass(
      float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
      int instanceId) {
    float distance = calculateDistance(x, y, targetX, targetY);
    if (distance > 65.0f) {
      return false;
    }
    Vector3f pos = new Vector3f(x, y, z);
    Vector3f dir = new Vector3f(targetX, targetY, targetZ).subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, instanceId);
    int collisions = this.collideWith(r, results);
    return results.size() == 0 && collisions == 0; // Use size() instead of isEmpty()
  }

  private float calculateDistance(float x, float y, float targetX, float targetY) {
    float x2 = x - targetX;
    float y2 = y - targetY;
    return (float) Math.sqrt(x2 * x2 + y2 * y2);
  }

  public float getZW(float x, float y) {
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), true, 1);
    Vector3f pos = new Vector3f(x, y, 4000.0f);
    Vector3f dir = new Vector3f(x, y, 0.0f);
    float limit = pos.distance(dir);
    dir.subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    this.collideWith(r, results);
    Vector3f terrain = null;
    if (terrainData.length == 1) {
      terrain = new Vector3f(x, y, terrainData[0] / 32.0f);
    } else {
      terrain = terraionCollision(x, y, r);
    }
    if (terrain != null) {
      CollisionResult result = new CollisionResult(terrain, Math.max(0.0f, 4000.0f - terrain.z));
      results.addCollision(result);
    }
    if (results.size() == 0) { // Use size() instead of isEmpty()
      return 0.0f;
    }
    return results.getClosestCollision().getContactPoint().z;
  }

  public float getZW(float x, float y, float z, int instanceId) {
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), true, instanceId);
    Vector3f pos = new Vector3f(x, y, z + 2.0f);
    Vector3f dir = new Vector3f(x, y, z - 100.0f);
    float limit = pos.distance(dir);
    dir.subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    this.collideWith(r, results);
    Vector3f terrain = null;
    if (terrainData.length == 1) {
      if (terrainData[0] != 0) {
        terrain = new Vector3f(x, y, terrainData[0] / 32.0f);
      }
    } else {
      terrain = terraionCollision(x, y, r);
    }
    if (terrain != null && terrain.z > 0.0f && terrain.z < z + 2.0f) {
      CollisionResult result = new CollisionResult(terrain, Math.abs(z - terrain.z + 2.0f));
      results.addCollision(result);
    }
    if (results.size() == 0) { // Use size() instead of isEmpty()
      return z;
    }
    return results.getClosestCollision().getContactPoint().z;
  }

  public void setDoorState(int instanceId, String name, boolean isOpened) {
    DoorGeometry door = doors.get(name);
    if (door != null) {
      door.setDoorState(instanceId, isOpened);
    }
  }

  @Override
  public int attachChild(Spatial child) {
    if (child instanceof DoorGeometry) {
      doors.put(child.getName(), (DoorGeometry) child);
    }

    for (Spatial spatial : getChildren()) {
      if (spatial instanceof Node && tmpBox.stream().anyMatch(box -> box.intersects(child.getWorldBound()))) {
        ((Node) spatial).attachChild(child);
      }
    }
    return 0;
  }

  /**
   * @param terrainData The terrainData to set.
   */
  public void setTerrainData(short[] terrainData) {
    this.terrainData = terrainData;
  }

  public float getZ(float x, float y) {
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1);
    Vector3f pos = new Vector3f(x, y, 4000);
    Vector3f dir = new Vector3f(x, y, 0);
    float limit = pos.distance(dir);
    dir.subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    collideWith(r, results);
    Vector3f terrain = null;
    if (terrainData.length == 1) {
      terrain = new Vector3f(x, y, terrainData[0] / 32f);
    } else {
      terrain = terraionCollision(x, y, r);
    }
    if (terrain != null) {
      CollisionResult result = new CollisionResult(terrain, Math.max(0, 4000 - terrain.z));
      results.addCollision(result);
    }
    if (results.size() == 0) { // Use size() instead of isEmpty()
      return 0;
    }
    return results.getClosestCollision().getContactPoint().z;
  }

  public float getZ(float x, float y, float z, int instanceId) {
    CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, instanceId);
    Vector3f pos = new Vector3f(x, y, z + 2);
    Vector3f dir = new Vector3f(x, y, z - 100);
    float limit = pos.distance(dir);
    dir.subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    collideWith(r, results);
    Vector3f terrain = null;
    if (terrainData.length == 1) {
      if (terrainData[0] != 0) {
        terrain = new Vector3f(x, y, terrainData[0] / 32f);
      }
    } else {
      terrain = terraionCollision(x, y, r);
    }
    if (terrain != null && terrain.z > 0 && terrain.z < z + 2) {
      CollisionResult result = new CollisionResult(terrain, Math.abs(z - terrain.z + 2));
      results.addCollision(result);
    }
    if (results.size() == 0) { // Use size() instead of isEmpty()
      return z;
    }
    return results.getClosestCollision().getContactPoint().z;
  }

  public Vector3f getClosestCollision(
      float x,
      float y,
      float z,
      float targetX,
      float targetY,
      float targetZ,
      boolean changeDirection,
      boolean fly,
      int instanceId,
      byte intentions) {
    float zChecked1 = 0;
    float zChecked2 = 0;
    if (!fly && changeDirection) {
      zChecked1 = z;
      z = getZ(x, y, z + 2, instanceId);
    }
    z += 1f;
    targetZ += 1f;
    Vector3f start = new Vector3f(x, y, z);
    Vector3f end = new Vector3f(targetX, targetY, targetZ);
    Vector3f pos = new Vector3f(x, y, z);
    Vector3f dir = new Vector3f(targetX, targetY, targetZ);

    CollisionResults results = new CollisionResults(intentions, false, instanceId);

    float limit = pos.distance(dir);
    dir.subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    Vector3f terrain = calculateTerrainCollision(start.x, start.y, start.z, end.x, end.y, end.z, r);
    if (terrain != null) {
      CollisionResult result = new CollisionResult(terrain, terrain.distance(pos));
      results.addCollision(result);
    }

    collideWith(r, results);

    float geoZ = 0;
    if (results.size() == 0) { // Use size() instead of isEmpty()
      if (fly) {
        return end;
      }
      if (zChecked1 > 0 && targetX == x && targetY == y && targetZ - 1f == zChecked1) {
        geoZ = z - 1f;
      } else {
        zChecked2 = targetZ;
        geoZ = getZ(targetX, targetY, targetZ + 2, instanceId);
      }
      if (Math.abs(geoZ - targetZ) < start.distance(end)) {
        return end.setZ(geoZ);
      }
      return start;
    }
    Vector3f contactPoint = results.getClosestCollision().getContactPoint();
    float distance = results.getClosestCollision().getDistance();
    if (distance < 1) {
      return start;
    }
    // -1m
    contactPoint = contactPoint.subtract(dir);
    if (!fly && changeDirection) {
      if (zChecked1 > 0 && contactPoint.x == x && contactPoint.y == y && contactPoint.z == zChecked1) {
        contactPoint.z = z - 1f;
      } else if (zChecked2 > 0 && contactPoint.x == targetX && contactPoint.y == targetY
          && contactPoint.z == zChecked2) {
        contactPoint.z = geoZ;
      } else {
        contactPoint.z = getZ(contactPoint.x, contactPoint.y, contactPoint.z + 2, instanceId);
      }
    }
    if (!fly && Math.abs(start.z - contactPoint.z) > distance) {
      return start;
    }

    return contactPoint;
  }

  public CollisionResults getCollisions(
      float x,
      float y,
      float z,
      float targetX,
      float targetY,
      float targetZ,
      boolean changeDirection,
      boolean fly,
      int instanceId,
      byte intentions) {
    if (!fly && changeDirection) {
      z = getZ(x, y, z + 2, instanceId);
    }
    z += 1f;
    targetZ += 1f;
    Vector3f start = new Vector3f(x, y, z);
    Vector3f end = new Vector3f(targetX, targetY, targetZ);
    Vector3f pos = new Vector3f(x, y, z);
    Vector3f dir = new Vector3f(targetX, targetY, targetZ).subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(pos.distance(end)); // Use distance for limit
    Vector3f terrain = calculateTerrainCollision(start.x, start.y, start.z, end.x, end.y, end.z, r);
    if (terrain != null) {
      CollisionResult result = new CollisionResult(terrain, terrain.distance(pos));
      CollisionResults results = new CollisionResults(intentions, false, instanceId);
      results.addCollision(result);
      return results;
    }
    CollisionResults results = new CollisionResults(intentions, false, instanceId);
    collideWith(r, results);
    return results;
  }

  /**
   * @param z
   * @param targetZ
   */
  private Vector3f calculateTerrainCollision(
      float x, float y, float z, float targetX, float targetY, float targetZ, Ray ray) {
    float x2 = targetX - x;
    float y2 = targetY - y;
    int intD = (int) Math.abs(ray.getLimit());

    for (float s = 0; s < intD; s += 2) {
      float tempX = x + (x2 * s / ray.getLimit());
      float tempY = y + (y2 * s / ray.getLimit());
      Vector3f result = terraionCollision(tempX, tempY, ray);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  public void setTerrainCutouts(int[] cutoutData) {
    if (cutoutData != null) {
      this.terrainCutoutData = cutoutData.clone();
      Arrays.sort(this.terrainCutoutData);
    } else {
      this.terrainCutoutData = null; // Handle null input
    }
  }

  private Vector3f terraionCollision(float x, float y, Ray ray) {
    y /= 2f;
    x /= 2f;
    int xInt = (int) x;
    int yInt = (int) y;

    float p1, p2, p3, p4;
    if (terrainData.length == 1) {
      p1 = p2 = p3 = p4 = terrainData[0] / 32f;
    } else {
      int size = (int) Math.sqrt(terrainData.length);
      if (GeoDataConfig.GEO_MONONO2_IN_USE) {
        int index = yInt + (xInt * size);
        if (index < 0 || index + size + 1 >= terrainData.length) {
          return null;
        }
        p1 = terrainData[index] / 32f;
        p2 = terrainData[index + 1] / 32f;
        p3 = terrainData[index + size] / 32f;
        p4 = terrainData[index + size + 1] / 32f;

        if (terrainCutoutData != null) {
          if (Arrays.binarySearch(terrainCutoutData, index) >= 0) {
            return null;
          }
        }
      } else {
        int index1 = yInt + (xInt * size);
        int index2 = (yInt + 1) + (xInt * size);
        int index3 = (yInt) + ((xInt + 1) * size);
        int index4 = (yInt + 1) + ((xInt + 1) * size);
        if (index1 < 0
            || index2 < 0
            || index3 < 0
            || index4 < 0
            || index1 >= terrainData.length
            || index2 >= terrainData.length
            || index3 >= terrainData.length
            || index4 >= terrainData.length) {
          return null;
        }
        p1 = terrainData[index1] / 32f;
        p2 = terrainData[index2] / 32f;
        p3 = terrainData[index3] / 32f;
        p4 = terrainData[index4] / 32f;
      }
    }
    Vector3f result = new Vector3f();
    if (p1 >= 0 && p2 >= 0 && p3 >= 0) {
      Triangle tringle1 =
          new Triangle(
              new Vector3f(xInt * 2, yInt * 2, p1),
              new Vector3f(xInt * 2, (yInt + 1) * 2, p2),
              new Vector3f((xInt + 1) * 2, yInt * 2, p3));
      if (ray.intersectWhere(tringle1, result)) {
        return result;
      }
    }
    if (p4 >= 0 && p2 >= 0 && p3 >= 0) {
      Triangle tringle2 =
          new Triangle(
              new Vector3f((xInt + 1) * 2, (yInt + 1) * 2, p4),
              new Vector3f(xInt * 2, (yInt + 1) * 2, p2),
              new Vector3f((xInt + 1) * 2, yInt * 2, p3));
      if (ray.intersectWhere(tringle2, result)) {
        return result;
      }
    }
    return null;
  }

  public boolean canSee(
      float x,
      float y,
      float z,
      float targetX,
      float targetY,
      float targetZ,
      float limit,
      int instanceId) {
    targetZ += 1;
    z += 1;
    // Another fix can see in instances
    // if (getZ(targetX, targetY) > targetZ)
    // return false;

    float distance = calculateDistance(x, y, targetX, targetY);
    if (distance > 80f) {
      return false;
    }
    int intD = (int) distance; // Corrected to int

    Vector3f pos = new Vector3f(x, y, z);
    Vector3f dir = new Vector3f(targetX, targetY, targetZ).subtractLocal(pos).normalizeLocal();
    Ray r = new Ray(pos, dir);
    r.setLimit(limit);
    for (int s = 2; s < intD; s += 2) { // Changed from float to int and s++
      float tempX = targetX + ((x - targetX) * s / distance);
      float tempY = targetY + ((y - targetY) * s / distance);
      Vector3f result = terraionCollision(tempX, tempY, r);
      if (result != null) {
        return false;
      }
    }
    CollisionResults results =
        new CollisionResults(
            (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId()),
            false,
            instanceId);
    int collisions = this.collideWith(r, results);
    return (results.size() == 0 && collisions == 0);
  }

  @Override
  public void updateModelBound() {
    if (getChildren() != null) {
      Iterator<Spatial> i = getChildren().iterator();
      while (i.hasNext()) {
        Spatial s = i.next();
        if (s instanceof Node && ((Node) s).getChildren().isEmpty()) {
          i.remove();
        }
      }
    }
    super.updateModelBound();
  }
}