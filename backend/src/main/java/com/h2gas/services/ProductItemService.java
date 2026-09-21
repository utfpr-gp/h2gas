package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.model.repository.ProductItemRepository;
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

    public List<ProductItemDTO> findAllProductItems() {
        return productItemRepository.findAll()
                .stream()
                .map(productItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<ProductItemDTO> findBySupplierActiveAndSupplierOpen(Pageable pageable) {
        return productItemRepository.findBySupplierActiveAndSupplierOpen(true,true, pageable)
                .map(productItemMapper::toDto);
    }

    public Page<ProductItemDTO> findProductsBySupplier(String supplierId, Pageable pageable) {
        return productItemRepository.findBySupplierId(supplierId, pageable)
                .map(productItemMapper::toDto);
    }

    public Page<ProductItemDTO> findProductsFiltered(String supplierId, String search, String category, Pageable pageable) {
        Page<ProductItem> page;

        if (category != null && !category.isBlank()) {
            page = productItemRepository.findBySupplierIdAndCategoryProduct_ProductType_NameIgnoreCase(supplierId, category, pageable);
        } else if (search != null && !search.isBlank()) {
            page = productItemRepository.findBySupplierIdAndBrandContainingIgnoreCase(supplierId, search, pageable);
        } else {
            page = productItemRepository.findBySupplierId(supplierId, pageable);
        }

        return page.map(productItemMapper::toDto);
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
            throw new RuntimeException("Categoria informada não pertence ao fornecedor logado");
        }

        if (file != null) {
            try {
                Map cloudnaryResponse = cloudinaryService.upload(file);
                productItem.setImageUrl(cloudnaryResponse.get("url").toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        productItem.setCategoryProduct(categoryProduct);
        productItem.setSupplier(supplier);

        return productItemMapper.toDto(productItemRepository.save(productItem));
    }


    public void deleteProductItem(Long id) {
        ProductItem productItem = productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto com " + id + " não encontrado"));

        productItemRepository.delete(productItem);
    }

    public ProductItemDTO updateProductItem(Long id, CreateProductItemDTO productItemDto) {
        ProductItem productItemUpdate = productItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto com " + id + " não encontrado"));

        if (productItemDto.getCategoryProductId() != null &&
                !productItemDto.getCategoryProductId().equals(productItemUpdate.getCategoryProduct().getId())) {
            CategoryProduct categoryProduct = categoryProductService.findEntityById(
                    productItemDto.getCategoryProductId()
            );
            productItemUpdate.setCategoryProduct(categoryProduct);
        }

        productItemUpdate.setBrand(productItemDto.getBrand());
        productItemUpdate.setPriceDelivery(productItemDto.getPrice());
        productItemUpdate.setPricePickup(productItemDto.getPrice());
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
