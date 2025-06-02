package br.com.kandu.entity;

import br.com.kandu.enums.NivelHierarquia;
import jakarta.persistence.*; // Pacote para JPA com Jakarta EE
import lombok.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = { // Garante unicidade a nível de banco
        @UniqueConstraint(columnNames = "nome_usuario"),
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id") // Considera apenas o ID para equals e hashCode
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID gerado pelo banco (auto-incremento)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "nome_usuario", nullable = false, unique = true, length = 50)
    private String nomeUsuario;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255) // Tamanho para acomodar hashes de senha
    private String senha;

    @Enumerated(EnumType.STRING) // Armazena o nome do enum ("ADM", "COMUM") no banco
    @Column(name = "nivel_hierarquia", nullable = false, length = 20)
    private NivelHierarquia nivelHierarquia;

    @Column(nullable = false)
    private boolean ativo = true; // Por padrão, novos usuários são ativos

    // Relacionamento com Empresa será adicionado no Marco 2, Issue #6
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "empresa_id") // nullable = false será definido quando Empresa for integrada
    // private Empresa empresa;
}