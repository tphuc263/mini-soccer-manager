package com.mini.soccer.service.field;

import com.mini.soccer.dto.request.FieldRequest;
import com.mini.soccer.dto.response.FieldResponse;
import com.mini.soccer.model.Field;
import com.mini.soccer.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldService implements IFieldService {

    private final FieldRepository fieldRepository;

    @Override
    @Transactional
    public FieldResponse createField(FieldRequest request) {
        Field field = Field.builder()
                .name(request.getName().trim())
                .pricePerHour(normalizePrice(request.getPricePerHour()))
                .description(normalizeDescription(request.getDescription()))
                .build();
        Field saved = fieldRepository.save(field);
        return toFieldResponse(saved);
    }

    @Override
    @Transactional
    public FieldResponse updateField(Long fieldId, FieldRequest request) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));
        field.setName(request.getName().trim());
        field.setPricePerHour(normalizePrice(request.getPricePerHour()));
        field.setDescription(normalizeDescription(request.getDescription()));
        Field saved = fieldRepository.save(field);
        return toFieldResponse(saved);
    }

    @Override
    @Transactional
    public void deleteField(Long fieldId) {
        if (!fieldRepository.existsById(fieldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found");
        }
        fieldRepository.deleteById(fieldId);
    }

    @Override
    public Page<FieldResponse> getFields(Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "fieldId")
            );
        }
        return fieldRepository.findAll(effectivePageable)
                .map(this::toFieldResponse);
    }

    private FieldResponse toFieldResponse(Field field) {
        return FieldResponse.builder()
                .fieldId(field.getFieldId())
                .name(field.getName())
                .pricePerHour(field.getPricePerHour())
                .description(field.getDescription())
                .build();
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
