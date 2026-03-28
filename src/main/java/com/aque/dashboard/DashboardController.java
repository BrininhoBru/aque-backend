package com.aque.dashboard;

import com.aque.dashboard.dto.response.DashboardSummaryResponse;
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
}