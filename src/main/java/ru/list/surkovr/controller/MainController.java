package ru.list.surkovr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.list.surkovr.model.GroupsStatsResult;
import ru.list.surkovr.service.VkGroupService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller
public class MainController {

    private final VkGroupService vkGroupService;

    @Autowired
    public MainController(VkGroupService vkGroupService) {
        this.vkGroupService = vkGroupService;
    }

    @GetMapping(path = "/stats/{period}")
    public String getGroupStats(@PathVariable("period") String period, Model model) {
        String res;
        if (!vkGroupService.hasValidAccessToken()) {
            return "redirect:/login";
        } else {
            List<GroupsStatsResult> stats;
            try {
                stats = Objects.requireNonNull(vkGroupService.getGroupStatByPeriod(period));
            } catch (NullPointerException e) {
                e.printStackTrace();
                return "redirect:/login";
            }
            model.addAttribute("period", period.substring(0, 1).toUpperCase() + period.substring(1));
            model.addAttribute("stats", stats);
            res = "index";
        }
        return res;
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(vkGroupService.getUserOAuthUrl());
    }

    @RequestMapping("/")
    public void home(HttpServletResponse response) throws IOException {
        response.sendRedirect("/stats/today");
    }

    @RequestMapping("/auth")
    public String auth(@RequestParam(value = "code") String code) {
        System.out.println("===> index " + code);
        return vkGroupService.authUser(code);
    }
}
