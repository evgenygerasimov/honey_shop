package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.DeleteProductException;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    @Value("${myapp.upload.image.directory}")
    private String UPLOAD_DIRECTORY;
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public Product getProductById(UUID productId) {
        log.info("Get product by id: {}", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    public List<Product> getAllProducts() {
        log.info("Get all products grouped by category and sorted by price.");

        List<Product> productList = productRepository.findAll();

        Map<Category, List<Product>> groupedByCategory = productList.stream()
                .filter(product -> product.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(Product::getPrice).reversed())
                                        .collect(Collectors.toList())
                        )
                ));

        List<Product> sortedProductList = new ArrayList<>();

        for (Category category : groupedByCategory.keySet()) {
            sortedProductList.addAll(groupedByCategory.get(category));
        }

        List<Product> uncategorized = productList.stream()
                .filter(product -> product.getCategory() == null)
                .sorted(Comparator.comparing(Product::getPrice).reversed())
                .toList();
        sortedProductList.addAll(uncategorized);

        return sortedProductList;
    }


    public void createProduct(Product product, List<MultipartFile> pictures, String imageOrder) {
        Category category = categoryService.findByName(product.getCategory().getName());
        List<String> links = imageSelectionProcessing(product, pictures);
        List<String> orderedLinks = orderImagesForCreating(links, imageOrder);
        product = Product.builder()
                .name(product.getName())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .length(product.getLength())
                .width(product.getWidth())
                .height(product.getHeight())
                .weight(product.getWeight())
                .images(orderedLinks)
                .stockQuantity(product.getStockQuantity())
                .category(category)
                .build();
        log.info("Create product: {}", product.getName());
        productRepository.save(product);

    }

    public Product updateProduct(Product product, List<MultipartFile> pictures, String imageOrder) {
        Product exisitingProduct = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Category category = categoryService.findByName(product.getCategory().getName());

        handleProductImagesForUpdating(exisitingProduct, imageOrder, pictures);
        exisitingProduct.setProductId(product.getProductId());
        exisitingProduct.setName(product.getName());
        exisitingProduct.setDescription(product.getDescription());
        exisitingProduct.setShortDescription(product.getShortDescription());
        exisitingProduct.setPrice(product.getPrice());
        exisitingProduct.setLength(product.getLength());
        exisitingProduct.setWidth(product.getWidth());
        exisitingProduct.setHeight(product.getHeight());
        exisitingProduct.setWeight(product.getWeight());
        exisitingProduct.setStockQuantity(product.getStockQuantity());
        exisitingProduct.setCategory(category);

        log.info("Update product: {}", product.getProductId());
        productRepository.save(exisitingProduct);
        return exisitingProduct;
    }

    private void handleProductImagesForUpdating(Product exisitingProduct, String imageOrder, List<MultipartFile> pictures) {
        if (imageOrder != null && !imageOrder.isEmpty()) {
            String[] imageOrderArray = imageOrder.split(",");
            List<String> orderedImages = Arrays.asList(imageOrderArray);
            List<String> currentImages = new ArrayList<>();

            for (String orderedImage : orderedImages) {
                String fullImagePath = UPLOAD_DIRECTORY + "/" + orderedImage;
                currentImages.add(fullImagePath);
            }

            exisitingProduct.setImages(currentImages);
        } else {
            imageSelectionProcessing(exisitingProduct, pictures);
        }
    }

    private List<String> imageSelectionProcessing(Product product, List<MultipartFile> pictures) {
        List<String> links = new ArrayList<>();
        if (pictures != null && !pictures.isEmpty() && !pictures.getFirst().isEmpty()) {
            for (MultipartFile multipartFile : pictures) {
                String fileName = multipartFile.getOriginalFilename();
                if (fileName != null && !fileName.isEmpty()) {
                    Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, fileName);
                    links.add(fileNameAndPath.toString());
                    try {
                        Files.createDirectories(fileNameAndPath.getParent());
                        Files.write(fileNameAndPath, multipartFile.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            product.getImages().addAll(links);
        }
        return links;
    }

    private List<String> orderImagesForCreating(List<String> images, String imageOrder) {
        if (imageOrder == null || imageOrder.trim().isEmpty()) {
            return images;
        }

        List<String> orderedImages = new ArrayList<>();
        for (String index : imageOrder.split(",")) {
            if (index.trim().isEmpty()) continue;
            int idx = Integer.parseInt(index.trim());
            if (idx >= 0 && idx < images.size()) {
                orderedImages.add(images.get(idx));
            }
        }
        return orderedImages;
    }

    public void deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new DeleteProductException("Продукт с id=" + productId + " не найден, удаление невозможно.");
        }
        try {
            productRepository.deleteById(productId);
        } catch (Exception e) {
            log.error("Delete product with productId={} failed", productId, e);
            throw new DeleteProductException("Ошибка при удалении продукта, возможно он используется в других таблицах.");
        }
    }

        public void removeImageFromProduct (UUID productId, String imageFilename){
            Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found!"));
            product.getImages().remove(imageFilename);
            productRepository.save(product);
        }

        public void updateStockForReduction (Product product,int quantity){
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }

        public void updateStockForAddition (Product product,int quantity){
            product.setStockQuantity(product.getStockQuantity() + quantity);
            productRepository.save(product);
        }
    }

