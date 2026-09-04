package com.aque.recurring;

import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.exception.BusinessException;
import com.aque.recurring.dto.request.RecurringTransactionRequest;
import com.aque.recurring.dto.response.RecurringTransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RecurringTransactionJob recurringTransactionJob;

    @InjectMocks
    private RecurringTransactionService service;

    private Category category;
    private RecurringTransaction recurring;

    @BeforeEach
    void setup() {
        category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Moradia");
        category.setType(CategoryType.DESPESA);

        recurring = new RecurringTransaction();
        recurring.setId(UUID.randomUUID());
        recurring.setDescription("Aluguel");
        recurring.setCategory(category);
        recurring.setType(CategoryType.DESPESA);
        recurring.setDefaultAmount(BigDecimal.valueOf(1500));
        recurring.setActive(true);
    }

    @Test
    void findAll_semFiltro_usaFindAll() {
        when(recurringRepository.findAll()).thenReturn(List.of(recurring));

        List<RecurringTransactionResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        verify(recurringRepository).findAll();
    }

    @Test
    void findAll_comFiltroDeStatus_usaFindByActive() {
        when(recurringRepository.findByActive(true)).thenReturn(List.of(recurring));

        List<RecurringTransactionResponse> result = service.findAll(true);

        assertThat(result).hasSize(1);
        verify(recurringRepository).findByActive(true);
    }

    @Test
    void create_categoriaExistente_criaComoAtivo() {
        var request = new RecurringTransactionRequest("Aluguel", category.getId(), CategoryType.DESPESA, BigDecimal.valueOf(1500), null);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(recurringRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransactionResponse response = service.create(request);

        assertThat(response.description()).isEqualTo("Aluguel");
        assertThat(response.active()).isTrue();
    }

    @Test
    void create_typeDivergenteDaCategoria_lancaBusinessException400() {
        var request = new RecurringTransactionRequest("Salário", category.getId(), CategoryType.RECEITA, BigDecimal.valueOf(1500), null);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_categoriaNaoEncontrada_lancaBusinessException404() {
        UUID categoryId = UUID.randomUUID();
        var request = new RecurringTransactionRequest("Aluguel", categoryId, CategoryType.DESPESA, BigDecimal.valueOf(1500), null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_recorrenteExistente_atualizaComSucesso() {
        var request = new RecurringTransactionRequest("Aluguel Novo", category.getId(), CategoryType.DESPESA, BigDecimal.valueOf(1600), null);
        when(recurringRepository.findById(recurring.getId())).thenReturn(Optional.of(recurring));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(recurringRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransactionResponse response = service.update(recurring.getId(), request);

        assertThat(response.description()).isEqualTo("Aluguel Novo");
        assertThat(response.defaultAmount()).isEqualByComparingTo("1600");
    }

    @Test
    void update_typeDivergenteDaCategoria_lancaBusinessException400() {
        var request = new RecurringTransactionRequest("Aluguel Novo", category.getId(), CategoryType.RECEITA, BigDecimal.valueOf(1600), null);
        when(recurringRepository.findById(recurring.getId())).thenReturn(Optional.of(recurring));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.update(recurring.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_recorrenteNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(recurringRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new RecurringTransactionRequest("X", category.getId(), CategoryType.DESPESA, BigDecimal.ONE, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deactivate_recorrenteExistente_marcaComoInativo() {
        when(recurringRepository.findById(recurring.getId())).thenReturn(Optional.of(recurring));
        when(recurringRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(recurring.getId());

        assertThat(recurring.isActive()).isFalse();
        verify(recurringRepository).save(recurring);
    }

    @Test
    void deactivate_recorrenteNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(recurringRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generate_delegaParaRecurringTransactionJob() {
        when(recurringTransactionJob.generate(2026, 3)).thenReturn(2);

        int count = service.generate(2026, 3);

        assertThat(count).isEqualTo(2);
        verify(recurringTransactionJob).generate(2026, 3);
    }
}
