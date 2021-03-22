package com.provectus.kafka.ui.exception;

import com.google.common.collect.Sets;
import com.provectus.kafka.ui.model.ErrorResponse;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

	public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
										  ResourceProperties resourceProperties,
										  ApplicationContext applicationContext,
										  ServerCodecConfigurer codecConfigurer) {
		super(errorAttributes, resourceProperties, applicationContext);
		this.setMessageWriters(codecConfigurer.getWriters());
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
	}

	private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		Throwable throwable = getError(request);

		// validation and params binding errors
		if (throwable instanceof WebExchangeBindException) {
			return render((WebExchangeBindException) throwable, request);
		}

		// requests mapping & access errors
		if (throwable instanceof ResponseStatusException) {
			return render((ResponseStatusException) throwable, request);
		}

		// custom exceptions
		if (throwable instanceof CustomBaseException) {
			return render((CustomBaseException) throwable, request);
		}

		return renderDefault(throwable, request);
	}

	private Mono<ServerResponse> renderDefault(Throwable throwable, ServerRequest request){
		var response = new ErrorResponse();
		response.setCode(ErrorCode.UNEXPECTED.code());
		response.setMessage(throwable.getMessage());
		response.setRequestId(requestId(request));
		response.setTimestamp(currentTimestamp());
		return ServerResponse
				.status(ErrorCode.UNEXPECTED.httpStatus())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response);
	}

	private Mono<ServerResponse> render(CustomBaseException baseException, ServerRequest request) {
		ErrorCode errorCode = baseException.getErrorCode();
		var response = new ErrorResponse();
		response.setCode(errorCode.code());
		response.setMessage(baseException.getMessage());
		response.setRequestId(requestId(request));
		response.setTimestamp(currentTimestamp());
		return ServerResponse
				.status(errorCode.httpStatus())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response);
	}

	private Mono<ServerResponse> render(WebExchangeBindException exception, ServerRequest request) {
		Map<String, Set<String>> fieldErrorsMap = exception.getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField, f -> Set.of(extractFieldErrorMsg(f)), Sets::union));

		var fieldsErrors = fieldErrorsMap.entrySet().stream()
				.map(e -> {
					var err = new com.provectus.kafka.ui.model.FieldError();
					err.setFieldName(e.getKey());
					err.setRestrictions(List.copyOf(e.getValue()));
					return err;
				}).collect(Collectors.toList());

		var message = fieldsErrors.isEmpty()
				? exception.getMessage()
				: "Fields validation failure";

		var response = new ErrorResponse();
		response.setCode(ErrorCode.BINDING_FAIL.code());
		response.setMessage(message);
		response.setRequestId(requestId(request));
		response.setTimestamp(currentTimestamp());
		response.setFieldsErrors(fieldsErrors);
		return ServerResponse
				.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response);
	}

	private Mono<ServerResponse> render(ResponseStatusException exception, ServerRequest request) {
		String msg = coalesce(exception.getReason(), exception.getMessage(), "Server error");
		var response = new ErrorResponse();
		response.setCode(ErrorCode.UNEXPECTED.code());
		response.setMessage(msg);
		response.setRequestId(requestId(request));
		response.setTimestamp(currentTimestamp());
		return ServerResponse
				.status(exception.getStatus())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response);
	}

	private String requestId(ServerRequest request){
		return request.exchange().getRequest().getId();
	}

	private BigDecimal currentTimestamp(){
		return BigDecimal.valueOf(System.currentTimeMillis());
	}

	private String extractFieldErrorMsg(FieldError fieldError) {
		return coalesce(fieldError.getDefaultMessage(), fieldError.getCode(), "Invalid field value");
	}

	private <T> T coalesce(T... items) {
		return Stream.of(items).filter(Objects::nonNull).findFirst().orElse(null);
	}

}
