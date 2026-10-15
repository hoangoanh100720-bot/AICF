package vn.aicf.mobile.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.common.CrudService;
import vn.aicf.mobile.domain.DiningTable;
import vn.aicf.mobile.repository.DiningTableRepository;
import vn.aicf.mobile.web.dto.CatalogDtos.TableRequest;
import vn.aicf.mobile.web.dto.CatalogDtos.TableResponse;

/** CRUD bàn - thêm một ví dụ tái sử dụng {@link CrudService} (toàn bộ class chỉ ~50 dòng). */
@Service
public class TableService extends CrudService<DiningTable, TableRequest, TableResponse> {

    private final DiningTableRepository tables;

    public TableService(DiningTableRepository tables) {
        super(tables);
        this.tables = tables;
    }

    @Override
    protected String resourceName() {
        return "bàn";
    }

    @Override
    protected DiningTable newEntity() {
        return new DiningTable();
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by("name");
    }

    @Override
    protected void validate(TableRequest r, DiningTable existing) {
        String name = r.name().trim();
        boolean duplicated = existing == null
                ? tables.existsByNameIgnoreCase(name)
                : tables.existsByNameIgnoreCaseAndIdNot(name, existing.getId());
        if (duplicated) {
            throw ApiException.conflict("Đã có bàn tên \"" + name + "\"");
        }
    }

    @Override
    protected void apply(TableRequest r, DiningTable t) {
        t.setName(r.name().trim());
        t.setArea(r.area());
        t.setSeats(r.seats());
        t.setActive(r.active());
    }

    @Override
    protected TableResponse toResponse(DiningTable t) {
        return new TableResponse(t.getId(), t.getName(), t.getArea(), t.getSeats(), t.isActive());
    }
}
