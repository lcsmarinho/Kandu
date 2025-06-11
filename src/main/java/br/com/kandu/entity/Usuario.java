// src/main/java/br/com/kandu/entity/Usuario.java
package br.com.kandu.entity;

import br.com.kandu.enums.NivelHierarquia;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "nome_usuario", name = "uk_usuario_nome_usuario"),
        @UniqueConstraint(columnNames = "email", name = "uk_usuario_email")
        // Para unicidade por empresa, considerar:
        // @UniqueConstraint(columnNames = {"nome_usuario", "empresa_id"}, name = "uk_usuario_nome_empresa"),
        // @UniqueConstraint(columnNames = {"email", "empresa_id"}, name = "uk_usuario_email_empresa")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "nome_usuario", nullable = false, length = 50)
    private String nomeUsuario;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_hierarquia", nullable = false, length = 20)
    private NivelHierarquia nivelHierarquia;

    @Column(length = 100) // Novo campo
    private String funcao; // Cargo ou função do usuário

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToMany(mappedBy = "usuario")
    private List<ParticipanteOS> participacoesEmOS;
}
