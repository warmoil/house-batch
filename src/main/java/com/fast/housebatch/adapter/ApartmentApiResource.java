package com.fast.housebatch.adapter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;


/**
* 아파트 실거래가 API 를 호출하기 위한 파라미터
 * 1. serviceKey - API 를 호출하기 위한 인증키
 * 2.LAWD_CD - 법적동콛드 10자리중 앞 5자리 - 구 지역코드 guLawdCd  ex)41135
 * 3.DEAL_YMD - 거래가 발생한 년월   ex)202107
* */
@Slf4j
@Component
public class ApartmentApiResource {
    @Value("${external.apartment-api.path}")
    private String path;
    @Value("${external.apartment-api.service-key}")
    private String serviceKey;


    public Resource getResource(String lawdCd , YearMonth month)  {
        String url = String.format("%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s",path,serviceKey,lawdCd
                ,month.format(DateTimeFormatter.ofPattern("yyyyMM")));
        log.info("resourceUrl = " + url);
        try {
            return new UrlResource(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("잘못된 URL 입니다:"+url);
        }
    }

}
