package com.health.check.dept;

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
@RequestMapping("/api/depts")
public class DeptController {

    private final DeptService service;

    public DeptController(DeptService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Dept>> list() {
        return ApiResponse.success(service.list());
    }

    /** 可排检查项的科室（仅开放），检查项下单时用。 */
    @GetMapping("/available")
    public ApiResponse<List<Dept>> available() {
        return ApiResponse.success(service.listAvailable());
    }

    @GetMapping("/{id}")
    public ApiResponse<Dept> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Dept> create(@RequestBody Dept in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Dept> update(@PathVariable Long id, @RequestBody Dept in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Dept> changeStatus(@PathVariable Long id,
                                          @RequestParam(value = "status", required = false) String status) {
        if (status == null || status.isBlank()) {
            throw new BizException("目标状态不可为空");
        }
        return ApiResponse.success(service.changeStatus(id, status.trim()));
    }
}
