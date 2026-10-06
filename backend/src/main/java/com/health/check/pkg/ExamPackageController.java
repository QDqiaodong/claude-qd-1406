package com.health.check.pkg;

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
@RequestMapping("/api/packages")
public class ExamPackageController {

    private final ExamPackageService service;

    public ExamPackageController(ExamPackageService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ExamPackage>> list() {
        return ApiResponse.success(service.list());
    }

    /** 可被预约的套餐（仅在售）。 */
    @GetMapping("/bookable")
    public ApiResponse<List<ExamPackage>> bookable() {
        return ApiResponse.success(service.listBookable());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamPackage> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<ExamPackage> create(@RequestBody ExamPackage in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamPackage> update(@PathVariable Long id, @RequestBody ExamPackage in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<ExamPackage> changeStatus(@PathVariable Long id,
                                                 @RequestParam(value = "status", required = false) String status) {
        if (status == null || status.isBlank()) {
            throw new BizException("目标状态不可为空");
        }
        return ApiResponse.success(service.changeStatus(id, status.trim()));
    }
}
