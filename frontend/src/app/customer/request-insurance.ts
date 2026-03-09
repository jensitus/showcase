export interface RequestInsurance {
  insuranceType: string;
  customerId: string;
  mudslideRisk?: boolean;
  floodRisk?: boolean;
  sufficientIncome: boolean;
  insuranceCoverage?: string;
  insuranceSum?: string;
  paymentSchedule: string;
  amount: number;
}
