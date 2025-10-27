package com.mini.soccer.controller;

import com.mini.soccer.dto.request.FieldRequest;
import com.mini.soccer.dto.response.ApiResponse;
import com.mini.soccer.dto.response.FieldResponse;
import com.mini.soccer.service.field.IFieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/fields")
@RequiredArgsConstructor
public class FieldController {

    private final IFieldService fieldService;

    @PostMapping
    public ResponseEntity<ApiResponse<FieldResponse>> createField(
            @Valid @RequestBody FieldRequest request) {
        FieldResponse field = fieldService.createField(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(field, "Field created successfully"));
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<ApiResponse<FieldResponse>> updateField(
            @PathVariable Long fieldId,
            @Valid @RequestBody FieldRequest request) {
        FieldResponse field = fieldService.updateField(fieldId, request);
        return ResponseEntity.ok(ApiResponse.success(field, "Field updated successfully"));
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<ApiResponse<Void>> deleteField(@PathVariable Long fieldId) {
        fieldService.deleteField(fieldId);
        return ResponseEntity.ok(ApiResponse.success(null, "Field deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FieldResponse>>> getFields(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<FieldResponse> fields = fieldService.getFields(pageable);
        return ResponseEntity.ok(ApiResponse.success(fields, "Retrieved fields"));
    }
}
