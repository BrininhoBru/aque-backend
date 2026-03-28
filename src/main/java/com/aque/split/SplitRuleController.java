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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/split")
@RequiredArgsConstructor
@Tag(name = "Divisão", description = "Gerenciamento de regras de divisão de custos por mês")
@SecurityRequirement(name = "Bearer")
public class SplitRuleController {

    private final SplitRuleService splitRuleService;

    @Operation(
            summary = "Consultar regra de divisão",
            description = "Retorna a regra de divisão configurada para o mês/ano informado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regra encontrada",
                            content = @Content(schema = @Schema(implementation = SplitRuleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Regra não configurada para o mês",
                            content = @Content)
            }
    )
    @GetMapping("/{year}/{month}")
    public ResponseEntity<SplitRuleResponse> findByMonth(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable int month) {
        return ResponseEntity.ok(splitRuleService.findByMonth(year, month));
    }

    @Operation(
            summary = "Salvar regra de divisão",
            description = "Cria ou substitui a regra de divisão do mês. A soma dos percentuais deve ser 100%.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regra salva com sucesso",
                            content = @Content(schema = @Schema(implementation = SplitRuleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Soma dos percentuais diferente de 100%",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                            content = @Content)
            }
    )
    @PutMapping("/{year}/{month}")
    public ResponseEntity<SplitRuleResponse> save(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable int month,
            @Valid @RequestBody SplitRuleRequest request) {
        return ResponseEntity.ok(splitRuleService.save(year, month, request));
    }
}