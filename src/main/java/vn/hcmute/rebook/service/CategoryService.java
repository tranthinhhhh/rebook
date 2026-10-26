package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.CategoryForm;
import vn.hcmute.rebook.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> findAll();

    Category getById(Long id);

    Category getBySlug(String slug);

    void save(CategoryForm form);

    void delete(Long id);

    long countProducts(Long categoryId);
}
