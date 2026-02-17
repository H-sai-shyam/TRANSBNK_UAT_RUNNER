package com.example.transbnk_uat_runner.controller;

import com.example.transbnk_uat_runner.service.ApiRunnerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/run")
public class RunController {

    private final ApiRunnerService service;

    public RunController(ApiRunnerService service) {
        this.service = service;
    }

    @PostMapping("/{apiName}")
    public Object run(@PathVariable String apiName) throws Exception {
        return service.runApi(apiName);
    }
}
