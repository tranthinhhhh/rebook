package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.CategoryForm;
import vn.hcmute.rebook.entity.Category;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.CategoryRepository;
import vn.hcmute.rebook.repository.ProductRepository;
import vn.hcmute.rebook.service.CategoryService;
import vn.hcmute.rebook.util.SlugUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục"));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục"));
    }

    @Override
    public void save(CategoryForm form) {
        String name = form.getName().trim();
        Category category = form.getId() == null ? new Category() : getById(form.getId());
        if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Danh mục \"" + name + "\" đã tồn tại");
        }
        category.setName(name);
        category.setSlug(SlugUtils.toSlug(name));
        category.setIcon(form.getIcon() == null || form.getIcon().isBlank() ? "bi-book" : form.getIcon().trim());
        category.setDescription(form.getDescription());
        categoryRepository.save(category);
    }

    @Override
    public void delete(Long id) {
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("Danh mục đang có sách, không thể xóa");
        }
        categoryRepository.delete(getById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts(Long categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }
}
