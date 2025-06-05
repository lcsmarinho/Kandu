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
// Importe exceções customizadas se criá-las
// import br.com.kandu.exception.PermissaoNegadaException;
// import br.com.kandu.exception.RecursoNaoEncontradoException;
// import br.com.kandu.exception.ValidacaoException;
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
    private final EmpresaService empresaService; // Para buscar empresa do admin logado

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

    // --- Método de Cadastro Público (da Issue #7) ---
    @Transactional
    public Usuario cadastrarUsuario(UsuarioCadastroDTO dto) {
        if (dto.getNomeUsuario() == null || dto.getNomeUsuario().trim().isEmpty() || /* ... outras validações ... */
                dto.getCodigoInscricao() == null || dto.getCodigoInscricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Dados de cadastro inválidos...");
        }
        Empresa empresaAssociada = empresaService.findByCodigoInscricao(dto.getCodigoInscricao());

        // Validação de unicidade GLOBAL por enquanto, pode ser mudada para POR EMPRESA se necessário
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

    // --- Método de Autenticação (da Issue #4) ---
    public String autenticar(LoginDTO loginDTO) {
        // ... (código existente) ...
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

    // --- Novos Métodos para Gerenciamento por Admin da Empresa (Issue #9) ---

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String nomeUsuarioAutenticado = authentication.getName();
        return usuarioRepository.findByNomeUsuario(nomeUsuarioAutenticado)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no banco de dados."));
        // Idealmente, UsernameNotFoundException ou uma exceção customizada.
    }

    /**
     * Lista usuários da empresa do administrador logado.
     * ADM do sistema pode listar de qualquer empresa (se um empresaId for fornecido como parâmetro,
     * mas para este escopo, vamos focar no admin da empresa listando os seus).
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosDaEmpresa() {
        Usuario adminLogado = getUsuarioAutenticado();
        // ADM do sistema pode ter uma lógica diferente (ex: receber empresaId como parâmetro)
        // if (adminLogado.getNivelHierarquia() == NivelHierarquia.ADM) { /* lógica para ADM */ }
        return usuarioRepository.findByEmpresaId(adminLogado.getEmpresa().getId());
    }

    /**
     * Admin da empresa (DIRETOR/GESTOR) cria um novo usuário para sua própria empresa.
     */
    @Transactional
    public Usuario criarUsuarioPorAdmin(UsuarioAdminCriacaoDTO dto) {
        Usuario adminLogado = getUsuarioAutenticado();
        Empresa empresaDoAdmin = adminLogado.getEmpresa();

        // Validações de dados do DTO
        if (dto.getNomeUsuario() == null || dto.getNomeUsuario().trim().isEmpty() || /* ... */ dto.getSenha() == null || dto.getSenha().isEmpty()) {
            throw new IllegalArgumentException("Dados para criação de usuário inválidos.");
        }

        // Validação de permissão hierárquica
        validarPermissaoHierarquica(adminLogado.getNivelHierarquia(), dto.getNivelHierarquia(), false);

        // Validação de unicidade DENTRO DA EMPRESA
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
        novoUsuario.setAtivo(true); // Por padrão, admin cria usuário ativo
        novoUsuario.setEmpresa(empresaDoAdmin);

        return usuarioRepository.save(novoUsuario);
    }

    /**
     * Admin da empresa (DIRETOR/GESTOR) atualiza um usuário da sua própria empresa.
     */
    @Transactional
    public Usuario atualizarUsuarioPorAdmin(Long usuarioIdParaAtualizar, UsuarioAdminAtualizacaoDTO dto) {
        Usuario adminLogado = getUsuarioAutenticado();
        Empresa empresaDoAdmin = adminLogado.getEmpresa();

        Usuario usuarioParaAtualizar = usuarioRepository.findById(usuarioIdParaAtualizar)
                .orElseThrow(() -> new IllegalArgumentException("Usuário a ser atualizado não encontrado com ID: " + usuarioIdParaAtualizar));
        // .orElseThrow(() -> new RecursoNaoEncontradoException(...));


        // Verifica se o usuário a ser atualizado pertence à mesma empresa do admin
        if (!usuarioParaAtualizar.getEmpresa().getId().equals(empresaDoAdmin.getId()) && adminLogado.getNivelHierarquia() != NivelHierarquia.ADM) {
            throw new SecurityException("Operação não permitida: Usuário não pertence à sua empresa.");
            // throw new PermissaoNegadaException(...);
        }

        // Validação de permissão hierárquica para atualização
        // Admin não pode rebaixar a si mesmo ou promover alguém para seu nível ou acima (exceto ADM do sistema)
        validarPermissaoHierarquica(adminLogado.getNivelHierarquia(), dto.getNivelHierarquia(), true, usuarioParaAtualizar.getNivelHierarquia(), adminLogado.getId().equals(usuarioIdParaAtualizar));


        if (dto.getNivelHierarquia() != null) {
            usuarioParaAtualizar.setNivelHierarquia(dto.getNivelHierarquia());
        }
        if (dto.getFuncao() != null) {
            usuarioParaAtualizar.setFuncao(dto.getFuncao().trim());
        }
        if (dto.getAtivo() != null) {
            // Lógica adicional: admin não pode desativar a si mesmo (exceto ADM do sistema)
            if (adminLogado.getId().equals(usuarioIdParaAtualizar) && !dto.getAtivo() && adminLogado.getNivelHierarquia() != NivelHierarquia.ADM) {
                throw new IllegalArgumentException("Você não pode desativar sua própria conta.");
                // throw new OperacaoNaoPermitidaException(...);
            }
            usuarioParaAtualizar.setAtivo(dto.getAtivo());
        }

        return usuarioRepository.save(usuarioParaAtualizar);
    }


    /**
     * Valida se o admin logado pode atribuir/modificar o nível hierárquico alvo.
     *
     * @param adminNivel Nível do administrador realizando a ação.
     * @param alvoNivel Nível que está sendo atribuído/modificado.
     * @param isUpdate true se for uma atualização, false se for criação.
     * @param alvoNivelAtual (Opcional) Nível atual do usuário alvo, relevante para atualizações.
     * @param isAdminModificandoASiMesmo true se o admin está tentando modificar a própria conta.
     */
    private void validarPermissaoHierarquica(NivelHierarquia adminNivel, NivelHierarquia alvoNivel,
                                             boolean isUpdate, NivelHierarquia alvoNivelAtual,
                                             boolean isAdminModificandoASiMesmo) {
        if (adminNivel == NivelHierarquia.ADM) {
            return; // ADM do sistema pode tudo (quase)
        }

        // Nenhum admin (exceto ADM sistema) pode criar/promover para ADM
        if (alvoNivel == NivelHierarquia.ADM) {
            throw new SecurityException("Apenas administradores do sistema podem definir o nível ADM.");
            // throw new PermissaoNegadaException(...);
        }

        // Ninguém pode se promover para um nível superior ao seu
        if (isAdminModificandoASiMesmo && alvoNivel.ordinal() > adminNivel.ordinal()) {
            throw new SecurityException("Você não pode se promover para um nível hierárquico superior.");
        }

        // Um admin não pode definir um nível hierárquico igual ou superior ao seu para outro usuário
        if (!isAdminModificandoASiMesmo && alvoNivel.ordinal() >= adminNivel.ordinal()) {
            throw new SecurityException("Você não pode atribuir um nível hierárquico igual ou superior ao seu para outros usuários.");
            // throw new PermissaoNegadaException(...);
        }

        // Regras específicas de quem pode gerenciar quem:
        // DIRETOR pode gerenciar GESTOR, SUPERVISOR, COMUM
        // GESTOR pode gerenciar SUPERVISOR, COMUM
        // SUPERVISOR pode gerenciar COMUM (será implementado em outra issue, mas já pensando)
        if (adminNivel == NivelHierarquia.DIRETOR && alvoNivel == NivelHierarquia.DIRETOR) {
            throw new SecurityException("Diretor não pode criar ou promover outro Diretor.");
        }
        if (adminNivel == NivelHierarquia.GESTOR && (alvoNivel == NivelHierarquia.GESTOR || alvoNivel == NivelHierarquia.DIRETOR)) {
            throw new SecurityException("Gestor não pode criar ou promover para Gestor ou Diretor.");
        }
        // Adicionar regra para SUPERVISOR quando implementado

        // Se for atualização, não pode rebaixar alguém de um nível superior ao seu (se o admin não for ADM)
        if (isUpdate && alvoNivelAtual != null && alvoNivelAtual.ordinal() > adminNivel.ordinal()) {
            throw new SecurityException("Você não pode alterar o nível hierárquico de um usuário que está acima de você.");
        }
    }
    // Sobrecarga para criação
    private void validarPermissaoHierarquica(NivelHierarquia adminNivel, NivelHierarquia alvoNivel, boolean isCriacao) {
        validarPermissaoHierarquica(adminNivel, alvoNivel, !isCriacao, null, false);
    }


    public Usuario buscarUsuarioPorIdNaEmpresa(Long usuarioId) {
        Usuario adminLogado = getUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + usuarioId));

        if (adminLogado.getNivelHierarquia() != NivelHierarquia.ADM &&
                !usuario.getEmpresa().getId().equals(adminLogado.getEmpresa().getId())) {
            throw new SecurityException("Acesso negado ao usuário de outra empresa.");
        }
        return usuario;
    }
}
