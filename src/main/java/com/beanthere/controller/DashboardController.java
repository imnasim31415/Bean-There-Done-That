package com.beanthere.controller;

import com.beanthere.service.DependencyGraphService;
import com.beanthere.store.BeanRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final BeanRegistry beanRegistry;
    private final DependencyGraphService graphService;

    public DashboardController(BeanRegistry beanRegistry, DependencyGraphService graphService) {
        this.beanRegistry = beanRegistry;
        this.graphService = graphService;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        var all = beanRegistry.getAll();
        model.addAttribute("totalBeans", all.size());
        model.addAttribute("singletons", all.stream().filter(b -> "singleton".equals(b.getScope())).count());
        model.addAttribute("prototypes", all.stream().filter(b -> "prototype".equals(b.getScope())).count());
        model.addAttribute("startupPhases", beanRegistry.getStartupTimeline().size());
        return "dashboard";
    }

    @GetMapping("/beans")
    public String beans(Model model) {
        model.addAttribute("beans", graphService.buildGraph());
        return "beans";
    }

    @GetMapping("/lifecycle")
    public String lifecycle(Model model) {
        model.addAttribute("beans", beanRegistry.getAll());
        model.addAttribute("startupTimeline", beanRegistry.getStartupTimeline());
        return "lifecycle";
    }

    @GetMapping("/beans/{beanName}")
    public String beanDetail(@PathVariable String beanName, Model model) {
        var info = beanRegistry.get(beanName);
        if (info == null) return "redirect:/dashboard/beans";
        model.addAttribute("bean", info);
        return "bean-detail";
    }
}
