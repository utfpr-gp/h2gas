package com.h2gas.controller;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.CityService;
import com.h2gas.util.PaginationUtil;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    @Autowired
    private CityService cityService;

    @Autowired
    private CityMapper cityMapper;

//    @RolesAllowed({"ADMIN", "SUPPLIER"})
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<CityDTO>> getAllCities() {
        List<CityDTO> cityDTO = cityService.findAllCities();
        return ResponseEntity.ok(cityDTO);
    }

    @GetMapping("/cities-page")
    @ResponseBody
    public ResponseEntity<PageResponse<CityDTO>> listarCidades(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<CityDTO> result = cityService.findAllCities(pageable);
        PageResponse<CityDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<CityDTO> createCity(@RequestBody CityDTO cityDTO) {
        cityService.createCity(cityDTO);
        return ResponseEntity.ok(cityDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Long> deleteCity(@PathVariable("id") Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(id);
    }
}
