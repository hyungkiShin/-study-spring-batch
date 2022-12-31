package spring.batch.part3;


import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.ArrayList;
import java.util.List;

/**
 * ListItemReader 를 custom 하게 만든다면 아래와 같이 만들 수 있다.
 * @param <T>
 */

public class CustomItemReader<T> implements ItemReader<T> {

    private final List<T> items;

    /**
     * List 를 생성자 주입으로 받는다
     * @param items
     */
    public CustomItemReader(final List<T> items) {
        this.items = new ArrayList<>(items);
    }

    /**
     * elements 를 하나씩 읽어서 제거한다.
     */
    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(items.isEmpty() == false) {
            return items.remove(0);
        }
        return null; // null 을 리턴하면 더이상 읽을게 없다는 의미이다.
    }
}
