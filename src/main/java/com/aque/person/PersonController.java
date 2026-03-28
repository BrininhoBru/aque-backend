package com.aque.person;

import com.aque.person.dto.request.PersonRequest;
import com.aque.person.dto.response.PersonResponse;
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
@RequestMapping("/persons")
@RequiredArgsConstructor
@Tag(name = "Pessoas", description = "Gerenciamento de pessoas para divisão de custos")
@SecurityRequirement(name = "Bearer")
public class PersonController {

    private final PersonService personService;

    @Operation(
            summary = "Listar pessoas",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
            }
    )
    @GetMapping
    public ResponseEntity<List<PersonResponse>> findAll() {
        return ResponseEntity.ok(personService.findAll());
    }

    @Operation(
            summary = "Criar pessoa",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pessoa criada com sucesso",
                            content = @Content(schema = @Schema(implementation = PersonResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos",
                            content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personService.create(request));
    }

    @Operation(
            summary = "Editar pessoa",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pessoa atualizada com sucesso",
                            content = @Content(schema = @Schema(implementation = PersonResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                            content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(@Parameter(description = "ID da pessoa") @PathVariable UUID id, @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    @Operation(
            summary = "Excluir pessoa",
            description = "Exclui uma pessoa. Retorna 400 se estiver vinculada a uma regra de divisão.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Pessoa excluída com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Pessoa vinculada a regra de divisão",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                            content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID da pessoa") @PathVariable UUID id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}