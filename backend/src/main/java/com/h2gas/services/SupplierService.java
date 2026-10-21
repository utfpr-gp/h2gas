package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSettingsDTO;
import com.h2gas.model.dto.SupplierProfileDTO;
import com.h2gas.model.dto.SupplierProfileRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import com.h2gas.model.dto.SupplierSummaryDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.*;
import com.h2gas.model.dto.StoreReviewDTO;
import com.h2gas.model.dto.StorefrontDTO;
import com.h2gas.model.repository.ReviewRepository;
import com.h2gas.model.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private UserService userService;

    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private CityService cityService;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private AddressService addressService ;

    @Autowired
    private SupplierSummaryMapper supplierSummaryMapper;

//    public List<SupplierDTO> findAllSuppliers() {
//        return supplierRepository.findAll()
//                .stream()
//                .map(supplierMapper::toDto)
//                .collect(Collectors.toList());
//    }

    public Page<SupplierDTO> findAllSuppliers(Boolean active, String q, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return supplierRepository.findForApproval(active, term, pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> searchSuppliers(String name, Pageable pageable) {
        return supplierRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> findAllSuppliersOpenAndActive(Pageable pageable) {
        return supplierRepository.findByActiveAndOpen(true, true, pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> findAllSuppliersOpenAndActive(Long cityId, Pageable pageable) {
        return supplierRepository.findOpenForCatalog(cityId, null, pageable).map(supplierMapper::toDto);
    }

    // HU13: o catálogo devolve a vitrine pública das lojas (sem dados internos), com a nota
    public Page<StorefrontDTO> findAllSuppliersOpenAndActive(Long cityId, String q, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim().toLowerCase(java.util.Locale.ROOT);
        Page<SupplierDTO> page = supplierRepository.findOpenForCatalog(cityId, term, pageable).map(supplierMapper::toDto);
        addRatings(page.getContent());
        return page.map(SupplierService::toStorefront);
    }

    // HU13: página da loja na vitrine pública
    public StorefrontDTO findStorefront(String id) {
        return toStorefront(findById(id));
    }

    private static StorefrontDTO toStorefront(SupplierDTO d) {
        return new StorefrontDTO(d.getId(), d.getName(), d.getDescription(), d.getLogoUrl(), d.getBannerUrl(),
                d.getOpeningTime(), d.getClosingTime(), d.getAverageDeliveryTime(), d.isOpen(), d.getCityId(), d.getCityName(),
                d.getAddressNeighborhood(), d.getAverageRating(), d.getReviewCount());
    }

    // F7 §5.27: avaliações da loja com o primeiro nome do cliente; comentário em branco vira nulo
    public Page<StoreReviewDTO> findReviews(String supplierId, Pageable pageable) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + supplierId);
        }
        return reviewRepository.findStoreReviews(supplierId, pageable).map(row -> new StoreReviewDTO(
                row.getId(),
                firstName(row.getCustomerName()),
                row.getRating(),
                row.getComment() == null || row.getComment().isBlank() ? null : row.getComment().trim(),
                row.getDeliveredAt()));
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Cliente";
        }
        return fullName.trim().split("\\s+")[0];
    }

    // F7 §5.25: nota e contagem das lojas da página, lidas em uma consulta agrupada
    private void addRatings(List<SupplierDTO> suppliers) {
        if (suppliers.isEmpty()) {
            return;
        }
        Map<String, ReviewRepository.SupplierRatingSummary> byId = reviewRepository
                .summarizeBySupplierIds(suppliers.stream().map(SupplierDTO::getId).toList())
                .stream()
                .collect(Collectors.toMap(ReviewRepository.SupplierRatingSummary::getSupplierId, Function.identity()));
        suppliers.forEach(dto -> {
            ReviewRepository.SupplierRatingSummary summary = byId.get(dto.getId());
            if (summary != null) {
                dto.setAverageRating(Math.round(summary.getAverage() * 10) / 10.0);
                dto.setReviewCount(summary.getTotal());
            }
        });
    }

    public List<SupplierSummaryDTO> findAllSuppliersSummary() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierSummaryMapper::toDto)
                .collect(Collectors.toList());
    }

    public SupplierDTO findById(String id) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);
        SupplierDTO dto = supplierOptional.map(supplierMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
        addRatings(List.of(dto)); // F7 §5.26: cabeçalho da loja mostra a nota
        return dto;
    }

    public Supplier findEntityById(String id) {
        return supplierRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public Supplier findEntityByName(String name) {
        return supplierRepository.findByName(name).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse nome: " + name));
    }

    public SupplierDTO createSupplier(SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierMapper.toEntitySupplierCreate(supplierDTO);

        City city = cityService.findByIdEntity(supplierDTO.getCityId());
        Address address = null;

        if (city == null) {
            throw new ResourceNotFoundException("Cidade não encontrada.");
        }
        if (supplierDTO.getAddressId() != null) {
             address = addressService.findByIdEntity(supplierDTO.getAddressId());
        }
//        if (address == null) {
//            throw new ResourceNotFoundException("Address not found");
//        }

//        User user = userMapper.toEntityUser(userService.findByIdUser(supplierDTO.getUserId()));

//        if (user == null) {
//            throw new ResourceNotFoundException("User not found");
//        }

        supplier.setCity(city);
        supplier.setAddress(address);
        supplier.setActive(false);
        supplier.setCnpj(supplierDTO.getCnpj());
        supplier.setName(supplierDTO.getName());
//        supplier.setUser(user);
        Supplier savedSupplier = supplierRepository.save(supplier);

        return supplierMapper.toDto(savedSupplier);
    }

    public Supplier createSupplier(Supplier supplier) {
//        if (supplier.getCity() == null || supplier.getAddress() == null) {
//            throw new BusinessRuleException("Cidade e endereço devem estar definidos antes de salvar.");
//        }

        supplier.setActive(false);
        return supplierRepository.save(supplier);
    }


    public SupplierDTO update(String id, SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com id " + id + " não encontrado"));

        boolean currentStatus = supplier.isActive();
        City newCity = cityMapper.toEntity(cityService.findByIdCity(supplierDTO.getCityId()));
        if (!supplier.getCity().getId().equals(supplierDTO.getCityId())) {
            supplier.setCity(newCity);
        }

        supplier.setName(supplierDTO.getName());
        supplier.setPhoneNumber(supplierDTO.getPhone());
        supplier.setActive(currentStatus);
        supplier.setDescription(supplierDTO.getDescription());
        supplier.setOpeningTime(LocalTime.parse(supplierDTO.getOpeningTime()));
        supplier.setClosingTime(LocalTime.parse(supplierDTO.getClosingTime()));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(supplierDTO.getAverageDeliveryTime()));

        supplierRepository.save(supplier);
        return supplierMapper.toDto(supplier);
    }

    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplierRepository.delete(supplier);
    }

    public void updateStatusSupplier(String id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setActive(active);
        supplierRepository.save(supplier);
    }

//    public void updateTimesSupplier(String id, Map<String, String> times) {
//        Supplier supplier = supplierRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));
//
//        supplier.setOpeningTime(LocalTime.parse(times.get("openingTime")));
//        supplier.setClosingTime(LocalTime.parse(times.get("closingTime")));
//        supplierRepository.save(supplier);
//    }

    public void updateTimesSupplier(String id, Map<String, String> times) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        if (times.containsKey("openingTime") && times.get("openingTime") != null && !times.get("openingTime").isEmpty()) {
            supplier.setOpeningTime(LocalTime.parse(times.get("openingTime")));
        }

        if (times.containsKey("closingTime") && times.get("closingTime") != null && !times.get("closingTime").isEmpty()) {
            supplier.setClosingTime(LocalTime.parse(times.get("closingTime")));
        }

        if (times.containsKey("averageDeliveryTime") && times.get("averageDeliveryTime") != null && !times.get("averageDeliveryTime").isEmpty()) {
            try {
                long minutes = Long.parseLong(times.get("averageDeliveryTime"));
                supplier.setAverageDeliveryTime(Duration.ofMinutes(minutes));
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Tempo médio de entrega inválido: informe os minutos.");
            }
        }
        supplierRepository.save(supplier);
    }



    public static final String INVALID_LOGO_MESSAGE = "Envie uma imagem JPG, PNG ou WEBP de até 1 MB.";
    private static final long MAX_LOGO_BYTES = 1_048_576;
    private static final java.util.Set<String> LOGO_TYPES = java.util.Set.of("image/jpeg", "image/png", "image/webp");
    public static final String INVALID_BANNER_MESSAGE = "Envie uma imagem JPG, PNG ou WEBP de até 2 MB.";
    private static final long MAX_BANNER_BYTES = 2_097_152;

    // F7 fatia 6: perfil e logo da loja; o id vem sempre do token (controller)
    public SupplierProfileDTO findMyProfile(String supplierId) {
        return toProfile(findSupplier(supplierId));
    }

    // horário, cidade e aprovação ficam de fora: são da configuração, do endereço e do admin
    @Transactional
    public SupplierProfileDTO updateMyProfile(String supplierId, SupplierProfileRequestDTO request) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setName(request.name().trim());
        supplier.setPhoneNumber(request.phone());
        supplier.setDescription(request.description() == null ? null : request.description().trim());
        return toProfile(supplierRepository.save(supplier));
    }

    @Transactional
    public String updateMyLogo(String supplierId, MultipartFile file) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setLogoUrl(uploadStoreImage(file, MAX_LOGO_BYTES, INVALID_LOGO_MESSAGE));
        return supplierRepository.save(supplier).getLogoUrl();
    }

    // F7 §5.26: banner da loja — mesma validação e envio da logo, com limite de 2 MB
    @Transactional
    public String updateMyBanner(String supplierId, MultipartFile file) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setBannerUrl(uploadStoreImage(file, MAX_BANNER_BYTES, INVALID_BANNER_MESSAGE));
        return supplierRepository.save(supplier).getBannerUrl();
    }

    @Transactional
    public void removeMyBanner(String supplierId) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setBannerUrl(null);
        supplierRepository.save(supplier);
    }

    private String uploadStoreImage(MultipartFile file, long maxBytes, String invalidMessage) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes || !LOGO_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException(invalidMessage);
        }
        Map<?, ?> uploaded;
        try {
            uploaded = cloudinaryService.upload(file);
        } catch (IOException | RuntimeException e) {
            // o SDK do Cloudinary lança RuntimeException quando a API recusa (credencial, assinatura, rede)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível enviar a imagem. Tente novamente.", e);
        }
        Object secure = uploaded.get("secure_url");
        return String.valueOf(secure != null ? secure : uploaded.get("url"));
    }

    @Transactional
    public void removeMyLogo(String supplierId) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setLogoUrl(null);
        supplierRepository.save(supplier);
    }

    private static SupplierProfileDTO toProfile(Supplier s) {
        Address a = s.getAddress();
        City city = a != null && a.getCity() != null ? a.getCity() : s.getCity();
        return new SupplierProfileDTO(s.getName(), s.getEmail(), s.getPhoneNumber(), s.getDescription(), s.getCnpj(), s.getLogoUrl(), s.getBannerUrl(),
                a == null ? null : a.getId(), a == null ? null : a.getStreetName(), a == null ? null : a.getNumber(),
                a == null ? null : a.getNeighborhood(), city == null ? null : city.getName(), city == null ? null : city.getState());
    }

    // F7 fatia 6: configurações da loja; o id vem sempre do token (controller)
    public SupplierSettingsDTO findMySettings(String supplierId) {
        return toSettings(findSupplier(supplierId));
    }

    @Transactional
    public SupplierSettingsDTO updateMySettings(String supplierId, SupplierSettingsDTO settings) {
        LocalTime opening = LocalTime.parse(settings.openingTime());
        LocalTime closing = LocalTime.parse(settings.closingTime());
        if (opening.equals(closing)) {
            throw new BusinessRuleException("O horário de fechamento precisa ser diferente da abertura.");
        }
        Supplier supplier = findSupplier(supplierId);
        supplier.setOpeningTime(opening);
        supplier.setClosingTime(closing);
        supplier.setAverageDeliveryTime(Duration.ofMinutes(settings.averageDeliveryTime()));
        supplier.setLowStockThreshold(settings.lowStockThreshold());
        return toSettings(supplierRepository.save(supplier));
    }

    private Supplier findSupplier(String id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));
    }

    private static SupplierSettingsDTO toSettings(Supplier supplier) {
        return new SupplierSettingsDTO(
                supplier.getOpeningTime() == null ? null : supplier.getOpeningTime().toString(),
                supplier.getClosingTime() == null ? null : supplier.getClosingTime().toString(),
                supplier.getAverageDeliveryTime() == null ? null : (int) supplier.getAverageDeliveryTime().toMinutes(),
                supplier.getLowStockThreshold());
    }

    public void updateStatusOpenClosed(String id, boolean openClose) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setOpen(openClose);
        supplierRepository.save(supplier);
    }

//    public SupplierDTO findByUserIdSupplier(String userId) {
//        Optional<Supplier> supplierOptional = supplierRepository.findByUserId(userId);
//        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
//                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id de usuario: " + userId));
//    }
}
