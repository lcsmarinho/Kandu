package br.com.kandu.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    private String nomeUsuario; // Ou email, dependendo da sua preferência para login
    private String senha;
}