
package com.stockflow.api.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.authentication.BadCredentialsException;

import java.util.stream.Collectors;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.NOT_FOUND, ex.getMessage());
                problem.setTitle("Ressource introuvable");
                return problem;
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT, ex.getMessage());
                problem.setTitle("Ressource déjà existante");
                return problem;
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ProblemDetail> handleValidation(
                        MethodArgumentNotValidException ex) {

                String details = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST, details);
                problem.setTitle("Données invalides");

                return ResponseEntity.badRequest().body(problem);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ProblemDetail handleUnreadableBody(
                        HttpMessageNotReadableException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                "Le corps de la requête est absent ou contient un JSON invalide.");
                problem.setTitle("Requête invalide");
                return problem;
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ProblemDetail handleDataIntegrity(
                        DataIntegrityViolationException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT,
                                "L'opération viole une contrainte de la base de données.");
                problem.setTitle("Conflit de données");
                return problem;
        }

        @ExceptionHandler(ResourceInUseException.class)
        public ProblemDetail handleResourceInUse(ResourceInUseException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT,
                                ex.getMessage());
                problem.setTitle("Ressource utilisée");
                return problem;
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleBadCredentials(
                        BadCredentialsException exception) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message",
                                                "Adresse e-mail ou mot de passe incorrect."));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ProblemDetail handleIllegalState(IllegalStateException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT, ex.getMessage());
                problem.setTitle("Conflit métier");
                return problem;
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST, ex.getMessage());
                problem.setTitle("Requête invalide");
                return problem;
        }
}
