package com.health.check.examinee;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.health.check.common.ApiResponse;
import com.health.check.common.BizException;

@RestController
@RequestMapping("/api/examinees")
public class ExamineeController {

    private final ExamineeService service;

    public ExamineeController(ExamineeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Examinee>> list(@RequestParam(value = "packageId", required = false) Long packageId) {
        if (packageId == null) {
            return ApiResponse.success(service.list());
        }
        return ApiResponse.success(service.listByPackage(packageId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Examinee> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Examinee> create(@RequestBody Examinee in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Examinee> update(@PathVariable Long id, @RequestBody Examinee in) {
        return ApiResponse.success(service.update(id, in));
    }

    /** 开始体检：已预约 → 体检中。 */
    @PostMapping("/{id}/start")
    public ApiResponse<Examinee> start(@PathVariable Long id) {
        return ApiResponse.success(service.start(id));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Examinee> changeStatus(@PathVariable Long id,
                                              @RequestParam(value = "status", required = false) String status) {
        if (status == null || status.isBlank()) {
            throw new BizException("目标状态不可为空");
        }
        return ApiResponse.success(service.changeStatus(id, status.trim()));
    }
}
