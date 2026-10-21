package com.h2gas.services;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.h2gas.enums.UserTypeEnum;
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
import com.h2gas.enums.StockFilterEnum;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        return productItemRepository.findAvailableProducts(cityId, category, pageable)
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
        CategoryProduct categoryProduct = categoryProductService.findEntityById(dto.getCategoryProductId());

        if (!categoryProduct.getSupplier().getId().equals(supplier.getId())) {
            throw new ForbiddenResourceException("Categoria informada não pertence ao fornecedor logado.");
        }

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

        // HU10: só o dono (ou ADMIN) edita; a categoria nova também precisa ser do dono do produto
        boolean isAdmin = actor.getUserType() == UserTypeEnum.ADMIN;
        String ownerId = productItemUpdate.getSupplier().getId();
        if (!isAdmin && !ownerId.equals(actor.getId())) {
            throw new ForbiddenResourceException("Produto não pertence ao fornecedor logado.");
        }

        if (productItemDto.getCategoryProductId() != null &&
                !productItemDto.getCategoryProductId().equals(productItemUpdate.getCategoryProduct().getId())) {
            CategoryProduct categoryProduct = categoryProductService.findEntityById(
                    productItemDto.getCategoryProductId()
            );
            if (categoryProduct.getSupplier() == null || !categoryProduct.getSupplier().getId().equals(ownerId)) {
                throw new ForbiddenResourceException("Categoria informada não pertence ao fornecedor do produto.");
            }
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
