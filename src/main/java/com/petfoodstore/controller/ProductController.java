package com.petfoodstore.controller;

import com.petfoodstore.dto.MessageResponse;
import com.petfoodstore.dto.ProductDTO;
import com.petfoodstore.entity.Product;
import com.petfoodstore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Public endpoints
    @GetMapping("/public/all")
    public ResponseEntity<List<Product>> getAllActiveProducts() {
        List<Product> products = productService.getAllActiveProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/public/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/public/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/public/pet-type/{petType}")
    public ResponseEntity<List<Product>> getProductsByPetType(@PathVariable Product.PetType petType) {
        List<Product> products = productService.getProductsByPetType(petType);
        return ResponseEntity.ok(products);
    }

    // Admin endpoints
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.createProduct(productDTO);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new MessageResponse("Product deleted successfully!"));
    }

    @PutMapping("/{id}/quantity")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Product> updateProductQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        Product product = productService.updateProductQuantity(id, quantity);
        return ResponseEntity.ok(product);
    }
}