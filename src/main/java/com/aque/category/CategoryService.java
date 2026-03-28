package com.aque.category;

import com.aque.category.dto.request.CategoryRequest;
import com.aque.category.dto.response.CategoryResponse;
import com.aque.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll(CategoryType type) {
        List<Category> categories = type != null
                ? categoryRepository.findByType(type)
                : categoryRepository.findAll();

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setType(request.type());
        category.setPredefined(false);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findById(id);

        if (category.isPredefined()) {
            throw new BusinessException(
                    "Categorias pré-definidas não podem ser editadas",
                    HttpStatus.BAD_REQUEST
            );
        }

        category.setName(request.name());
        category.setType(request.type());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        Category category = findById(id);

        if (category.isPredefined()) {
            throw new BusinessException(
                    "Categorias pré-definidas não podem ser excluídas",
                    HttpStatus.BAD_REQUEST
            );
        }

        categoryRepository.delete(category);
    }

    private Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Categoria não encontrada",
                        HttpStatus.NOT_FOUND
                ));
    }
}