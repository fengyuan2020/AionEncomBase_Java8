/**
 * This file is part of the Aion Reconstruction Project Server.
 *
 * The Aion Reconstruction Project Server is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Aion Reconstruction Project Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with the Aion Reconstruction Project Server. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @AionReconstructionProjectTeam
 */
package com.aionemu.gameserver.world.geo.nav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.scene.NavGeometry;
import com.aionemu.gameserver.world.geo.nav.NavService.NavPathway;

/**
 * Implements a pathfinding algorithm similar to A* to traverse through {@link NavGeometry}.
 *
 * @author Yon (Aion Reconstruction Project)
 * Modify: MATTY
 */
class NavHelper {

    /**
     * The {@link Logger} for this class. This is only ever written to if
     * {@link #retrace(NavHeapNode)} gives up early (for debugging purposes).
     */
    private static final Logger log = LoggerFactory.getLogger(NavHelper.class);

    /**
     * A value used when attempting to pathfind to a target that is not on the Nav Mesh.
     * <p>
     * If the estimated distance of the target is within this value, then the path will be
     * considered complete; from there, a straight line path will be used to finish traversal.
     * <p>
     * If this value is too large, then entities that are traversing to targets that are not on
     * the Nav Mesh may clip through walls or other geometry in strange ways.
     */
    public final static float ARBITRARY_SMALL_VALUE = GeoDataConfig.GEO_NAV_ARBITRARY_SMALL_VALUE;

    /**
     * A value used when retracing or opening the list of nodes to create a pathway corridor.
     * <p>
     * This value limits how many line segments can be stored in a single list. If this
     * value is exceeded, pathfinding attempts are considered to have failed.
     * <p>
     * If this value is too small, pathfinding will fail over long path distances. If this
     * value is too large, then the JVM can consume all of its memory trying to store the corridor.
     * <p>
     * This value is mainly to prevent an issue with one of the pathfinding algorithm's assumptions
     * that corrupts the data structure and forces an infinite loop, but is also used as a limit
     * of operations while pathing.
     */
    public final static int ARBITRARY_LARGE_VALUE = GeoDataConfig.GEO_NAV_ARBITRARY_LARGE_VALUE;

    /**
     * A percentage of pathCost to add onto the basic path cost calculation
     * if the next node is moving away from the target node.
     * <p>
     * A node is considered to be in a direction moving away from the target if a vector towards
     * the target from the vertex opposite the edge the path passes through does not pass through
     * said edge. See {@link NavGeometry#isTowardsEdge(byte, float[])}.
     */
    public final static float PATH_WEIGHT = GeoDataConfig.GEO_NAV_PATH_WEIGHT;

    /**
     * A multiplier for {@link NavHeapNode#targetDist}. When the target distance is estimated,
     * it will be multiplied by this value. This is to give nodes that are closer to the target
     * a higher priority than nodes that are further away.
     */
    public final static float TARGET_WEIGHT = GeoDataConfig.GEO_NAV_TARGET_WEIGHT;

    /**
     * Executor service for asynchronous cleanup of NavHelper objects.
     */
    private static final ExecutorService cleanupExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("NavHelper-Cleanup-Thread");
                thread.setDaemon(true); // Allow JVM to exit even if this thread is running
                return thread;
            });

    /**
     * Threshold for comparing float values.
     */
    private static final float FLOAT_COMPARISON_EPSILON = 0.0001f;

    /**
     * Object pool for reusing `NavHelper` instances.
     */
    private static final ObjectPool<NavHelper> navHelperPool;

    static {
        GenericObjectPoolConfig<NavHelper> poolConfig = new GenericObjectPoolConfig<>();
        // Adjust pool configuration as needed. For example:
        poolConfig.setMaxTotal(100); // Maximum number of instances in the pool
        poolConfig.setMinIdle(10);    // Minimum number of idle instances
        poolConfig.setMaxIdle(50);    // Maximum number of idle instances
        poolConfig.setTestOnBorrow(true); // Validate objects on borrow (optional)
        poolConfig.setTestOnReturn(true); // Validate objects on return (optional)
        poolConfig.setBlockWhenExhausted(true); // Block when the pool is exhausted
        poolConfig.setLifo(false); // Use FIFO instead of LIFO.

        navHelperPool = new GenericObjectPool<>(new NavHelperFactory(), poolConfig);
    }

    /**
     * Factory class for creating `NavHelper` instances for the object pool.
     */
	private static class NavHelperFactory extends org.apache.commons.pool2.BasePooledObjectFactory<NavHelper> {

		@Override
		public NavHelper create() {
			return new NavHelper(); // Use the default constructor
		}

		@Override
		public org.apache.commons.pool2.PooledObject<NavHelper> wrap(NavHelper obj) {
			return new org.apache.commons.pool2.impl.DefaultPooledObject<>(obj);
		}

		@Override
		public void destroyObject(org.apache.commons.pool2.PooledObject<NavHelper> p) {
			p.getObject().destroy();  // Call the destroy method to release resources
		}

		@Override
		public boolean validateObject(org.apache.commons.pool2.PooledObject<NavHelper> p) {
			return p.getObject().isValid(); // Implement validation logic
		}
	}


    /**
     * A node designed to be stored in an array treated like a heap data structure.
     * The heap structure should place nodes based on the {@link #compareTo(NavHeapNode)}
     * method, with the lowest values at the top of the heap.
     *
     * @author Yon (Aion Reconstruction Project)
     */
    private class NavHeapNode implements Comparable<NavHeapNode> {

        /**
         * If true, this node has been explored and opened by the pathfinding algorithm.
         */
        boolean open = false;

        /**
         * The {@link NavGeometry} this node represents.
         */
        NavGeometry tile;

        /**
         * The {@link NavHeapNode node} with the shortest {@link #pathCost} that connects to this node.
         */
        NavHeapNode parent;

        /**
         * A lookup value for the heap this node is stored within.
         */
        int heapIndex;

        /**
         * A value used to compare this node to other nodes. After a summation with another value,
         * the summed value determines the priority of this node within the heap.
         */
        float pathCost, targetDist;

        /**
         * Basic constructor. Only used by the initial starting node of the path.
         *
         * @param node -- The {@link NavGeometry} this node represents.
         */
        NavHeapNode(NavGeometry node) {
            this.tile = node;
            if (tile == endTile) {
                targetDist = 0;
            } else {
                targetDist = node.getPriority(x2, y2, z2) * TARGET_WEIGHT;
            }
        }

        /**
         * Constructor. This node is created with the given parent node (which cannot be null),
         * and estimates its {@link #pathCost} based on said parent node. The {@link #targetDist}
         * is also estimated.
         *
         * @param node -- The {@link NavGeometry} this node represents.
         * @param parent -- The {@link NavHeapNode} that opened onto this node.
         * @param useWeight -- If true, the {@link #pathCost} will have an extra percentage added onto it
         * (see {@link NavHelper#PATH_WEIGHT PATH WEIGHT}).
         */
        NavHeapNode(NavGeometry node, NavHeapNode parent, boolean useWeight) {
            this(node);
            this.parent = parent;
            float basePriority = parent.pathCost + parent.tile.getInRad();
            if (useWeight) {
                pathCost = basePriority + basePriority * PATH_WEIGHT;
            } else {
                pathCost = basePriority;
            }
        }

        /**
         * Considers the passed in node and accepts it as the new {@link #parent} if
         * it has a lower {@link #pathCost}. If the new parent is accepted, then the
         * path cost of this node is updated.
         *
         * @param newParent -- The node to consider as a new parent.
         * @param useWeight -- If {@link NavHelper#PATH_WEIGHT} should be applied to
         * the new path cost if the new parent is accepted.
         */
        void checkAndUpdateParent(NavHeapNode newParent, boolean useWeight) {
            if (newParent == null) {
                log.warn("New parent is null, this should not happen.");
                return;
            }
            if (parent == null) return;
            if (newParent.parent == this) return;
            if (parent.pathCost > newParent.pathCost) {
                parent = newParent;
                pathCost = parent.pathCost + parent.tile.getInRad();
                if (useWeight) {
                    pathCost += pathCost * PATH_WEIGHT;
                }
                onUpdateNode(this);
            }
        }

        /**
         * Sums {@link #pathCost} and {@link #targetDist} and returns the result.
         * This value represents the overall priority of this node. Lower values
         * are of a higher priority, and the position on the heap should take this
         * value into consideration above all others. See {@link #compareTo(NavHeapNode)}.
         *
         * @return the summation of {@link #pathCost} and {@link #targetDist}.
         */
        float getPriority() {
            return pathCost + targetDist;
        }

        /**
         * Opens this node by examining what the edges of {@link #tile} connect to. If the
         * edges do not connect to anything, they are skipped. If the edge connection exists,
         * and has yet to be explored, a new node is created to represent it and added to the heap.
         * If the edge connection exists, and has already been explored, this node will attempt to
         * become the new parent of the existing node by calling {@link #checkAndUpdateParent(NavHeapNode, boolean)}.
         * <p>
         * This operation sets {@link #open} to true.
         */
        void open() {
            if (open) return;
            open = true;

            //Check connections to see if they are part of the heap
            float[] vec = {x2, y2};

            //This commented code made things worse.
            //if (parent != null) {
            //	vec = new float[] {x2 - parent.tile.incenter[0], y2 - parent.tile.incenter[1]};
            //} else {
            //	vec = new float[] {x2 - x1, y2 - y1};
            //}
            if (tile.getEdge1() != null) {
                processEdge(tile.getEdge1(), (byte) 1, vec);
            }

            if (tile.getEdge2() != null) {
                processEdge(tile.getEdge2(), (byte) 2, vec);
            }

            if (tile.getEdge3() != null) {
                processEdge(tile.getEdge3(), (byte) 3, vec);
            }
        }

        /**
         * Processes an edge of the current tile.
         *
         * @param edge The NavGeometry representing the edge.
         * @param edgeNumber The edge number (1, 2, or 3).
         * @param vec The direction vector.
         */
        private void processEdge(NavGeometry edge, byte edgeNumber, float[] vec) {
            if (!contains(edge)) {
                //If they aren't, then create and add them
                NavHeapNode newNode = new NavHeapNode(edge, this, !tile.isTowardsEdge(edgeNumber, vec));
                add(newNode);
            } else {
                //If they are, run checkAndUpdateParent
                NavHeapNode child = getNode(edge);
                if (child != parent) {
                    child.checkAndUpdateParent(this, !tile.isTowardsEdge(edgeNumber, vec));
                }
            }
        }

        /**
         * Considers the priority of this node compared to the other. If this node has a higher
         * priority (lower value from {@link #getPriority()}) then -1 is returned. If this node
         * has a lower priority, then 1 is returned. If the overall priority is equal to the
         * given node, {@link #targetDist} is used as a tie-breaker (lower values are higher priority).
         * If both the overall priority and the target distance are equal, 0 is returned.
         * <p>
         * Note: this class has a natural ordering that is inconsistent with equals.
         */
        @Override
        public int compareTo(NavHeapNode other) {
            float pThis = getPriority();
            float pOther = other.getPriority();

            if (Math.abs(pThis - pOther) > FLOAT_COMPARISON_EPSILON) {
                return Float.compare(pThis, pOther);
            }

            return Float.compare(targetDist, other.targetDist);
        }
    }

    /**
     * Starting coordinate component
     */
    float x1, y1, z1;

    /**
     * Target coordinate component
     */
    float x2, y2, z2;

    /**
     * The target {@link NavGeometry} to find a path to.
     */
    NavGeometry endTile;

    /**
     * A list of {@link NavGeometry} that has been explored and stored within a {@link NavHeapNode}.
     * <p>
     * This list is used to determine if a Nav Mesh node has already been explored after it's been
     * removed from the heap.
     */
    private final Map<NavGeometry, NavHeapNode> exploredNodes;

    /**
     * A simple array that is treated as the underlying structure of a heap. The {@link NavHelper} class maintains
     * this heap, and enforces the ordering within.
     * <p>
     * This array is expanded as needed. Due to the size being larger than the contents, {@link #currentHeapCount}
     * is used to track how many indices of this array are relevant.
     */
    private NavHeapNode[] heap;

    /**
     * The current number of items being stored on the {@link #heap}.
     */
    private int currentHeapCount = 0;

    private final AtomicInteger instanceId = new AtomicInteger(0);

    /**
     * Private default constructor, used by the object pool.
     */
    private NavHelper() {
        instanceId.incrementAndGet();
        heap = new NavHeapNode[100];
        exploredNodes = new HashMap<>();
        this.x1 = 0; // Initialize to default values
        this.y1 = 0;
        this.z1 = 0;
        this.x2 = 0;
        this.y2 = 0;
        this.z2 = 0;
    }

    /**
     * Creates a new {@link NavHelper} that is ready to {@link #createPathway() construct a path}
     * from the given starting point to the given end point.
     * <p>
     * Callers should run {@link #destroy()} when they are done with this object.
     *
     * @param startTile -- The {@link NavGeometry} this should create a path from. Cannot be null.
     * @param endTile -- The {@link NavGeometry} this should pathfind to. Can be null.
     * @param x1 -- The x-component of the starting position.
     * @param y1 -- The y-component of the starting position.
     * @param z1 -- The z-component of the starting position.
     * @param x2 -- The x-component of the end position.
     * @param y2 -- The y-component of the end position.
     * @param z2 -- The z-component of the end position.
     */
    private NavHelper(NavGeometry startTile, NavGeometry endTile, float x1, float y1, float z1, float x2, float y2, float z2) {
        this(); // Call the default constructor
        assert startTile != null;
        this.endTile = endTile;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        init(startTile);
    }

    /**
     * Creates the first node to be added to the {@link #heap} from the starting {@link NavGeometry},
     * and adds it to the heap.
     *
     * @param tile -- The starting {@link NavGeometry} for the path to be generated.
     */
    private void init(NavGeometry tile) {
        NavHeapNode startNode = new NavHeapNode(tile);
        add(startNode);
    }

    /**
     * Asynchronously releases resources held by this NavHelper.
     *
     * This method submits a task to a background executor to clear the
     * references to parent nodes in the NavHeapNodes and clear the list. This
     * helps prevent memory leaks, especially when many NavHelper objects are
     * created and destroyed.
     */
    public void destroy() {
        cleanupExecutor.submit(() -> {
            try {
                for (NavHeapNode node : exploredNodes.values()) {
                    node.parent = null;
                    node.tile = null; // Release the NavGeometry reference
                }
                exploredNodes.clear();
                clearHeap();
                endTile = null;
            } catch (Exception e) {
                log.error("Exception while destroying NavHelper: ", e);
            }
        });
    }

    private void clearHeap() {
        if (heap != null) {
            for (int i = 0; i < currentHeapCount; i++) {
                heap[i] = null; // Help GC by nulling references
            }
        }
        currentHeapCount = 0;
    }


    /**
     * Creates a list of {@link NavHeapNode NavHeapNodes} that connect the starting
     * node to the ending node or destination. The list is passed to {@link #retrace(NavHeapNode)},
     * returning the result.
     * <p>
     * If this method considers more than an {@link #ARBITRARY_LARGE_VALUE} number of nodes, it gives up,
     * returning an empty pathway.
     *
     * @return A {@link NavPathway} that represents a corridor to path through.
     */
    public NavPathway[] createPathway() {
        boolean finished = false;
        NavHeapNode current = null;
        short opCount = 0;

        while (currentHeapCount > 0 && opCount++ <= ARBITRARY_LARGE_VALUE) {
            current = removeFirst();

            if (endTile == null) {
                if (current.targetDist < ARBITRARY_SMALL_VALUE * TARGET_WEIGHT) {
                    finished = true;
                    break;
                }
            } else {
                if (current.tile == endTile) {
                    finished = true;
                    break;
                }
            }

            current.open();
        }

        if (finished && current != null) {
            return retrace(current);
        }

        return new NavPathway[0];
    }

    /**
     * Attempts to backtrack through the given {@link NavHeapNode node's} parent references until
     * the starting point is discovered. If more than an {@link #ARBITRARY_LARGE_VALUE} number of nodes
     * is considered, this method will give up and return an empty pathway.
     *
     * @param node -- The {@link NavHeapNode} representing the final {@link NavGeometry} on the path.
     * @return The {@link NavPathway} that represents the corridor of the found path.
     */
    private NavPathway[] retrace(NavHeapNode node) {
        ArrayList<NavPathway> ret = new ArrayList<>();
        NavHeapNode child = node;
        NavHeapNode parent = node;
        int retraceCount = 0;

        while (parent.parent != null && retraceCount++ <= ARBITRARY_LARGE_VALUE) {
            parent = parent.parent;

            if (parent.parent == child) {
                log.error("Retracing path: Parent of parent node is child node! Infinite Loop!  x1: {}, y1: {}, z1: {}, x2: {}, y2: {}, z2: {}", x1, y1, z1, x2, y2, z2);
                return new NavPathway[0];
            }

            byte edge = 0;
            if (parent.tile.getEdge1() == child.tile) {
                edge = 1;
            } else if (parent.tile.getEdge2() == child.tile) {
                edge = 2;
            } else if (parent.tile.getEdge3() == child.tile) {
                edge = 3;
            } else {
                log.error("Retracing path: Child tile is not an edge of the parent tile. x1: {}, y1: {}, z1: {}, x2: {}, y2: {}, z2: {}", x1, y1, z1, x2, y2, z2);
                return new NavPathway[0];
            }

            ret.add(new NavPathway(parent.tile, edge));
            child = parent;
        }

        if (retraceCount > ARBITRARY_LARGE_VALUE) {
            log.error("Retracing path produced too many portals: x1: {}, y1: {}, z1: {}, x2: {}, y2: {}, z2: {}", x1, y1, z1, x2, y2, z2);
            return new NavPathway[0];
        }

        return ret.toArray(new NavPathway[0]);
    }

    /**
     * Adds the given {@link NavHeapNode} to the heap, and enforces a new heap order as needed.
     *
     * @param node -- The {@link NavHeapNode} to be added to the heap.
     */
    private void add(NavHeapNode node) {
        if (node == null) {
            log.warn("Attempted to add a null node to the heap.");
            return;
        }
        exploredNodes.put(node.tile, node);
        ensureHeapCapacity();
        node.heapIndex = currentHeapCount;
        heap[currentHeapCount++] = node;
        sortUp(node);
    }

    /**
     * Ensures that the heap array has enough capacity to hold new elements.
     */
    private void ensureHeapCapacity() {
        if (currentHeapCount == heap.length) {
            NavHeapNode[] tempHeap = new NavHeapNode[heap.length + 50];
            System.arraycopy(heap, 0, tempHeap, 0, currentHeapCount);
            heap = tempHeap;
        }
    }

    /**
     * Checks if this {@link NavHelper} has considered the given {@link NavGeometry} yet, and returns
     * true if it has. False otherwise.
     *
     * @param tile -- the {@link NavGeometry} to check for.
     * @return True if this NavHelper has encountered the given {@link NavGeometry}, false otherwise.
     */
    private boolean contains(NavGeometry tile) {
        return exploredNodes.containsKey(tile);
    }

    /**
     * Returns the {@link NavHeapNode} that represents the given {@link NavGeometry} as specified
     * by the {@link Map#get(Object) get()} method for {@link #exploredNodes}.
     *
     * @param tile -- the {@link NavGeometry} that the retrieved {@link NavHeapNode} represents.
     * @return The {@link NavHeapNode} representing the given {@link NavGeometry}.
     */
    private NavHeapNode getNode(NavGeometry tile) {
        return exploredNodes.get(tile);
    }

    /**
     * Removes the highest priority {@link NavHeapNode} from the {@link #heap}, and returns it.
     * Before returning, the heap is rearranged to maintain the correct order.
     *
     * @return The highest priority {@link NavHeapNode} stored within the {@link #heap}.
     */
    private NavHeapNode removeFirst() {
        if (currentHeapCount == 0) return null;

        NavHeapNode ret = heap[0];
        exploredNodes.remove(ret.tile);  // Remove from the list as it's no longer in the heap

        currentHeapCount--;
        if (currentHeapCount > 0) {
            heap[0] = heap[currentHeapCount];
            heap[0].heapIndex = 0;
            sortDown(heap[0]);
        }
        return ret;
    }

    /**
     * Verifies that the given {@link NavHeapNode} is in the correct position on the {@link #heap}
     * after its values have been adjusted.
     *
     * @param node -- the {@link NavHeapNode} to verify the position of.
     */
    private void onUpdateNode(NavHeapNode node) {
        if (node.open) return;
        sortUp(node);
        sortDown(node); // Unneeded for this application, but kept for completeness
    }

    /**
     * Enforces the correct position of the given {@link NavHeapNode} within the heap.
     * This method only verifies that the node should not be further down the {@link #heap}.
     *
     * @param node -- The {@link NavHeapNode} to validate the position of.
     */
    private void sortDown(NavHeapNode node) {
        int currentIndex = node.heapIndex;
        while (true) {
            int ciLeft = currentIndex * 2 + 1;
            int ciRight = currentIndex * 2 + 2;
            int swapIndex = currentIndex;

            if (ciLeft < currentHeapCount) {
                if (heap[ciLeft].compareTo(heap[swapIndex]) < 0) {
                    swapIndex = ciLeft;
                }
                if (ciRight < currentHeapCount && heap[ciRight].compareTo(heap[swapIndex]) < 0) {
                    swapIndex = ciRight;
                }

                if (swapIndex != currentIndex) {
                    swap(heap[currentIndex], heap[swapIndex]);
                    currentIndex = swapIndex;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * Enforces the correct position of the given {@link NavHeapNode} within the heap.
     * This method only verifies that the node should not be further up the {@link #heap}.
     *
     * @param node -- The {@link NavHeapNode} to validate the position of.
     */
    private void sortUp(NavHeapNode node) {
        int currentIndex = node.heapIndex;
        while (currentIndex > 0) {
            int pi = (currentIndex - 1) / 2;
            if (heap[pi].compareTo(node) > 0) {
                swap(heap[pi], node);
                currentIndex = node.heapIndex;  // Update current index to the new position
            } else {
                break;
            }
        }
    }

    /**
     * Swaps the position of the two given {@link NavHeapNode nodes} within the {@link #heap}.
     *
     * @param node1
     * @param node2
     */
    private void swap(NavHeapNode node1, NavHeapNode node2) {
        heap[node1.heapIndex] = node2;
        heap[node2.heapIndex] = node1;
        int heapIndex1 = node1.heapIndex;
        node1.heapIndex = node2.heapIndex;
        node2.heapIndex = heapIndex1;
    }

    /**
     * Retrieves a `NavHelper` instance from the object pool.
     *
     * @param startTile The starting NavGeometry.
     * @param endTile The target NavGeometry.
     * @param x1 The starting X coordinate.
     * @param y1 The starting Y coordinate.
     * @param z1 The starting Z coordinate.
     * @param x2 The target X coordinate.
     * @param y2 The target Y coordinate.
     * @param z2 The target Z coordinate.
     * @return A `NavHelper` instance.
     * @throws Exception If an error occurs while obtaining the object from the pool.
     */
    public static NavHelper borrowObject(NavGeometry startTile, NavGeometry endTile, float x1, float y1, float z1, float x2, float y2, float z2) throws Exception {
        NavHelper helper = navHelperPool.borrowObject();
        // Initialize the borrowed object with the parameters
        helper.endTile = endTile;
        helper.x1 = x1;
        helper.y1 = y1;
        helper.z1 = z1;
        helper.x2 = x2;
        helper.y2 = y2;
        helper.z2 = z2;
        helper.init(startTile); //Re-init after borrow.
        return helper;
    }


    /**
     * Returns a `NavHelper` instance to the object pool.
     *
     * @param helper The `NavHelper` instance to return.
     */
    public static void returnObject(NavHelper helper) {
        if (helper != null) {
            try {
                navHelperPool.returnObject(helper);
            } catch (Exception e) {
                log.error("Exception returning NavHelper to pool: ", e);
            }
        }
    }

    /**
     * Checks if this NavHelper is currently valid (e.g., not in a broken state).
     *
     * @return True if the object is valid, false otherwise.  This is used in the object pool.
     */
    private boolean isValid() {
        return true; // Implement your validation logic here. For example, check if all references are null.
    }
}