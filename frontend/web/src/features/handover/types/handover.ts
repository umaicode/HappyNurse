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
  // 원본 본문 발췌 — BE 가 보내기 시작하면 자동 사용. 현재는 미응답이라 undefined 일 수 있음.
  snippet?: string | null;
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
