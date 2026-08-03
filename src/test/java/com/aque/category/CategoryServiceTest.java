package com.aque.category;

import com.aque.category.dto.request.CategoryRequest;
import com.aque.category.dto.response.CategoryResponse;
import com.aque.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService service;

    private Category predefined;
    private Category custom;

    @BeforeEach
    void setup() {
        predefined = new Category();
        predefined.setId(UUID.randomUUID());
        predefined.setName("Moradia");
        predefined.setType(CategoryType.DESPESA);
        predefined.setPredefined(true);

        custom = new Category();
        custom.setId(UUID.randomUUID());
        custom.setName("Pet");
        custom.setType(CategoryType.DESPESA);
        custom.setPredefined(false);
    }

    @Test
    void findAll_semFiltro_retornaTodas() {
        when(categoryRepository.findAll()).thenReturn(List.of(predefined, custom));

        List<CategoryResponse> result = service.findAll(null);

        assertThat(result).hasSize(2);
        verify(categoryRepository).findAll();
    }

    @Test
    void findAll_comFiltroDeTipo_usaFindByType() {
        when(categoryRepository.findByType(CategoryType.DESPESA)).thenReturn(List.of(predefined, custom));

        List<CategoryResponse> result = service.findAll(CategoryType.DESPESA);

        assertThat(result).hasSize(2);
        verify(categoryRepository).findByType(CategoryType.DESPESA);
    }

    @Test
    void create_deveSalvarComoNaoPredefinida() {
        var request = new CategoryRequest("Viagem", CategoryType.DESPESA);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Viagem");
        assertThat(response.predefined()).isFalse();
    }

    @Test
    void update_categoriaCustomizada_atualizaComSucesso() {
        var request = new CategoryRequest("Pet Atualizado", CategoryType.DESPESA);
        when(categoryRepository.findById(custom.getId())).thenReturn(Optional.of(custom));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.update(custom.getId(), request);

        assertThat(response.name()).isEqualTo("Pet Atualizado");
    }

    @Test
    void update_categoriaPredefinida_lancaBusinessException400() {
        var request = new CategoryRequest("Moradia Editada", CategoryType.DESPESA);
        when(categoryRepository.findById(predefined.getId())).thenReturn(Optional.of(predefined));

        assertThatThrownBy(() -> service.update(predefined.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void delete_categoriaCustomizada_removeComSucesso() {
        when(categoryRepository.findById(custom.getId())).thenReturn(Optional.of(custom));

        service.delete(custom.getId());

        verify(categoryRepository).delete(custom);
    }

    @Test
    void delete_categoriaPredefinida_lancaBusinessException400() {
        when(categoryRepository.findById(predefined.getId())).thenReturn(Optional.of(predefined));

        assertThatThrownBy(() -> service.delete(predefined.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_categoriaNaoEncontrada_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new CategoryRequest("X", CategoryType.DESPESA)))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
