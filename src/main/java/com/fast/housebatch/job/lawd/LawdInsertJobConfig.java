package com.fast.housebatch.job.lawd;

import com.fast.housebatch.core.entity.Lawd;
import com.fast.housebatch.job.validator.FilePathParameterValidator;
import com.fast.housebatch.service.LawdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static com.fast.housebatch.job.lawd.LawdFieldSetMapper.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LawdInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final LawdService lawdService;

    @Bean
    public Job lawdInsertJob(Step lawdInsertStep) {
        return jobBuilderFactory.get("lawdInsertJob")
                .incrementer(new RunIdIncrementer())
                .validator(new FilePathParameterValidator())
                .start(lawdInsertStep)
                .build();
    }

    @JobScope
    @Bean
    public Step lawdInsertStep(FlatFileItemReader lawdFileItemReader , ItemWriter<Lawd> itemWriter) {
            return stepBuilderFactory.get("lawdInsertStep")
                    .<Lawd , Lawd>chunk(1000)
                    .reader(lawdFileItemReader)
                    .writer(itemWriter())
                    .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Lawd> lawdFlatFileItemReader(@Value("#{jobParameters[filePath]}") String filePath) {
        return new FlatFileItemReaderBuilder<Lawd>()
                .name("lawdFileItemReader")
                .delimited()
                .delimiter("\t")
                .names(LAWD_CD,LAWD_DONG,LAWD_EXIST)
                .linesToSkip(1)
                .fieldSetMapper(new LawdFieldSetMapper())
                .resource(new ClassPathResource(filePath))
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Lawd> itemWriter() {
        return items -> items.forEach(lawdService::upsert);
    }

}
