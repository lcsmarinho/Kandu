// src/main/java/br/com/kandu/dto/UsuarioAdminCriacaoDTO.java
package br.com.kandu.dto;

import br.com.kandu.enums.NivelHierarquia;
import lombok.Getter;
import lombok.Setter;
// Adicionar anotações de validação (Jakarta Bean Validation) conforme necessário
// import jakarta.validation.constraints.*;

@Getter
@Setter
public class UsuarioAdminCriacaoDTO {
    // @NotBlank
    private String nomeCompleto;
    // @NotBlank
    private String nomeUsuario;
    // @NotBlank @Email
    private String email;
    // @NotBlank @Size(min = 8) // Senha inicial definida pelo admin
    private String senha;
    // @NotNull
    private NivelHierarquia nivelHierarquia; // Admin define o nível
    private String funcao;
    // O código de inscrição da empresa do admin logado será usado implicitamente.
    // Não precisamos dele no DTO se o admin só pode criar usuários para sua própria empresa.
    // Se o ADM do sistema puder criar para qualquer empresa, precisaríamos do codigoInscricaoEmpresa.
}
