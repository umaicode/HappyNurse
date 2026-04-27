export type OrderStatus = "접수" | "진행" | "검사중" | "완료";

export type OrderCategory = "수액" | "지시" | "투약" | "LIS" | "영상";

export interface DoctorOrder {
  id: string;
  category: OrderCategory;
  code: string;
  name: string;
  dose: string;
  frequency: string;
  unit: string;
  method: string;
  status: OrderStatus;
  time: string;
  remarks: string;
  patientName: string;
  isChanged?: boolean;
}
