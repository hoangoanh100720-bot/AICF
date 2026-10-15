package vn.aicf.mobile.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.common.CrudService;
import vn.aicf.mobile.domain.Category;
import vn.aicf.mobile.repository.CategoryRepository;
import vn.aicf.mobile.repository.ProductRepository;
import vn.aicf.mobile.web.dto.CatalogDtos.CategoryRequest;
import vn.aicf.mobile.web.dto.CatalogDtos.CategoryResponse;

/**
 * CRUD danh mục - VÍ DỤ TÁI SỬ DỤNG {@link CrudService}: chỉ viết phần riêng
 * (chép trường, đổi DTO, kiểm tra trùng tên, chặn xóa khi còn món); phần thêm/sửa/xóa/tìm đã có ở lớp cha.
 */
@Service
public class CategoryService extends CrudService<Category, CategoryRequest, CategoryResponse> {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        super(categories);
        this.categories = categories;
        this.products = products;
    }

    @Override
    protected String resourceName() {
        return "danh mục";
    }

    @Override
    protected Category newEntity() {
        return new Category();
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by("sortOrder", "name");
    }

    @Override
    protected void validate(CategoryRequest r, Category existing) {
        String name = r.name().trim();
        boolean duplicated = existing == null
                ? categories.existsByNameIgnoreCase(name)
                : categories.existsByNameIgnoreCaseAndIdNot(name, existing.getId());
        if (duplicated) {
            throw ApiException.conflict("Đã có danh mục tên \"" + name + "\"");
        }
    }

    @Override
    protected void apply(CategoryRequest r, Category c) {
        c.setName(r.name().trim());
        c.setDescription(r.description());
        c.setIcon(r.icon());
        c.setSortOrder(r.sortOrder());
    }

    @Override
    protected void beforeDelete(Category c) {
        long count = products.countByCategoryId(c.getId());
        if (count > 0) {
            throw ApiException.conflict("Danh mục còn " + count + " món, hãy chuyển hoặc xóa các món trước");
        }
    }

    @Override
    protected CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon(), c.getSortOrder());
    }
}
