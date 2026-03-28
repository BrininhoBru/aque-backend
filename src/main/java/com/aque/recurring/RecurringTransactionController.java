package com.aque.recurring;

import com.aque.recurring.dto.request.RecurringTransactionRequest;
import com.aque.recurring.dto.response.RecurringTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recurring")
@RequiredArgsConstructor
@Tag(name = "Recorrentes", description = "Gerenciamento de templates de lançamentos recorrentes")
@SecurityRequirement(name = "Bearer")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    @Operation(
            summary = "Listar recorrentes",
            description = "Retorna todos os templates. Filtre por status usando o parâmetro `active`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
            }
    )
    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> findAll(
            @Parameter(description = "Filtrar por status: true = ativos, false = inativos")
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(recurringService.findAll(active));
    }

    @Operation(
            summary = "Criar recorrente",
            description = "Cria um novo template de lançamento recorrente. Inicia como ativo.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Recorrente criado com sucesso",
                            content = @Content(schema = @Schema(implementation = RecurringTransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(
            @Valid @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringService.create(request));
    }

    @Operation(
            summary = "Editar recorrente",
            description = "Edita o template. Afeta apenas meses futuros.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recorrente atualizado com sucesso",
                            content = @Content(schema = @Schema(implementation = RecurringTransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Recorrente não encontrado", content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> update(
            @Parameter(description = "ID do recorrente") @PathVariable UUID id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.ok(recurringService.update(id, request));
    }

    @Operation(
            summary = "Desativar recorrente",
            description = "Desativa o template (active = false). Instâncias já geradas não são afetadas.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Recorrente desativado com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Recorrente não encontrado", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID do recorrente") @PathVariable UUID id) {
        recurringService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}