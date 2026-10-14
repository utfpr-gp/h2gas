package com.h2gas.controller;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.services.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Endereços")
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
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<AddressDTO> createAddress(@RequestBody AddressDTO addressDTO) {
        // nunca confiar no userId do corpo — o dono é sempre o usuário autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User usuarioLogado = (User) authentication.getPrincipal();
        addressDTO.setUserId(usuarioLogado.getId());
        AddressDTO created = addressService.createAddress(addressDTO);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/by-user/{userId}")
    @ResponseBody
    public ResponseEntity<AddressDTO> getAddressByUserId(@PathVariable String userId) {
        AddressDTO addressDTO = addressService.findByUserIdAddress(userId);
        return ResponseEntity.ok(addressDTO);
    }

    @Operation(summary = "Lista todos os endereços de um usuário",
            description = "Retorna os endereços cadastrados do usuário, do mais recente ao mais antigo. Só o próprio usuário (ou um ADMIN) pode consultar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de endereços do usuário"),
            @ApiResponse(responseCode = "403", description = "O usuário logado não é o dono dos endereços")
    })
    @GetMapping("/by-user/{userId}/all")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<List<AddressDTO>> getAllAddressesByUserId(
            @Parameter(description = "ID do usuário dono dos endereços") @PathVariable String userId) {
        User usuarioLogado = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = usuarioLogado.getUserType() == UserTypeEnum.ADMIN;
        if (!isAdmin && !usuarioLogado.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(addressService.findAllByUserIdAddress(userId));
    }

    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long id, @RequestBody AddressDTO dto) {
        AddressDTO updated = addressService.updateAddress(id, dto);
        return ResponseEntity.ok(updated);
    }

}
