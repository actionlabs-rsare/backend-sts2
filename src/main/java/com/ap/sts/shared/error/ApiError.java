package com.ap.sts.shared.error;

import java.util.List;

/** Error body matching common.openapi.yaml #/schemas/Error. */
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
