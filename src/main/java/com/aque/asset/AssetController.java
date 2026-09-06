package com.aque.asset;

import com.aque.asset.dto.request.AssetRequest;
import com.aque.asset.dto.response.AssetImportResponse;
import com.aque.asset.dto.response.AssetResponse;
import com.aque.asset.dto.response.NetWorthResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@Tag(name = "Patrimônio", description = "Gerenciamento de ativos e investimentos")
@SecurityRequirement(name = "Bearer")
public class AssetController {

    private final AssetService assetService;

    @Operation(
            summary = "Listar ativos",
            description = "Retorna todos os ativos. Filtre por pessoa usando o parâmetro `personId`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
            }
    )
    @GetMapping
    public ResponseEntity<List<AssetResponse>> findAll(
            @Parameter(description = "Filtrar por pessoa") @RequestParam(required = false) UUID personId) {
        return ResponseEntity.ok(assetService.findAll(personId));
    }

    @Operation(
            summary = "Patrimônio total",
            description = "Retorna a soma do valor atual de todos os ativos",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Total retornado com sucesso")
            }
    )
    @GetMapping("/net-worth")
    public ResponseEntity<NetWorthResponse> getNetWorth() {
        return ResponseEntity.ok(assetService.getNetWorth());
    }

    @Operation(
            summary = "Criar ativo",
            description = "Cria um novo ativo/investimento",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ativo criado com sucesso",
                            content = @Content(schema = @Schema(implementation = AssetResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                            content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<AssetResponse> create(
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assetService.create(request));
    }

    @Operation(
            summary = "Editar ativo",
            description = "Edita um ativo existente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ativo atualizado com sucesso",
                            content = @Content(schema = @Schema(implementation = AssetResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Ativo ou pessoa não encontrada",
                            content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(
            @Parameter(description = "ID do ativo") @PathVariable UUID id,
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.update(id, request));
    }

    @Operation(
            summary = "Excluir ativo",
            description = "Exclui um ativo existente",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Ativo excluído com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Ativo não encontrado",
                            content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do ativo") @PathVariable UUID id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Importar posição da B3",
            description = "Importa ativos a partir do arquivo .xlsx de 'Posição' exportado pela Área do Investidor da B3",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Import processado (ver `created`/`updated`/`errors`)"),
                    @ApiResponse(responseCode = "400", description = "Arquivo vazio ou não é um .xlsx válido",
                            content = @Content)
            }
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetImportResponse> importFromXlsx(
            @Parameter(description = "Arquivo .xlsx de 'Posição' da B3") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(assetService.importFromXlsx(file));
    }
}
