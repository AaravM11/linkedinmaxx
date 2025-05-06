package com.aarav.sparkapp;

import static spark.Spark.*;
import com.google.gson.Gson;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class App {
    // In-memory user store
    static Map<String, User> users = new ConcurrentHashMap<>();
    static Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println(">>> Starting Spark on port 4567");
        staticFiles.externalLocation("src/main/resources/public");
        port(4567);

        before((req, res) -> System.out.println("→ " + req.requestMethod() + " " + req.uri()));

        // Health-check
        get("/ping", (req, res) -> {
            res.type("text/plain");
            return "pong";
        });

        // Profile update: comma-separated friends + optional PDF resume
        post("/api/profile", (req, res) -> {
            String contentType = req.contentType();
            if (contentType == null || !contentType.startsWith("multipart/")) {
                res.status(400);
                return gson.toJson(Map.of("status", "must be multipart/form-data"));
            }
            req.raw().setAttribute("org.eclipse.jetty.multipartConfig",
                                  new MultipartConfigElement("/tmp"));

            String username = req.queryParams("user");
            User user = users.get(username);
            if (user == null) {
                user = new User(username, "", "");
                users.put(username, user);
            }

            // 1) Parse comma-separated friends list
            String friendsText = req.raw().getParameter("friendsList");
            if (friendsText != null && !friendsText.trim().isEmpty()) {
                List<String> newFriends = Arrays.stream(friendsText.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

                // Clear existing friendships
                for (User f : user.friends) f.friends.remove(user);
                user.friends.clear();
                // Add new friendships
                for (String fname : newFriends) {
                    User f = users.computeIfAbsent(fname, n -> new User(n, "", ""));
                    user.friends.add(f);
                    f.friends.add(user);
                }
            }

            // 2) Parse resume PDF
            Part resumePart = req.raw().getPart("resumeFile");
            if (resumePart != null && resumePart.getSize() > 0) {
                try (InputStream rs = resumePart.getInputStream()) {
                    String text = extractText(rs);
                    user.interests.clear();
                    parseResume(user, text);
                }
            }

            res.type("application/json");
            return gson.toJson(Map.of("status", "updated"));
        });

        // Retrieve profile
        get("/api/profile", (req, res) -> {
            String username = req.queryParams("user");
            User user = users.get(username);
            if (user == null) {
                res.status(400);
                return gson.toJson(Map.of("status", "user not found"));
            }
            Map<String,Object> profile = Map.of(
                "username",  user.username,
                "email",     user.email,
                "name",      user.name,
                "friends",   user.friends.stream()
                                         .map(u -> u.username)
                                         .collect(Collectors.toList()),
                "interests", user.interests
            );
            res.type("application/json");
            return gson.toJson(profile);
        });

        // Export graph of users only
        get("/api/graph", (req, res) -> {
            Map<String,Object> graph = GraphBuilder.buildGraph(users);
            res.type("application/json");
            return gson.toJson(graph);
        });

        // Compute shortest friendship path
        get("/api/distance", (req, res) -> {
            String u1 = req.queryParams("user1");
            String u2 = req.queryParams("user2");
            if (u1 == null || u2 == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "user1 and user2 required"));
            }
            List<String> path = findFriendPath(u1, u2);
            if (path == null) {
                res.status(404);
                return gson.toJson(Map.of("error", "no path found"));
            }
            Map<String,Object> result = Map.of(
                "distance", path.size() - 1,
                "path",     path
            );
            res.type("application/json");
            return gson.toJson(result);
        });
    }

    // BFS for shortest friend path
    private static List<String> findFriendPath(String start, String end) {
        if (!users.containsKey(start) || !users.containsKey(end)) return null;
        Queue<String> queue = new LinkedList<>();
        Map<String,String> prev = new HashMap<>();
        queue.add(start);
        prev.put(start, null);

        while (!queue.isEmpty()) {
            String u = queue.poll();
            if (u.equals(end)) break;
            for (User f : users.get(u).friends) {
                if (!prev.containsKey(f.username)) {
                    prev.put(f.username, u);
                    queue.add(f.username);
                }
            }
        }
        if (!prev.containsKey(end)) return null;
        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = prev.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    private static String extractText(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static void parseResume(User user, String text) {
        String lower = text.toLowerCase();
        if (lower.contains("java"))   user.interests.add("Java");
        if (lower.contains("spark"))  user.interests.add("Spark");
        if (lower.contains("pdfbox")) user.interests.add("PDFBox");
    }
}
