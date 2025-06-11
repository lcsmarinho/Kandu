// src/main/java/br/com/kandu/entity/LogHistoricoOS.java
package br.com.kandu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade para registrar o histórico de eventos de uma Ordem de Serviço.
 * Por que foi implementado: Para fornecer uma trilha de auditoria completa,
 * permitindo que os gestores e utilizadores consultem todas as alterações
 * e eventos importantes que ocorreram durante o ciclo de vida de uma OS.
 */
@Entity
@Table(name = "log_historico_os")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class LogHistoricoOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemDeServico ordemDeServico;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * O utilizador que realizou a ação que gerou este log.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsavel_acao_id", nullable = false)
    private Usuario usuarioResponsavelAcao;

    @Column(nullable = false, length = 255)
    private String descricaoAcao;

    /**
     * Armazena o estado anterior de campos relevantes em formato JSON. Opcional.
     * Ex: {"status": "ABERTA", "responsavelId": null}
     */
    @Column(columnDefinition = "TEXT")
    private String dadosAntigos;

    /**
     * Armazena o novo estado de campos relevantes em formato JSON. Opcional.
     * Ex: {"status": "EM_ANDAMENTO", "responsavelId": 101}
     */
    @Column(columnDefinition = "TEXT")
    private String dadosNovos;
}
