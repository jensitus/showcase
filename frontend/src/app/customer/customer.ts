import {Insurance} from "../insurance/insurance";

export interface Customer {
  id?: string;
  gender?: string;
  firstname?: string;
  lastname?: string;
  dateOfBirth?: string;
  email?: string;
  street?: string;
  zipCode?: string;
  city?: string;
  country?: string;
  customerNumber?: number;
  phoneNumber?: string;
  insurances?: Insurance[];
}
