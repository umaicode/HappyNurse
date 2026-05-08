package com.ssafy.happynurse.domain.webapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.happynurse.domain.webapp.entity.SymptomPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SymptomClassificationServiceTest {

    private SymptomClassificationService service;
    private SymptomClassificationLlmClient llmClient;

    @BeforeEach
    void setUp() {
        llmClient = mock(SymptomClassificationLlmClient.class);
        given(llmClient.classify(anyString(), any())).willReturn(
                new SymptomClassificationService.SymptomClassificationResult(SymptomPriority.MEDIUM, null));
        service = new SymptomClassificationService(new ObjectMapper(), llmClient);
        service.loadDictionary();
    }

    @Test
    @DisplayName("호흡 키워드는 CRITICAL로 분류된다")
    void classify_성공_CRITICAL_호흡_키워드() {
        var result = service.classify("숨이 답답해요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("출혈 키워드는 CRITICAL로 분류된다")
    void classify_성공_CRITICAL_출혈_키워드() {
        var result = service.classify("피가 멈추지 않아요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("통증 단독은 HIGH로 분류된다")
    void classify_성공_HIGH_통증_키워드() {
        var result = service.classify("어깨가 욱신거려요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("일반 정보 문의는 LOW로 분류된다")
    void classify_성공_LOW_일반_문의() {
        var result = service.classify("퇴원은 언제 해요?", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.LOW);
    }

    @Test
    @DisplayName("여러 카테고리가 매칭되면 가장 높은 우선순위가 채택된다")
    void classify_성공_여러_카테고리_매칭_시_최고_우선순위() {
        var result = service.classify("숨이 답답하고 어깨도 아파요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("compound upgrade — 통증 + 소화기 증상은 CRITICAL로 승격된다")
    void classify_성공_compound_upgrade_통증과_소화기증상_CRITICAL() {
        var result = service.classify("배가 엄청 아프고 계속 토해요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("compound upgrade — 감염 징후 키워드 2개 이상이면 CRITICAL로 승격된다")
    void classify_성공_compound_upgrade_감염징후_2개이상_CRITICAL() {
        var result = service.classify("열도 나고 수술한 데가 빨갛고 고름도 나와요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("부서 override — 정형외과에서 감각 이상 호소는 CRITICAL")
    void classify_성공_부서_override_정형외과_CRITICAL() {
        var result = service.classify("다리에 감각이 없어요", "OS");

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("부서 override는 해당 부서 코드일 때만 적용된다")
    void classify_성공_부서_override_다른_부서는_적용_안됨() {
        var result = service.classify("다리가 퍼렇게 됐어요", null);

        assertThat(result.priority()).isNotEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("어떤 키워드도 매칭 안 되면 MEDIUM 기본값을 반환한다")
    void classify_성공_미매칭_MEDIUM_기본값() {
        var result = service.classify("ㅁㅁㅁㅁㅁ 알 수 없는 텍스트 zxcvbnm", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("빈 문자열은 MEDIUM 기본값을 반환한다")
    void classify_성공_빈_문자열_MEDIUM() {
        var result = service.classify("", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("null 입력은 MEDIUM 기본값을 반환한다")
    void classify_성공_null_MEDIUM() {
        var result = service.classify(null, null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("사전 보강 — '어지러' 활용형은 CRITICAL로 매칭된다")
    void classify_성공_어지러_CRITICAL() {
        var result = service.classify("머리가 너무 어지러워요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("사전 보강 — '고열' 활용형은 HIGH로 매칭된다")
    void classify_성공_고열_HIGH() {
        var result = service.classify("어젯밤부터 고열이 나요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("사전 보강 — '메스꺼' 활용형은 HIGH로 매칭된다")
    void classify_성공_메스꺼_HIGH() {
        var result = service.classify("속이 메스꺼워서 못 먹겠어요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("사전 보강 — '숨이 가빠' 표현은 CRITICAL로 매칭된다")
    void classify_성공_가빠_CRITICAL() {
        var result = service.classify("계단 오를 때 숨이 가빠요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("사전 보강 — '엎어졌' 표현은 CRITICAL(낙상)로 매칭된다")
    void classify_성공_엎어_CRITICAL() {
        var result = service.classify("화장실 가다 엎어졌어요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("사전 보강 — '기억이 안 나' 표현은 CRITICAL(신경계)로 매칭된다")
    void classify_성공_기억이_안_CRITICAL() {
        var result = service.classify("방금 일이 기억이 안 나요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("classifyButton — '통증' 버튼은 HIGH를 반환한다")
    void classifyButton_성공_통증_HIGH() {
        var result = service.classifyButton("통증");

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("classifyButton — '수액' 버튼은 CRITICAL을 반환한다")
    void classifyButton_성공_수액_CRITICAL() {
        var result = service.classifyButton("수액");

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("classifyButton — '호흡 불편' 버튼은 CRITICAL을 반환한다")
    void classifyButton_성공_호흡불편_CRITICAL() {
        var result = service.classifyButton("호흡 불편");

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("classifyButton — '화장실' 버튼은 MEDIUM을 반환한다")
    void classifyButton_성공_화장실_MEDIUM() {
        var result = service.classifyButton("화장실");

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("classifyButton — 등록되지 않은 라벨은 MEDIUM 기본값을 반환한다")
    void classifyButton_성공_미등록_라벨_MEDIUM() {
        var result = service.classifyButton("알수없는라벨");

        assertThat(result.priority()).isEqualTo(SymptomPriority.MEDIUM);
    }

    @Test
    @DisplayName("safety_rules.no_downgrade — 키워드로 CRITICAL 산출되면 LLM 호출 안 한다")
    void classify_성공_CRITICAL_키워드_매칭_시_LLM_호출_안함() {
        service.classify("숨이 답답해요", null);

        verify(llmClient, never()).classify(anyString(), any());
    }

    @Test
    @DisplayName("키워드 미매칭이면 LLM에 위임된다")
    void classify_성공_키워드_미매칭_LLM_위임() {
        given(llmClient.classify(anyString(), any())).willReturn(
                new SymptomClassificationService.SymptomClassificationResult(
                        SymptomPriority.HIGH, BigDecimal.valueOf(0.82)));

        var result = service.classify("뭔가 이상한 느낌이 드는데 잘 모르겠어요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
        assertThat(result.confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.82));
        verify(llmClient).classify("뭔가 이상한 느낌이 드는데 잘 모르겠어요", null);
    }

    @Test
    @DisplayName("LLM 위임 시 부서 코드도 함께 전달된다")
    void classify_성공_LLM_위임_시_부서코드_전달() {
        service.classify("뭔가 이상해요", "GS");

        verify(llmClient).classify("뭔가 이상해요", "GS");
    }

    @Test
    @DisplayName("'실밥 언제 풀어요?'는 일정 문의로 LOW로 분류된다")
    void classify_성공_LOW_실밥_언제_풀어요_일정문의() {
        var result = service.classify("실밥 언제 풀어요?", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.LOW);
    }

    @Test
    @DisplayName("'실밥이 빠졌어요'는 처치 필요로 HIGH로 분류된다")
    void classify_성공_HIGH_실밥이_빠졌어요_처치필요() {
        var result = service.classify("실밥이 빠졌어요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("'실밥이 터졌어요'는 처치 필요로 HIGH로 분류된다")
    void classify_성공_HIGH_실밥이_터졌어요_처치필요() {
        var result = service.classify("실밥이 터졌어요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.HIGH);
    }

    @Test
    @DisplayName("'실밥 풀어주세요'는 키워드 미매칭으로 LLM에 위임된다")
    void classify_성공_실밥_풀어주세요_LLM_위임() {
        service.classify("실밥 풀어주세요", null);

        verify(llmClient).classify("실밥 풀어주세요", null);
    }

    @Test
    @DisplayName("띄어쓴 '식은 땀' 발화도 CRITICAL로 매칭된다")
    void classify_성공_식은땀_띄어쓰기_CRITICAL() {
        var result = service.classify("계속 식은 땀이 나요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("붙여쓴 '식은땀' 발화도 기존대로 CRITICAL을 유지한다")
    void classify_성공_식은땀_붙여쓰기_회귀_CRITICAL() {
        var result = service.classify("식은땀이 나요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("띄어쓴 '가슴 통증' 발화도 CRITICAL로 매칭된다")
    void classify_성공_가슴통증_띄어쓰기_CRITICAL() {
        var result = service.classify("가슴 통증이 심해요", null);

        assertThat(result.priority()).isEqualTo(SymptomPriority.CRITICAL);
    }

    @Test
    @DisplayName("띄어쓴 '소변 줄' 발화는 wound_treatment 매칭으로 HIGH 이상이다")
    void classify_성공_소변줄_띄어쓰기_HIGH_이상() {
        var result = service.classify("소변 줄이 빠진 것 같아요", null);

        assertThat(result.priority().ordinal())
                .isLessThanOrEqualTo(SymptomPriority.HIGH.ordinal());
    }
}
