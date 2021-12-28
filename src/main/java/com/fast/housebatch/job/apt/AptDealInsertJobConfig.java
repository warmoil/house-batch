package com.fast.housebatch.job.apt;

import com.fast.housebatch.adapter.ApartmentApiResource;
import com.fast.housebatch.core.dto.AptDealDto;
import com.fast.housebatch.core.repository.LawdRepository;
import com.fast.housebatch.job.validator.LawdCdParameterValidator;
import com.fast.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ApartmentApiResource apartmentApiResource;
    private final LawdRepository lawdRepository;

    @Bean
    public Job aptDealInsertJob(
//            Step aptDealInsertStep, //이건 실제로 API호출 하루 천건 재한
            Step guLawdCdStep,
            Step stepContextPrint
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                //  .validator(aptDealJobParameterValidator())
                .start(guLawdCdStep)//step
                .next(stepContextPrint)
//                .next(aptDealInsertStep)
                .build();

    }

    @JobScope
    @Bean
    public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
        return stepBuilderFactory.get("guLawdCdStep")
                .tasklet(guLawdCdTasklet)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet guLawdCdTasklet() {
        return (contribution, chunkContext) -> {
            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();

            //데이터가 있으면 다음스텝을 실행하고 데이터가 없으면종료
            //데이터가 있으면 CONTINUABLE
            List<String> guLawdCd = lawdRepository.searchDistinctGuLawdCd();
            executionContext.putString("guLawdCd", guLawdCd.get(0));
            return RepeatStatus.FINISHED;
        };
    }

    @JobScope
    @Bean
    public Step stepContextPrint(Tasklet contextTaskletPrint) {
        return stepBuilderFactory.get("stepContextPrint")
                .tasklet(contextTaskletPrint)
                .build();
    }

    @StepScope
    @Bean
    public Tasklet contextTaskletPrint(
            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd
    ) {
        return ((contribution, chunkContext) -> {
            System.out.println("[contextPrintStep]" + guLawdCd);
            return RepeatStatus.FINISHED;
        });
    }

    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(
                new YearMonthParameterValidator()
                //    new LawdCdParameterValidator()
        ));
        return validator;
    }

    @JobScope
    @Bean
    public Step aptDealInsertStep(StaxEventItemReader<AptDealDto> aptDealResourceReader, ItemWriter<AptDealDto> aptDealDtoItemWriter) {
        return stepBuilderFactory.get("aptDealInsertStep")
                .<AptDealDto, AptDealDto>chunk(10)
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
                    @Value("#{jobExecutionContext['guLawdCd']}") String lawdCd,
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
