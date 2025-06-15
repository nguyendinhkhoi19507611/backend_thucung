package com.petfoodstore.repository;

import com.petfoodstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();

    List<Product> findByCategoryAndActiveTrue(String category);

    List<Product> findByPetTypeAndActiveTrue(Product.PetType petType);

    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND p.active = true")
    List<Product> searchProducts(@Param("keyword") String keyword);
}