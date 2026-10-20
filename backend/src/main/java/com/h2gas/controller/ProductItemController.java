package com.h2gas.controller;


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
            description = "Exige price (Preço, valor de retirada) e shippingFee (Preço do frete). Entrega = price + shippingFee; "
                    + "frete zero vira a etiqueta Entrega Grátis na leitura. A categoria precisa ser do próprio fornecedor. "
                    + "multipart/form-data: parte 'product' (JSON) e parte 'file' (imagem)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Preço ou frete ausente/negativo, marca/categoria ausentes ou estoque negativo"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou categoria de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Categoria inexistente"),
            @ApiResponse(responseCode = "502", description = "Falha ao enviar a imagem")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductItemDTO> createCategoryProduct(
            @RequestPart("product") @Valid CreateProductItemDTO productItemDTO,
            @RequestPart("file") MultipartFile file
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        ProductItemDTO productItemCreate = productItemService.createProductItem(productItemDTO, file, userName);
        return ResponseEntity.ok(productItemCreate);
    }

    @GetMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductItemDTO> getProductById(@PathVariable Long id) {
        ProductItemDTO productItem = productItemService.findById(id);
        return ResponseEntity.ok(productItem);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Long> deleteProduct(@PathVariable("id") Long id) {
        productItemService.deleteProductItem(id);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Edita um produto do fornecedor logado",
            description = "Atualiza marca, price e shippingFee (retirada = price; entrega = price + shippingFee), estoque e descrição. Só o dono do produto "
                    + "(ou ADMIN) edita, e a categoria nova precisa ser do mesmo fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Preço ausente ou negativo, marca/categoria ausentes ou estoque negativo"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão, produto ou categoria de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Produto ou categoria inexistente")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductItemDTO> update(@Parameter(description = "ID do produto") @PathVariable Long id,
                                                 @Valid @RequestBody CreateProductItemDTO dto) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ProductItemDTO updated = productItemService.updateProductItem(id, dto, actor);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Lista produtos de um fornecedor",
            description = "Retorna os produtos de um fornecedor específico, opcionalmente filtrados por categoria e/ou busca por marca (os dois filtros podem ser combinados)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos do fornecedor")
    })
    @GetMapping("/by-supplier/{supplierId}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<PageResponse<ProductItemDTO>> getProductBySupplier(
            @Parameter(description = "ID do fornecedor") @PathVariable String supplierId,
            @Parameter(description = "Página") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filtra pela marca do produto") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Filtra pelo nome da categoria (ex.: Gás, Água)") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Oculta produtos com estoque zero (RN02). Para CUSTOMER é sempre aplicado, mesmo que o parâmetro seja false; "
                    + "fornecedor e admin veem os zerados por padrão, para repor") @RequestParam(value = "onlyAvailable", defaultValue = "false") boolean onlyAvailable
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<ProductItemDTO> result = productItemService.findProductsFiltered(supplierId, search, category, onlyAvailable || isCustomer(), pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);
        return ResponseEntity.ok(response);
    }

    // RN02 decidida no backend: o cliente nunca vê produto zerado, qualquer que seja o parâmetro enviado
    private boolean isCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
    }

    @Operation(summary = "Lista produtos disponíveis na plataforma",
            description = "Retorna produtos de fornecedores ativos e abertos com estoque disponível (RN02: item zerado não aparece), "
                    + "opcionalmente filtrados por cidade do endereço ativo do cliente (RN05), por categoria e ordenados por preço de entrega")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos disponíveis")
    })
    @GetMapping("/by-supplier-open")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<PageResponse<ProductItemDTO>> getAllProductsBySupplierOpen(
            @Parameter(description = "Página") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filtra por cidade do endereço ativo do cliente (RN05)") @RequestParam(value = "cityId", required = false) Long cityId,
            @Parameter(description = "Filtra pelo nome da categoria (ex.: Gás, Água)") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Ordenação, ex.: priceDelivery,asc") @RequestParam(value = "sort", required = false) String sort
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size, sort);
        Page<ProductItemDTO> result = productItemService.findBySupplierActiveAndSupplierOpen(cityId, category, pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

}
