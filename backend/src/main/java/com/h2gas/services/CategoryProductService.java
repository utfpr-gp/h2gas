package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.repository.CategoryProductRepository;
import com.h2gas.model.repository.ProductItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

// HU19: as categorias (P13, P45, Galão 20L…) são globais — só o admin cadastra, junto com os tipos
@Component
public class CategoryProductService {
    // ordem alfabética em pt-BR: o banco (H2 nos testes) põe "Gás" antes de "Água"
    private static final Collator ALFABETICA_PT_BR = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
    private static final Comparator<CategoryProductDTO> POR_TIPO_E_NOME = Comparator
            .comparing(CategoryProductDTO::getType, ALFABETICA_PT_BR)
            .thenComparing(CategoryProductDTO::getName, ALFABETICA_PT_BR);

    @Autowired
    private CategoryProductRepository categoryProductRepository;

    @Autowired
    private CategoryProductMapper categoryProductMapper;

    @Autowired
    private ProductTypeService productTypeService;

    @Autowired
    private ProductItemRepository productItemRepository;

    public List<CategoryProductDTO> findAllCategoryProducts() {
        return categoryProductRepository.findAll()
                .stream()
                .map(categoryProductMapper::toDto)
                .sorted(POR_TIPO_E_NOME)
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

    @Transactional
    public CategoryProductDTO createCategory(CategoryRequestDTO request) {
        String name = request.name().trim();
        ProductType productType = productTypeService.findEntityById(request.productTypeId());
        if (categoryProductRepository.existsByProductTypeIdAndNameIgnoreCase(productType.getId(), name)) {
            throw new BusinessRuleException("Categoria já cadastrada.");
        }
        CategoryProduct categoryProduct = new CategoryProduct();
        categoryProduct.setName(name);
        categoryProduct.setProductType(productType);
        return categoryProductMapper.toDto(categoryProductRepository.save(categoryProduct));
    }

    // categorias que a loja usa (as dos produtos dela), com a contagem dos produtos dela
    public List<SupplierCategoryDTO> findMine(String supplierId) {
        return categoryProductRepository.findMine(supplierId);
    }

    @Transactional
    public void deleteCategory(Long id) {
        CategoryProduct category = findEntityById(id);
        // o cascade da entidade apagaria os produtos junto: com produto, a categoria fica
        long products = productItemRepository.countByCategoryProductId(id);
        if (products > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, category.getName() + " tem " + products
                    + (products == 1 ? " produto" : " produtos") + ". Mova ou exclua os produtos antes de excluir a categoria.");
        }
        categoryProductRepository.delete(category);
    }

    @Transactional
    public CategoryProductDTO update(Long id, CategoryRequestDTO request) {
        CategoryProduct categoryEdit = findEntityById(id);
        String name = request.name().trim();
        ProductType productType = productTypeService.findEntityById(request.productTypeId());
        if (categoryProductRepository.existsByProductTypeIdAndNameIgnoreCaseAndIdNot(productType.getId(), name, id)) {
            throw new BusinessRuleException("Categoria já cadastrada.");
        }
        categoryEdit.setProductType(productType);
        categoryEdit.setName(name);
        return categoryProductMapper.toDto(categoryProductRepository.save(categoryEdit));
    }
}
