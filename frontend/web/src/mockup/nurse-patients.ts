/**
 * 간호사용 웹 환자 목록 / 상세 목업 데이터.
 * 실제 환자 조회 API 연동 시 통째로 삭제 가능.
 */
import type { Patient, PatientDetail } from "@/features/patient/types/patient";

export const INITIAL_PATIENTS: Patient[] = [
  {
    id: "1",
    name: "김영수",
    age: 68,
    gender: "M",
    birthday: "1958.01.01",
    assignedNurse: "김영희",
    unconfirmedCount: 0,
    bedNo: "01A",
    roomNo: "302호",
    diagnosis: "I50.0 울혈성 심부전",
    pendingSTTCount: 2,
  },
  {
    id: "2",
    name: "박미선",
    age: 55,
    gender: "F",
    birthday: "1971.01.01",
    assignedNurse: "김영희",
    unconfirmedCount: 0,
    bedNo: "02B",
    roomNo: "302호",
    diagnosis: "J18.9 폐렴",
    pendingSTTCount: 0,
  },
  {
    id: "3",
    name: "이철호",
    age: 72,
    gender: "M",
    birthday: "1954.01.01",
    assignedNurse: "김영희",
    unconfirmedCount: 0,
    bedNo: "03A",
    roomNo: "302호",
    diagnosis: "N18.3 만성 신장병 3기",
    pendingSTTCount: 1,
  },
  {
    id: "4",
    name: "정은지",
    age: 45,
    gender: "F",
    birthday: "1981.01.01",
    assignedNurse: "김영희",
    unconfirmedCount: 0,
    bedNo: "04A",
    roomNo: "302호",
    diagnosis: "E11.9 당뇨병",
    pendingSTTCount: 0,
  },
];

export const INITIAL_PATIENT_DETAIL: PatientDetail = {
  id: "1",
  name: "김영수",
  age: 68,
  gender: "M",
  birthday: "1958.01.01",
  assignedNurse: "김영희",
  unconfirmedCount: 0,
  bedNo: "01A",
  roomNo: "302호",
  diagnosis: "I50.0 울혈성 심부전",
  pendingSTTCount: 2,
  admittedAt: "2025-04-01",
  nurseId: "nurse-01",
};
