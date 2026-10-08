import SignInForm from "@/components/auth/SignInForm";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Next.js SignIn Page | TailAdmin - Next.js Dashboard Template",
  description: "This is Next.js Signin Page TailAdmin Dashboard Template",
};

// ?reset=1 comes from the reset page, once the new password is set.
// `searchParams` is a Promise in this version of Next and must be awaited.
export default async function SignIn({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { reset } = await searchParams;
  return <SignInForm passwordReset={reset === "1"} />;
}
