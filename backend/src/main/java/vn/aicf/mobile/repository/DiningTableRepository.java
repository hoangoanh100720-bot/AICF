package vn.aicf.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.aicf.mobile.domain.DiningTable;

/** Truy vấn bảng DINING_TABLES. */
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
