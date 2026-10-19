package com.cosmocats.cosmo_cats.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.*;

// Цей клас ловить винятки з усіх контролерів і перетворює їх у відповідь для клієнта
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERRORS_BASE_URI = "https://cosmo-cats.market/errors/";

    // Продукт не знайдено -> 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.NOT_FOUND, "not-found", "Product not found",
                ex.getMessage(), request);
    }

    // Така назва вже є -> 409
    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ProblemDetail handleConflict(ProductAlreadyExistsException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "conflict", "Product already exists",
                ex.getMessage(), request);
    }

    // В адресі замість UUID прийшов текст (наприклад ne-uuid) -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                            HttpServletRequest request) {
        String detail = "Parameter '" + ex.getName() + "' has invalid value '" + ex.getValue() + "'.";
        return buildProblem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
                detail, request);
    }

    // Дані не пройшли @Valid (погана ціна, порожня назва...) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex,
                                           HttpServletRequest request) {
        // Збираємо всі помилки полів у список
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            errors.add(error);
        }

        // У detail пишемо першу помилку, як у прикладі з лаби
        String detail = "Request body is invalid.";
        if (!errors.isEmpty()) {
            detail = "Field '" + errors.get(0).get("field") + "' " + errors.get(0).get("message");
        }

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "validation",
                "Validation failed", detail, request);
        problem.setProperty("errors", errors); // повний список для клієнта
        return problem;
    }

    // Тіло запиту порожнє або це не JSON -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex,
                                              HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "validation", "Malformed request",
                "Request body is missing or is not valid JSON.", request);
    }

    // Неправильні page або size -> 400
    @ExceptionHandler(InvalidPageRequestException.class)
    public ProblemDetail handleInvalidPage(InvalidPageRequestException ex,
                                           HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
                ex.getMessage(), request);
    }

    // Метод не підтримується (наприклад PATCH) -> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        ProblemDetail problem = buildProblem(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed",
                "Method not allowed", ex.getMessage(), request);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        // за стандартом HTTP відповідь 405 має казати, які методи дозволені (заголовок Allow)
        Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
        if (allowed != null) {
            response.allow(allowed.toArray(new HttpMethod[0]));
        }
        return response.body(problem);
    }

    // Content-Type не JSON -> 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                    HttpServletRequest request) {
        return buildProblem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                "Unsupported media type", ex.getMessage(), request);
    }


    // Спільний метод: збирає відповідь з полів type, title, status, detail, instance
    private ProblemDetail buildProblem(HttpStatus status, String typeSlug, String title,
                                       String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERRORS_BASE_URI + typeSlug));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}