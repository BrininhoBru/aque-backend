package com.aque.category;

import com.aque.BaseIntegrationTest;
import com.aque.category.dto.request.CategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CategoryControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category predefined;
    private Category custom;

    @BeforeEach
    void setupCategories() {
        categoryRepository.deleteAll();

        predefined = new Category();
        predefined.setName("Moradia");
        predefined.setType(CategoryType.DESPESA);
        predefined.setPredefined(true);
        categoryRepository.save(predefined);

        custom = new Category();
        custom.setName("Pet");
        custom.setType(CategoryType.DESPESA);
        custom.setPredefined(false);
        categoryRepository.save(custom);
    }

    @Test
    void listarCategorias_deveRetornarTodas() throws Exception {
        mockMvc.perform(get("/categories")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listarCategorias_filtrandoPorTipo() throws Exception {
        mockMvc.perform(get("/categories?type=DESPESA")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void criarCategoria_deveRetornar201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CategoryRequest("Viagem", CategoryType.DESPESA));

        mockMvc.perform(post("/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Viagem"))
                .andExpect(jsonPath("$.predefined").value(false));
    }

    @Test
    void excluirCategoriaPredefinida_deveRetornar400() throws Exception {
        mockMvc.perform(delete("/categories/" + predefined.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void excluirCategoriaCustomizada_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/categories/" + custom.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void editarCategoriaPredefinida_deveRetornar400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CategoryRequest("Moradia Editada", CategoryType.DESPESA));

        mockMvc.perform(put("/categories/" + predefined.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoriaNaoEncontrada_deveRetornar404() throws Exception {
        mockMvc.perform(delete("/categories/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}