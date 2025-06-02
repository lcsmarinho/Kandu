package br.com.kandu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {
    private String tipo = "Bearer"; // Tipo de token, comum ser "Bearer"
    private String token;

    public TokenDTO(String token) {
        this.token = token;
    }
}