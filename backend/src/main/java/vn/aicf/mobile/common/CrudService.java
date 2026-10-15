package vn.aicf.mobile.common;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * SERVICE CRUD TỔNG QUÁT - viết 1 lần, dùng lại cho mọi tài nguyên đơn giản (danh mục, món, bàn...).
 *
 * <p>Áp dụng mẫu thiết kế <b>Template Method</b>: lớp cha cài sẵn "khung" của các thao tác
 * Thêm / Đọc / Sửa / Xóa (mở giao dịch, tìm theo id, báo lỗi 404, lưu, đổi sang DTO...),
 * lớp con chỉ viết phần KHÁC NHAU:
 * <ul>
 *   <li>{@link #newEntity()}     : tạo đối tượng rỗng khi thêm mới</li>
 *   <li>{@link #apply}           : chép dữ liệu từ request vào entity</li>
 *   <li>{@link #toResponse}      : đổi entity sang DTO trả về cho app</li>
 *   <li>các "móc" (hook) tùy chọn: {@link #validate}, {@link #beforeDelete}, {@link #findAll(Map)}, {@link #defaultSort()}</li>
 * </ul>
 * Thêm một tài nguyên mới chỉ cần: 1 Entity + 1 Repository + 1 lớp con của CrudService + 1 lớp con của CrudController.
 *
 * @param <E>   kiểu Entity (phải kế thừa {@link BaseEntity} để có id)
 * @param <REQ> kiểu DTO nhận vào khi thêm / sửa
 * @param <RES> kiểu DTO trả ra
 */
public abstract class CrudService<E extends BaseEntity, REQ, RES> {

    protected final JpaRepository<E, Long> repository;

    protected CrudService(JpaRepository<E, Long> repository) {
        this.repository = repository;
    }

    // ===================== Phần lớp con BẮT BUỘC viết =====================

    /** Tên tài nguyên bằng tiếng Việt, dùng trong thông báo lỗi, vd "danh mục". */
    protected abstract String resourceName();

    /** Tạo entity rỗng (gọi khi thêm mới). */
    protected abstract E newEntity();

    /** Chép các trường từ request sang entity (dùng chung cho thêm và sửa). */
    protected abstract void apply(REQ request, E entity);

    /** Đổi entity sang DTO trả về. */
    protected abstract RES toResponse(E entity);

    // ===================== "Móc" tùy chọn, lớp con ghi đè khi cần =====================

    /** Thứ tự mặc định khi lấy danh sách. */
    protected Sort defaultSort() {
        return Sort.by("id");
    }

    /**
     * Kiểm tra nghiệp vụ trước khi lưu (vd. tên không trùng). Ném {@link ApiException} nếu sai.
     *
     * @param existing entity đang sửa, hoặc null khi thêm mới
     */
    protected void validate(REQ request, E existing) {
    }

    /** Kiểm tra trước khi xóa (vd. danh mục còn món thì không cho xóa). */
    protected void beforeDelete(E entity) {
    }

    // ===================== Các thao tác CRUD dùng chung =====================

    /** Lấy tất cả, không lọc. */
    @Transactional(readOnly = true)
    public List<RES> findAll() {
        return repository.findAll(defaultSort()).stream().map(this::toResponse).toList();
    }

    /**
     * Lấy danh sách có lọc theo tham số trên URL (vd. ?categoryId=1&q=sữa).
     * Mặc định bỏ qua bộ lọc; lớp con nào cần lọc thì ghi đè.
     */
    @Transactional(readOnly = true)
    public List<RES> findAll(Map<String, String> filters) {
        return findAll();
    }

    /** Lấy 1 bản ghi, không có thì lỗi 404. */
    @Transactional(readOnly = true)
    public RES findById(Long id) {
        return toResponse(getEntity(id));
    }

    /** Lấy entity theo id cho các service khác dùng (vd. OrderService lấy Product). */
    @Transactional(readOnly = true)
    public E getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy " + resourceName() + " #" + id));
    }

    /** Thêm mới. */
    @Transactional
    public RES create(REQ request) {
        validate(request, null);
        E entity = newEntity();
        apply(request, entity);
        return toResponse(repository.save(entity));
    }

    /** Sửa toàn bộ các trường (PUT). */
    @Transactional
    public RES update(Long id, REQ request) {
        E entity = getEntity(id);
        validate(request, entity);
        apply(request, entity);
        return toResponse(repository.save(entity));
    }

    /** Xóa. */
    @Transactional
    public void delete(Long id) {
        E entity = getEntity(id);
        beforeDelete(entity);
        repository.delete(entity);
    }
}
