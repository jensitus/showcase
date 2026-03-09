export interface Insurance {
  id: string;
  name: string;
  simpleName: string;
  insuranceType: string;
  insuranceNumber: string;
  createdAt: string;
  updatedAt?: string;
  insuranceSum?: string;
  paymentSchedule: string;
  insuranceCoverage: string;
  amount: number;
  state: string;
}
