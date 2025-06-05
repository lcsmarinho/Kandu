// src/main/java/br/com/kandu/dto/UsuarioAdminAtualizacaoDTO.java
package br.com.kandu.dto;

import br.com.kandu.enums.NivelHierarquia;
import lombok.Getter;
import lombok.Setter;
// import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class UsuarioAdminAtualizacaoDTO {
    // Não permitir alterar nomeCompleto, nomeUsuario, email por este DTO para simplificar.
    // A senha seria alterada por um endpoint/fluxo específico de reset/mudança de senha.
    // @NotNull
    private NivelHierarquia nivelHierarquia;
    private String funcao;
    // @NotNull
    private Boolean ativo; // Usar Boolean para permitir nulo se não for alterado
}
