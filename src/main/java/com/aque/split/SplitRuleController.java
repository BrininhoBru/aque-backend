package com.aque.split;

import com.aque.split.dto.request.SplitRuleRequest;
import com.aque.split.dto.response.SplitRuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/split")
@RequiredArgsConstructor
@Tag(name = "Divisão", description = "Gerenciamento da regra de divisão de custos, vigente por período")
@SecurityRequirement(name = "Bearer")
public class SplitRuleController {

    private final SplitRuleService splitRuleService;

    @Operation(
            summary = "Consultar regra de divisão vigente num mês",
            description = "Retorna a versão da regra de divisão que estava vigente no mês/ano informado — pode ser uma edição anterior, não necessariamente a mais recente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regra encontrada",
                            content = @Content(schema = @Schema(implementation = SplitRuleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Nenhuma regra vigente para o mês (nunca foi configurada até aquele momento)",
                            content = @Content)
            }
    )
    @GetMapping("/{year}/{month}")
    public ResponseEntity<SplitRuleResponse> findByMonth(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(splitRuleService.findByMonth(year, month));
    }

    @Operation(
            summary = "Salvar regra de divisão",
            description = "Cria uma nova versão da regra, vigente a partir do mês atual em diante. Não altera o split de meses já fechados. A soma dos percentuais deve ser 100%.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regra salva com sucesso",
                            content = @Content(schema = @Schema(implementation = SplitRuleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Soma dos percentuais diferente de 100%, ou pessoa duplicada",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                            content = @Content)
            }
    )
    @PutMapping
    public ResponseEntity<SplitRuleResponse> save(@Valid @RequestBody SplitRuleRequest request) {
        return ResponseEntity.ok(splitRuleService.save(request));
    }
}
