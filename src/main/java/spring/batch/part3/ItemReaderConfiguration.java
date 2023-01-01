package spring.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    public ItemReaderConfiguration(JobBuilderFactory jobBuilderFactory,
                                   StepBuilderFactory stepBuilderFactory,
                                   DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileStep())
                .next(this.jdbcCursorStep())
                .next(this.jdbcPagingStep())
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return this.stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getItems()))
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step csvFileStep() throws Exception {
        return this.stepBuilderFactory.get("csvFileStep")
                .<Person, Person>chunk(10)
                .reader(this.csvFileItemReader())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step jdbcCursorStep() throws Exception {
        return this.stepBuilderFactory.get("jdbcCursorStep")
                .<Person, Person>chunk(10)
                .reader(jdbcCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step jdbcPagingStep() throws Exception {
        return this.stepBuilderFactory.get("jdbcPagingStep")
                .<Person, Person>chunk(10)
                .reader(jdbcPagingItemReader())
                .writer(itemWriter())
                .build();
    }

    private JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        final JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader") // name 설정
                .dataSource(dataSource) // data source 는 생성자로 주입 받음
                .sql("select id, name, age, address from person") // Query 작성
                .rowMapper((rs, rowNum) -> new Person( // column index 는 0 번 부터가 아닌 1번부터 시작한다.
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4))) // 조회된 데이터를 Person 객체에 매핑
                .build();

        itemReader.afterPropertiesSet(); // 여기서 afterPropertiesSet 이 하는 일은 각각의 Writer 들이 실행되기 위해 필요한 필수값들이 제대로 세팅되어있는지를 체크합니다.
        return itemReader;
    }

    private JdbcPagingItemReader<Person> jdbcPagingItemReader() throws Exception {

        final JdbcPagingItemReader<Person> itemReader = new JdbcPagingItemReaderBuilder<Person>()
                .name("jdbcPagingItemReader")
                .dataSource(dataSource)
                .pageSize(10)
                .fetchSize(10)
                .rowMapper((rs, rowNum) -> new Person( // column index 는 0 번 부터가 아닌 1번부터 시작한다.
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4))) // 조회된 데이터를 Person 객체에 매핑
                .queryProvider(createQueryProvider())
                .build();

        itemReader.afterPropertiesSet();
        return itemReader;
    }

    public PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, name, age, address");
        queryProvider.setFromClause("from person");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.ASCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "name", "age", "address");
        lineMapper.setLineTokenizer(tokenizer);

        lineMapper.setFieldSetMapper(fileSet -> {
            int id = fileSet.readInt("id");
            String name = fileSet.readString("name");
            String age = fileSet.readString("age");
            String address = fileSet.readString("address");
            return new Person(id, name, age, address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")
                .encoding("UTF-8")
                .linesToSkip(1)
                .resource(new ClassPathResource("test.csv"))
                .lineMapper(lineMapper)
                .build();

        itemReader.afterPropertiesSet();
        return itemReader;
    }

    private ItemWriter<Person> itemWriter() {
        return items -> log.info(items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", "))
        );
    }

    /**
     * 10 개의 Person 객체를 생성해서 반환한다.
     *
     * @return
     */
    private List<Person> getItems() {
        final ArrayList<Person> items = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            items.add(new Person(i + 1, "test name" + i, "test age", "test address"));
        }
        return items;
    }
}