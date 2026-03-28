package com.aque.transaction;

import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.exception.BusinessException;
import com.aque.transaction.dto.request.TransactionRequest;
import com.aque.transaction.dto.response.TransactionResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public List<TransactionResponse> findAll(Integer month, Integer year, UUID categoryId, CategoryType type, TransactionStatus status) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (month != null) predicates.add(cb.equal(root.get("referenceMonth"), month));
            if (year != null) predicates.add(cb.equal(root.get("referenceYear"), year));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    public TransactionResponse create(TransactionRequest request) {
        Category category = findCategory(request.categoryId());

        Transaction transaction = new Transaction();
        mapperTrasaction(request, transaction, category);

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public TransactionResponse update(UUID id, TransactionRequest request) {
        Transaction transaction = findById(id);
        Category category = findCategory(request.categoryId());

        mapperTrasaction(request, transaction, category);

        if (transaction.getRecurringId() != null) {
            transaction.setOverride(true);
        }

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    private void mapperTrasaction(TransactionRequest request, Transaction transaction, Category category) {
        transaction.setDescription(request.description());
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setReferenceMonth(request.referenceMonth());
        transaction.setReferenceYear(request.referenceYear());
        transaction.setAmountExpected(request.amountExpected());
        applyPayment(transaction, request.amountPaid());
    }

    public void delete(UUID id) {
        transactionRepository.delete(findById(id));
    }

    private void applyPayment(Transaction transaction, java.math.BigDecimal amountPaid) {
        if (amountPaid != null) {
            transaction.setAmountPaid(amountPaid);
            transaction.setStatus(TransactionStatus.PAGO);
        } else {
            transaction.setAmountPaid(null);
            transaction.setStatus(TransactionStatus.PENDENTE);
        }
    }

    private Transaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Lançamento não encontrado",
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