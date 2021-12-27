package com.fast.housebatch.service;

import com.fast.housebatch.core.entity.Lawd;
import com.fast.housebatch.core.repository.LawdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LawdService {
    private final LawdRepository lawdRepository;

    @Transactional
    public void upsert(Lawd lawd) {
        // 데이터가 존재시 수정 , 없으면 생성
        Lawd saved = lawdRepository.findLawdByLawdCd(lawd.getLawdCd())
                .orElseGet(Lawd::new);
        saved.setLawdCd(lawd.getLawdCd());
        saved.setLawdDong(lawd.getLawdDong());
        saved.setExist(lawd.getExist());
        lawdRepository.save(saved);
    }


}
