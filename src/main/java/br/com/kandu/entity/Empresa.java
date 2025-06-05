package br.com.kandu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List; // Será usado para relacionamentos futuros (ex: com Usuario, OS, etc.)

@Entity
@Table(name = "empresas", uniqueConstraints = {
        @UniqueConstraint(columnNames = "codigo_inscricao", name = "uk_empresa_codigo_inscricao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id") // Considera apenas o ID para equals e hashCode
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * Código único utilizado pelos funcionários para se registrarem na empresa correta.
     * Deve ser único globalmente ou por sistema.
     */
    @Column(name = "codigo_inscricao", nullable = false, unique = true, length = 20)
    private String codigoInscricao;

    // Relacionamento com Usuario (Uma Empresa para Muitos Usuários)
    // O mappedBy indica que a entidade Usuario é a dona do relacionamento.
    // CascadeType.ALL pode ser perigoso aqui, especialmente para remoção.
    // Vamos gerenciar o ciclo de vida dos usuários separadamente por enquanto.
    // FetchType.LAZY é bom para performance.
    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY) // cascade = CascadeType.PERSIST ?
    private List<Usuario> usuarios;

    // Outros relacionamentos (OS, Projetos Kanban) serão adicionados depois.
}
