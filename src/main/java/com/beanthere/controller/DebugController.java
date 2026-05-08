package com.beanthere.controller;

import com.beanthere.model.BeanInfo;
import com.beanthere.model.DependencyNode;
import com.beanthere.model.StartupEvent;
import com.beanthere.service.DependencyGraphService;
import com.beanthere.store.BeanRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final BeanRegistry beanRegistry;
    private final DependencyGraphService graphService;

    public DebugController(BeanRegistry beanRegistry, DependencyGraphService graphService) {
        this.beanRegistry = beanRegistry;
        this.graphService = graphService;
    }

    /** All tracked beans with their dependency graph. */
    @GetMapping("/beans")
    public List<DependencyNode> allBeans() {
        return graphService.buildGraph();
    }

    /** Full lifecycle detail for one bean. */
    @GetMapping("/beans/{beanName}")
    public ResponseEntity<BeanInfo> beanDetail(@PathVariable String beanName) {
        BeanInfo info = beanRegistry.get(beanName);
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.notFound().build();
    }

    /** Full application startup phase timeline. */
    @GetMapping("/startup")
    public List<StartupEvent> startupTimeline() {
        return beanRegistry.getStartupTimeline();
    }

    /** Lightweight circular dependency hints (name-match heuristic). */
    @GetMapping("/circular")
    public Map<String, Object> circularHints() {
        List<String> hints = graphService.detectCircularHints();
        return Map.of(
                "potentialCycles", hints,
                "note", "Heuristic based on class-name matching. Spring itself blocks true cycles by default."
        );
    }

    /** Summary counts. */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Collection<BeanInfo> all = beanRegistry.getAll();
        long singletons  = all.stream().filter(b -> "singleton".equals(b.getScope())).count();
        long prototypes  = all.stream().filter(b -> "prototype".equals(b.getScope())).count();
        return Map.of(
                "totalBeans", all.size(),
                "singletons", singletons,
                "prototypes", prototypes,
                "startupPhases", beanRegistry.getStartupTimeline().size()
        );
    }
}
