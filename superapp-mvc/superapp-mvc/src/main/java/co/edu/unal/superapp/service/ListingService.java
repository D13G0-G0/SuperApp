package co.edu.unal.superapp.service;

import co.edu.unal.superapp.dto.ListingRequest;
import co.edu.unal.superapp.dto.ListingResponse;
import co.edu.unal.superapp.entity.Listing;
import co.edu.unal.superapp.repository.ListingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** SERVICE: aquí vive la lógica de negocio. Convierte DTO <-> entity y usa el repository. */
@Service
public class ListingService {

    private final ListingRepository repository;

    public ListingService(ListingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ListingResponse create(ListingRequest request) {
        Listing saved = repository.save(new Listing(request.title(), request.description(), request.price()));
        return ListingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> findAvailable() {
        return repository.findByAvailableTrue().stream().map(ListingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse findById(Long id) {
        return ListingResponse.from(getOrThrow(id));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Listing getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing " + id + " no existe"));
    }
}
