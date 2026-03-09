package org.service_b.workflow.security.controller;

import org.service_b.workflow.security.WrongCredentialsException;
import org.service_b.workflow.workflow.service.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<Message> login(@RequestBody LoginRequest loginRequest) {
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.username(), loginRequest.password());
        try {
            this.authenticationManager.authenticate(authenticationRequest);
        } catch (Exception e) {
            throw new WrongCredentialsException(e.getMessage());
        }
        return ResponseEntity.ok(new Message("yeah"));
    }

    public record LoginRequest(String username, String password) {
    }

    @GetMapping("get-basic")
    public ResponseEntity<String> getBasic() {
        return ResponseEntity.ok("hell yeah");
    }

    @ExceptionHandler(WrongCredentialsException.class)
    public ResponseEntity<Message> handleWrongCredentials(WrongCredentialsException wrongCredentialsException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Message(wrongCredentialsException.getMessage()));
    }
}
