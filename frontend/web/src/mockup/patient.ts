export const symptomsMock = [
  { id: "pain", label: "통증", sub: "약물 요청" },
  { id: "toilet", label: "화장실", sub: "이동 도움" },
  { id: "dressing", label: "드레싱", sub: "드레싱 교체" },
  { id: "iv", label: "수액", sub: "수액 점검" },
  { id: "position", label: "체위 변경", sub: "자세 도움" },
  { id: "breathing", label: "호흡 불편", sub: "응급 요청" },
] as const;

export type SymptomId = (typeof symptomsMock)[number]["id"];

export type FaqItem = {
  id: string;
  question: string;
  answer: string;
  surgeryType: string;
  ward: string;
};

export const faqMock: FaqItem[] = [
  {
    id: "faq-1",
    question: "🛠️수술 후 언제부터 걸을 수 있나요?",
    answer:
      "🛠️일반적으로 수술 다음 날부터 보조기를 착용하고 짧은 거리 보행 연습을 시작합니다. 담당 의료진의 지시에 따라 무리하지 않는 범위에서 조금씩 거리를 늘려주세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-2",
    question: "🛠️수술 부위가 붓고 멍이 들었어요. 정상인가요?",
    answer:
      "🛠️수술 후 2~3주간은 부종과 멍이 흔하게 나타납니다. 얼음찜질과 다리 올리기를 자주 해주시면 완화됩니다. 열감이나 통증이 심해지면 간호사를 호출해 주세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-3",
    question: "🛠️진통제는 얼마나 자주 복용할 수 있나요?",
    answer:
      "🛠️처방된 진통제는 보통 6시간 간격으로 복용합니다. 통증이 심할 경우 간호사에게 알려주시면 추가 조치를 안내드립니다. 자의로 용량을 늘리지 마세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-4",
    question: "🛠️샤워는 언제부터 가능한가요?",
    answer:
      "🛠️수술 부위의 방수 드레싱이 유지되는 동안에는 가벼운 샤워가 가능합니다. 드레싱이 젖거나 떨어졌을 경우 간호사에게 교체를 요청해 주세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-5",
    question: "🛠️재활 운동은 언제 시작하나요?",
    answer:
      "🛠️수술 다음 날부터 침상에서 발목 돌리기, 허벅지 힘주기 등 가벼운 운동을 시작합니다. 물리치료사의 지도에 따라 무릎 굽히기 각도를 단계적으로 늘려갑니다.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-6",
    question: "🛠️식사는 어떻게 해야 하나요?",
    answer:
      "🛠️수술 당일은 미음 또는 죽 형태로 시작하며, 다음 날부터 일반식이 가능합니다. 회복을 위해 단백질과 칼슘이 풍부한 음식을 충분히 섭취해 주세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-7",
    question: "🛠️퇴원 후 주의사항이 궁금해요.",
    answer:
      "🛠️무리한 계단 이용, 쪼그려 앉기, 양반다리 자세는 피해주세요. 처방된 약 복용, 재활 운동, 외래 진료 일정을 꼭 지켜주시기 바랍니다.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
  {
    id: "faq-8",
    question: "🛠️밤에 잠을 자기 어려워요. 어떻게 하면 좋을까요?",
    answer:
      "🛠️수술 후 통증이나 자세 불편으로 수면에 어려움을 겪을 수 있습니다. 베개를 활용해 다리를 편안하게 받치고, 필요시 간호사에게 수면 보조를 요청해 주세요.",
    surgeryType: "🛠️퇴행성 무릎 관절염",
    ward: "🛠️정형외과 병동",
  },
];
