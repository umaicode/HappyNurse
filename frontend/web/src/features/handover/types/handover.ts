/**
 * 인수인계 (AI 서버) 응답 타입.
 *
 * 출처: `ai/nursing_ai/app/services/handover/schemas.py`
 *   - RosterSummary / RosterPatientItem
 *   - HandoverPayload / Slots / Slot / SlotItem / Citation / RuleFired / Meta
 *
 * `auto_summary_json` (jsonb) 은 HandoverPayload 와 동일 구조로 BE DB 에 저장된다.
 */

// ---------- PASS-BAR Slots ----------

export type SeverityFlag = "stable" | "watcher" | "unstable";
export type VerificationStatus = "ok" | "partial" | "failed";
export type SourceLayer = 1 | 2 | 3 | 4;

export interface Citation {
  id: string;
  // 원본 간호기록 PK — 출처 하이퍼링크의 키.
  record_id: string;
  line_range: number[];
  // ISO datetime
  ts: string;
  label: string;
  // 본문 발췌. BE/AI 가 line_range 구간의 원본 텍스트를 잘라 채워줌. 없을 수도 있음(옛 리포트).
  excerpt?: string;
}

export interface SlotItem {
  kind: string | null;
  value: string | null;
  quote: string | null;
  citation_ids: string[];
  confidence: number | null;
  source_layer: SourceLayer;
  time_window: string | null;
  trend: string | null;
  contingency: string | null;
  severity_flag: SeverityFlag | null;
}

export interface Slot {
  items: SlotItem[];
  verification: VerificationStatus;
}

export interface Slots {
  patient_problem: Slot;
  assessment: Slot;
  situation: Slot;
  safety: Slot;
  background: Slot;
  action: Slot;
  recommendation: Slot;
  synthesis: Slot;
}

export interface RuleFired {
  rule_id: string;
  label: string;
  source: string;
  severity: "low" | "medium" | "high";
  matched_citation_ids: string[];
}

export interface HandoverMeta {
  model: string;
  lexicon_version: string;
  rule_set_version: string;
  context_tiers_used: string[];
  last_record_ts: string;
  generated_at: string;
  token_usage: Record<string, number>;
  verifier: Record<string, string[]>;
  failures: Array<Record<string, unknown>>;
}

export interface HandoverPayload {
  schema_version: "2.0";
  header: string;
  illness_severity: SeverityFlag;
  slots: Slots;
  citations: Citation[];
  rules_fired: RuleFired[];
  meta: HandoverMeta;
}

// ---------- Roster Summary (진입 요약) ----------

export interface RosterPatientItem {
  encounter_id: number;
  handover_id: number;
  header: string;
  risk_score: number;
  rules_fired_brief: string[];
  // { ok: number, partial: number, failed: number } 추정
  verification_summary: Record<string, number>;
  // { new_records_since_report: number } 추정
  freshness: Record<string, number>;
}

export interface RosterSummary {
  schema_version: "1.0";
  kind: "roster_summary";
  narrative_header: string;
  // 백엔드가 dict 로 내려주는 자유 형식 (patient_count 등)
  stats: Record<string, unknown>;
  patients: RosterPatientItem[];
  verification_followup: Array<Record<string, unknown>>;
  meta: Record<string, unknown>;
}

// ---------- 단건 조회 응답 ----------

export interface HandoverDetailResponse {
  handoverId: string;
  encounterId: string;
  // 텍스트 요약 (HandoverPayload.header 와 같거나 비슷)
  autoSummary: string | null;
  // jsonb — DB 저장 형태에 따라 string 또는 object 일 수 있음. api 함수에서 normalize.
  autoSummaryJson: HandoverPayload | null;
  createdAt: string;
}

// ---------- 체크리스트 (BE) ----------
// BE /handover/{handoverId} 는 PASS-BAR 풀 데이터가 아니라 체크 상태만 반환.
// PASS-BAR 본체는 여전히 AI 서버 (`/api/handover/{id}`) 담당.

// 체크된 항목 1건의 메타 — 누른 사람 / 시각.
export interface HandoverCheckEntry {
  by: number;
  at: string;
}

// 키 = `{slotKey}.{itemIndex}` (현재 BE 가 synthesis 슬롯만 허용).
// 키 존재 = 체크 ON, 없으면 OFF.
export interface HandoverChecksResponse {
  handoverId: string;
  checkedItemsJson: Record<string, HandoverCheckEntry>;
}

// PATCH /handover/{handoverId}/checks body — 델타 방식.
// 값 true = ON 으로 만들기, false = OFF 로 만들기. 안 보낸 키는 BE 가 유지.
export type HandoverChecksPatch = Record<string, boolean>;

// ---------- 입퇴원 이벤트 (BE) ----------
// GET /handover/ward-events — 오늘 우리 병동의 입원/퇴원 환자 목록.
// 인수인계 화면 상단의 "교대 통합 요약" 자리 대체 (narrative_header 박스 → 입퇴원 박스).

export interface WardAdmissionItem {
  encounterId: number;
  patientName: string;
  roomName: string;
  bedName: string;
  // 입원 경로 — "응급" / "외래" / "재원" 등.
  classCode: string;
  chiefComplaint: string;
  diseaseName: string;
  surgeryName: string;
  // ISO datetime
  periodStart: string;
}

export interface WardDischargeItem {
  encounterId: number;
  patientName: string;
  roomName: string;
  bedName: string;
  classCode: string;
  chiefComplaint: string;
  diseaseName: string;
  surgeryName: string;
  // ISO datetime
  periodEnd: string;
}

export interface WardEventsResponse {
  // periodStart 오름차순
  admissions: WardAdmissionItem[];
  // periodEnd 오름차순
  discharges: WardDischargeItem[];
}

// ---------- generate 응답 ----------

export interface HandoverJobResponse {
  jobId: string;
}

// ---------- SSE 이벤트 ----------

export type HandoverSseEventName =
  | "started"
  | "complete"
  | "error"
  | "roster_summary"
  | "job_done";

export interface SseStartedPayload {
  encounter_id: string;
}
export interface SseCompletePayload {
  encounter_id: string;
  handover_id: string;
  verification_summary: Record<string, number>;
}
export interface SseErrorPayload {
  encounter_id: string;
  reason: string;
  fallback_handover_id: string | null;
}
// roster_summary 이벤트 데이터는 RosterSummary 와 동일 구조
export type SseRosterSummaryPayload = RosterSummary;
