package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.City;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.repository.CategoryProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public CategoryProductDTO createCategory(CategoryProductDTO cityDTO) {
        CategoryProduct categoryProduct = categoryProductMapper.toEntity(cityDTO);


//        cityRepository.save(city);
//        return cityMapper.toDto(city);
    }
}
