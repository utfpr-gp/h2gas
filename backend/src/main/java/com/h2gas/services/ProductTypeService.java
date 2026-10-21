package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
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
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductTypeService {
    private static final String DUPLICATED_TYPE = "Tipo já cadastrado.";
    private static final String INVALID_ICON = "Ícone inválido.";
    static final Set<String> ICONS = Set.of(
            "local_fire_department", "propane_tank", "water_drop", "local_drink", "liquor", "ac_unit",
            "outdoor_grill", "cleaning_services", "shopping_basket", "inventory_2", ProductType.DEFAULT_ICON);

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


    // HU12: só o admin cria e renomeia; o nome é único sem diferenciar maiúsculas
    public ProductTypeDTO createProductType(ProductTypeDTO productTypeDTO) {
        String name = productTypeDTO.getName().trim();
        if (productTypeRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(DUPLICATED_TYPE);
        }
        ProductType productType = new ProductType();
        productType.setName(name);
        productType.setIcon(productTypeDTO.getIcon() == null ? ProductType.DEFAULT_ICON : validIcon(productTypeDTO.getIcon()));
        return productTypeMapper.toDto(productTypeRepository.save(productType));
    }

    public ProductTypeDTO updateProductType(Long id, ProductTypeDTO productTypeDTO) {
        ProductType productType = findEntityById(id);
        String name = productTypeDTO.getName().trim();
        if (productTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException(DUPLICATED_TYPE);
        }
        productType.setName(name);
        if (productTypeDTO.getIcon() != null) {
            productType.setIcon(validIcon(productTypeDTO.getIcon()));
        }
        return productTypeMapper.toDto(productTypeRepository.save(productType));
    }

    private String validIcon(String icon) {
        if (!ICONS.contains(icon)) {
            throw new BusinessRuleException(INVALID_ICON);
        }
        return icon;
    }
}
