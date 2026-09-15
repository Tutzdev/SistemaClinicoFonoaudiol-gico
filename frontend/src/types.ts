export type Role = "ADMIN" | "RECEPCAO";
export type Session = { id: string; name: string; email: string; role: Role };
export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};
export type BaseRecord = {
  id: string;
  name: string;
  active: boolean;
  version: number;
};
export type Patient = BaseRecord & {
  birthDate: string;
  phone: string;
  email: string;
  guardianName?: string;
  guardianRelationship?: string;
  guardianPhone?: string;
};
export type Professional = BaseRecord & {
  email?: string;
  phone?: string;
  registration?: string;
  region?: string;
  bio?: string;
  serviceIds: string[];
  published: boolean;
};
export type Service = BaseRecord & {
  description: string;
  durationMinutes: number;
  published: boolean;
};
export type User = BaseRecord & { email: string; role: Role };
export type RecordKind = "patients" | "professionals" | "services" | "users";
export type Clinic = {
  displayName: string;
  description: string;
  phone: string;
  email: string;
  whatsapp: string;
  publicAddress: string;
  addressConfirmed: boolean;
  version: number;
  legalName?: string;
  cnpj?: string;
};
export type AppointmentStatus =
  | "AGENDADO"
  | "CONFIRMADO"
  | "CONCLUIDO"
  | "CANCELADO"
  | "NAO_COMPARECEU";
export type Appointment = {
  id: string;
  patientId: string;
  patientName: string;
  professionalId: string;
  professionalName: string;
  serviceId: string;
  serviceName: string;
  start: string;
  end: string;
  durationMinutes: number;
  status: AppointmentStatus;
  version: number;
};
export type AppointmentHistory = {
  id: string;
  action: string;
  actorName: string;
  createdAt: string;
  previousStart?: string;
  newStart?: string;
  previousStatus?: AppointmentStatus;
  newStatus?: AppointmentStatus;
};
export type Period = { dayOfWeek: number; startTime: string; endTime: string };
export type Block = { id: string; start: string; end: string };
