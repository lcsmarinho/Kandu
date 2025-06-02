package br.com.kandu.controller;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.TokenDTO;
import br.com.kandu.dto.UsuarioCadastroDTO; // Para o endpoint de cadastro
import br.com.kandu.entity.Usuario;       // Para o retorno do cadastro
import br.com.kandu.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth") // Rota base para autenticação
public class AutenticacaoController {

    private final UsuarioService usuarioService;

    @Autowired
    public AutenticacaoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Endpoint de Cadastro (movido ou criado aqui para centralizar auth)
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarUsuario(@RequestBody UsuarioCadastroDTO dto) {
        try {
            Usuario novoUsuario = usuarioService.cadastrarUsuario(dto);
            // Não retornar a senha, mesmo hasheada. Idealmente, retornar um DTO de Usuário sem a senha.
            // Por simplicidade, retornando o objeto Usuario, mas cuidado com dados sensíveis.
            // Para ser mais seguro, crie um UsuarioResponseDTO.
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário " + novoUsuario.getNomeUsuario() + " cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Logar a exceção e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao cadastrar usuário.");
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        try {
            String token = usuarioService.autenticar(loginDTO);
            return ResponseEntity.ok(new TokenDTO(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falha na autenticação: " + e.getMessage());
        } catch (Exception e) {
            // Logar a exceção e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno durante a autenticação.");
        }
    }
}