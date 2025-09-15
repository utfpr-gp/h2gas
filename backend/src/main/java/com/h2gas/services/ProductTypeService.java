package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.ProductTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductTypeService {
    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ProductTypeMapper productTypeMapper;

    public List<ProductTypeDTO> findAllProductTypes() {
        return productTypeRepository.findAll()
                .stream()
                .map(productTypeMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductTypeDTO findByIdTypesProducts(Long id) {
        Optional<ProductType> typeProductEntityOptional = productTypeRepository.findById(id);
        return typeProductEntityOptional.map(productTypeMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Tipo não encontrada com esse id: " + id));
    }

    public ProductType findEntityById(Long id) {
        return productTypeRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Tipo não encontrada com esse id: " + id));
    }


    public ProductTypeDTO createProductType(ProductTypeDTO productTypeDTO) {
        ProductType productType = productTypeMapper.toEntity(productTypeDTO);
        return productTypeMapper.toDto(productTypeRepository.save(productType));
    }
}
