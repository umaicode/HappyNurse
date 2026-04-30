/**
 * 기관(병원)/병동 조회 타입.
 *
 * - [간호사용 웹] LoginForm step 1 의 병원/병동 select 데이터
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — api 함수에서 data 만 추출.
 */

export interface Organization {
  organizationId: number;
  name: string;
  typeCode: string;
}

export interface WardSummary {
  wardId: number;
  wardName: string;
}
