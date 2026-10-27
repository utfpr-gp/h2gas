package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.enums.StorefrontSortEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSettingsDTO;
import com.h2gas.model.dto.SupplierProfileDTO;
import com.h2gas.model.dto.SupplierProfileRequestDTO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "jwt.secret=segredoFake123")
@Transactional
class SupplierServiceTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private com.h2gas.model.repository.OrderRepository orderRepository;

    @Autowired
    private com.h2gas.model.repository.ReviewRepository reviewRepository;

    private String validId;


    @BeforeEach
    public void setUp() {
        User user = new User(null,
                "Pablo Teste",
                "pablo@gmail.com",
                "1234545",
                "234234234",
                "4534534",
                UserTypeEnum.SUPPLIER
        );

        userRepository.save(user);

        User user2 = new User(null,
                "Teste novo",
                "pablodddd@gmail.com",
                "12dd34545",
                "123435345dd",
                "234234234",
                UserTypeEnum.SUPPLIER
        );

        userRepository.save(user2);

        City cityFirst = new City(
                null,
                "Pato Branco",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
                null,
                "rua teste fornecedor",
                "casa aqui",
                "123412343",
                "perto do lugar teste 3",
                "bairro do testado",
                "924839847",
                cityFirst,
                null
        );
        addressRepository.save(address);

        Address address2 = new Address(
                null,
                "rua teste fornecedor testando",
                "trabalho teste",
                "1234123433333",
                "perto do lugar teste 33",
                "bairro do testado2",
                "924839847",
                cityFirst,
                null
        );

        addressRepository.save(address2);
        Supplier supplierFirst = getSupplier(
                "Fornecedor Teste",
                "fornecedor@gmail",
                "1234567890",
                "12343445",
                "senhateste",
                "12345678901234",
                "Testando descricao",
                true,
                address2,
                cityFirst
        );


        supplierRepository.save(supplierFirst);

        Supplier supplierSecond = getSupplier(
                "Fornecedor Teste 2",
                "fornecedor2@gmail",
                "1234567890333",
                "12343445333",
                "senhateste2",
                "12345678901234",
                "Testando descricao 2",
                true,
                address,
                cityFirst
        );

        supplierRepository.save(supplierSecond);

        validId = supplierFirst.getId();
    }

    @NotNull
    private static Supplier getSupplier(
            String name,
            String email,
            String phoneNumber,
            String cpf,
            String password,
            String cnpj,
            String description,
            boolean active,
            Address address,
            City city
    ) {
        Supplier supplier = new Supplier();

        // Campos da superclasse User
        supplier.setId(null);
        supplier.setName(name);
        supplier.setEmail(email);
        supplier.setPhoneNumber(phoneNumber);
        supplier.setCpf(cpf);
        supplier.setPassword(password);
        supplier.setUserType(UserTypeEnum.SUPPLIER);

        // Campos específicos do Supplier
        supplier.setCnpj(cnpj);
        supplier.setDescription(description);
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(active);
        supplier.setAddress(address);
        supplier.setCity(city);
        supplier.setProductItems(null);

        return supplier;
    }


//    @Test
//    @DisplayName("Busca todos os fornecedores")
//    void findAllSuppliers() {
//        List<SupplierDTO> result = supplierService.findAllSuppliers();
//        assertEquals(2, result.size());
//        assertNotNull(result);
//        assertEquals("Fornecedor Teste", result.get(0).getName());
//        assertEquals("Fornecedor Teste 2", result.get(1).getName());
//    }

    @Test
    @DisplayName("Busca os fornecedores pelo id")
    void findByIdSuccess() {
        SupplierDTO result = supplierService.findById(validId);
        assertNotNull(result);
        assertEquals("Fornecedor Teste", result.getName());
    }

    @Test
    @DisplayName("Retorna erro ao informa id invalido")
    void findByIdError() {
        String idInvalid = "7fb34e6f-6c77-453f-8915-b814483f65bd";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.findById(idInvalid);
        });
        assertEquals("Não encontrado nenhum Fornecedor com esse id: " +
                idInvalid, exception.getMessage());
    }

    @Test
    @DisplayName("Deve criar um fornecedor com sucesso")
    void createSupplierSuccess() {
        City city = cityRepository.findAll().get(0);

        User newUser = new User(
                null,
                "Usuário Novo",
                "usernovo@gmail.com",
                "senha123",
                "123456789",
                "555666777",
                UserTypeEnum.SUPPLIER
        );
        userRepository.save(newUser);

        Address newAddress = new Address(
                null,
                "Rua Nova do Test333333e",
                "casa 123",
                "999999",
                "Perto da praça",
                "Bairro Central",
                "8888888",
                city,
                null
        );
        addressRepository.save(newAddress);
        Supplier supplierCreate = getSupplier(
                "Fornecedor Novo",
                "fornecedorNovo@gmail",
                "1234567890333",
                "12343445333",
                "senhateste2",
                "12345678901234",
                "Testando descricao 2",
                true,
                newAddress,
                city
        );
//        SupplierCreateDTO dto = new SupplierCreateDTO();
//        dto.setName("Fornecedor Novo");
//        dto.setDescription("Fornecedor criado no teste");
//        dto.setOpeningTime(String.valueOf(LocalTime.of(9, 0)));
//        dto.setClosingTime(String.valueOf(LocalTime.of(17, 0)));
//        dto.setCityId(city.getId());
//        dto.setAddressId(newAddress.getId());
//        dto.setUserId(newUser.getId());

        SupplierDTO result = supplierMapper.toDto(supplierService.createSupplier(supplierCreate));

        assertNotNull(result);
        assertEquals("Fornecedor Novo", result.getName());

        Supplier fornecedorSalvo = supplierRepository.findById(result.getId()).orElse(null);
        assertNotNull(fornecedorSalvo);
        assertEquals("Fornecedor Novo", fornecedorSalvo.getName());
    }



//
//    @Test
//    void deleteSupplier() {
//    }

    @Test
    @DisplayName("Deve atualizar o status de ativo do fornecedor com sucesso")
    void updateStatusSupplier() {
        supplierService.updateStatusSupplier(validId, false);
        Supplier atualizado = supplierRepository.findById(validId).orElse(null);
        assertNotNull(atualizado);
        assertFalse(atualizado.isActive());
    }

    @Test
    @DisplayName("HU01: deve filtrar fornecedores por cidade quando cityId é informado")
    void deveFiltrarFornecedoresPorCidadeQuandoCityIdInformado() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));

        User userCuritiba = userRepository.save(new User(null,
                "Fornecedor Curitiba", "fornecedorcuritiba@gmail.com",
                "senha123", "111222333", "444555666", UserTypeEnum.SUPPLIER));

        Address addressCuritiba = addressRepository.save(new Address(
                null, "Rua Curitiba", "loja", "100", "perto do centro",
                "Centro", "80000-000", curitiba, null));

        Supplier supplierCuritiba = getSupplier(
                "Fornecedor Curitiba", "fornecedorcuritiba@gmail",
                "111222333", "999888777", "senhateste",
                "98765432101234", "Fornecedor de Curitiba",
                true, addressCuritiba, curitiba);
        supplierRepository.save(supplierCuritiba);

        Pageable pageable = PageRequest.of(0, 10);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Fornecedor Curitiba", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("HU01: deve retornar todos os ativos e abertos quando cityId é nulo")
    void deveRetornarTodosAtivosEAbertosQuandoCityIdNulo() {
        City cityAberta = cityRepository.save(new City(null, "Guarapuava", "PR"));

        User userAberto = userRepository.save(new User(null,
                "Fornecedor Aberto", "fornecedoraberto@gmail.com",
                "senha123", "222333444", "555666777", UserTypeEnum.SUPPLIER));

        Address addressAberta = addressRepository.save(new Address(
                null, "Rua Aberta", "loja", "200", "perto do centro",
                "Centro", "85000-000", cityAberta, null));

        Supplier supplierAberto = getSupplier(
                "Fornecedor Aberto", "fornecedoraberto@gmail",
                "222333444", "888777666", "senhateste",
                "12312312312345", "Fornecedor sempre aberto",
                true, addressAberta, cityAberta);
        supplierRepository.save(supplierAberto);

        Pageable pageable = PageRequest.of(0, 10);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(null, pageable);

        assertTrue(result.getContent().stream()
                .anyMatch(dto -> "Fornecedor Aberto".equals(dto.getName())));
    }

    @Test
    @DisplayName("F7: tempo médio de entrega não numérico é 400 com mensagem clara")
    void deveRejeitarTempoMedioDeEntregaNaoNumericoComMensagemClara() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> supplierService.updateTimesSupplier(validId, java.util.Map.of("averageDeliveryTime", "meia hora")));

        assertEquals("Tempo médio de entrega inválido: informe os minutos.", erro.getMessage());
    }

    @Test
    @DisplayName("F7 fatia 6: lê horário, tempo de entrega e estoque mínimo do fornecedor")
    void deveLerConfiguracoesDoProprioFornecedor() {
        SupplierSettingsDTO settings = supplierService.findMySettings(validId);

        assertEquals("08:00", settings.openingTime());
        assertEquals("18:00", settings.closingTime());
        assertEquals(30, settings.averageDeliveryTime());
        assertEquals(8, settings.lowStockThreshold());
    }

    @Test
    @DisplayName("F7 fatia 6: salva horário (até depois da meia-noite), tempo de entrega e estoque mínimo")
    void deveSalvarHorarioTempoEEstoqueMinimo() {
        SupplierSettingsDTO salvo = supplierService.updateMySettings(validId,
                new SupplierSettingsDTO("18:00", "02:00", 45, 5));

        Supplier supplier = supplierRepository.findById(validId).orElseThrow();
        assertEquals(LocalTime.of(18, 0), supplier.getOpeningTime());
        assertEquals(LocalTime.of(2, 0), supplier.getClosingTime());
        assertEquals(Duration.ofMinutes(45), supplier.getAverageDeliveryTime());
        assertEquals(5, supplier.getLowStockThreshold());
        assertEquals(new SupplierSettingsDTO("18:00", "02:00", 45, 5), salvo);
    }

    @Test
    @DisplayName("F7 fatia 6: fechamento igual à abertura é 400")
    void deveRecusarFechamentoIgualAAbertura() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> supplierService.updateMySettings(validId, new SupplierSettingsDTO("08:00", "08:00", 30, 8)));

        assertEquals("O horário de fechamento precisa ser diferente da abertura.", erro.getMessage());
    }

    private Supplier salvarFornecedor(String nome, String email, String cnpj, boolean active) {
        City city = cityRepository.save(new City(null, "Francisco Beltrão", "PR"));
        Address address = addressRepository.save(new Address(
                null, "Rua " + nome, "loja", "10", "centro", "Centro", "85600-000", city, null));
        return supplierRepository.save(getSupplier(nome, email, "46999990000", "000111222",
                "senhateste", cnpj, "descricao", active, address, city));
    }

    @Test
    @DisplayName("F7 fatia 6: active=false traz só quem aguarda aprovação")
    void deveFiltrarFornecedoresAguardandoAprovacao() {
        salvarFornecedor("Zeta Gás Aguardando", "zeta@h2gas.test", "11222333000144", false);

        Page<SupplierDTO> aguardando = supplierService.findAllSuppliers(false, null, PageRequest.of(0, 50));
        Page<SupplierDTO> ativos = supplierService.findAllSuppliers(true, null, PageRequest.of(0, 50));

        assertTrue(aguardando.getContent().stream().noneMatch(SupplierDTO::isActive));
        assertTrue(aguardando.getContent().stream().anyMatch(dto -> "Zeta Gás Aguardando".equals(dto.getName())));
        assertTrue(ativos.getContent().stream().noneMatch(dto -> "Zeta Gás Aguardando".equals(dto.getName())));
        assertTrue(ativos.getContent().stream().anyMatch(dto -> "Fornecedor Teste".equals(dto.getName())));
    }

    @Test
    @DisplayName("F7 fatia 6: busca por nome, e-mail ou CNPJ sem diferenciar maiúsculas")
    void deveBuscarFornecedoresPorNomeEmailOuCnpj() {
        salvarFornecedor("Ômega Água Mineral", "contato@omega.test", "99888777000166", false);

        assertEquals(1, supplierService.findAllSuppliers(null, "ômega água", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierService.findAllSuppliers(null, "CONTATO@OMEGA", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierService.findAllSuppliers(false, "99888777", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, supplierService.findAllSuppliers(true, "99888777", PageRequest.of(0, 10)).getTotalElements());
    }

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    @DisplayName("F7 fatia 6: perfil da loja muda nome, telefone e descrição sem mexer no horário")
    void deveAtualizarPerfilSemMexerNoHorario() {
        SupplierProfileDTO perfil = supplierService.updateMyProfile(validId,
                new SupplierProfileRequestDTO("  Gás Nova Era ", "46999990000", "Entrega rápida no centro"));

        Supplier supplier = supplierRepository.findById(validId).orElseThrow();
        assertEquals("Gás Nova Era", supplier.getName());
        assertEquals("46999990000", supplier.getPhoneNumber());
        assertEquals("Entrega rápida no centro", supplier.getDescription());
        assertEquals(LocalTime.of(8, 0), supplier.getOpeningTime());
        assertEquals(LocalTime.of(18, 0), supplier.getClosingTime());
        assertEquals(Duration.ofMinutes(30), supplier.getAverageDeliveryTime());
        assertEquals("Gás Nova Era", perfil.name());
        assertEquals("12345678901234", perfil.cnpj());
        assertEquals("Pato Branco", perfil.cityName());
        assertEquals("PR", perfil.cityState());
    }

    @Test
    @DisplayName("F7 fatia 6: o perfil da loja traz o e-mail da conta (só leitura)")
    void deveTrazerOEmailNoPerfil() {
        assertEquals("fornecedor@gmail", supplierService.findMyProfile(validId).email());
    }

    @Test
    @DisplayName("F7 fatia 6: logo enviada ao Cloudinary, gravada e removida")
    void deveSalvarERemoverALogo() throws Exception {
        when(cloudinaryService.upload(any())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/h2gas/logo.png", "url", "http://x/logo.png"));

        String url = supplierService.updateMyLogo(validId, new MockMultipartFile("file", "logo.png", "image/png", new byte[2048]));

        assertEquals("https://res.cloudinary.com/h2gas/logo.png", url);
        assertEquals(url, supplierRepository.findById(validId).orElseThrow().getLogoUrl());
        assertEquals(url, supplierService.findMyProfile(validId).logoUrl());

        supplierService.removeMyLogo(validId);
        assertNull(supplierRepository.findById(validId).orElseThrow().getLogoUrl());
    }

    @Test
    @DisplayName("F7 fatia 6: arquivo que não é JPG, PNG ou WEBP é 400")
    void deveRecusarArquivoQueNaoEImagem() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class, () -> supplierService.updateMyLogo(validId,
                new MockMultipartFile("file", "logo.gif", "image/gif", new byte[100])));

        assertEquals("Envie uma imagem JPG, PNG ou WEBP de até 1 MB.", erro.getMessage());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("F7 fatia 6: imagem acima de 1 MB é 400")
    void deveRecusarImagemMaiorQue1MB() {
        assertThrows(BusinessRuleException.class, () -> supplierService.updateMyLogo(validId,
                new MockMultipartFile("file", "logo.jpg", "image/jpeg", new byte[1_048_577])));
        assertThrows(BusinessRuleException.class, () -> supplierService.updateMyLogo(validId,
                new MockMultipartFile("file", "logo.jpg", "image/jpeg", new byte[0])));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("F7 fatia 6: Cloudinary recusando o envio (credencial, rede) vira 502 com mensagem clara")
    void deveResponder502QuandoOCloudinaryRecusa() throws Exception {
        when(cloudinaryService.upload(any())).thenThrow(new RuntimeException("Invalid Signature abc"));

        org.springframework.web.server.ResponseStatusException erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> supplierService.updateMyLogo(validId, new MockMultipartFile("file", "logo.png", "image/png", new byte[10])));

        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        assertEquals("Não foi possível enviar a imagem. Tente novamente.", erro.getReason());
        assertNull(supplierRepository.findById(validId).orElseThrow().getLogoUrl());
    }

    @Test
    @DisplayName("F7 §5.21: busca de lojas abertas pelo nome, só na cidade do endereço (RN05)")
    void deveBuscarLojasAbertasDaCidadePeloNome() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        City londrina = cityRepository.save(new City(null, "Londrina", "PR"));
        for (Object[] loja : new Object[][]{{"Gás Azul Curitiba", curitiba, true}, {"Gás Azul Londrina", londrina, true},
                {"Gás Azul Fechado", curitiba, false}, {"Água Verde Curitiba", curitiba, true}}) {
            Address a = addressRepository.save(new Address(null, "Rua " + loja[0], "loja", "1", "c", "Centro", "80000-000", (City) loja[1], null));
            Supplier fornecedor = getSupplier((String) loja[0], loja[0].toString().replace(" ", "") + "@t", "41900000000", "1",
                    "s", "11222333000144", "d", true, a, (City) loja[1]);
            if (!(Boolean) loja[2]) {
                // HU18: fechada pelo horário (o relógio dos testes marca 10:00)
                fornecedor.setOpeningTime(LocalTime.of(19, 0));
                fornecedor.setClosingTime(LocalTime.of(23, 0));
            }
            supplierRepository.save(fornecedor);
        }

        Page<com.h2gas.model.dto.StorefrontDTO> result = supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), "GÁS AZUL", null, StorefrontSortEnum.NAME, PageRequest.of(0, 10));

        assertEquals(List.of("Gás Azul Curitiba"), result.getContent().stream().map(com.h2gas.model.dto.StorefrontDTO::name).toList());
        assertEquals(2, supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), null, null, StorefrontSortEnum.NAME, PageRequest.of(0, 10)).getTotalElements());
    }

    private void avaliar(Supplier loja, User cliente, int nota) {
        avaliar(loja, cliente, nota, null, null);
    }

    private void avaliar(Supplier loja, User cliente, int nota, String comentario, java.time.LocalDateTime entregueEm) {
        com.h2gas.model.entity.Order pedido = new com.h2gas.model.entity.Order();
        pedido.setCustomer(cliente);
        pedido.setSupplier(loja);
        pedido.setStatus(com.h2gas.enums.OrderStatusEnum.DELIVERED);
        pedido.setDeliveredAt(entregueEm);
        pedido = orderRepository.save(pedido);
        com.h2gas.model.entity.Review avaliacao = new com.h2gas.model.entity.Review();
        avaliacao.setOrder(pedido);
        avaliacao.setSupplier(loja);
        avaliacao.setRating(nota);
        avaliacao.setComment(comentario);
        reviewRepository.save(avaliacao);
    }

    private Supplier lojaAberta(String nome, City cidade) {
        Address a = addressRepository.save(new Address(null, "Rua " + nome, "loja", "1", "c", "Centro", "80000-000", cidade, null));
        return supplierRepository.save(getSupplier(nome, nome.replace(" ", "") + "@t", "41900000000", "1",
                "s", "11222333000144", "d", true, a, cidade));
    }

    @Test
    @DisplayName("F7 §5.25: catálogo traz média (1 casa) e contagem de avaliações por loja")
    void deveTrazerMediaEContagemDeAvaliacoesNoCatalogo() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = userRepository.save(new User(null, "Cliente", "cli@t", "41999990000", "99988877766", "x", UserTypeEnum.CUSTOMER));
        Supplier avaliada = lojaAberta("Loja Avaliada", curitiba);
        lojaAberta("Loja Sem Nota", curitiba);
        avaliar(avaliada, cliente, 5);
        avaliar(avaliada, cliente, 4);
        avaliar(avaliada, cliente, 4);

        Page<com.h2gas.model.dto.StorefrontDTO> result = supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), null, null, StorefrontSortEnum.NAME, PageRequest.of(0, 10));

        com.h2gas.model.dto.StorefrontDTO dto = result.getContent().stream().filter(d -> d.name().equals("Loja Avaliada")).findFirst().orElseThrow();
        assertEquals(4.3, dto.averageRating());
        assertEquals(3L, dto.reviewCount());
    }

    @Test
    @DisplayName("F7 §5.25: loja sem avaliação vem com média nula e contagem zero")
    void deveTrazerNulaESemContagemQuandoAindaNaoHaAvaliacao() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        lojaAberta("Loja Sem Nota", curitiba);

        com.h2gas.model.dto.StorefrontDTO dto = supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), null, null, StorefrontSortEnum.NAME, PageRequest.of(0, 10)).getContent().get(0);

        assertNull(dto.averageRating());
        assertEquals(0L, dto.reviewCount());
    }

    @Test
    @DisplayName("F7 §5.26: banner enviado ao Cloudinary e gravado na loja do fornecedor")
    void deveSalvarOBannerEnviadoPeloFornecedor() throws Exception {
        when(cloudinaryService.upload(any())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/h2gas/banner.jpg"));

        String url = supplierService.updateMyBanner(validId, new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[1_500_000]));

        assertEquals("https://res.cloudinary.com/h2gas/banner.jpg", url);
        assertEquals(url, supplierRepository.findById(validId).orElseThrow().getBannerUrl());
        assertEquals(url, supplierService.findMyProfile(validId).bannerUrl());
        assertEquals(url, supplierService.findById(validId).getBannerUrl());
    }

    @Test
    @DisplayName("F7 §5.26: banner que não é JPG, PNG ou WEBP, vazio ou acima de 2 MB é 400")
    void deveRecusarBannerDeOutroTipoOuAcimaDe2MB() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class, () -> supplierService.updateMyBanner(validId,
                new MockMultipartFile("file", "banner.gif", "image/gif", new byte[100])));
        assertEquals("Envie uma imagem JPG, PNG ou WEBP de até 2 MB.", erro.getMessage());
        assertThrows(BusinessRuleException.class, () -> supplierService.updateMyBanner(validId,
                new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[2_097_153])));
        assertThrows(BusinessRuleException.class, () -> supplierService.updateMyBanner(validId,
                new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[0])));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("F7 §5.26: remover o banner volta para o padrão")
    void deveRemoverOBanner() {
        Supplier loja = supplierRepository.findById(validId).orElseThrow();
        loja.setBannerUrl("https://res.cloudinary.com/h2gas/banner.jpg");
        supplierRepository.save(loja);

        supplierService.removeMyBanner(validId);

        assertNull(supplierRepository.findById(validId).orElseThrow().getBannerUrl());
    }

    @Test
    @DisplayName("F7 §5.26: a leitura da loja traz média e contagem de avaliações")
    void deveTrazerNotaNaLeituraDaLoja() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = userRepository.save(new User(null, "Cliente", "cli2@t", "41999990001", "99988877700", "x", UserTypeEnum.CUSTOMER));
        Supplier loja = lojaAberta("Loja Lida", curitiba);
        avaliar(loja, cliente, 5);
        avaliar(loja, cliente, 4);

        SupplierDTO dto = supplierService.findById(loja.getId());

        assertEquals(4.5, dto.getAverageRating());
        assertEquals(2L, dto.getReviewCount());
    }

    @Test
    @DisplayName("F7 §5.27: lista as avaliações da loja só com o primeiro nome do cliente, nota, comentário e data")
    void deveListarAvaliacoesComPrimeiroNomeEComentario() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = userRepository.save(new User(null, "Maria Aparecida Souza", "maria@t", "41999990002", "99988877711", "x", UserTypeEnum.CUSTOMER));
        Supplier loja = lojaAberta("Loja Comentada", curitiba);
        java.time.LocalDateTime entrega = java.time.LocalDateTime.of(2026, 10, 18, 14, 30);
        avaliar(loja, cliente, 4, "Entrega rápida, recomendo.", entrega);

        Page<com.h2gas.model.dto.StoreReviewDTO> page = supplierService.findReviews(loja.getId(), PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        com.h2gas.model.dto.StoreReviewDTO review = page.getContent().get(0);
        assertEquals("Maria", review.customerFirstName());
        assertEquals(4, review.rating());
        assertEquals("Entrega rápida, recomendo.", review.comment());
        assertEquals(entrega, review.reviewedAt());
    }

    @Test
    @DisplayName("F7 §5.27: avaliações vêm da entrega mais recente para a mais antiga, paginadas")
    void deveOrdenarAvaliacoesPelaEntregaMaisRecente() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User ana = userRepository.save(new User(null, "Ana", "ana@t", "41999990003", "99988877722", "x", UserTypeEnum.CUSTOMER));
        User bruno = userRepository.save(new User(null, "Bruno Lima", "bruno@t", "41999990004", "99988877733", "x", UserTypeEnum.CUSTOMER));
        Supplier loja = lojaAberta("Loja Ordenada", curitiba);
        Supplier outra = lojaAberta("Outra Loja", curitiba);
        avaliar(loja, ana, 5, null, java.time.LocalDateTime.of(2026, 10, 1, 10, 0));
        avaliar(loja, bruno, 3, null, java.time.LocalDateTime.of(2026, 10, 20, 10, 0));
        avaliar(loja, ana, 4, null, java.time.LocalDateTime.of(2026, 10, 10, 10, 0));
        avaliar(outra, bruno, 1, "não deve aparecer", java.time.LocalDateTime.of(2026, 10, 21, 10, 0));

        Page<com.h2gas.model.dto.StoreReviewDTO> primeira = supplierService.findReviews(loja.getId(), PageRequest.of(0, 2));

        assertEquals(3, primeira.getTotalElements());
        assertEquals(List.of("Bruno", "Ana"), primeira.getContent().stream().map(com.h2gas.model.dto.StoreReviewDTO::customerFirstName).toList());
        assertEquals(List.of(3, 4), primeira.getContent().stream().map(com.h2gas.model.dto.StoreReviewDTO::rating).toList());
        assertEquals(5, supplierService.findReviews(loja.getId(), PageRequest.of(1, 2)).getContent().get(0).rating());
    }

    @Test
    @DisplayName("F7 §5.27: comentário vazio ou só com espaços vem nulo")
    void deveTratarComentarioEmBrancoComoNulo() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = userRepository.save(new User(null, "  Carlos  Eduardo ", "carlos@t", "41999990005", "99988877744", "x", UserTypeEnum.CUSTOMER));
        Supplier loja = lojaAberta("Loja Sem Texto", curitiba);
        avaliar(loja, cliente, 5, "   ", null);

        com.h2gas.model.dto.StoreReviewDTO review = supplierService.findReviews(loja.getId(), PageRequest.of(0, 10)).getContent().get(0);

        assertNull(review.comment());
        assertNull(review.reviewedAt());
        assertEquals("Carlos", review.customerFirstName());
    }

    @Test
    @DisplayName("F7 §5.27: avaliações de loja inexistente é 404")
    void deveResponder404AoListarAvaliacoesDeLojaInexistente() {
        assertThrows(com.h2gas.exceptions.ResourceNotFoundException.class, () -> supplierService.findReviews("nao-existe", PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("HU13: a vitrine da loja traz só o que o visitante vê (sem e-mail, CNPJ, telefone e endereço completo), com a nota")
    void deveMontarAVitrineSemDadosInternos() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = userRepository.save(new User(null, "Cliente Vitrine", "vitrine@t", "41999990009", "99988877799", "x", UserTypeEnum.CUSTOMER));
        Supplier loja = lojaAberta("Loja Vitrine", curitiba);
        avaliar(loja, cliente, 5);

        com.h2gas.model.dto.StorefrontDTO vitrine = supplierService.findStorefront(loja.getId());

        assertEquals("Loja Vitrine", vitrine.name());
        assertEquals("Curitiba", vitrine.cityName());
        assertEquals(curitiba.getId(), vitrine.cityId());
        assertEquals("Centro", vitrine.addressNeighborhood());
        assertEquals(5.0, vitrine.averageRating());
        assertEquals(1L, vitrine.reviewCount());
        List<String> campos = java.util.Arrays.stream(com.h2gas.model.dto.StorefrontDTO.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        for (String interno : List.of("email", "cnpj", "phone", "addressStreetName", "addressNumber", "addressComplement", "products")) {
            assertFalse(campos.contains(interno), "a vitrine não pode expor " + interno);
        }
    }

    @Test
    @DisplayName("HU13: vitrine de loja inexistente é 404")
    void deveResponder404NaVitrineDeLojaInexistente() {
        assertThrows(com.h2gas.exceptions.ResourceNotFoundException.class, () -> supplierService.findStorefront("nao-existe"));
    }

    // ---- HU15: tipos de produto na vitrine ----

    @Autowired
    private com.h2gas.model.repository.ProductTypeRepository productTypeRepository;

    @Autowired
    private com.h2gas.model.repository.CategoryProductRepository categoryProductRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private com.h2gas.model.entity.ProductType tipo(String nome, String icone) {
        com.h2gas.model.entity.ProductType tipo = new com.h2gas.model.entity.ProductType();
        tipo.setName(nome);
        tipo.setIcon(icone);
        return productTypeRepository.save(tipo);
    }

    private void categoria(Supplier loja, com.h2gas.model.entity.ProductType tipo, String nome) {
        com.h2gas.model.entity.CategoryProduct categoria = new com.h2gas.model.entity.CategoryProduct();
        categoria.setName(nome);
        categoria.setProductType(tipo);
        categoria.setSupplier(loja);
        categoryProductRepository.save(categoria);
    }

    private List<com.h2gas.model.dto.StorefrontDTO> vitrine(Long cityId, String q, Long productTypeId) {
        return supplierService.findAllSuppliersOpenAndActive(cityId, q, productTypeId, StorefrontSortEnum.NAME, PageRequest.of(0, 10)).getContent();
    }

    @Test
    @DisplayName("HU15: a vitrine traz os tipos que a loja vende, com o ícone do tipo")
    void deveTrazerOsTiposDeProdutoDaLoja() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier loja = lojaAberta("Loja Completa", curitiba);
        categoria(loja, tipo("Gás", "local_fire_department"), "P13");
        categoria(loja, tipo("Água", "water_drop"), "Galão 20L");

        com.h2gas.model.dto.StorefrontDTO dto = vitrine(curitiba.getId(), null, null).get(0);

        assertEquals(List.of("Água", "Gás"), dto.productTypes().stream().map(com.h2gas.model.dto.ProductTypeChipDTO::name).toList());
        assertEquals(List.of("water_drop", "local_fire_department"),
                dto.productTypes().stream().map(com.h2gas.model.dto.ProductTypeChipDTO::icon).toList());
    }

    @Test
    @DisplayName("HU15: tipo não se repete quando a loja tem várias categorias dele")
    void naoDeveRepetirTipoQuandoALojaTemVariasCategorias() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier loja = lojaAberta("Loja Só de Gás", curitiba);
        com.h2gas.model.entity.ProductType gas = tipo("Gás", "local_fire_department");
        categoria(loja, gas, "P13");
        categoria(loja, gas, "P45");

        com.h2gas.model.dto.StorefrontDTO dto = vitrine(curitiba.getId(), null, null).get(0);

        assertEquals(List.of("Gás"), dto.productTypes().stream().map(com.h2gas.model.dto.ProductTypeChipDTO::name).toList());
    }

    @Test
    @DisplayName("HU15: loja sem categoria vem com lista de tipos vazia, nunca nula")
    void deveDevolverListaVaziaDeTiposParaLojaSemCategoria() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        lojaAberta("Loja Sem Catálogo", curitiba);

        com.h2gas.model.dto.StorefrontDTO dto = vitrine(curitiba.getId(), null, null).get(0);

        assertNotNull(dto.productTypes());
        assertTrue(dto.productTypes().isEmpty());
    }

    @Test
    @DisplayName("HU15: o chip de tipo filtra as lojas que vendem aquele tipo")
    void deveFiltrarLojasPeloTipoDeProduto() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        com.h2gas.model.entity.ProductType gas = tipo("Gás", "local_fire_department");
        com.h2gas.model.entity.ProductType agua = tipo("Água", "water_drop");
        categoria(lojaAberta("Só Gás", curitiba), gas, "P13");
        categoria(lojaAberta("Só Água", curitiba), agua, "Galão 20L");
        lojaAberta("Sem Catálogo", curitiba);

        assertEquals(List.of("Só Gás"), vitrine(curitiba.getId(), null, gas.getId()).stream()
                .map(com.h2gas.model.dto.StorefrontDTO::name).toList());
        assertEquals(List.of("Só Água"), vitrine(curitiba.getId(), null, agua.getId()).stream()
                .map(com.h2gas.model.dto.StorefrontDTO::name).toList());
        assertEquals(3, vitrine(curitiba.getId(), null, null).size());
    }

    @Test
    @DisplayName("HU15: tipo inexistente devolve página vazia, não erro")
    void deveDevolverPaginaVaziaParaTipoInexistente() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        categoria(lojaAberta("Só Gás", curitiba), tipo("Gás", "local_fire_department"), "P13");

        assertTrue(vitrine(curitiba.getId(), null, 999_999L).isEmpty());
    }

    @Test
    @DisplayName("HU15: tipo, cidade (RN05) e busca valem juntos")
    void deveCombinarTipoCidadeEBusca() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        City londrina = cityRepository.save(new City(null, "Londrina", "PR"));
        com.h2gas.model.entity.ProductType gas = tipo("Gás", "local_fire_department");
        categoria(lojaAberta("Gás Azul Curitiba", curitiba), gas, "P13");
        categoria(lojaAberta("Gás Azul Londrina", londrina), gas, "P13");
        categoria(lojaAberta("Gás Verde Curitiba", curitiba), gas, "P13");
        categoria(lojaAberta("Água Azul Curitiba", curitiba), tipo("Água", "water_drop"), "Galão 20L");

        assertEquals(List.of("Gás Azul Curitiba"), vitrine(curitiba.getId(), "azul", gas.getId()).stream()
                .map(com.h2gas.model.dto.StorefrontDTO::name).toList());
    }

    @Test
    @DisplayName("HU15: os tipos da página saem em uma consulta só (não cresce com o número de lojas)")
    void deveBuscarOsTiposEmUmaConsultaSoParaAPagina() {
        City umaLoja = cityRepository.save(new City(null, "Cidade de Uma Loja", "PR"));
        City muitasLojas = cityRepository.save(new City(null, "Cidade de Muitas Lojas", "PR"));
        com.h2gas.model.entity.ProductType gas = tipo("Gás", "local_fire_department");
        categoria(lojaAberta("Única", umaLoja), gas, "P13");
        for (String nome : List.of("Primeira", "Segunda", "Terceira")) {
            categoria(lojaAberta(nome, muitasLojas), gas, "P13");
        }

        org.hibernate.stat.Statistics estatisticas = entityManager.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        entityManager.flush();

        estatisticas.clear();
        vitrine(umaLoja.getId(), null, null);
        long consultasComUmaLoja = estatisticas.getPrepareStatementCount();

        estatisticas.clear();
        List<com.h2gas.model.dto.StorefrontDTO> tres = vitrine(muitasLojas.getId(), null, null);
        long consultasComTresLojas = estatisticas.getPrepareStatementCount();

        assertEquals(3, tres.size());
        assertEquals(consultasComUmaLoja, consultasComTresLojas);
    }

    // ---- HU16: ordenação da vitrine ----

    private List<String> nomes(Long cityId, String q, Long tipo, StorefrontSortEnum ordem) {
        return supplierService.findAllSuppliersOpenAndActive(cityId, q, tipo, ordem, PageRequest.of(0, 10))
                .getContent().stream().map(com.h2gas.model.dto.StorefrontDTO::name).toList();
    }

    private Supplier comTempo(Supplier loja, Integer minutos) {
        loja.setAverageDeliveryTime(minutos == null ? null : Duration.ofMinutes(minutos));
        return supplierRepository.save(loja);
    }

    private Supplier fechada(String nome, City cidade) {
        Supplier loja = lojaAberta(nome, cidade);
        // HU18: fechada pelo horário (o relógio dos testes marca 10:00)
        loja.setOpeningTime(java.time.LocalTime.of(19, 0));
        loja.setClosingTime(java.time.LocalTime.of(23, 0));
        return supplierRepository.save(loja);
    }

    private User cliente() {
        return userRepository.save(new User(null, "Cliente", "cliente-hu16@t", "41999990000", "99988877766", "x",
                UserTypeEnum.CUSTOMER));
    }

    @Test
    @DisplayName("HU16: melhor avaliação vem da maior para a menor média")
    void deveOrdenarPorMelhorAvaliacao() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = cliente();
        Supplier media = lojaAberta("A Média", curitiba);
        Supplier otima = lojaAberta("B Ótima", curitiba);
        avaliar(media, cliente, 3);
        avaliar(otima, cliente, 5);

        assertEquals(List.of("B Ótima", "A Média"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.RATING));
    }

    @Test
    @DisplayName("HU16: loja sem avaliação fica no fim da ordenação por nota")
    void deveColocarLojaSemAvaliacaoNoFim() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        User cliente = cliente();
        lojaAberta("A Sem Nota", curitiba);
        avaliar(lojaAberta("B Nota Baixa", curitiba), cliente, 1);

        assertEquals(List.of("B Nota Baixa", "A Sem Nota"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.RATING));
    }

    @Test
    @DisplayName("HU16: menor tempo de entrega primeiro")
    void deveOrdenarPorMenorTempoDeEntrega() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comTempo(lojaAberta("A Demorada", curitiba), 90);
        comTempo(lojaAberta("B Rápida", curitiba), 20);
        comTempo(lojaAberta("C Média", curitiba), 45);

        assertEquals(List.of("B Rápida", "C Média", "A Demorada"),
                nomes(curitiba.getId(), null, null, StorefrontSortEnum.DELIVERY_TIME));
    }

    @Test
    @DisplayName("HU16: loja sem tempo cadastrado fica no fim da ordenação por tempo")
    void deveColocarLojaSemTempoNoFim() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comTempo(lojaAberta("A Sem Tempo", curitiba), null);
        comTempo(lojaAberta("B Com Tempo", curitiba), 60);

        assertEquals(List.of("B Com Tempo", "A Sem Tempo"),
                nomes(curitiba.getId(), null, null, StorefrontSortEnum.DELIVERY_TIME));
    }

    @Test
    @DisplayName("HU16: abertas primeiro traz também as fechadas, depois das abertas")
    void deveListarAbertasPrimeiroIncluindoFechadas() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        fechada("A Fechada", curitiba);
        lojaAberta("B Aberta", curitiba);

        assertEquals(List.of("B Aberta", "A Fechada"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.OPEN_FIRST));
    }

    @Test
    @DisplayName("HU16: nas demais ordenações a vitrine segue só com lojas abertas")
    void naoDeveTrazerLojaFechadaNasDemaisOrdenacoes() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        fechada("A Fechada", curitiba);
        lojaAberta("B Aberta", curitiba);

        for (StorefrontSortEnum ordem : List.of(StorefrontSortEnum.NAME, StorefrontSortEnum.RATING,
                StorefrontSortEnum.DELIVERY_TIME)) {
            assertEquals(List.of("B Aberta"), nomes(curitiba.getId(), null, null, ordem), ordem.name());
        }
    }

    @Test
    @DisplayName("HU16: cidade (RN05), busca e tipo continuam valendo ao ordenar")
    void deveManterCidadeBuscaETipoAoOrdenar() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        City londrina = cityRepository.save(new City(null, "Londrina", "PR"));
        com.h2gas.model.entity.ProductType gas = tipo("Gás", "local_fire_department");
        categoria(comTempo(lojaAberta("Gás Azul Curitiba", curitiba), 30), gas, "P13");
        categoria(comTempo(lojaAberta("Gás Azul Londrina", londrina), 10), gas, "P13");
        comTempo(lojaAberta("Gás Azul Sem Catálogo", curitiba), 5);
        categoria(comTempo(lojaAberta("Água Verde Curitiba", curitiba), 1), gas, "P13");

        assertEquals(List.of("Gás Azul Curitiba"),
                nomes(curitiba.getId(), "azul", gas.getId(), StorefrontSortEnum.DELIVERY_TIME));
    }

    // ---- HU18: aberta pelo horário (o relógio dos testes marca 10:00 em Brasília) ----

    private Supplier comHorario(Supplier loja, Integer abre, Integer fecha) {
        loja.setOpeningTime(abre == null ? null : LocalTime.of(abre, 0));
        loja.setClosingTime(fecha == null ? null : LocalTime.of(fecha, 0));
        return supplierRepository.save(loja);
    }

    @Test
    @DisplayName("HU18: a vitrine traz só as lojas dentro do horário")
    void deveListarSoLojasDentroDoHorario() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comHorario(lojaAberta("A Dentro", curitiba), 8, 18);
        comHorario(lojaAberta("B Abre Mais Tarde", curitiba), 11, 20);
        comHorario(lojaAberta("C Já Fechou", curitiba), 6, 10);

        assertEquals(List.of("A Dentro"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.NAME));
    }

    @Test
    @DisplayName("HU18: horário que vira a meia-noite vale na janela certa")
    void deveIncluirLojaDaMadrugadaNaJanelaCertaDoHorario() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comHorario(lojaAberta("A Vira A Noite E Vai Até 11h", curitiba), 22, 11);
        comHorario(lojaAberta("B Noturna Até 2h", curitiba), 18, 2);

        assertEquals(List.of("A Vira A Noite E Vai Até 11h"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.NAME));
    }

    @Test
    @DisplayName("HU18: loja sem horário cadastrado fica fora da vitrine")
    void deveTratarLojaSemHorarioComoFechadaNaVitrine() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comHorario(lojaAberta("A Sem Horário", curitiba), null, 18);
        lojaAberta("B Com Horário", curitiba);

        assertEquals(List.of("B Com Horário"), nomes(curitiba.getId(), null, null, StorefrontSortEnum.NAME));
    }

    @Test
    @DisplayName("HU18: abertas primeiro traz as fora do horário depois das abertas")
    void deveTrazerAsForaDoHorarioDepoisEmAbertasPrimeiro() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        comHorario(lojaAberta("A Fora Do Horário", curitiba), 19, 23);
        lojaAberta("B Dentro Do Horário", curitiba);

        List<com.h2gas.model.dto.StorefrontDTO> lojas = supplierService.findAllSuppliersOpenAndActive(
                curitiba.getId(), null, null, StorefrontSortEnum.OPEN_FIRST, PageRequest.of(0, 10)).getContent();

        assertEquals(List.of("B Dentro Do Horário", "A Fora Do Horário"),
                lojas.stream().map(com.h2gas.model.dto.StorefrontDTO::name).toList());
        assertTrue(lojas.get(0).open());
        assertFalse(lojas.get(1).open());
    }

    @Test
    @DisplayName("HU18: o open da vitrine da loja é calculado pelo horário")
    void deveCalcularOpenNaVitrine() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier dentro = lojaAberta("Dentro", curitiba);
        Supplier fora = comHorario(lojaAberta("Fora", curitiba), 19, 23);

        assertTrue(supplierService.findStorefront(dentro.getId()).open());
        assertFalse(supplierService.findStorefront(fora.getId()).open());
        assertTrue(supplierService.isOpenNow(dentro.getId()));
        assertFalse(supplierService.isOpenNow(fora.getId()));
    }
}
