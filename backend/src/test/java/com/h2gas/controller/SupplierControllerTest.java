package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private SupplierMapper supplierMapper;

    @Test
    void deveAceitarParametroCityIdOpcionalNoEndpointActivesOpen() throws Exception {
        SupplierDTO dto = new SupplierDTO();
        dto.setName("Fornecedor Curitiba");
        Page<SupplierDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        when(supplierService.findAllSuppliersOpenAndActive(eq(7L), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open").param("cityId", "7"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(eq(7L), any());
    }

    @Test
    void deveFuncionarSemCityIdMantendoComportamentoAtual() throws Exception {
        Page<SupplierDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(supplierService.findAllSuppliersOpenAndActive(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(isNull(), any());
    }
}
