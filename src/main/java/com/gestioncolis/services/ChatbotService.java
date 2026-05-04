package com.gestioncolis.services;

import com.gestioncolis.entities.Post;
import com.gestioncolis.entities.User;

import java.util.List;
import java.util.Optional;

public class ChatbotService {

    private final PostService postService;
    private final ForumService forumService;
    private final UserService userService;
    private final CommentService commentService;
    private final NotificationService notificationService;
    private final AuthService authService;

    public ChatbotService(PostService postService, ForumService forumService, UserService userService, CommentService commentService, NotificationService notificationService, AuthService authService) {
        this.postService = postService;
        this.forumService = forumService;
        this.userService = userService;
        this.commentService = commentService;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    public String processQuery(String input) {
        String query = input.toLowerCase().trim();
        long currentUserId = authService.requireAdmin().getId();

        // 1. Analytics & Statistics
        if (query.equals("stats") || query.equals("metrics") || query.equals("total") || query.contains("statistics")) {
            long postCount = postService.countAll();
            long userCount = userService.countAll();
            long commentCount = commentService.countAll();
            long forumCount = forumService.countAll();
            return "📊 **Forum Statistics:**\n" +
                   "- Total Users: " + userCount + "\n" +
                   "- Total Forums: " + forumCount + "\n" +
                   "- Total Posts: " + postCount + "\n" +
                   "- Total Comments: " + commentCount + "\n" +
                   "Everything looks stable!";
        }

        // 2. Data Lookup (Most active user)
        if (query.contains("active user") || query.contains("top poster") || query.contains("top user")) {
            Optional<User> topUserOpt = userService.getMostActiveUser();
            if (topUserOpt.isPresent()) {
                User topUser = topUserOpt.get();
                return "🏆 **Top Contributor:**\n" +
                       "User: " + topUser.getNom() + " (" + topUser.getEmail() + ")\n" +
                       "Has the highest overall engagement in the database.";
            } else {
                return "No active users found.";
            }
        }
        
        // 3. Hotspots / Toxic / Flagged
        if (query.contains("toxic") || query.contains("flagged") || query.contains("hot")) {
            List<Post> hotPosts = postService.getHotPosts(3, currentUserId);
            if (hotPosts.isEmpty()) return "No highly active or potentially hot posts found.";
            
            StringBuilder response = new StringBuilder("🔥 **Recent Hot/Active Posts (Potential issues):**\n");
            for (Post p : hotPosts) {
                response.append("- [ID: ").append(p.getId()).append("] ").append(p.getTitle()).append(" (by ").append(p.getAuthor().getNom()).append(")\n");
            }
            return response.toString();
        }

        // 4. Quick Search & Discovery
        if (query.startsWith("find post about") || query.startsWith("search post")) {
            String term = query.replace("find post about", "").replace("search post", "").trim();
            List<Post> results = postService.searchAllPostsForAdmin(term, currentUserId);
            if (results.isEmpty()) return "No posts found containing: '" + term + "'";
            
            StringBuilder response = new StringBuilder("🔍 **Search Results:**\n");
            int max = Math.min(5, results.size());
            for (int i = 0; i < max; i++) {
                Post p = results.get(i);
                response.append("- [ID: ").append(p.getId()).append("] ").append(p.getTitle()).append("\n");
            }
            if (results.size() > 5) response.append("... and ").append(results.size() - 5).append(" more.");
            return response.toString();
        }

        // User lookup
        if (query.startsWith("user ")) {
            String email = query.replace("user", "").trim();
            Optional<User> userOpt = userService.findByEmailForAdmin(email);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                return "👤 **User Profile:**\n" +
                       "- ID: " + u.getId() + "\n" +
                       "- Name: " + u.getNom() + "\n" +
                       "- Email: " + u.getEmail() + "\n" +
                       "- Role: " + u.getRole() + "\n" +
                       "- Posts: " + postService.countByAuthor(u.getId()) + "\n" +
                       "- Comments: " + commentService.countByAuthor(u.getId());
            } else {
                return "User with email '" + email + "' not found.";
            }
        }

        // 5. Command Execution & Moderation
        if (query.startsWith("delete post")) {
            try {
                long postId = Long.parseLong(query.replaceAll("[^0-9]", ""));
                postService.deletePost(postId);
                return "🗑️ **Action Executed:** Post #" + postId + " has been successfully deleted.";
            } catch (Exception e) {
                return "❌ Error: Please specify a valid Post ID or the post doesn't exist. Example: 'delete post 12'";
            }
        }

        if (query.startsWith("clear notifications for")) {
            try {
                long userId = Long.parseLong(query.replaceAll("[^0-9]", ""));
                notificationService.markAllAsRead(userId);
                return "✅ **Action Executed:** All notifications for User #" + userId + " have been marked as read.";
            } catch (Exception e) {
                return "❌ Error: Please specify a valid User ID. Example: 'clear notifications for 5'";
            }
        }

        // Default Fallback
        return "🤖 I'm sorry, I didn't understand that command.\n" +
               "Try typing:\n" +
               "- 'stats'\n" +
               "- 'top user'\n" +
               "- 'hot' or 'flagged'\n" +
               "- 'search post [keyword]'\n" +
               "- 'user [email]'\n" +
               "- 'delete post [id]'\n" +
               "- 'clear notifications for [userId]'";
    }
}
