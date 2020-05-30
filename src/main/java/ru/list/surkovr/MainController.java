package ru.list.surkovr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // Код получаем после авторизации. Если код отсутствует, то редирект на авторизацию
    // После авторизации, код запоминается в приложении и используется для автоматического обновления
    // При каждом запросе данных, код будет обновляться (если он есть в запросе)
    @GetMapping(path = "/stats/{period}")
    public String getGroupStats(@PathVariable("period") String period,
                                @RequestParam(value = "code", required = false) String code, Model model,
                                HttpServletResponse response) throws IOException {


        if (!vkGroupService.setCode(code)) return "redirect:/login";
        validateCode(response);
            List<GroupsStatsResultDTO> stats = null;
            try {
                stats = Objects.requireNonNull(vkGroupService.getGroupStatByPeriod(period));
            } catch (NullPointerException e) {
                e.printStackTrace();
                return "redirect:/login";
            }
            model.addAttribute("period", period.substring(0,1).toUpperCase() + period.substring(1));
            model.addAttribute("stats", stats);
            return "index";
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(vkGroupService.getUserOAuthUrl());
    }

    @RequestMapping("/")
    public void home(@RequestParam(value = "code", required = false) String code,
                      HttpServletResponse response) throws IOException {
        response.sendRedirect("/stats/today?code=" + code);
    }

    @RequestMapping("/index")
    public void index(@RequestParam(value = "code", required = false) String code,
                      HttpServletResponse response) throws IOException {
        response.sendRedirect("/stats/today?code=" + code);
    }

    private void validateCode(HttpServletResponse response) throws IOException {
        if (!vkGroupService.hasValidCode()) response.sendRedirect(vkGroupService.getUserOAuthUrl());
    }
}
