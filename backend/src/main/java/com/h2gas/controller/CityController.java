package com.h2gas.controller;

import com.cloudinary.http44.api.Response;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.services.CityService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<List<CityDTO>> getAllCities() {
        List<CityDTO> cityDTO = cityService.findAllCities();
        return ResponseEntity.ok(cityDTO);
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
