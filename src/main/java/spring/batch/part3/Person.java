package spring.batch.part3;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * ListItemReader 를 custom 하게 만든 예제에서 사용하기 위해 Test 용도로 만들었다.
 */
@Entity
@Getter
@NoArgsConstructor
public class Person {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String age;
    private String address;


    public Person(final int id, final String name, final String age, final String address) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
    }
}
