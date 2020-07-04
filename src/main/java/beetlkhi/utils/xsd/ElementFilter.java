package beetlkhi.utils.xsd;

import java.util.List;
import java.util.Optional;

public class ElementFilter {

    public static <T> Optional<T> getClass(List<Object> objectList, Class<T> clazz) {
        return objectList
                .stream()
                .filter(obj -> obj.getClass() == clazz)
                .map(clazz::cast)
                .findAny();
    }

}
