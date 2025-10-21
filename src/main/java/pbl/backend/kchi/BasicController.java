package pbl.backend.kchi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/api")
public class BasicController {
    private final JdbcTemplate jdbcTemplate;
    public BasicController (
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("test")
    public  String test() {
        String sql = "CREATE TABLE IF NOT EXISTS test_table (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL " +
                ")";

        jdbcTemplate.execute(sql);
        return "Đã tạo bảng thành công";
    }
}
