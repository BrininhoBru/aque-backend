package com.aque.person;

import com.aque.BaseIntegrationTest;
import com.aque.person.dto.request.PersonRequest;
import com.aque.split.SplitRule;
import com.aque.split.SplitRuleItem;
import com.aque.split.SplitRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PersonControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private SplitRuleRepository splitRuleRepository;

    private Person person;

    @BeforeEach
    void setupPersons() {
        splitRuleRepository.deleteAll();
        personRepository.deleteAll();

        person = new Person();
        person.setName("Eu");
        personRepository.save(person);
    }

    @Test
    void listarPessoas_deveRetornarTodas() throws Exception {
        mockMvc.perform(get("/persons")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void criarPessoa_deveRetornar201() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonRequest("Esposa"));

        mockMvc.perform(post("/persons")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Esposa"));
    }

    @Test
    void editarPessoa_deveRetornar200() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonRequest("Nome Editado"));

        mockMvc.perform(put("/persons/" + person.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Editado"));
    }

    @Test
    void editarPessoaInexistente_deveRetornar404() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonRequest("X"));

        mockMvc.perform(put("/persons/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirPessoaSemVinculo_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/persons/" + person.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluirPessoaVinculadaARegraDeDivisao_deveRetornar400() throws Exception {
        SplitRule rule = new SplitRule();
        rule.setReferenceMonth(3);
        rule.setReferenceYear(2026);
        SplitRuleItem item = new SplitRuleItem();
        item.setSplitRule(rule);
        item.setPerson(person);
        item.setPercentage(BigDecimal.valueOf(100));
        rule.getItems().add(item);
        splitRuleRepository.save(rule);

        mockMvc.perform(delete("/persons/" + person.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
