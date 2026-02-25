package com.example.transbnk_uat_runner.controller;

import com.example.transbnk_uat_runner.model.ApiResult;
import com.example.transbnk_uat_runner.service.ApiRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RunController {

    private static final Logger log =
            LoggerFactory.getLogger(RunController.class);

    private final ApiRunnerService service;

    public RunController(ApiRunnerService service) {
        this.service = service;
    }

    @PostMapping("/{apiName}")
    public ApiResult run(@PathVariable String apiName) throws Exception {
        log.info(" Incoming request | apiName={}", apiName);
        ApiResult result = service.runApi(apiName);
        log.info(
            "Completed | apiName={} | httpStatus={} | businessStatus={}",
            apiName,
            result.getStatusCode(),
            result.getBusinessStatus()
        );
        return result;
    }
}
