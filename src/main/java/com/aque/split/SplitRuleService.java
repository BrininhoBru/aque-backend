package com.aque.split;

import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import com.aque.split.dto.request.SplitRuleItemRequest;
import com.aque.split.dto.request.SplitRuleRequest;
import com.aque.split.dto.response.SplitRuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SplitRuleService {

    private final SplitRuleRepository splitRuleRepository;
    private final PersonRepository personRepository;

    public SplitRuleResponse findByMonth(int year, int month) {
        return splitRuleRepository
                .findByReferenceMonthAndReferenceYear(month, year)
                .map(SplitRuleResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "Regra de divisão não encontrada para " + month + "/" + year,
                        HttpStatus.NOT_FOUND
                ));
    }

    @Transactional
    public SplitRuleResponse save(int year, int month, SplitRuleRequest request) {
        validatePercentages(request.items());

        SplitRule rule = splitRuleRepository
                .findByReferenceMonthAndReferenceYear(month, year)
                .orElseGet(() -> {
                    SplitRule newRule = new SplitRule();
                    newRule.setReferenceMonth(month);
                    newRule.setReferenceYear(year);
                    return newRule;
                });

        rule.getItems().clear();

        for (SplitRuleItemRequest itemRequest : request.items()) {
            Person person = personRepository.findById(itemRequest.personId())
                    .orElseThrow(() -> new BusinessException(
                            "Pessoa não encontrada: " + itemRequest.personId(),
                            HttpStatus.NOT_FOUND
                    ));

            SplitRuleItem item = new SplitRuleItem();
            item.setSplitRule(rule);
            item.setPerson(person);
            item.setPercentage(itemRequest.percentage());
            rule.getItems().add(item);
        }

        return SplitRuleResponse.from(splitRuleRepository.save(rule));
    }

    private void validatePercentages(List<SplitRuleItemRequest> items) {
        BigDecimal total = items.stream()
                .map(SplitRuleItemRequest::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BusinessException(
                    "A soma dos percentuais deve ser exatamente 100%. Total informado: " + total + "%",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}