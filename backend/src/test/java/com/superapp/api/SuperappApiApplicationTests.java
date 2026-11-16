package com.superapp.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.superapp.api.catalog.domain.ListingStatus;
import com.superapp.api.catalog.repository.ListingRepository;
import com.superapp.api.identity.domain.Role;
import com.superapp.api.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Arranca la app con las migraciones de Flyway y ddl-auto=validate:
 * si una entidad no coincide con las tablas, esta prueba falla.
 */
@SpringBootTest
@ActiveProfiles("test")
class SuperappApiApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void seedHasOneAccountPerRole() {
		assertThat(userRepository.findAll()).hasSize(4);
		assertThat(userRepository.findByEmailIgnoreCase("admin@superapp.test")).get()
				.satisfies(u -> assertThat(u.getRoles()).containsExactly(Role.ADMIN));
		assertThat(userRepository.findByEmailIgnoreCase("vendedor@superapp.test")).get()
				.satisfies(u -> assertThat(u.getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.SELLER));
		assertThat(userRepository.findByEmailIgnoreCase("conductor@superapp.test")).get()
				.satisfies(u -> assertThat(u.getRoles()).containsExactly(Role.DRIVER));
	}

	@Test
	void seedHasSixPublishedListings() {
		assertThat(listingRepository.findAll())
				.hasSize(6)
				.allSatisfy(l -> assertThat(l.getStatus()).isEqualTo(ListingStatus.PUBLISHED));
	}
}
