// 퀵 수정용 의학 용어 사전
// 치환 후에도 퀵메뉴 버튼(trigger)이 유지되도록 치환 결과 단어에도 역매핑을 둔다.
// 치환 결과는 공백을 포함하지 않아야 content.split(" ") 기반 렌더링이 깨지지 않는다.
export const MEDICAL_SUGGESTIONS: Record<string, string[]> = {
  충우염: ["충수염"],
  충수염: ["Appendicitis"],
  Appendicitis: ["충수염"],
  타이래놀: ["타이레놀"],
  타이레놀: ["타이래놀"],
  바이탈: ["V/S", "활력징후"],
  "V/S": ["바이탈", "활력징후"],
  활력징후: ["바이탈", "V/S"],
  도뇨관: ["Foley", "폴리"],
  Foley: ["도뇨관", "폴리"],
  폴리: ["도뇨관", "Foley"],
  수액: ["N/S", "D/W", "Fluid"],
  "N/S": ["수액", "D/W", "Fluid"],
  "D/W": ["수액", "N/S", "Fluid"],
  Fluid: ["수액", "N/S", "D/W"],
};
