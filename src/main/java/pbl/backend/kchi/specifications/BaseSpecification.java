package pbl.backend.kchi.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder; // Cần import thêm
import jakarta.persistence.criteria.CriteriaQuery; // Cần import thêm
import jakarta.persistence.criteria.Root; // Cần import thêm

public class BaseSpecification<T> {

    // Phương thức lọc theo từ khóa (giữ nguyên)
    public static <T> Specification<T> keywordSpec(String keyword, String... fields){
        return (root, query, criteriaBuilder) -> {
            if(keyword == null || keyword.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            Predicate[] predicates = new Predicate[fields.length];
            for(int i = 0; i < fields.length; i++){
                predicates[i] = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(fields[i])),
                        "%" + keyword.toLowerCase() + "%"
                );
            }
            return criteriaBuilder.or(predicates);
        };
    }

    // ⭐ PHƯƠNG THỨC ĐƯỢC SỬA ĐỔI: whereSpec (Xử lý classId) ⭐
    public static <T> Specification<T> whereSpec(Map<String, String> filters){
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                try {
                    // 1. KIỂM TRA HẬU TỐ "Id"
                    if (key.endsWith("Id")) {
                        // Lấy tên quan hệ (ví dụ: "classId" -> "class")
                        String relationName = key.substring(0, key.length() - 2);

                        // ⭐ LOGIC XỬ LÝ QUAN HỆ CỦA BẠN ⭐
                        // Dùng if/else if để xác định tên thuộc tính trong Entity
                        if (key.equalsIgnoreCase("classId")) {
                            // Entity Assignments có thuộc tính là "classes", ID là "id"
                            predicates.add(criteriaBuilder.equal(
                                    root.get("classes").get("id"),
                                    Long.parseLong(value) // Chuyển đổi giá trị thành Long
                            ));
                        }
                        // Bạn có thể thêm xử lý cho các ID quan hệ khác ở đây (ví dụ: "userId")
                        /*
                        else if (key.equalsIgnoreCase("userId")) {
                             predicates.add(criteriaBuilder.equal(
                                 root.get("user").get("id"), // Giả định thuộc tính là 'user'
                                 Long.parseLong(value)
                             ));
                        }
                        */

                    } else {
                        // 2. XỬ LÝ CÁC TRƯỜNG ĐƠN GIẢN
                        predicates.add(criteriaBuilder.equal(root.get(key), value));
                    }
                } catch (Exception e) {
                    // Nếu có lỗi (ví dụ: giá trị lọc không phải số), ném ra ngoại lệ
                    throw new IllegalArgumentException("Giá trị lọc không hợp lệ cho trường " + key + ": " + value, e);
                }
            });

            // Kết hợp tất cả các Predicate bằng AND
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    // Phương thức lọc phức tạp (giữ nguyên)
    public static <T> Specification<T> complexWhereSpec(Map<String, Map<String, String>> filters){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = filters.entrySet().stream()
                    .flatMap((entry) -> entry.getValue().entrySet().stream()
                            .map(condition -> {
                                String field = entry.getKey();
                                String operator = condition.getKey();
                                String value = condition.getValue();

                                switch (operator.toLowerCase()) {
                                    case "eq" -> {
                                        return criteriaBuilder.equal(root.get(field), value);
                                    }
                                    case "gt" -> {
                                        return criteriaBuilder.greaterThan(root.get(field), value);
                                    }
                                    case "gte" -> {
                                        return criteriaBuilder.greaterThanOrEqualTo(root.get(field), value);
                                    }
                                    case "lt" -> {
                                        return criteriaBuilder.lessThan(root.get(field), value);
                                    }
                                    case "lte" -> {
                                        return criteriaBuilder.lessThanOrEqualTo(root.get(field), value);
                                    }
                                    case "in" -> {
                                        List<String> values = List.of(value.split(","));
                                        return root.get(field).in(values);
                                    }
                                    default -> throw new IllegalArgumentException("Toán tử " + operator + " không được hỗ trợ");
                                }
                            }))
                    .collect(Collectors.toList());
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}