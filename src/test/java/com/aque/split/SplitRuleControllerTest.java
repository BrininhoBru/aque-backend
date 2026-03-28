package com.aque.split;

import com.aque.BaseIntegrationTest;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import com.aque.split.dto.request.SplitRuleItemRequest;
import com.aque.split.dto.request.SplitRuleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SplitRuleControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SplitRuleRepository splitRuleRepository;

    @Autowired
    private PersonRepository personRepository;

    private Person person1;
    private Person person2;

    @BeforeEach
    void setup() {
        splitRuleRepository.deleteAll();
        personRepository.deleteAll();

        person1 = new Person();
        person1.setName("Eu");
        personRepository.save(person1);

        person2 = new Person();
        person2.setName("Esposa");
        personRepository.save(person2);
    }

    @Test
    void salvarDivisao_somaCorreta_deveRetornar200() throws Exception {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(70)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        mockMvc.perform(put("/split/2026/3")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void salvarDivisao_somaErrada_deveRetornar400() throws Exception {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(60)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        mockMvc.perform(put("/split/2026/3")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void consultarDivisao_naoExistente_deveRetornar404() throws Exception {
        mockMvc.perform(get("/split/2026/4")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void salvarDivisao_duasVezes_deveSubstituir() throws Exception {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(70)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        mockMvc.perform(put("/split/2026/3")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        var updated = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(50)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(50))
        ));

        mockMvc.perform(put("/split/2026/3")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].percentage").value(50));
    }
}