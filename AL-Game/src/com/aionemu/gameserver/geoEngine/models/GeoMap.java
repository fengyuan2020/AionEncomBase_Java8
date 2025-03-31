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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import javolution.util.FastMap;

/**
 * @author Mr. Poke
 */
import java.lang.ThreadLocal;

public class GeoMap extends Node {
    // 对象池优化
    private final ThreadLocal<Vector3f> tempVector1 = ThreadLocal.withInitial(Vector3f::new);
    private final ThreadLocal<Vector3f> tempVector2 = ThreadLocal.withInitial(Vector3f::new);
    private final ThreadLocal<Vector3f> tempVector3 = ThreadLocal.withInitial(Vector3f::new);
    private final ThreadLocal<Triangle> tempTriangle = ThreadLocal.withInitial(Triangle::new);
    
    // 碰撞结果缓存优化
    private final ThreadLocal<CollisionResults> physicalResults = ThreadLocal.withInitial(
        () -> new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1));
    private final ThreadLocal<CollisionResults> doorResults = ThreadLocal.withInitial(
        () -> new CollisionResults(CollisionIntention.DOOR.getId(), false, 1));
    private final ThreadLocal<CollisionResults> mixedResults = ThreadLocal.withInitial(
        () -> new CollisionResults((byte)(CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId()), false, 1));	

	private short[] terrainData;
	private List<BoundingBox> tmpBox = new ArrayList<BoundingBox>();
	private Map<String, DoorGeometry> doors = new FastMap<String, DoorGeometry>();

	// 地形数据预处理优化
	private float[] preprocessedTerrainData;
		
	public void setTerrainData(short[] terrainData) {
		this.terrainData = terrainData;
		// 预处理地形数据
		if (terrainData.length > 1) {
			preprocessedTerrainData = new float[terrainData.length];
			for (int i = 0; i < terrainData.length; i++) {
				preprocessedTerrainData[i] = terrainData[i] / 32f;
			}
		}
	}

	/**
	 * @param name
	 */
	public GeoMap(String name, int worldSize) {
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
		for (Entry<String, DoorGeometry> door : doors.entrySet()) {
			if (!(door.getKey().startsWith(Integer.toString(worldId)) && door.getKey().endsWith(mesh))) {
				continue;
			}
			DoorGeometry checkDoor = doors.get(door.getKey());
			float doorDistance = checkDoor.getWorldBound().distanceTo(templatePoint);
			if (distance > doorDistance) {
				distance = doorDistance;
				foundDoor = checkDoor;
			}
			if (checkDoor.getWorldBound().intersects(templatePoint)) {
				foundDoor = checkDoor;
				break;
			}
		}
		if (foundDoor == null) {
			return null;
		}
		foundDoor.setFoundTemplate(true);
		return foundDoor.getName();
	}

	@ Deprecated
	public boolean canPassWalker(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		// 复用Vector3f对象
		Vector3f pos = tempVector1.get().set(x, y, z);
		Vector3f dir = tempVector2.get().set(targetX, targetY, targetZ);
		
		// 使用距离平方比较优化
		float dx = x - targetX;
		float dy = y - targetY;
		float distSq = dx * dx + dy * dy;
		if (distSq > 2500f) { // 50的平方
			return false;
		}
		
		// 复用CollisionResults对象
		CollisionResults results = physicalResults.get();
		results.clear();
		results.setInstanceId(instanceId);
		
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		
		int collisions = this.collideWith(r, results);
		return results.size() == 0 && collisions == 0;
	}

	public boolean canPass(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		// 复用Vector3f对象
		Vector3f pos = tempVector1.get().set(x, y, z);
		Vector3f dir = tempVector2.get().set(targetX, targetY, targetZ);
		
		// 使用距离平方比较优化
		float dx = x - targetX;
		float dy = y - targetY;
		float distSq = dx * dx + dy * dy;
		if (distSq > 4225f) { // 65的平方
		return false;
		}
		
		// 复用CollisionResults对象
		CollisionResults results = physicalResults.get();
		results.clear();
		results.setInstanceId(instanceId);
		
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		
		int collisions = this.collideWith(r, results);
		return results.size() == 0 && collisions == 0;
	}

	@Deprecated
	public float getZW(float x, float y) {
		return getZ(x, y); // 直接调用优化后的方法
	}

	@Deprecated
	public float getZW(float x, float y, float z, int instanceId) {
		return getZ(x, y, z, instanceId); // 直接调用优化后的方法
	}

	public void setDoorState(int instanceId, String name, boolean isOpened) {
		DoorGeometry door = doors.get(name);
		if (door != null) {
			door.setDoorState(instanceId, isOpened);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * aionjHungary.geoEngine.scene.Node#attachChild(aionjHungary.geoEngine.scene.
	 * Spatial)
	 */
	@Override
	public int attachChild(Spatial child) {
		int i = 0;

		if (child instanceof DoorGeometry) {
			doors.put(child.getName(), (DoorGeometry) child);
		}

		for (Spatial spatial : getChildren()) {
			if (tmpBox.get(i).intersects(child.getWorldBound())) {
				((Node) spatial).attachChild(child);
			}
			i++;
		}
		return 0;
	}

	public float getZ(float x, float y) {
	    // 复用CollisionResults对象
	    CollisionResults results = physicalResults.get();
	    results.clear();
	    results.setInstanceId(1);
	    
	    Vector3f pos = tempVector1.get().set(x, y, 4000);
	    Vector3f dir = tempVector2.get().set(x, y, 0);
	    float limit = pos.distance(dir);
	    dir.subtractLocal(pos).normalizeLocal();
	    Ray r = new Ray(pos, dir);
	    r.setLimit(limit);
	    
	    collideWith(r, results);
	    Vector3f terrain = null;
	    if (terrainData.length == 1) {
	        terrain = tempVector3.get().set(x, y, terrainData[0] / 32f);
	    } else {
	        terrain = terrainCollision(x, y, r); 
	    }
	    if (terrain != null) {
	        results.addCollision(new CollisionResult(terrain, Math.max(0, Math.max(4000 - terrain.z, terrain.z))));
	    }
	    return results.size() == 0 ? 0 : results.getClosestCollision().getContactPoint().z;
	}

	public float getZ(float x, float y, float z, int instanceId) {
		// 复用CollisionResults对象
		CollisionResults results = physicalResults.get();
		results.clear();
		results.setInstanceId(instanceId);
		
		// 复用Vector3f对象
		Vector3f pos = tempVector1.get().set(x, y, z + 2);
		Vector3f dir = tempVector2.get().set(x, y, z - 100);
		
		float limit = pos.distance(dir);
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		
		collideWith(r, results);
		
		Vector3f terrain = null;
		if (terrainData.length == 1) {
			if (terrainData[0] != 0) {
				terrain = tempVector3.get().set(x, y, terrainData[0] / 32f);
			}
		} else {
			terrain = terrainCollision(x, y, r);
		}
		
		if (terrain != null && terrain.z > 0 && terrain.z < z + 2) {
			results.addCollision(new CollisionResult(terrain, Math.abs(z - terrain.z + 2)));
		}
		
		return results.size() == 0 ? z : results.getClosestCollision().getContactPoint().z;
	}

	public Vector3f getClosestCollision(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
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

		Float limit = pos.distance(dir);
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
		if (results.size() == 0) {
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

	public CollisionResults getCollisions(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
		if (!fly && changeDirection) {
			z = getZ(x, y, z + 2, instanceId);
		}
		z += 1f;
		targetZ += 1f;
		Vector3f start = new Vector3f(x, y, z);
		Vector3f end = new Vector3f(targetX, targetY, targetZ);
		Vector3f pos = new Vector3f(x, y, z);
		Vector3f dir = new Vector3f(targetX, targetY, targetZ);

		CollisionResults results = new CollisionResults(intentions, false, instanceId);

		Float limit = pos.distance(dir);
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		Vector3f terrain = calculateTerrainCollision(start.x, start.y, start.z, end.x, end.y, end.z, r);
		if (terrain != null) {
			CollisionResult result = new CollisionResult(terrain, terrain.distance(pos));
			results.addCollision(result);
		}
		collideWith(r, results);
		return results;
	}

	/**
	 * @param z
	 * @param targetZ
	 */
	private Vector3f calculateTerrainCollision(float x, float y, float z, float targetX, float targetY, float targetZ,
			Ray ray) {

		float x2 = targetX - x;
		float y2 = targetY - y;
		int intD = (int) Math.abs(ray.getLimit());

		for (float s = 0; s < intD; s += 2) {
			float tempX = x + (x2 * s / ray.getLimit());
			float tempY = y + (y2 * s / ray.getLimit());
			Vector3f result = terrainCollision(tempX, tempY, ray);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private int[] terrainCutoutData;

	public void setTerrainCutouts(int[] cutoutData) {
		int[] arr = cutoutData.clone();
		Arrays.sort(arr);
		this.terrainCutoutData = arr;
	}

	// 优化后的terrainCollision方法
    private Vector3f terrainCollision(float x, float y, Ray ray) {
        y /= 2f; x /= 2f;
        int xInt = (int)x, yInt = (int)y;
        
        // 增强边界检查
        int size = (int)Math.sqrt(terrainData.length);
        if (xInt < 0 || yInt < 0 || xInt >= size-1 || yInt >= size-1) {
            return null;
        }

        float p1, p2, p3, p4;
        if (terrainData.length == 1) {
            p1 = p2 = p3 = p4 = preprocessedTerrainData != null ? 
                 preprocessedTerrainData[0] : terrainData[0]/32f;
        } else {
            try {
                int index = yInt + xInt * size;
                // 使用预处理数据
                if (preprocessedTerrainData != null) {
                    p1 = preprocessedTerrainData[index];
                    p2 = preprocessedTerrainData[index+1];
                    p3 = preprocessedTerrainData[index+size];
                    p4 = preprocessedTerrainData[index+size+1];
                } else {
                    p1 = terrainData[index]/32f;
                    p2 = terrainData[index+1]/32f;
                    p3 = terrainData[index+size]/32f;
                    p4 = terrainData[index+size+1]/32f;
                }
                
                // 地形切割检查
                if (terrainCutoutData != null && 
                    Arrays.binarySearch(terrainCutoutData, index) >= 0) {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        // 复用对象
        Vector3f result = tempVector3.get();
        Triangle tri = tempTriangle.get();
        
        // 三角形碰撞检测
        if (p1 >= 0 && p2 >= 0 && p3 >= 0) {
            tri.set(new Vector3f(xInt*2, yInt*2, p1),
                   new Vector3f(xInt*2, (yInt+1)*2, p2),
                   new Vector3f((xInt+1)*2, yInt*2, p3));
            if (ray.intersectWhere(tri, result)) {
                return result;
            }
        }
        if (p4 >= 0 && p2 >= 0 && p3 >= 0) {
            tri.set(new Vector3f((xInt+1)*2, (yInt+1)*2, p4),
                   new Vector3f(xInt*2, (yInt+1)*2, p2),
                   new Vector3f((xInt+1)*2, yInt*2, p3));
            if (ray.intersectWhere(tri, result)) {
                return result;
            }
        }
        return null;
    }

	// 优化后的canSee方法
    public boolean canSee(float x, float y, float z, float targetX, float targetY, float targetZ, 
                         float limit, int instanceId) {
        // 复用对象
        Vector3f pos = tempVector1.get().set(x, y, z + 1);
        Vector3f dir = tempVector2.get().set(targetX, targetY, targetZ + 1);
        
        // 距离平方优化
        float dx = x - targetX;
        float dy = y - targetY;
        float distSq = dx * dx + dy * dy;
        if (distSq > 6400f) { // 80*80
            return false;
        }

        // 动态步长检测
        float distance = (float) Math.sqrt(distSq);
        dir.subtractLocal(pos).normalizeLocal();
        Ray r = new Ray(pos, dir);
        r.setLimit(limit);

        float step = Math.max(2f, distance / 40f);
        for (float s = step; s < distance; s += step) {
            float ratio = s / distance;
            if (terrainCollision(
                targetX + (dx * ratio), 
                targetY + (dy * ratio), 
                r) != null) {
                return false;
            }
        }

        // 复用碰撞结果
        CollisionResults results = mixedResults.get();
        results.clear();
        results.setInstanceId(instanceId);
        
        return collideWith(r, results) == 0 && results.size() == 0;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see aionjHungary.geoEngine.scene.Spatial#updateModelBound()
	 */
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
