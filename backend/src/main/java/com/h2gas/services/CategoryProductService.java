package com.h2gas.services;

import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.repository.ProductItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private ProductItemRepository productItemRepository;


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

    // F7 fatia 6: o fornecedor vem sempre do token (controller); nome único por tipo dentro da loja
    @Transactional
    public CategoryProductDTO createCategory(String supplierId, CategoryRequestDTO request) {
        String name = request.name().trim();
        ProductType productType = productTypeService.findEntityById(request.productTypeId());
        if (categoryProductRepository.existsBySupplierIdAndProductTypeIdAndNameIgnoreCase(supplierId, productType.getId(), name)) {
            throw duplicated(name, productType);
        }
        CategoryProduct categoryProduct = new CategoryProduct();
        categoryProduct.setName(name);
        categoryProduct.setProductType(productType);
        categoryProduct.setSupplier(supplierService.findEntityById(supplierId));
        return categoryProductMapper.toDto(categoryProductRepository.save(categoryProduct));
    }

    public List<SupplierCategoryDTO> findMine(String supplierId) {
        return categoryProductRepository.findMine(supplierId);
    }

    @Transactional
    public void deleteCategory(Long id, String supplierId) {
        CategoryProduct category = findOwned(id, supplierId);
        // o cascade da entidade apagaria os produtos junto: com produto, a categoria fica
        long products = productItemRepository.countByCategoryProductId(id);
        if (products > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, category.getName() + " tem " + products
                    + (products == 1 ? " produto" : " produtos") + ". Mova ou exclua os produtos antes de excluir a categoria.");
        }
        categoryProductRepository.delete(category);
    }

    private CategoryProduct findOwned(Long id, String supplierId) {
        CategoryProduct category = categoryProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));
        if (category.getSupplier() == null || !category.getSupplier().getId().equals(supplierId)) {
            throw new ForbiddenResourceException("Você só pode alterar as categorias da sua loja.");
        }
        return category;
    }

    private static ResponseStatusException duplicated(String name, ProductType productType) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Você já tem a categoria " + name + " em " + productType.getName() + ".");
    }

    public Page<CategoryProductDTO> findAllBySupplierId(String supplierId, Pageable pageable) {
        return categoryProductRepository.findBySupplierId(supplierId, pageable)
                .map(categoryProductMapper::toDto);
    }

    @Transactional
    public CategoryProductDTO update(Long id, CategoryRequestDTO request, String supplierId) {
        CategoryProduct categoryEdit = findOwned(id, supplierId);
        String name = request.name().trim();
        ProductType productType = productTypeService.findEntityById(request.productTypeId());
        if (categoryProductRepository.existsBySupplierIdAndProductTypeIdAndNameIgnoreCaseAndIdNot(supplierId, productType.getId(), name, id)) {
            throw duplicated(name, productType);
        }
        categoryEdit.setProductType(productType);
        categoryEdit.setName(name);
        return categoryProductMapper.toDto(categoryProductRepository.save(categoryEdit));
    }
}
