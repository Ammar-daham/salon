import SignUpForm from "@/components/auth/SignUpForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Add Staff Account | Salon Admin",
  description: "Create a new employee or admin login for your business",
};

export default function SignUp() {
  return <SignUpForm />;
}
