package com.fast.housebatch.job.apt;

import com.fast.housebatch.adapter.ApartmentApiResource;
import com.fast.housebatch.core.dto.AptDealDto;
import com.fast.housebatch.job.validator.LawdCdParameterValidator;
import com.fast.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ApartmentApiResource apartmentApiResource;


    @Bean
    public Job aptDealInsertJob(Step aptDealInsertStep) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                .validator(aptDealJobParameterValidator())
                .start(aptDealInsertStep)//step
                .build();

    }

    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(
                new YearMonthParameterValidator(),
                new LawdCdParameterValidator()
        ));
        return validator;
    }

    @JobScope
    @Bean
    public Step aptDealInsertStep(StaxEventItemReader<AptDealDto> aptDealResourceReader ,ItemWriter<AptDealDto> aptDealDtoItemWriter) {
        return stepBuilderFactory.get("aptDealInsertStep")
                .<AptDealDto , AptDealDto>chunk(10)
                .reader(aptDealResourceReader)
                .writer(aptDealDtoItemWriter)
                .build();
    }

    @StepScope
    @Bean
    public StaxEventItemReader<AptDealDto> aptDealResourceReader
            (
                    //@Value("#{jobParameters['filePath']}") String filePath,
                    @Value("#{jobParameters['yearMonth']}") String yearMonth,
                    @Value("#{jobParameters['lawdCd']}") String lawdCd,
                    Jaxb2Marshaller aptDealDtoMarshaller) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
                .name("aptDealResourceReader")
                .resource(apartmentApiResource.getResource(lawdCd, YearMonth.parse(yearMonth)))
                .addFragmentRootElements("item")//루트이름
                .unmarshaller(aptDealDtoMarshaller)//
                .build();
    }

    @Bean
    @StepScope
    public Jaxb2Marshaller aptDealDtoMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
        return jaxb2Marshaller;

    }

    @Bean
    @StepScope
    public ItemWriter<AptDealDto> aptDealDtoItemWriter() {
        return items -> items.forEach(System.out::println);
    }

}
