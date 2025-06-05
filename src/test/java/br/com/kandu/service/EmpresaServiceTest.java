// src/test/java/br/com/kandu/service/EmpresaServiceTest.java
package br.com.kandu.service;

import br.com.kandu.dto.EmpresaDTO;
import br.com.kandu.entity.Empresa;
import br.com.kandu.repository.EmpresaRepository;
import br.com.kandu.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioRepository usuarioRepository; // Mock para o teste de deleção

    @InjectMocks
    private EmpresaService empresaService;

    private EmpresaDTO empresaDTO;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresaDTO = new EmpresaDTO();
        empresaDTO.setNome("Nova Empresa Teste");
        empresaDTO.setCodigoInscricao("NET123");

        empresa = Empresa.builder()
                .id(1L)
                .nome("Nova Empresa Teste")
                .codigoInscricao("NET123")
                .build();
    }

    @Test
    @DisplayName("Deve criar empresa com sucesso")
    void deveCriarEmpresaComSucesso() {
        when(empresaRepository.existsByCodigoInscricao(empresaDTO.getCodigoInscricao())).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresa);

        Empresa resultado = empresaService.criarEmpresa(empresaDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo(empresaDTO.getNome());
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar empresa com código de inscrição existente")
    void deveLancarExcecaoAoCriarEmpresaComCodigoExistente() {
        when(empresaRepository.existsByCodigoInscricao(empresaDTO.getCodigoInscricao())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            empresaService.criarEmpresa(empresaDTO);
        });
        assertThat(exception.getMessage()).contains("já está em uso");
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("Deve buscar empresa por ID com sucesso")
    void deveBuscarEmpresaPorIdComSucesso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        Empresa resultado = empresaService.buscarEmpresaPorId(1L);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar empresa por ID inexistente")
    void deveLancarExcecaoAoBuscarEmpresaPorIdInexistente() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            empresaService.buscarEmpresaPorId(99L);
        });
    }

    @Test
    @DisplayName("Deve listar todas as empresas")
    void deveListarTodasEmpresas() {
        when(empresaRepository.findAll()).thenReturn(Collections.singletonList(empresa));
        List<Empresa> resultado = empresaService.listarTodasEmpresas();
        assertThat(resultado).isNotEmpty();
        assertThat(resultado.get(0).getNome()).isEqualTo(empresa.getNome());
    }

    @Test
    @DisplayName("Deve atualizar empresa com sucesso")
    void deveAtualizarEmpresaComSucesso() {
        EmpresaDTO dtoAtualizacao = new EmpresaDTO();
        dtoAtualizacao.setNome("Empresa Atualizada");
        // Não vamos testar atualização de código de inscrição aqui para manter simples

        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Retorna o objeto passado para save

        Empresa resultado = empresaService.atualizarEmpresa(1L, dtoAtualizacao);

        assertThat(resultado.getNome()).isEqualTo("Empresa Atualizada");
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("Deve deletar empresa com sucesso se não houver usuários")
    void deveDeletarEmpresaComSucesso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(usuarioRepository.existsByEmpresaId(1L)).thenReturn(false); // Nenhum usuário vinculado
        doNothing().when(empresaRepository).delete(empresa);

        empresaService.deletarEmpresa(1L);

        verify(empresaRepository).delete(empresa);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar empresa com usuários vinculados")
    void deveLancarExcecaoAoDeletarEmpresaComUsuarios() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(usuarioRepository.existsByEmpresaId(1L)).thenReturn(true); // Existem usuários vinculados

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            empresaService.deletarEmpresa(1L);
        });
        assertThat(exception.getMessage()).contains("existem usuários vinculados");
        verify(empresaRepository, never()).delete(any(Empresa.class));
    }
}
