package br.com.kandu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Para obter o usuário autenticado
import org.springframework.security.core.context.SecurityContextHolder; // Para obter o contexto de segurança
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<String> getProtectedResource() {
        // Obtém o nome do usuário autenticado do contexto de segurança
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        return ResponseEntity.ok("Olá, " + currentPrincipalName + "! Você acessou um recurso protegido. Seu nível é: " + authentication.getAuthorities());
    }

    @GetMapping("/public") // Um endpoint público para contraste, se quiser testar
    public ResponseEntity<String> getPublicResource() {
        return ResponseEntity.ok("Este é um recurso público, qualquer um pode acessar.");
    }
}