package com.ssafy.happynurse.domain.webapp.repository;

import com.ssafy.happynurse.domain.webapp.entity.QuickSymptomButton;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuickSymptomButtonRepository extends JpaRepository<QuickSymptomButton, Long> {

    List<QuickSymptomButton> findAllByOrderByDisplayOrderAsc();
}
