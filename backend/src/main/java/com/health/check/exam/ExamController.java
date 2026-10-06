package com.health.check.exam;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    /** 检查项列表：可按体检人、按科室过滤。 */
    @GetMapping
    public ApiResponse<List<Exam>> list(@RequestParam(value = "examineeId", required = false) Long examineeId,
                                        @RequestParam(value = "deptId", required = false) Long deptId) {
        if (examineeId != null) {
            return ApiResponse.success(service.listByExaminee(examineeId));
        }
        if (deptId != null) {
            return ApiResponse.success(service.listByDept(deptId));
        }
        return ApiResponse.success(service.list());
    }

    /** 检查进度：某体检人的检查项完成度，供进度条使用。 */
    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> progress(
            @RequestParam(value = "examineeId", required = false) Long examineeId) {
        if (examineeId == null) {
            throw new BizException("请先选择体检人");
        }
        return ApiResponse.success(service.progress(examineeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Exam> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    /** 排检查项：科室须开放，体检人须体检中。 */
    @PostMapping
    public ApiResponse<Exam> create(@RequestBody Exam in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Exam> update(@PathVariable Long id, @RequestBody Exam in) {
        return ApiResponse.success(service.update(id, in));
    }

    /**
     * 检查项状态流转：检查中 / 已完成 / 已弃检。
     * 标记已完成必须填结果并写入检查时间；标记异常必须填结果；弃检必须填理由。
     */
    @PostMapping("/{id}/status")
    public ApiResponse<Exam> changeStatus(@PathVariable Long id,
                                          @RequestParam(value = "status", required = false) String status,
                                          @RequestParam(value = "result", required = false) String result,
                                          @RequestParam(value = "abnormal", required = false) Boolean abnormal) {
        if (status == null || status.isBlank()) {
            throw new BizException("目标状态不可为空");
        }
        return ApiResponse.success(service.changeStatus(id, status.trim(), result, abnormal));
    }

    /** 撤销待检的检查项。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(true);
    }
}
