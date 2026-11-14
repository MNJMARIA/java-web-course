package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.Category;
import com.cosmocats.cosmo_cats.exception.CategoryNotFoundException;
import com.cosmocats.cosmo_cats.repository.CategoryRepository;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setPublicId(category.id());
        entity.setName(category.name());
        return toDomain(categoryRepository.save(entity));
    }

    public Category findById(UUID id) {
        return categoryRepository.findByPublicId(id)
                .map(this::toDomain)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Transactional
    public void delete(UUID id) {
        categoryRepository.findByPublicId(id).ifPresent(categoryRepository::delete);
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(entity.getPublicId(), entity.getName());
    }
}