package com.h2gas.controller;

import com.h2gas.model.dto.CityCreateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.CityService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cidades")
@RestController
@RequestMapping("/api/cities")
public class CityController {
    @Autowired
    private CityService cityService;

    @Autowired
    private CityMapper cityMapper;

    @Operation(summary = "Lista as cidades atendidas",
            description = "Todas as cidades atendidas, ordenadas por UF e nome, com o código IBGE "
                    + "(nulo nas cidades cadastradas antes dele). Usada nos cadastros de endereço e fornecedor")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Cidades atendidas")})
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

    @Operation(summary = "Passa a atender uma cidade (admin)",
            description = "Recebe só o código de município do IBGE; o nome e a UF oficiais vêm do IBGE, nunca do corpo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cidade cadastrada"),
            @ApiResponse(responseCode = "400", description = "Código ausente ou inexistente no IBGE"),
            @ApiResponse(responseCode = "403", description = "Papel diferente de ADMIN"),
            @ApiResponse(responseCode = "409", description = "Cidade já atendida"),
            @ApiResponse(responseCode = "502", description = "IBGE fora do ar")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<CityDTO> createCity(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Município escolhido na base do IBGE")
            @Valid @RequestBody CityCreateDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(body.ibgeCode()));
    }

    @Operation(summary = "Deixa de atender uma cidade (admin)",
            description = "Só exclui cidade sem fornecedores nem endereços cadastrados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cidade excluída; devolve o id"),
            @ApiResponse(responseCode = "403", description = "Papel diferente de ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cidade não encontrada"),
            @ApiResponse(responseCode = "409", description = "Cidade com fornecedores ou endereços")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Long> deleteCity(@Parameter(description = "Id da cidade") @PathVariable("id") Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(id);
    }
}
