package com.becajava.ms_user.infra.config;

import com.becajava.ms_user.core.domain.UserRole;
import com.becajava.ms_user.core.usecase.AtualizarUsuarioUseCase;
import com.becajava.ms_user.core.usecase.BuscarUsuarioUseCase;
import com.becajava.ms_user.core.usecase.CriarUsuarioUseCase;
import com.becajava.ms_user.core.usecase.DeletarUseCase;
import com.becajava.ms_user.dto.UsuarioResponseDTO;
import com.becajava.ms_user.infra.controller.UserController;
import com.becajava.ms_user.infra.persistence.UsuarioEntity;
import com.becajava.ms_user.infra.persistence.UsuarioRepository;
import com.becajava.ms_user.infra.service.ExcelImportService;
import com.becajava.ms_user.infra.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String TOKEN_SERVICO = "token-servico";
    private static final String TOKEN_USUARIO = "token-usuario";
    private static final String JSON = "{\"nome\":\"X\",\"cpf\":\"1\",\"email\":\"x@x.com\",\"senha\":\"1\"}";

    @Autowired private MockMvc mockMvc;

    @MockBean private CriarUsuarioUseCase criarUsuarioUseCase;
    @MockBean private DeletarUseCase deletarUseCase;
    @MockBean private BuscarUsuarioUseCase buscarUsuarioUseCase;
    @MockBean private AtualizarUsuarioUseCase atualizarUsuarioUseCase;
    @MockBean private ExcelImportService excelImportService;
    @MockBean private TokenService tokenService;
    @MockBean private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setup() {
        when(tokenService.validarToken(TOKEN_SERVICO)).thenReturn("ms-transaction-worker");
        when(tokenService.obterRole(TOKEN_SERVICO)).thenReturn("SERVICE");

        when(tokenService.validarToken(TOKEN_USUARIO)).thenReturn("user@x.com");
        when(tokenService.obterRole(TOKEN_USUARIO)).thenReturn("USER");
        when(usuarioRepository.findByEmail("user@x.com")).thenReturn(Optional.of(
                new UsuarioEntity(1L, "User", "1", "user@x.com", "hash", UserRole.USER)));

        when(tokenService.validarToken("invalido")).thenReturn("");

        when(buscarUsuarioUseCase.execute(1L)).thenReturn(
                new UsuarioResponseDTO(1L, "User", "1", "user@x.com", UserRole.USER));
    }

    @Test
    @DisplayName("Token de serviço consulta usuário sem precisar existir no banco")
    void servicoPodeConsultarUsuario() throws Exception {
        mockMvc.perform(get("/users/1").header("Authorization", "Bearer " + TOKEN_SERVICO))
                .andExpect(status().isOk());

        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Token de serviço NÃO pode alterar nem apagar usuários")
    void servicoNaoPodeAlterarNemApagar() throws Exception {
        mockMvc.perform(put("/users/1").header("Authorization", "Bearer " + TOKEN_SERVICO)
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/users/1").header("Authorization", "Bearer " + TOKEN_SERVICO))
                .andExpect(status().isForbidden());

        verify(atualizarUsuarioUseCase, never()).execute(any(), any());
        verify(deletarUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Usuário comum continua a consultar e alterar, mas não a apagar")
    void usuarioComum() throws Exception {
        mockMvc.perform(get("/users/1").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/1").header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/1").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem token ou com token inválido -> 403")
    void semTokenOuInvalido() throws Exception {
        mockMvc.perform(get("/users/1")).andExpect(status().isForbidden());

        mockMvc.perform(get("/users/1").header("Authorization", "Bearer invalido"))
                .andExpect(status().isForbidden());
    }
}
