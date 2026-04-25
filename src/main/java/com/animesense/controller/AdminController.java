package com.animesense.controller;

import com.animesense.model.Anime;
import com.animesense.model.BroadcastMessage;
import com.animesense.model.Favorite;
import com.animesense.model.User;
import com.animesense.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    private final AnimeService animeService;
    private final FavoriteService favoriteService;
    private final BroadcastService broadcastService;
    
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Statistics
        long totalUsers = userService.getTotalUserCount();
        long activeUsers7d = userService.getActiveUserCount(7);
        long activeUsers30d = userService.getActiveUserCount(30);
        long totalAnime = animeService.getTotalAnimeCount();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers7d", activeUsers7d);
        model.addAttribute("activeUsers30d", activeUsers30d);
        model.addAttribute("totalAnime", totalAnime);
        
        // Recent users
        List<User> recentUsers = userService.getActiveUsers(7);
        model.addAttribute("recentUsers", recentUsers.stream().limit(10).collect(Collectors.toList()));
        
        // Featured anime
        List<Anime> featuredAnime = animeService.getFeaturedAnime();
        model.addAttribute("featuredAnime", featuredAnime);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userService.getAllUsers();
        
        // Add favorite counts for each user
        Map<Long, Long> favoriteCounts = users.stream()
            .collect(Collectors.toMap(
                User::getUserId,
                user -> favoriteService.getUserFavoriteCount(user.getUserId())
            ));
        
        model.addAttribute("users", users);
        model.addAttribute("favoriteCounts", favoriteCounts);
        
        return "admin/users";
    }
    
    @GetMapping("/users/{userId}")
    public String userDetails(@PathVariable Long userId, Model model) {
        User user = userService.getUserById(userId).orElseThrow();
        List<Favorite> favorites = favoriteService.getUserFavorites(userId);
        Double avgRating = favoriteService.getUserAverageRating(userId);
        
        model.addAttribute("user", user);
        model.addAttribute("favorites", favorites);
        model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
        
        return "admin/user-details";
    }
    
    @GetMapping("/anime")
    public String anime(Model model, @RequestParam(required = false) String search) {
        List<Anime> animeList;
        
        if (search != null && !search.isEmpty()) {
            animeList = animeService.searchAnime(search);
        } else {
            animeList = animeService.getAllAnime();
        }
        
        model.addAttribute("animeList", animeList);
        model.addAttribute("search", search);
        
        return "admin/anime";
    }
    
    @GetMapping("/anime/add")
    public String addAnimePage() {
        return "admin/anime-add";
    }
    
    @PostMapping("/anime/add")
    public String addAnime(@RequestParam Long animeId, RedirectAttributes redirectAttributes) {
        try {
            Anime anime = animeService.fetchAndSaveAnime(animeId);
            if (anime != null) {
                redirectAttributes.addFlashAttribute("success", "Anime added successfully: " + anime.getTitle());
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to fetch anime from MyAnimeList");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/anime";
    }
    
    @GetMapping("/anime/{animeId}/edit")
    public String editAnimePage(@PathVariable Long animeId, Model model) {
        Anime anime = animeService.getAnimeById(animeId).orElseThrow();
        model.addAttribute("anime", anime);
        return "admin/anime-edit";
    }
    
    @PostMapping("/anime/{animeId}/edit")
    public String editAnime(@PathVariable Long animeId, 
                           @RequestParam(required = false) Boolean featured,
                           @RequestParam(required = false) Double adminBoost,
                           RedirectAttributes redirectAttributes) {
        try {
            if (featured != null) {
                animeService.setFeatured(animeId, featured);
            }
            if (adminBoost != null) {
                animeService.setAdminBoost(animeId, adminBoost);
            }
            redirectAttributes.addFlashAttribute("success", "Anime updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/anime/" + animeId + "/edit";
    }
    
    @PostMapping("/anime/{animeId}/delete")
    public String deleteAnime(@PathVariable Long animeId, RedirectAttributes redirectAttributes) {
        try {
            animeService.deleteAnime(animeId);
            redirectAttributes.addFlashAttribute("success", "Anime deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/anime";
    }
    
    @GetMapping("/broadcast")
    public String broadcastPage(Model model) {
        List<BroadcastMessage> history = broadcastService.getBroadcastHistory();
        model.addAttribute("history", history);
        return "admin/broadcast";
    }
    
    @PostMapping("/broadcast")
    public String sendBroadcast(@RequestParam String message, RedirectAttributes redirectAttributes) {
        try {
            int sent = broadcastService.sendBroadcastMessage(message, 0L); // Admin ID placeholder
            redirectAttributes.addFlashAttribute("success", 
                String.format("Broadcast sent to %d users", sent));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/broadcast";
    }
}
