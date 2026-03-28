package com.aque.category;

import com.aque.category.dto.request.CategoryRequest;
import com.aque.category.dto.response.CategoryResponse;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Gerenciamento de categorias de receitas e despesas")
@SecurityRequirement(name = "Bearer")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Listar categorias",
            description = "Retorna todas as categorias. Filtre por tipo usando o parâmetro `type`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
            }
    )
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll(
            @Parameter(description = "Filtrar por tipo: RECEITA ou DESPESA")
            @RequestParam(required = false) CategoryType type) {
        return ResponseEntity.ok(categoryService.findAll(type));
    }

    @Operation(
            summary = "Criar categoria",
            description = "Cria uma nova categoria customizada",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos",
                            content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @Operation(
            summary = "Editar categoria",
            description = "Edita uma categoria customizada. Categorias pré-definidas não podem ser editadas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Tentativa de editar categoria pré-definida",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Categoria não encontrada",
                            content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @Parameter(description = "ID da categoria") @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @Operation(
            summary = "Excluir categoria",
            description = "Exclui uma categoria customizada. Categorias pré-definidas não podem ser excluídas.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Categoria excluída com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Tentativa de excluir categoria pré-definida",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Categoria não encontrada",
                            content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da categoria") @PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}