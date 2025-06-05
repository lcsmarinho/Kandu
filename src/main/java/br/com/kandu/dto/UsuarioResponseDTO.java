// src/main/java/br/com/kandu/dto/UsuarioResponseDTO.java
package br.com.kandu.dto;

import br.com.kandu.enums.NivelHierarquia;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String nomeCompleto;
    private String nomeUsuario;
    private String email;
    private NivelHierarquia nivelHierarquia;
    private String funcao; // Adicionaremos este campo à entidade Usuario
    private boolean ativo;
    private Long empresaId;
    private String empresaNome; // Para facilitar a exibição
}
