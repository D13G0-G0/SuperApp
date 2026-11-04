package co.edu.unal.superapp.repository;

import co.edu.unal.superapp.entity.Listing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** REPOSITORY: acceso a datos. Spring genera la implementación (save, findById, findAll...). */
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByAvailableTrue();
}
