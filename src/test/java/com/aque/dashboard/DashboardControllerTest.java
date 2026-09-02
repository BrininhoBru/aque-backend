package com.aque.dashboard;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import com.aque.split.SplitRule;
import com.aque.split.SplitRuleItem;
import com.aque.split.SplitRuleRepository;
import com.aque.transaction.Transaction;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DashboardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SplitRuleRepository splitRuleRepository;

    @Autowired
    private PersonRepository personRepository;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        splitRuleRepository.deleteAll();
        personRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void getSplit_somaDosItensDeveBaterComOTotal_apesarDoArredondamento() throws Exception {
        Category category = new Category();
        category.setName("Aluguel");
        category.setType(CategoryType.DESPESA);
        category.setPredefined(false);
        categoryRepository.save(category);

        Transaction transaction = new Transaction();
        transaction.setDescription("Aluguel");
        transaction.setCategory(category);
        transaction.setType(CategoryType.DESPESA);
        transaction.setReferenceMonth(3);
        transaction.setReferenceYear(2026);
        transaction.setAmountExpected(new BigDecimal("100.01"));
        transaction.setStatus(TransactionStatus.PENDENTE);
        transactionRepository.save(transaction);

        Person p1 = new Person();
        p1.setName("A");
        personRepository.save(p1);
        Person p2 = new Person();
        p2.setName("B");
        personRepository.save(p2);
        Person p3 = new Person();
        p3.setName("C");
        personRepository.save(p3);

        SplitRule rule = new SplitRule();
        rule.setEffectiveFrom(LocalDate.of(2026, 3, 1));
        rule.getItems().addAll(List.of(
                item(rule, p1, "33.34"),
                item(rule, p2, "33.33"),
                item(rule, p3, "33.33")
        ));
        splitRuleRepository.save(rule);

        MvcResult result = mockMvc.perform(get("/dashboard/split/2026/3")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal total = new BigDecimal(json.get("totalExpenseExpected").asText());
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode item : json.get("items")) {
            sum = sum.add(new BigDecimal(item.get("amount").asText()));
        }

        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    void getSummary_atrasadoEhGlobalENaoDependeDoMesConsultado() throws Exception {
        Category category = new Category();
        category.setName("Aluguel");
        category.setType(CategoryType.DESPESA);
        category.setPredefined(false);
        categoryRepository.save(category);

        Transaction overdueOtherMonth = new Transaction();
        overdueOtherMonth.setDescription("Aluguel atrasado");
        overdueOtherMonth.setCategory(category);
        overdueOtherMonth.setType(CategoryType.DESPESA);
        overdueOtherMonth.setReferenceMonth(1);
        overdueOtherMonth.setReferenceYear(2026);
        overdueOtherMonth.setAmountExpected(new BigDecimal("500"));
        overdueOtherMonth.setStatus(TransactionStatus.PENDENTE);
        overdueOtherMonth.setDueDate(LocalDate.now().minusDays(5));
        transactionRepository.save(overdueOtherMonth);

        Transaction paid = new Transaction();
        paid.setDescription("Conta paga");
        paid.setCategory(category);
        paid.setType(CategoryType.DESPESA);
        paid.setReferenceMonth(3);
        paid.setReferenceYear(2026);
        paid.setAmountExpected(new BigDecimal("100"));
        paid.setAmountPaid(new BigDecimal("100"));
        paid.setStatus(TransactionStatus.PAGO);
        paid.setDueDate(LocalDate.now().minusDays(5));
        transactionRepository.save(paid);

        Transaction futureDue = new Transaction();
        futureDue.setDescription("Conta futura");
        futureDue.setCategory(category);
        futureDue.setType(CategoryType.DESPESA);
        futureDue.setReferenceMonth(3);
        futureDue.setReferenceYear(2026);
        futureDue.setAmountExpected(new BigDecimal("300"));
        futureDue.setStatus(TransactionStatus.PENDENTE);
        futureDue.setDueDate(LocalDate.now().plusDays(5));
        transactionRepository.save(futureDue);

        MvcResult result = mockMvc.perform(get("/dashboard/summary/2026/3")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(new BigDecimal(json.get("totalOverdueAmount").asText())).isEqualByComparingTo("500");
        assertThat(json.get("totalOverdueCount").asLong()).isEqualTo(1L);
    }

    @Test
    void mesInvalido_deveRetornar400() throws Exception {
        mockMvc.perform(get("/dashboard/summary/2026/13")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    private SplitRuleItem item(SplitRule rule, Person person, String percentage) {
        SplitRuleItem item = new SplitRuleItem();
        item.setSplitRule(rule);
        item.setPerson(person);
        item.setPercentage(new BigDecimal(percentage));
        return item;
    }
}
