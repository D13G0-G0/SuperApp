package com.superapp.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.superapp.api.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Login, registro y reglas por rol (FR-01, FR-02, NFR-04). Cuentas semilla: contraseña Demo1234. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // cada prueba deshace lo que crea
class AuthSecurityTests {

    private static final String NEW_LISTING = """
            {"title":"Silla","description":"Silla de madera","price":50000,
             "category":"Hogar","imageUrl":"https://picsum.photos/seed/silla/600/400"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void loginWithCorrectPasswordReturnsJwt() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Vendedor@superapp.test\",\"password\":\"Demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("vendedor@superapp.test"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente@superapp.test\",\"password\":\"otra-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos"));
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIs403() throws Exception {
        String customer = login("cliente@superapp.test", "Demo1234");
        mvc.perform(post("/api/listings").header("Authorization", "Bearer " + customer)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LISTING))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/users/1").header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCanPublishAndSearchIsPublic() throws Exception {
        String seller = login("vendedor@superapp.test", "Demo1234");
        mvc.perform(post("/api/listings").header("Authorization", "Bearer " + seller)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LISTING))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerName").value("Valentín Vendedor"));
        mvc.perform(get("/api/listings")).andExpect(status().isOk());
    }

    @Test
    void sellerCrudAndFieldErrors() throws Exception {
        String seller = login("vendedor@superapp.test", "Demo1234");
        String customer = login("cliente@superapp.test", "Demo1234");
        String body = mvc.perform(post("/api/listings").header("Authorization", "Bearer " + seller)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LISTING))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/listings/mine").header("Authorization", "Bearer " + seller))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + seller)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LISTING.replace("Silla\"", "Silla roja\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Silla roja"));
        // Sin foto y precio 0: un mensaje por campo
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Silla\",\"description\":\"d\",\"price\":0,\"category\":\"Hogar\",\"imageUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.price").value("El precio debe ser mayor que 0"))
                .andExpect(jsonPath("$.fields.imageUrl").exists());
        mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + seller))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/listings/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void registerHashesPasswordAndRejectsAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ana Prueba","email":"ana@example.com",
                                 "password":"ClaveSegura1","roles":["CUSTOMER","SELLER"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
        assertThat(userRepository.findByEmailIgnoreCase("ana@example.com")).get()
                .satisfies(u -> assertThat(u.getPasswordHash()).startsWith("$2a$10$").doesNotContain("ClaveSegura1"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Malo","email":"malo@example.com",
                                 "password":"ClaveSegura1","roles":["ADMIN"]}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Corta","email":"corta@example.com",
                                 "password":"123","roles":["CUSTOMER"]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }
}
