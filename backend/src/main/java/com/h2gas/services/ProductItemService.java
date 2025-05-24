package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.model.repository.ProductItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public List<ProductItemDTO> findAllProductItems() {
        return productItemRepository.findAll()
                .stream()
                .map(productItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductItemDTO findById(Long id) {
        Optional<ProductItem> cityEntityOptional = productItemRepository.findById(id);
        return cityEntityOptional.map(productItemMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Produto não encontrado com esse id: " + id));
    }

    public ProductItemDTO createProductItem(CreateProductItemDTO productItemDto) {
        ProductItem productItem = createProductItemMapper.toEntity(productItemDto);
        Supplier supplier = supplierService.findEntityById(productItemDto.getDistributorId());
        CategoryProduct categoryProduct = categoryProductService.findEntityById(
                productItemDto.getCategoryProductId()
        );

        productItem.setCategoryProduct(categoryProduct);
        productItem.setSupplier(supplier);

        return productItemMapper.toDto(productItemRepository.save(productItem));
    }


}
