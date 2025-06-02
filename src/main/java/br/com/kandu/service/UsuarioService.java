// src/main/java/br/com/kandu/service/UsuarioService.java
package br.com.kandu.service;

import br.com.kandu.dto.LoginDTO; // Novo DTO
import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.UsuarioRepository;
import br.com.kandu.security.jwt.JwtTokenProvider; // Novo import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException; // Exceção padrão do Spring Security
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider; // Nova dependência

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) { // Injetar JwtTokenProvider
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional // Garante que a operação seja atômica
    public Usuario cadastrarUsuario(UsuarioCadastroDTO dto) {
        // ... (código de cadastro da Issue #3) ...
        if (dto.getNomeUsuario() == null || dto.getNomeUsuario().trim().isEmpty() ||
                dto.getEmail() == null || dto.getEmail().trim().isEmpty() ||
                dto.getSenha() == null || dto.getSenha().isEmpty() ||
                dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("Dados de cadastro inválidos. Todos os campos são obrigatórios.");
        }

        if (usuarioRepository.existsByNomeUsuario(dto.getNomeUsuario().trim())) {
            throw new IllegalArgumentException("Nome de usuário '" + dto.getNomeUsuario() + "' já está em uso.");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail().trim())) {
            throw new IllegalArgumentException("E-mail '" + dto.getEmail() + "' já está em uso.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNomeCompleto(dto.getNomeCompleto().trim());
        novoUsuario.setNomeUsuario(dto.getNomeUsuario().trim());
        novoUsuario.setEmail(dto.getEmail().trim());
        novoUsuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoUsuario.setNivelHierarquia(NivelHierarquia.COMUM);
        novoUsuario.setAtivo(true);

        return usuarioRepository.save(novoUsuario);
    }

    /**
     * Autentica um usuário com base nas credenciais fornecidas e gera um token JWT.
     *
     * @param loginDTO DTO contendo nome de usuário e senha.
     * @return String contendo o token JWT.
     * @throws BadCredentialsException Se as credenciais forem inválidas ou usuário inativo.
     */
    public String autenticar(LoginDTO loginDTO) {
        if (loginDTO.getNomeUsuario() == null || loginDTO.getNomeUsuario().trim().isEmpty() ||
                loginDTO.getSenha() == null || loginDTO.getSenha().isEmpty()) {
            throw new BadCredentialsException("Nome de usuário e senha são obrigatórios.");
        }

        // Busca o usuário pelo nome de usuário (ou email, se preferir)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario().trim());

        if (usuarioOpt.isEmpty()) {
            throw new BadCredentialsException("Usuário não encontrado ou credenciais inválidas.");
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.isAtivo()) {
            throw new BadCredentialsException("Usuário '" + loginDTO.getNomeUsuario() + "' está inativo.");
        }

        // Compara a senha fornecida com a senha hasheada armazenada
        if (!passwordEncoder.matches(loginDTO.getSenha(), usuario.getSenha())) {
            throw new BadCredentialsException("Usuário não encontrado ou credenciais inválidas."); // Mensagem genérica por segurança
        }

        // Se as credenciais são válidas e o usuário está ativo, gera o token
        return jwtTokenProvider.generateToken(usuario);
    }
}