package org.service_b.workflow.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WrongCredentialsException extends RuntimeException {
    private final String message;
}
