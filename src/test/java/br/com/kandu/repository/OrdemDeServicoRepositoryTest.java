// src/test/java/br/com/kandu/repository/OrdemDeServicoRepositoryTest.java
package br.com.kandu.repository;

import br.com.kandu.entity.Empresa;
import br.com.kandu.entity.OrdemDeServico;
import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import br.com.kandu.enums.StatusOS; // <-- IMPORT QUE ESTAVA FALTANDO
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class OrdemDeServicoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    private Empresa empresaTeste;
    private Usuario usuarioCriador;
    private Usuario usuarioResponsavel;

    @BeforeEach
    void setUp() {
        // Criar entidades pré-requisito para o teste
        empresaTeste = entityManager.persistAndFlush(Empresa.builder()
                .nome("Empresa para OS Teste")
                .codigoInscricao("OSTESTE01")
                .build());

        usuarioCriador = entityManager.persistAndFlush(Usuario.builder()
                .nomeCompleto("Criador de OS")
                .nomeUsuario("criador.os")
                .email("criador.os@example.com")
                .senha("senhaHasheada")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .empresa(empresaTeste)
                .build());

        usuarioResponsavel = entityManager.persistAndFlush(Usuario.builder()
                .nomeCompleto("Responsável pela OS")
                .nomeUsuario("responsavel.os")
                .email("responsavel.os@example.com")
                .senha("senhaHasheada2")
                .nivelHierarquia(NivelHierarquia.SUPERVISOR)
                .empresa(empresaTeste)
                .build());
    }

    @Test
    @DisplayName("Deve persistir uma Ordem de Serviço com todos os campos e relacionamentos")
    void devePersistirOrdemDeServicoComSucesso() {
        // Cenário
        OrdemDeServico novaOS = OrdemDeServico.builder()
                .titulo("Instalar novo servidor na filial centro")
                .descricao("Detalhes completos da instalação, incluindo especificações de hardware e software.")
                .local("Filial Centro - Sala de Servidores")
                .prazo(LocalDate.now().plusWeeks(2))
                .status(StatusOS.ABERTA) // Agora o compilador sabe o que é StatusOS
                .requisitos("Servidor Dell PowerEdge, 2 cabos de rede, acesso à sala.")
                .projetoPrivado(false)
                .empresa(empresaTeste)
                .criador(usuarioCriador)
                .responsavel(usuarioResponsavel)
                .build();
        // A dataCadastro será gerada automaticamente pela anotação @CreationTimestamp

        // Ação
        OrdemDeServico osSalva = ordemDeServicoRepository.save(novaOS);
        entityManager.flush();
        entityManager.clear();

        // Verificação
        OrdemDeServico osDoBanco = entityManager.find(OrdemDeServico.class, osSalva.getId());

        assertThat(osDoBanco).isNotNull();
        assertThat(osDoBanco.getId()).isEqualTo(osSalva.getId());
        assertThat(osDoBanco.getTitulo()).isEqualTo("Instalar novo servidor na filial centro");
        assertThat(osDoBanco.getStatus()).isEqualTo(StatusOS.ABERTA);
        assertThat(osDoBanco.getDataCadastro()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(osDoBanco.getEmpresa().getId()).isEqualTo(empresaTeste.getId());
        assertThat(osDoBanco.getCriador().getId()).isEqualTo(usuarioCriador.getId());
        assertThat(osDoBanco.getResponsavel().getId()).isEqualTo(usuarioResponsavel.getId());
    }

    @Test
    @DisplayName("Deve persistir uma Ordem de Serviço sem um responsável opcional")
    void devePersistirOrdemDeServicoSemResponsavel() {
        // Cenário
        OrdemDeServico novaOS = OrdemDeServico.builder()
                .titulo("Verificar problema na impressora")
                .descricao("A impressora do segundo andar não está respondendo.")
                .local("Segundo Andar")
                .prazo(LocalDate.now().plusDays(3))
                .status(StatusOS.ABERTA) // Agora o compilador sabe o que é StatusOS
                .empresa(empresaTeste)
                .criador(usuarioCriador)
                .responsavel(null)
                .build();

        // Ação
        OrdemDeServico osSalva = ordemDeServicoRepository.save(novaOS);
        entityManager.flush();
        entityManager.clear();

        // Verificação
        OrdemDeServico osDoBanco = entityManager.find(OrdemDeServico.class, osSalva.getId());

        assertThat(osDoBanco).isNotNull();
        assertThat(osDoBanco.getResponsavel()).isNull();
    }
}
