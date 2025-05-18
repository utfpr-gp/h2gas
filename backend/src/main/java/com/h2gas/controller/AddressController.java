package com.h2gas.controller;

import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.services.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressMapper addressMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<AddressDTO>> getAllAddresses() {
        List<AddressDTO> cityDTO = addressService.findAllAddresses();
        return ResponseEntity.ok(cityDTO);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<AddressDTO> createAddress(@RequestBody AddressDTO addressDTO) {
        addressService.createAddress(addressDTO);
        return ResponseEntity.ok(addressDTO);
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<AddressDTO> deleteCity(@RequestBody AddressDTO addressDTO) {
        addressService.createAddress(addressDTO);
        return ResponseEntity.ok(addressDTO);
    }

    @GetMapping("/by-user/{userId}")
    @ResponseBody
    public ResponseEntity<AddressDTO> getAddressByUserId(@PathVariable String userId) {
        AddressDTO addressDTO = addressService.findByUserIdAddress(userId);
        return ResponseEntity.ok(addressDTO);
    }

}
