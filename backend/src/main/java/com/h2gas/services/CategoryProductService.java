package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.CategoryProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CategoryProductService {
    @Autowired
    private CategoryProductRepository categoryProductRepository;

    @Autowired
    private CategoryProductMapper categoryProductMapper;

    @Autowired
    private ProductTypeService productTypeService;

    @Autowired
    private ProductTypeMapper productTypeMapper;

    @Autowired
    private SupplierService supplierService;


    public List<CategoryProductDTO> findAllCategoryProducts() {
        return categoryProductRepository.findAll()
                .stream()
                .map(categoryProductMapper::toDto)
                .collect(Collectors.toList());
    }

    public CategoryProductDTO findByIdCategory(Long id) {
        Optional<CategoryProduct> categoryEntityOptional = categoryProductRepository.findById(id);
        return categoryEntityOptional.map(categoryProductMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Categoria não encontrada com esse id: " + id));
    }

    public CategoryProduct findEntityById(Long id) {
        return categoryProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));
    }

    public CategoryProductDTO createCategory(CategoryProductDTO categoryDto) {
        CategoryProduct categoryProduct = categoryProductMapper.toEntity(categoryDto);
        ProductType productType = productTypeService.findEntityById(categoryDto.getProductTypeId());
        Supplier supplier = supplierService.findEntityById(categoryDto.getSupplierId());

        if (productType != null) {
            categoryProduct.setProductType(productType);
        }
        categoryProduct.setSupplier(supplier);

        CategoryProduct savedCategoryProduct = categoryProductRepository.save(categoryProduct);
        return categoryProductMapper.toDto(savedCategoryProduct);
    }

    public void deleteCategory(Long id) {
        CategoryProduct category = categoryProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        categoryProductRepository.delete(category);
    }

    public Page<CategoryProductDTO> findAllBySupplierId(String supplierId, Pageable pageable) {
        return categoryProductRepository.findBySupplierId(supplierId, pageable)
                .map(categoryProductMapper::toDto);
    }

    public CategoryProductDTO update(Long id, CategoryProductDTO categoryDto) {
        CategoryProduct categoryEdit = categoryProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        ProductType productType = productTypeService.findEntityById(categoryDto.getProductTypeId());

        if (!categoryEdit.getProductType().getId().equals(productType.getId())) {
            categoryEdit.setProductType(productType);
        }

        categoryEdit.setName(categoryDto.getName());

        categoryProductRepository.save(categoryEdit);

        return categoryProductMapper.toDto(categoryEdit);
    }
}
