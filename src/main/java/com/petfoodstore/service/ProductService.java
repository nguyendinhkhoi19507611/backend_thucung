package com.petfoodstore.service;

import com.petfoodstore.dto.ProductDTO;
import com.petfoodstore.entity.Product;
import com.petfoodstore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Product> getProductsByPetType(Product.PetType petType) {
        return productRepository.findByPetTypeAndActiveTrue(petType);
    }

    public Product createProduct(ProductDTO productDTO) {
        Product product = new Product();
        mapDtoToProduct(productDTO, product);
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, ProductDTO productDTO) {
        Product product = getProductById(id);
        mapDtoToProduct(productDTO, product);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product updateProductQuantity(Long id, Integer quantity) {
        Product product = getProductById(id);
        Integer oldQuantity = product.getQuantity();
        product.setQuantity(quantity);
        Product savedProduct = productRepository.save(product);

        // Check for low stock and send notification
        if (quantity <= 5 && quantity > 0 && oldQuantity > 5) {
            notificationService.createLowStockNotification(savedProduct);
        }

        return savedProduct;
    }
    private void checkLowStockAfterOrder(Product product) {
        if (product.getQuantity() <= 5 && product.getQuantity() > 0) {
            notificationService.createLowStockNotification(product);
        }
    }
    private void mapDtoToProduct(ProductDTO dto, Product product) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());
        product.setImageUrl(dto.getImageUrl());
        product.setSize(dto.getSize());
        product.setPetType(dto.getPetType());
    }
}