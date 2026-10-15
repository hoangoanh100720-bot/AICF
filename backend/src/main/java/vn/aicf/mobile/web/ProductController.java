package vn.aicf.mobile.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.aicf.mobile.common.CrudController;
import vn.aicf.mobile.common.CrudService;
import vn.aicf.mobile.service.ProductService;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductRequest;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductResponse;

/**
 * API MÓN: kế thừa 5 API CRUD từ {@link CrudController}.
 * Lọc danh sách bằng tham số: GET /api/products?categoryId=1&q=sữa&available=true
 */
@Tag(name = "4. Món")
@RestController
@RequestMapping("/api/products")
public class ProductController extends CrudController<ProductRequest, ProductResponse> {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Override
    protected CrudService<?, ProductRequest, ProductResponse> service() {
        return service;
    }
}
