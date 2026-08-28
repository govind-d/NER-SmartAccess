package com.ner.smartlogix.routing;

import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.util.GeometryUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dijkstra's algorithm over our own monitored road network.
 *
 * <p>Why this exists when OSRM is better at pathfinding: it answers a different question.
 * OSRM routes over every road in OpenStreetMap; this routes only over roads the platform
 * actually monitors and can therefore score. It is the fallback when OSRM is unreachable,
 * and the right choice when a convoy must be kept on inspected corridors.
 *
 * <p>How the graph is built: every road endpoint becomes a node, endpoints within
 * {@link #SNAP_RADIUS_METERS} of each other are treated as the same junction, and each
 * road becomes an edge weighted by travel time. The whole network is only a few dozen
 * roads, so building the graph on demand costs nothing measurable; a national-scale
 * network would want the graph cached instead.
 */
@Slf4j
@Component
@Order(50)   // between OSRM and the mock
@RequiredArgsConstructor
public class InternalGraphRoutingProvider implements RoutingProvider {

    /** Endpoints closer than this are the same junction. */
    private static final double SNAP_RADIUS_METERS = 800;
    private static final double AVERAGE_SPEED_KMPH = 35;

    private final RoadRepository roadRepository;

    @Override
    public List<RouteCandidate> findRoutes(double fromLat, double fromLon,
                                           double toLat, double toLon, int alternatives) {
        List<Road> roads = roadRepository.findAll();
        if (roads.isEmpty()) {
            return List.of();
        }

        Graph graph = buildGraph(roads);
        Node start = graph.nearest(fromLat, fromLon);
        Node end = graph.nearest(toLat, toLon);
        if (start == null || end == null || start.equals(end)) {
            return List.of();
        }

        List<Road> path = shortestPath(graph, start, end);
        if (path.isEmpty()) {
            log.debug("No internal path between the requested points");
            return List.of();
        }

        List<double[]> points = new ArrayList<>();
        points.add(new double[]{fromLat, fromLon});
        double distanceKm = 0;
        for (Road road : path) {
            distanceKm += road.getLengthKm() == null ? 0 : road.getLengthKm();
            for (Coordinate coordinate : road.getGeom().getCoordinates()) {
                points.add(new double[]{coordinate.y, coordinate.x});
            }
        }
        points.add(new double[]{toLat, toLon});

        int durationMin = (int) Math.round(distanceKm / AVERAGE_SPEED_KMPH * 60);
        return List.of(new RouteCandidate(Math.round(distanceKm * 100) / 100.0,
                Math.max(1, durationMin), points, "INTERNAL"));
    }

    @Override
    public String providerName() {
        return "INTERNAL";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // ------------------------------------------------------------------ the algorithm

    /**
     * Classic Dijkstra with a priority queue: repeatedly expand the cheapest node reached
     * so far, and stop when that node is the destination. Blocked roads are simply left
     * out of the graph, which is a cleaner way of avoiding them than penalising them.
     */
    private List<Road> shortestPath(Graph graph, Node start, Node end) {
        Map<Node, Double> bestCost = new HashMap<>();
        Map<Node, Road> arrivedBy = new HashMap<>();
        Map<Node, Node> cameFrom = new HashMap<>();
        PriorityQueue<Node> queue =
                new PriorityQueue<>(Comparator.comparingDouble(n -> bestCost.getOrDefault(n, Double.MAX_VALUE)));

        bestCost.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.equals(end)) {
                break;
            }
            for (Edge edge : graph.edgesFrom(current)) {
                double cost = bestCost.getOrDefault(current, Double.MAX_VALUE) + edge.costMinutes;
                if (cost < bestCost.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    bestCost.put(edge.to, cost);
                    cameFrom.put(edge.to, current);
                    arrivedBy.put(edge.to, edge.road);
                    queue.add(edge.to);
                }
            }
        }

        if (!cameFrom.containsKey(end)) {
            return List.of();
        }
        LinkedList<Road> path = new LinkedList<>();
        Node cursor = end;
        while (cameFrom.containsKey(cursor)) {
            path.addFirst(arrivedBy.get(cursor));
            cursor = cameFrom.get(cursor);
        }
        return path;
    }

    private Graph buildGraph(List<Road> roads) {
        Graph graph = new Graph();
        for (Road road : roads) {
            // A blocked road is not an expensive edge - it is not an edge at all.
            if (road.getStatus() == com.ner.smartlogix.enums.RoadStatus.BLOCKED
                    || road.getGeom() == null) {
                continue;
            }
            Coordinate[] coordinates = road.getGeom().getCoordinates();
            Node a = graph.nodeAt(coordinates[0].y, coordinates[0].x);
            Node b = graph.nodeAt(coordinates[coordinates.length - 1].y,
                    coordinates[coordinates.length - 1].x);

            double km = road.getLengthKm() == null ? 1 : road.getLengthKm();
            double minutes = km / AVERAGE_SPEED_KMPH * 60;

            // Roads are two-way here; a real network would honour one-way tags.
            graph.addEdge(a, b, road, minutes);
            graph.addEdge(b, a, road, minutes);
        }
        return graph;
    }

    // ------------------------------------------------------------------ tiny graph types

    private record Node(long snappedLat, long snappedLon) {
    }

    private record Edge(Node to, Road road, double costMinutes) {
    }

    /** Nodes are snapped onto a coarse grid so nearby endpoints merge into one junction. */
    private static final class Graph {
        private final Map<Node, List<Edge>> adjacency = new HashMap<>();
        private final Map<Node, double[]> positions = new HashMap<>();

        Node nodeAt(double lat, double lon) {
            // ~0.005 degrees is roughly 550 m, close enough to the intended snap radius.
            Node node = new Node(Math.round(lat / 0.005), Math.round(lon / 0.005));
            positions.putIfAbsent(node, new double[]{lat, lon});
            return node;
        }

        void addEdge(Node from, Node to, Road road, double costMinutes) {
            adjacency.computeIfAbsent(from, key -> new ArrayList<>())
                    .add(new Edge(to, road, costMinutes));
        }

        List<Edge> edgesFrom(Node node) {
            return adjacency.getOrDefault(node, List.of());
        }

        /** The junction closest to a free-form coordinate, within the snap radius. */
        Node nearest(double lat, double lon) {
            Node best = null;
            double bestKm = Double.MAX_VALUE;
            for (Map.Entry<Node, double[]> entry : positions.entrySet()) {
                double km = GeometryUtils.haversineKm(lat, lon,
                        entry.getValue()[0], entry.getValue()[1]);
                if (km < bestKm) {
                    bestKm = km;
                    best = entry.getKey();
                }
            }
            // Anything further than 50 km away is not a sensible entry point to the network.
            return bestKm <= 50 ? best : null;
        }
    }
}
