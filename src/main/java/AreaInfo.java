import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhuweitung
 * @since 2024/10/16
 */
@NoArgsConstructor
@Data
public class AreaInfo {
    private String id;
    private String pid = "0";
    private int level = 0;
    private String name;
}
