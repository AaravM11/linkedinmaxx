package com.aarav.sparkapp;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Build a user‐only graph:
 *  - Nodes: every username
 *  - Edges:
 *     • direct friendships
 *     • interest-based links between any two users who share at least one interest
 */
public class GraphBuilder {

    /**
     * @param users map of username → User
     * @return map with “nodes” (List of {id:…}) and “edges” (List of {source:…,target:…})
     */
    public static Map<String, Object> buildGraph(Map<String, User> users) {
        // 1) Nodes: one per user
        List<Map<String, String>> nodes = users.keySet().stream()
            .map(u -> Map.of("id", u))
            .collect(Collectors.toList());

        // 2) Build unique edges
        Set<String> seen = new HashSet<>();
        List<Map<String, String>> edges = new ArrayList<>();

        // 2a) Friend edges
        for (User u : users.values()) {
            for (User f : u.friends) {
                String a = u.username, b = f.username;
                String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
                if (seen.add(key)) {
                    edges.add(Map.of("source", a, "target", b));
                }
            }
        }

        // 2b) Interest edges (complete subgraph per shared interest)
        // Group users by each interest
        Map<String, List<String>> byInterest = new HashMap<>();
        for (User u : users.values()) {
            for (String interest : u.interests) {
                byInterest
                  .computeIfAbsent(interest, k -> new ArrayList<>())
                  .add(u.username);
            }
        }
        // For each interest group, connect every pair
        for (List<String> group : byInterest.values()) {
            int n = group.size();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    String a = group.get(i), b = group.get(j);
                    String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
                    if (seen.add(key)) {
                        edges.add(Map.of("source", a, "target", b));
                    }
                }
            }
        }

        return Map.of("nodes", nodes, "edges", edges);
    }
}
