package spring.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ChunkProcessingConfiguration {

    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;

    public ChunkProcessingConfiguration(final JobBuilderFactory jobBuilderFactory,
                                        final StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job chunkProcessingJob() {
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(this.taskBaseStep())
                .next(this.chunkBaseStep())
                .build();
    }

    @Bean
    public Step chunkBaseStep() {
        return stepBuilderFactory.get("chunkBaseStep")
                .<String, String>chunk(10)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemReader<String> itemReader() {
        return new ListItemReader<>(getItems());
    }

    private ItemProcessor<String, String> itemProcessor() {
        return item -> item + ", Spring Batch";
    }

    private ItemWriter<String> itemWriter() {
        return items -> log.info("chunk item size: {}", items.size());
//        return items -> items.forEach(log::info);
    }

    @Bean
    public Step taskBaseStep() {

        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(this.taskLet())
                .build();
    }

    /**
     * tasklet 을 chunk 처럼 만들어 보자.
     * -> 가능 하지만 코드량이 많아지고 chunk 처럼 나이스 하지 않다.
     * chunk 에선 100 개의 list 를 10 개씩 10번 실행했었는데, 이걸 tasklet 으로 구현해보자.
     *
     * 원본 코드는 아래와 같다.
     * ----------------------------------------
     private Tasklet tasklet() {
        return (contribution, chunkContext) -> {
            List<String> items = getItems();
            for (String item : items) {
                log.info("tasklet item = {}", item);
            }
            return RepeatStatus.FINISHED;
        };
     }
     * ----------------------------------------
     */
    private Tasklet taskLet() {
        List<String> items = getItems(); // getItems 로 생성했던 100 개 가 담길 list 를 밖으로 꺼내준다.

        return (contribution, chunkContext) -> {
            // StepExecution 을 가져와야 한다. contribution 에서 가져올 수 있다.
            // StepExecution 는 읽은 Item 크기를 저장 할 수 있다. -> 조회도 가능 하다
            StepExecution stepExecution = contribution.getStepExecution();

            int chunkSize = 10;
            int fromIndex = stepExecution.getReadCount(); // chunk 에서 읽은 item 의 크기 를 가져와서 할당해준다.
            int toIndex = fromIndex + chunkSize; // fromIndex 에 chunkSize 를 더해준다. -> fromIndex 부터 chunkSize (10) 개 의 item 만큼 읽어온다.

            if (fromIndex >= items.size()) {
                return RepeatStatus.FINISHED;
            }

            // index 10 번 부터 10 개의 item 을 꺼내온다. -> 페이징 처리 기능
            List<String> subList = items.subList(fromIndex, toIndex);

            log.info("task item size: {}", subList.size());

            stepExecution.setReadCount(toIndex);

            return RepeatStatus.CONTINUABLE; // 해당 tasklet 을 반복 처리 하라는 의미이다.
        };
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(i + " Hello");
        }
        return items;
    }
}
