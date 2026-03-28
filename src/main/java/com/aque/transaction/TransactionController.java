package com.aque.transaction;

import com.aque.category.CategoryType;
import com.aque.transaction.dto.request.TransactionRequest;
import com.aque.transaction.dto.response.TransactionResponse;
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
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Lançamentos", description = "Gerenciamento de lançamentos mensais de receitas e despesas")
@SecurityRequirement(name = "Bearer")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Listar lançamentos",
            description = "Retorna lançamentos com filtros opcionais por mês, ano, categoria, tipo e status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
            }
    )
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> findAll(
            @Parameter(description = "Mês de referência (1-12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Ano de referência")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "ID da categoria")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Tipo: RECEITA ou DESPESA")
            @RequestParam(required = false) CategoryType type,
            @Parameter(description = "Status: PENDENTE ou PAGO")
            @RequestParam(required = false) TransactionStatus status) {
        return ResponseEntity.ok(
                transactionService.findAll(month, year, categoryId, type, status));
    }

    @Operation(
            summary = "Criar lançamento",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Lançamento criado com sucesso",
                            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request));
    }

    @Operation(
            summary = "Editar lançamento",
            description = "Edita um lançamento. Se for originado de recorrente, marca isOverride = true.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lançamento atualizado com sucesso",
                            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado", content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @Parameter(description = "ID do lançamento") @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @Operation(
            summary = "Excluir lançamento",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Lançamento excluído com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do lançamento") @PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}