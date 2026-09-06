package com.aque.split;

import com.aque.BaseIntegrationTest;
import com.aque.asset.AssetRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private AssetRepository assetRepository;

    private Person person1;
    private Person person2;

    @BeforeEach
    void setup() {
        splitRuleRepository.deleteAll();
        assetRepository.deleteAll();
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

        mockMvc.perform(put("/split")
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

        mockMvc.perform(put("/split")
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
    void salvarDivisao_duasVezes_mesUltimaVersaoVigenteNoMesAtual() throws Exception {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(70)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        mockMvc.perform(put("/split")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        var updated = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(50)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(50))
        ));

        mockMvc.perform(put("/split")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)));

        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/split/" + now.getYear() + "/" + now.getMonthValue())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].percentage").value(50));
    }

    @Test
    void consultarDivisao_mesPassado_retornaVersaoVigenteNaEpoca() throws Exception {
        SplitRule versaoAntiga = new SplitRule();
        versaoAntiga.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        versaoAntiga.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        SplitRuleItem itemAntigo = new SplitRuleItem();
        itemAntigo.setSplitRule(versaoAntiga);
        itemAntigo.setPerson(person1);
        itemAntigo.setPercentage(BigDecimal.valueOf(100));
        versaoAntiga.getItems().add(itemAntigo);
        splitRuleRepository.save(versaoAntiga);

        SplitRule versaoNova = new SplitRule();
        versaoNova.setEffectiveFrom(LocalDate.of(2026, 6, 1));
        versaoNova.setCreatedAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        SplitRuleItem itemNovo = new SplitRuleItem();
        itemNovo.setSplitRule(versaoNova);
        itemNovo.setPerson(person1);
        itemNovo.setPercentage(BigDecimal.valueOf(70));
        SplitRuleItem itemNovo2 = new SplitRuleItem();
        itemNovo2.setSplitRule(versaoNova);
        itemNovo2.setPerson(person2);
        itemNovo2.setPercentage(BigDecimal.valueOf(30));
        versaoNova.getItems().add(itemNovo);
        versaoNova.getItems().add(itemNovo2);
        splitRuleRepository.save(versaoNova);

        mockMvc.perform(get("/split/2026/3")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].percentage").value(100));

        mockMvc.perform(get("/split/2026/8")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].percentage").value(70));
    }
}
