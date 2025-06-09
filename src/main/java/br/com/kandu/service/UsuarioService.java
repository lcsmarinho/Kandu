// src/main/java/br/com/kandu/service/UsuarioService.java
package br.com.kandu.service;

import br.com.kandu.dto.LoginDTO;
import br.com.kandu.dto.UsuarioAdminCriacaoDTO;
import br.com.kandu.dto.UsuarioAdminAtualizacaoDTO;
import br.com.kandu.dto.UsuarioCadastroDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.repository.UsuarioRepository;
import br.com.kandu.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmpresaService empresaService;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          EmpresaService empresaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.empresaService = empresaService;
    }

    @Transactional
    public Usuario cadastrarUsuario(UsuarioCadastroDTO dto) {
        if (dto.getNomeUsuario() == null || dto.getNomeUsuario().trim().isEmpty() ||
                dto.getEmail() == null || dto.getEmail().trim().isEmpty() ||
                dto.getSenha() == null || dto.getSenha().isEmpty() ||
                dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty() ||
                dto.getCodigoInscricao() == null || dto.getCodigoInscricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Dados de cadastro inválidos. Todos os campos, incluindo código de inscrição, são obrigatórios.");
        }

        Empresa empresaAssociada = empresaService.findByCodigoInscricao(dto.getCodigoInscricao());

        if (usuarioRepository.existsByNomeUsuario(dto.getNomeUsuario().trim())) {
            throw new IllegalArgumentException("Nome de usuário '" + dto.getNomeUsuario().trim() + "' já está em uso.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail().trim())) {
            throw new IllegalArgumentException("E-mail '" + dto.getEmail().trim() + "' já está em uso.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNomeCompleto(dto.getNomeCompleto().trim());
        novoUsuario.setNomeUsuario(dto.getNomeUsuario().trim());
        novoUsuario.setEmail(dto.getEmail().trim());
        novoUsuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoUsuario.setNivelHierarquia(NivelHierarquia.COMUM);
        novoUsuario.setAtivo(true);
        novoUsuario.setEmpresa(empresaAssociada);
        return usuarioRepository.save(novoUsuario);
    }

    public String autenticar(LoginDTO loginDTO) {
        if (loginDTO.getNomeUsuario() == null || loginDTO.getNomeUsuario().trim().isEmpty() ||
                loginDTO.getSenha() == null || loginDTO.getSenha().isEmpty()) {
            throw new BadCredentialsException("Nome de usuário e senha são obrigatórios.");
        }
        Usuario usuario = usuarioRepository.findByNomeUsuario(loginDTO.getNomeUsuario().trim())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado ou credenciais inválidas."));

        if (!usuario.isAtivo()) {
            throw new BadCredentialsException("Usuário '" + loginDTO.getNomeUsuario() + "' está inativo.");
        }
        if (!passwordEncoder.matches(loginDTO.getSenha(), usuario.getSenha())) {
            throw new BadCredentialsException("Usuário não encontrado ou credenciais inválidas.");
        }
        return jwtTokenProvider.generateToken(usuario);
    }

    public Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Nenhum utilizador autenticado encontrado no contexto de segurança.");
        }
        String nomeUsuarioAutenticado = authentication.getName();
        return usuarioRepository.findByNomeUsuario(nomeUsuarioAutenticado)
                .orElseThrow(() -> new IllegalStateException("Utilizador autenticado '" + nomeUsuarioAutenticado + "' não foi encontrado no banco de dados."));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosDaEmpresa() {
        Usuario adminLogado = getUsuarioAutenticado();
        if (adminLogado.getNivelHierarquia() == NivelHierarquia.ADM) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.findByEmpresaId(adminLogado.getEmpresa().getId());
    }

    @Transactional
    public Usuario criarUsuarioPorAdmin(UsuarioAdminCriacaoDTO dto) {
        Usuario adminLogado = getUsuarioAutenticado();
        Empresa empresaDoAdmin = adminLogado.getEmpresa();

        if (dto.getNomeUsuario() == null || dto.getNomeUsuario().trim().isEmpty() || dto.getSenha() == null || dto.getSenha().isEmpty()) {
            throw new IllegalArgumentException("Dados para criação de usuário inválidos.");
        }

        validarPermissaoHierarquica(adminLogado.getNivelHierarquia(), dto.getNivelHierarquia(), false, null, false);

        if (usuarioRepository.existsByNomeUsuarioAndEmpresaId(dto.getNomeUsuario().trim(), empresaDoAdmin.getId())) {
            throw new IllegalArgumentException("Nome de usuário '" + dto.getNomeUsuario().trim() + "' já existe nesta empresa.");
        }
        if (usuarioRepository.existsByEmailAndEmpresaId(dto.getEmail().trim(), empresaDoAdmin.getId())) {
            throw new IllegalArgumentException("E-mail '" + dto.getEmail().trim() + "' já existe nesta empresa.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNomeCompleto(dto.getNomeCompleto().trim());
        novoUsuario.setNomeUsuario(dto.getNomeUsuario().trim());
        novoUsuario.setEmail(dto.getEmail().trim());
        novoUsuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoUsuario.setNivelHierarquia(dto.getNivelHierarquia());
        novoUsuario.setFuncao(dto.getFuncao() != null ? dto.getFuncao().trim() : null);
        novoUsuario.setAtivo(true);
        novoUsuario.setEmpresa(empresaDoAdmin);

        return usuarioRepository.save(novoUsuario);
    }

    @Transactional
    public Usuario atualizarUsuarioPorAdmin(Long usuarioIdParaAtualizar, UsuarioAdminAtualizacaoDTO dto) {
        Usuario adminLogado = getUsuarioAutenticado();
        Usuario usuarioParaAtualizar = usuarioRepository.findById(usuarioIdParaAtualizar)
                .orElseThrow(() -> new IllegalArgumentException("Usuário a ser atualizado não encontrado com ID: " + usuarioIdParaAtualizar));

        if (!usuarioParaAtualizar.getEmpresa().getId().equals(adminLogado.getEmpresa().getId()) && adminLogado.getNivelHierarquia() != NivelHierarquia.ADM) {
            throw new SecurityException("Operação não permitida: Usuário não pertence à sua empresa.");
        }

        validarPermissaoHierarquica(adminLogado.getNivelHierarquia(), dto.getNivelHierarquia(), true, usuarioParaAtualizar.getNivelHierarquia(), adminLogado.getId().equals(usuarioIdParaAtualizar));

        if (dto.getNivelHierarquia() != null) {
            usuarioParaAtualizar.setNivelHierarquia(dto.getNivelHierarquia());
        }
        if (dto.getFuncao() != null) {
            usuarioParaAtualizar.setFuncao(dto.getFuncao().trim());
        }
        if (dto.getAtivo() != null) {
            if (adminLogado.getId().equals(usuarioIdParaAtualizar) && !dto.getAtivo() && adminLogado.getNivelHierarquia() != NivelHierarquia.ADM) {
                throw new IllegalArgumentException("Você não pode desativar sua própria conta.");
            }
            usuarioParaAtualizar.setAtivo(dto.getAtivo());
        }

        return usuarioRepository.save(usuarioParaAtualizar);
    }

    private void validarPermissaoHierarquica(NivelHierarquia adminNivel, NivelHierarquia alvoNivel, boolean isUpdate, NivelHierarquia alvoNivelAtual, boolean isAdminModificandoASiMesmo) {
        if (adminNivel == NivelHierarquia.ADM) return;
        if (alvoNivel == NivelHierarquia.ADM) throw new SecurityException("Apenas administradores do sistema podem definir o nível ADM.");
        if (isAdminModificandoASiMesmo && alvoNivel.ordinal() > adminNivel.ordinal()) throw new SecurityException("Você não pode se promover para um nível hierárquico superior.");
        if (!isAdminModificandoASiMesmo && alvoNivel.ordinal() >= adminNivel.ordinal()) throw new SecurityException("Você não pode atribuir um nível hierárquico igual ou superior ao seu para outros usuários.");
        if (isUpdate && alvoNivelAtual != null && alvoNivelAtual.ordinal() > adminNivel.ordinal()) throw new SecurityException("Você não pode alterar o nível hierárquico de um usuário que está acima de você.");
        if (adminNivel == NivelHierarquia.DIRETOR && alvoNivel == NivelHierarquia.DIRETOR) throw new SecurityException("Diretor não pode criar ou promover outro Diretor.");
        if (adminNivel == NivelHierarquia.GESTOR && (alvoNivel == NivelHierarquia.GESTOR || alvoNivel == NivelHierarquia.DIRETOR)) throw new SecurityException("Gestor não pode criar ou promover para Gestor ou Diretor.");
    }

    private void validarPermissaoHierarquica(NivelHierarquia adminNivel, NivelHierarquia alvoNivel, boolean isCriacao) {
        validarPermissaoHierarquica(adminNivel, alvoNivel, !isCriacao, null, false);
    }

    public Usuario buscarUsuarioPorIdNaEmpresa(Long usuarioId) {
        Usuario adminLogado = getUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + usuarioId));

        if (adminLogado.getNivelHierarquia() != NivelHierarquia.ADM && !usuario.getEmpresa().getId().equals(adminLogado.getEmpresa().getId())) {
            throw new SecurityException("Acesso negado ao usuário de outra empresa.");
        }
        return usuario;
    }
}
