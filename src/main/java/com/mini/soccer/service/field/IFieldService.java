package com.mini.soccer.service.field;

import com.mini.soccer.dto.request.FieldRequest;
import com.mini.soccer.dto.response.FieldResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFieldService {

    FieldResponse createField(FieldRequest request);

    FieldResponse updateField(Long fieldId, FieldRequest request);

    void deleteField(Long fieldId);

    Page<FieldResponse> getFields(Pageable pageable);
}
