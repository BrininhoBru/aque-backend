package com.aque.recurring;

import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.exception.BusinessException;
import com.aque.recurring.dto.request.RecurringTransactionRequest;
import com.aque.recurring.dto.response.RecurringTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final CategoryRepository categoryRepository;

    public List<RecurringTransactionResponse> findAll(Boolean active) {
        List<RecurringTransaction> list = active != null
                ? recurringRepository.findByActive(active)
                : recurringRepository.findAll();

        return list.stream()
                .map(RecurringTransactionResponse::from)
                .toList();
    }

    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Category category = findCategory(request.categoryId());

        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setDescription(request.description());
        recurring.setCategory(category);
        recurring.setType(request.type());
        recurring.setDefaultAmount(request.defaultAmount());
        recurring.setActive(true);

        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    public RecurringTransactionResponse update(UUID id, RecurringTransactionRequest request) {
        RecurringTransaction recurring = findById(id);
        Category category = findCategory(request.categoryId());

        recurring.setDescription(request.description());
        recurring.setCategory(category);
        recurring.setType(request.type());
        recurring.setDefaultAmount(request.defaultAmount());

        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    public void deactivate(UUID id) {
        RecurringTransaction recurring = findById(id);
        recurring.setActive(false);
        recurringRepository.save(recurring);
    }

    public RecurringTransaction findById(UUID id) {
        return recurringRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Lançamento recorrente não encontrado",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(
                        "Categoria não encontrada",
                        HttpStatus.NOT_FOUND
                ));
    }
}