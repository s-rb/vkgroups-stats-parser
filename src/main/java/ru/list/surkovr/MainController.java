package ru.list.surkovr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping(path = "/index", params = {"period", "code"})
    public String getGroupLikes(@RequestParam(value = "period", required = false) String period,
                                @RequestParam(value = "code", required = true) String code, Model model,
                                HttpServletResponse response) {


        period = "today";
        System.out.println("===> " + code);
        if (code == null || code.equals("")) {
            try {
                response.sendRedirect(vkGroupService.getUserOAuthUrl());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            List<GroupsStatsResultDTO> stats = null;
            try {
                stats = Objects.requireNonNull(vkGroupService.getGroupStatByPeriod(period, code));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            model.addAttribute("stats", stats);
            return "index";
        }
        return "redirect:/connect/vkontakte";
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(vkGroupService.getUserOAuthUrl());
    }

//    @RequestMapping("/")
//    public String indexPage() {
//        return "index";
//    }

}
