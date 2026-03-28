package com.aque.dashboard;

import com.aque.category.CategoryType;
import com.aque.dashboard.dto.response.CategoryTotalResponse;
import com.aque.dashboard.dto.response.DashboardSummaryResponse;
import com.aque.dashboard.dto.response.MonthEvolutionResponse;
import com.aque.dashboard.dto.response.SplitResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints de agregação para o dashboard financeiro")
@SecurityRequirement(name = "Bearer")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Saldo mensal",
            description = "Retorna totais de receitas e despesas previstas e pagas, e saldo calculado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso",
                            content = @Content(schema = @Schema(implementation = DashboardSummaryResponse.class)))
            }
    )
    @GetMapping("/summary/{year}/{month}")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable int month) {
        return ResponseEntity.ok(dashboardService.getSummary(year, month));
    }

    @Operation(
            summary = "Totais por categoria",
            description = "Retorna totais agrupados por categoria. Filtre por tipo com o parâmetro `type`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Totais retornados com sucesso")
            }
    )
    @GetMapping("/by-category/{year}/{month}")
    public ResponseEntity<List<CategoryTotalResponse>> getByCategory(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable int month,
            @Parameter(description = "Tipo: RECEITA ou DESPESA")
            @RequestParam(required = false) CategoryType type) {
        return ResponseEntity.ok(dashboardService.getByCategory(year, month, type));
    }

    @Operation(
            summary = "Evolução mensal anual",
            description = "Retorna os 12 meses do ano com totais de receitas e despesas. Meses sem lançamentos retornam zeros.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evolução retornada com sucesso")
            }
    )
    @GetMapping("/evolution/{year}")
    public ResponseEntity<List<MonthEvolutionResponse>> getEvolution(
            @Parameter(description = "Ano") @PathVariable int year) {
        return ResponseEntity.ok(dashboardService.getEvolution(year));
    }

    @Operation(
            summary = "Divisão calculada por pessoa",
            description = "Aplica a regra de divisão do mês sobre o total de despesas previstas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Divisão calculada com sucesso",
                            content = @Content(schema = @Schema(implementation = SplitResultResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Regra de divisão não configurada",
                            content = @Content)
            }
    )
    @GetMapping("/split/{year}/{month}")
    public ResponseEntity<SplitResultResponse> getSplit(
            @Parameter(description = "Ano") @PathVariable int year,
            @Parameter(description = "Mês (1-12)") @PathVariable int month) {
        return ResponseEntity.ok(dashboardService.getSplit(year, month));
    }
}