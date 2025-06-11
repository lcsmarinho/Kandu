// src/main/java/br/com/kandu/entity/ParticipanteOS.java
package br.com.kandu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade de junção que representa a participação de um Usuário em uma Ordem de Serviço.
 * Por que foi implementado: Para permitir que múltiplos utilizadores sejam associados a uma OS como
 * participantes. Esta abordagem de entidade de junção foi escolhida em vez de um @ManyToMany simples
 * para podermos adicionar metadados à relação, como a data de inclusão e de saída.
 */
@Entity
@Table(name = "participantes_os", uniqueConstraints = {
        // Garante que um utilizador não pode ser adicionado duas vezes à mesma OS
        @UniqueConstraint(columnNames = {"ordem_servico_id", "usuario_id"}, name = "uk_participante_os")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ParticipanteOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemDeServico ordemDeServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "data_inclusao", nullable = false, updatable = false)
    private LocalDateTime dataInclusao;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida; // Preenchido quando o participante é removido da OS

}
