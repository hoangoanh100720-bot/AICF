package vn.aicf.mobile.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.common.CrudService;
import vn.aicf.mobile.domain.Product;
import vn.aicf.mobile.repository.CategoryRepository;
import vn.aicf.mobile.repository.ProductRepository;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductRequest;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CRUD món + tìm kiếm / lọc. Kế thừa {@link CrudService}, ghi đè thêm {@link #findAll(Map)} để hỗ trợ:
 * <pre>
 *   GET /api/products?categoryId=1          món thuộc danh mục 1
 *   GET /api/products?q=sữa                 món có chữ "sữa" trong tên
 *   GET /api/products?available=true        chỉ món đang bán
 * </pre>
 */
@Service
public class ProductService extends CrudService<Product, ProductRequest, ProductResponse> {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        super(products);
        this.products = products;
        this.categories = categories;
    }

    @Override
    protected String resourceName() {
        return "món";
    }

    @Override
    protected Product newEntity() {
        return new Product();
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by("category.sortOrder", "name");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Map<String, String> filters) {
        Long categoryId = parseLong(filters.get("categoryId"));
        String q = filters.get("q");
        String keyword = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        boolean onlyAvailable = Boolean.parseBoolean(filters.get("available"));
        return products.search(categoryId, keyword, onlyAvailable).stream().map(this::toResponse).toList();
    }

    @Override
    protected void apply(ProductRequest r, Product p) {
        p.setCategory(categories.findById(r.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Danh mục #" + r.categoryId() + " không tồn tại")));
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setImageUrl(r.imageUrl() == null || r.imageUrl().isBlank() ? null : r.imageUrl().trim());
        p.setAvailable(r.available());
    }

    @Override
    protected ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getCategory().getId(), p.getCategory().getName(), p.getCategory().getIcon(),
                p.getName(), p.getDescription(), p.getPrice(), p.getImageUrl(), p.isAvailable());
    }

    private static Long parseLong(String s) {
        try {
            return s == null || s.isBlank() ? null : Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("categoryId phải là số");
        }
    }
}
