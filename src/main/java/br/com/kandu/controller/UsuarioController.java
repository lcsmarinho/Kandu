// src/main/java/br/com/kandu/controller/UsuarioController.java
package br.com.kandu.controller;

import br.com.kandu.dto.UsuarioAdminCriacaoDTO;
import br.com.kandu.dto.UsuarioAdminAtualizacaoDTO;
import br.com.kandu.dto.UsuarioResponseDTO;
import br.com.kandu.entity.Usuario;
import br.com.kandu.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios") // Rota base para gerenciamento de usuários
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint para administradores de empresa (DIRETOR, GESTOR) ou ADM do sistema
     * listarem usuários de sua respectiva empresa.
     * ADM do sistema poderia ter um parâmetro opcional para listar de uma empresa específica.
     *
     * @return Lista de UsuarioResponseDTO.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADM', 'DIRETOR', 'GESTOR')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosDaEmpresa() {
        // O UsuarioService internamente já filtra pela empresa do admin logado (exceto para ADM_SISTEMA)
        List<Usuario> usuarios = usuarioService.listarUsuariosDaEmpresa();
        List<UsuarioResponseDTO> dtos = usuarios.stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(),
                        u.getNomeCompleto(),
                        u.getNomeUsuario(),
                        u.getEmail(),
                        u.getNivelHierarquia(),
                        u.getFuncao(),
                        u.isAtivo(),
                        u.getEmpresa().getId(),
                        u.getEmpresa().getNome()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Endpoint para um admin de empresa (DIRETOR, GESTOR) ou ADM do sistema
     * criar um novo usuário em sua respectiva empresa.
     *
     * @param dto Dados do novo usuário.
     * @return UsuarioResponseDTO do usuário criado.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADM', 'DIRETOR', 'GESTOR')")
    public ResponseEntity<?> criarUsuarioPorAdmin(@RequestBody UsuarioAdminCriacaoDTO dto) {
        try {
            Usuario novoUsuario = usuarioService.criarUsuarioPorAdmin(dto);
            UsuarioResponseDTO responseDTO = new UsuarioResponseDTO(
                    novoUsuario.getId(), novoUsuario.getNomeCompleto(), novoUsuario.getNomeUsuario(),
                    novoUsuario.getEmail(), novoUsuario.getNivelHierarquia(), novoUsuario.getFuncao(),
                    novoUsuario.isAtivo(), novoUsuario.getEmpresa().getId(), novoUsuario.getEmpresa().getNome()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Logar e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao criar usuário: " + e.getMessage());
        }
    }

    /**
     * Endpoint para buscar um usuário específico por ID.
     * Acessível por ADM, DIRETOR, GESTOR (o serviço validará se o usuário pertence à empresa do admin).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADM', 'DIRETOR', 'GESTOR')")
    public ResponseEntity<?> buscarUsuarioPorId(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioService.buscarUsuarioPorIdNaEmpresa(id);
            UsuarioResponseDTO responseDTO = new UsuarioResponseDTO(
                    usuario.getId(), usuario.getNomeCompleto(), usuario.getNomeUsuario(),
                    usuario.getEmail(), usuario.getNivelHierarquia(), usuario.getFuncao(),
                    usuario.isAtivo(), usuario.getEmpresa().getId(), usuario.getEmpresa().getNome()
            );
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


    /**
     * Endpoint para um admin de empresa (DIRETOR, GESTOR) ou ADM do sistema
     * atualizar um usuário existente em sua respectiva empresa.
     *
     * @param id  ID do usuário a ser atualizado.
     * @param dto Dados para atualização.
     * @return UsuarioResponseDTO do usuário atualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADM', 'DIRETOR', 'GESTOR')")
    public ResponseEntity<?> atualizarUsuarioPorAdmin(@PathVariable Long id, @RequestBody UsuarioAdminAtualizacaoDTO dto) {
        try {
            Usuario usuarioAtualizado = usuarioService.atualizarUsuarioPorAdmin(id, dto);
            UsuarioResponseDTO responseDTO = new UsuarioResponseDTO(
                    usuarioAtualizado.getId(), usuarioAtualizado.getNomeCompleto(), usuarioAtualizado.getNomeUsuario(),
                    usuarioAtualizado.getEmail(), usuarioAtualizado.getNivelHierarquia(), usuarioAtualizado.getFuncao(),
                    usuarioAtualizado.isAtivo(), usuarioAtualizado.getEmpresa().getId(), usuarioAtualizado.getEmpresa().getNome()
            );
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException | SecurityException e) {
            // IllegalArgument pode ser por usuário não encontrado ou dados inválidos na atualização
            // SecurityException por tentativa de ação não permitida hierarquicamente
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Logar e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao atualizar usuário: " + e.getMessage());
        }
    }
}
