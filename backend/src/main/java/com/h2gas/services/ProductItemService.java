package com.h2gas.services;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.model.repository.ProductItemRepository;
import com.h2gas.model.repository.OrderItemRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.dto.ProductStockSummaryDTO;
import com.h2gas.enums.ProductSortEnum;
import com.h2gas.enums.StockFilterEnum;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductItemService {
    // HU18: hora de Brasília para "aberta agora"
    @Autowired
    private Clock clock;

    @Autowired
    private ProductItemRepository productItemRepository;

    @Autowired
    private CreateProductItemMapper createProductItemMapper;

    @Autowired
    private ProductItemMapper productItemMapper;

    @Autowired
    private CategoryProductService categoryProductService;

    @Autowired
    private CategoryProductMapper categoryProductMapper;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // F7 fatia 6: lista do fornecedor logado; "estoque baixo" usa o estoque mínimo da configuração da loja
    public Page<ProductItemDTO> findMine(String supplierId, String q, StockFilterEnum stock, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim();
        Integer min = null;
        Integer max = null;
        switch (stock) {
            case AVAILABLE -> min = 1;
            case LOW -> { min = 1; max = lowStockThreshold(supplierId); }
            case OUT -> { min = 0; max = 0; }
            case ALL -> { }
        }
        return productItemRepository.findMine(supplierId, term, min, max, pageable).map(productItemMapper::toDto);
    }

    public ProductStockSummaryDTO summarizeMine(String supplierId) {
        int threshold = lowStockThreshold(supplierId);
        return new ProductStockSummaryDTO(
                productItemRepository.countBySupplierId(supplierId),
                productItemRepository.countBySupplierIdAndStockQuantityGreaterThan(supplierId, 0),
                productItemRepository.countBySupplierIdAndStockQuantityBetween(supplierId, 1, threshold),
                productItemRepository.countBySupplierIdAndStockQuantity(supplierId, 0),
                threshold);
    }

    private int lowStockThreshold(String supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + supplierId + " não encontrado"))
                .getLowStockThreshold();
    }

    public List<ProductItemDTO> findAllProductItems() {
        return productItemRepository.findAll()
                .stream()
                .map(productItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<ProductItemDTO> findBySupplierActiveAndSupplierOpen(Long cityId, String category, Pageable pageable) {
        return findBySupplierActiveAndSupplierOpen(cityId, category, null, pageable);
    }

    public Page<ProductItemDTO> findBySupplierActiveAndSupplierOpen(Long cityId, String category, String q, Pageable pageable) {
        return findBySupplierActiveAndSupplierOpen(cityId, category, q, ProductSortEnum.NONE, pageable);
    }

    // HU16: a ordenação escolhida no catálogo vira o Sort da página, ou a nota da loja quando é "melhor avaliado"
    public Page<ProductItemDTO> findBySupplierActiveAndSupplierOpen(Long cityId, String category, String q,
                                                                     ProductSortEnum sort, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim().toLowerCase(java.util.Locale.ROOT);
        // sem ordenação escolhida, vale o Sort que já veio no Pageable (chamadas internas e a HU02)
        Pageable sorted = sort == ProductSortEnum.NONE ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.sort());
        // HU18: só produtos de loja aberta agora pelo horário (Brasília)
        return productItemRepository.findAvailableProducts(cityId, category, term, sort.byShopRating(),
                        LocalTime.now(clock), sorted)
                .map(productItemMapper::toDto);
    }

    public Page<ProductItemDTO> findProductsBySupplier(String supplierId, Pageable pageable) {
        return productItemRepository.findBySupplierId(supplierId, pageable)
                .map(productItemMapper::toDto);
    }

    public Page<ProductItemDTO> findProductsFiltered(String supplierId, String search, String category, boolean onlyAvailable, Pageable pageable) {
        return productItemRepository.findBySupplierIdFiltered(supplierId, category, search, onlyAvailable, pageable)
                .map(productItemMapper::toDto);
    }


    public ProductItemDTO findById(Long id) {
        Optional<ProductItem> cityEntityOptional = productItemRepository.findById(id);
        return cityEntityOptional.map(productItemMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Produto não encontrado com esse id: " + id));
    }

    // F12: o formulário de edição só abre produto da própria loja
    public ProductItemDTO findOwnById(Long id, User actor) {
        ProductItem productItem = productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com esse id: " + id));
        if (productItem.getSupplier() == null || !productItem.getSupplier().getId().equals(actor.getId())) {
            throw new ForbiddenResourceException("Este produto não é da sua loja.");
        }
        return productItemMapper.toDto(productItem);
    }

    public ProductItem findEntityById(Long id) {
        return productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com esse id: " + id));
    }

    @Transactional
    public ProductItem findEntityByIdWithLock(Long id) {
        return productItemRepository.findByIdWithLock(id);
    }

    public ProductItemDTO createProductItem(CreateProductItemDTO dto, MultipartFile file, String nameUser) {
        ProductItem productItem = createProductItemMapper.toEntity(dto);
        Supplier supplier = supplierService.findEntityByName(nameUser);
        // HU19: a categoria é global — qualquer uma serve; categoria inexistente é 404
        CategoryProduct categoryProduct = categoryProductService.findEntityById(dto.getCategoryProductId());

        if (file != null) {
            try {
                Map cloudnaryResponse = cloudinaryService.upload(file);
                productItem.setImageUrl(cloudnaryResponse.get("url").toString());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível enviar a imagem. Tente novamente.", e);
            }
        }

        productItem.setCategoryProduct(categoryProduct);
        productItem.setSupplier(supplier);

        return productItemMapper.toDto(productItemRepository.save(productItem));
    }


    public void deleteProductItem(Long id, User actor) {
        ProductItem productItem = productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto com " + id + " não encontrado"));
        if (productItem.getSupplier() == null || !productItem.getSupplier().getId().equals(actor.getId())) {
            throw new ForbiddenResourceException("Você só pode excluir produtos da sua loja.");
        }
        // o histórico de pedidos aponta para o produto (FK de order_items)
        if (orderItemRepository.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este produto já está em pedidos e não pode ser excluído. Zere o estoque para tirá-lo do catálogo.");
        }

        productItemRepository.delete(productItem);
    }

    public ProductItemDTO updateProductItem(Long id, CreateProductItemDTO productItemDto, User actor) {
        ProductItem productItemUpdate = productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto com " + id + " não encontrado"));

        // HU10/HU12: só o dono edita (o admin não mexe em produto); HU19: a categoria é global, qualquer uma serve
        String ownerId = productItemUpdate.getSupplier().getId();
        if (!ownerId.equals(actor.getId())) {
            throw new ForbiddenResourceException("Produto não pertence ao fornecedor logado.");
        }

        if (productItemDto.getCategoryProductId() != null &&
                !productItemDto.getCategoryProductId().equals(productItemUpdate.getCategoryProduct().getId())) {
            CategoryProduct categoryProduct = categoryProductService.findEntityById(
                    productItemDto.getCategoryProductId()
            );
            productItemUpdate.setCategoryProduct(categoryProduct);
        }

        productItemUpdate.setBrand(productItemDto.getBrand());
        createProductItemMapper.applyPrices(productItemUpdate, productItemDto);
        productItemUpdate.setDescription(productItemDto.getDescription());
        productItemUpdate.setStockQuantity(productItemDto.getStockQuantity());
        productItemRepository.save(productItemUpdate);

        return productItemMapper.toDto(productItemUpdate);
    }

    @Transactional
    public ProductItem save(ProductItem productItem) {
        return productItemRepository.save(productItem);
    }
}
