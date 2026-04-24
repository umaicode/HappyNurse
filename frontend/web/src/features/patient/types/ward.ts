export interface Patient {
  id: string;
  name: string;
  age: number;
  gender: string;
  birthday: string;
  assignedNurse: string;
  unconfirmedCount: number;
  room?: string;
}

export interface Room {
  id: string;
  name: string;
  capacity: number;
  patients: Patient[];
}

export interface Ward {
  id: string;
  name: string;
  rooms: Room[];
}
