package com.h2gas.controller;

import com.h2gas.util.CatalogAccess;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.h2gas.model.dto.ProductStockSummaryDTO;
import com.h2gas.enums.ProductSortEnum;
import com.h2gas.enums.StockFilterEnum;

import com.h2gas.model.dto.*;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.ProductItemService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.List;

@Tag(name = "Itens de Produto")
@RestController
@RequestMapping("/api/product-items")
public class ProductItemController {
    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private ProductItemMapper productItemMapper;

    @Autowired
    private CreateProductItemMapper createProductItemMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ProductItemDTO>> getAllProductsItems() {
        List<ProductItemDTO> productItemDTO = productItemService.findAllProductItems();
        return ResponseEntity.ok(productItemDTO);
    }

    @Operation(summary = "Cadastra um produto do fornecedor logado",
            description = "HU21: exige price, o preço único do produto (igual na entrega e na retirada; o frete vem da distância). "
                    + "Um shippingFee enviado por cliente antigo é ignorado. A categoria é global (HU19). "
                    + "multipart/form-data: parte 'product' (JSON) e parte 'file' (imagem)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Preço ausente ou negativo, marca/categoria ausentes ou estoque negativo"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou categoria de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Categoria inexistente"),
            @ApiResponse(responseCode = "502", description = "Falha ao enviar a imagem")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<ProductItemDTO> createCategoryProduct(
            @RequestPart("product") @Valid CreateProductItemDTO productItemDTO,
            @RequestPart("file") MultipartFile file
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        ProductItemDTO productItemCreate = productItemService.createProductItem(productItemDTO, file, userName);
        return ResponseEntity.ok(productItemCreate);
    }

    @Operation(summary = "Busca um produto para edição", description = "Usado pelo formulário de edição do fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente")
    })
    @GetMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<ProductItemDTO> getProductById(@Parameter(description = "ID do produto") @PathVariable Long id) {
        ProductItemDTO productItem = productItemService.findById(id);
        return ResponseEntity.ok(productItem);
    }

    @Operation(summary = "Remove um produto do fornecedor logado",
            description = "Só o dono remove. Produto que já está em pedidos não é removido: zere o estoque para tirá-lo do catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto removido; devolve o id"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel diferente de fornecedor (admin incluso) ou produto de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente"),
            @ApiResponse(responseCode = "409", description = "Produto já vendido em pedidos")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Long> deleteProduct(@Parameter(description = "ID do produto") @PathVariable("id") Long id) {
        productItemService.deleteProductItem(id, (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Edita um produto do fornecedor logado",
            description = "Atualiza marca, price (preço único, HU21), categoria, estoque e descrição. Só o fornecedor dono "
                    + "do produto edita")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Preço ausente ou negativo, marca/categoria ausentes ou estoque negativo"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão, produto ou categoria de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Produto ou categoria inexistente")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<ProductItemDTO> update(@Parameter(description = "ID do produto") @PathVariable Long id,
                                                 @Valid @RequestBody CreateProductItemDTO dto) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ProductItemDTO updated = productItemService.updateProductItem(id, dto, actor);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Lista os produtos do fornecedor logado",
            description = "Ordenada por marca. A busca procura na marca, na categoria e no tipo. stock: all (padrão), "
                    + "available (estoque > 0), low (de 1 até o estoque mínimo da configuração) ou out (estoque zero)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos"),
            @ApiResponse(responseCode = "400", description = "Filtro de estoque inválido"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<PageResponse<ProductItemDTO>> getMyProducts(
            @Parameter(description = "Trecho da marca, categoria ou tipo", example = "ultragaz") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "all, available, low ou out", example = "low") @RequestParam(value = "stock", required = false) String stock,
            @Parameter(description = "Página (0-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página (máx. 50)") @RequestParam(value = "size", required = false) Integer size
    ) {
        StockFilterEnum filter = StockFilterEnum.fromParam(stock);
        User supplier = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<ProductItemDTO> result = productItemService.findMine(supplier.getId(), q, filter, PaginationUtil.getPageableRequest(page, size));
        return ResponseEntity.ok(new PageResponse<>(result));
    }

    @Operation(summary = "Resumo do estoque do fornecedor logado",
            description = "Total, disponíveis, estoque baixo e esgotados, contados no banco com o estoque mínimo da configuração")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo do estoque"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine/summary")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<ProductStockSummaryDTO> getMyStockSummary() {
        User supplier = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(productItemService.summarizeMine(supplier.getId()));
    }

    @Operation(summary = "Lista produtos de um fornecedor (vitrine pública)",
            description = "Aberta a visitantes. Retorna os produtos de um fornecedor específico, opcionalmente filtrados por categoria e/ou busca "
                    + "por marca (os dois filtros podem ser combinados); cliente e visitante não veem produto zerado (RN02)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos do fornecedor")
    })
    @SecurityRequirements
    @GetMapping("/by-supplier/{supplierId}")
    @ResponseBody
    public ResponseEntity<PageResponse<ProductItemDTO>> getProductBySupplier(
            @Parameter(description = "ID do fornecedor") @PathVariable String supplierId,
            @Parameter(description = "Página") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filtra pela marca do produto") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Filtra pelo nome da categoria (ex.: Gás, Água)") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Oculta produtos com estoque zero (RN02). Para cliente e visitante é sempre aplicado, mesmo que o parâmetro seja false; "
                    + "fornecedor e admin veem os zerados por padrão, para repor") @RequestParam(value = "onlyAvailable", defaultValue = "false") boolean onlyAvailable
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<ProductItemDTO> result = productItemService.findProductsFiltered(supplierId, search, category, onlyAvailable || hidesOutOfStock(), pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);
        return ResponseEntity.ok(response);
    }

    // RN02 decidida no backend: cliente e visitante (HU13) nunca veem produto zerado, qualquer que seja o parâmetro;
    // só fornecedor e admin veem os zerados, para repor
    private boolean hidesOutOfStock() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_SUPPLIER".equals(authority.getAuthority()) || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @Operation(summary = "Lista produtos disponíveis na plataforma (vitrine pública)",
            description = "Aberta a visitantes. Retorna produtos de fornecedores ativos e abertos com estoque disponível (RN02: item zerado não aparece), "
                    + "filtrados pela cidade (RN05), por categoria e pela busca q (marca, categoria ou tipo), na ordem pedida em sort "
                    + "(HU16: menor ou maior preço, nome, mais recentes ou nota da loja). "
                    + "Sem login, cityId é obrigatório; o admin logado pode omitir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos disponíveis"),
            @ApiResponse(responseCode = "400", description = "Visitante sem cityId, ou sort fora da lista (\"Ordenação inválida.\")")
    })
    @SecurityRequirements
    @GetMapping("/by-supplier-open")
    @ResponseBody
    public ResponseEntity<PageResponse<ProductItemDTO>> getAllProductsBySupplierOpen(
            @Parameter(description = "Página") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filtra por cidade do endereço ativo do cliente (RN05)") @RequestParam(value = "cityId", required = false) Long cityId,
            @Parameter(description = "Filtra pelo nome da categoria (ex.: Gás, Água)") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "HU16: ordem da lista — price,asc (menor preço), price,desc (maior preço), "
                    + "brand,asc (nome), createdAt,desc (mais recentes) ou rating (nota da loja que vende); "
                    + "ausente = sem ordem definida; fora da lista = 400",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                            "price,asc", "price,desc", "brand,asc", "createdAt,desc", "rating"}))
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "Trecho da marca, da categoria ou do tipo", example = "minalba") @RequestParam(value = "q", required = false) String q
    ) {
        CatalogAccess.requireCityForVisitor(cityId);
        ProductSortEnum order = ProductSortEnum.fromParam(sort);
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<ProductItemDTO> result = productItemService.findBySupplierActiveAndSupplierOpen(cityId, category, q, order, pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

}
