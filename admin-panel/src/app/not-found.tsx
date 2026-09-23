import GridShape from "@/components/common/GridShape";
import Image from "next/image";
import Link from "next/link";
import React from "react";

export default function NotFound() {
  return (
    <div className="relative flex flex-col items-center justify-center min-h-screen p-6 overflow-hidden z-1">
      <GridShape />
      <div className="mx-auto w-full max-w-[242px] text-center sm:max-w-[472px]">
        <h1 className="mb-8 font-bold text-neutral-800 text-display dark:text-white/90 xl:text-display">
          ERROR
        </h1>

        <Image
          src="/images/error/404.svg"
          alt="404"
          className="dark:hidden"
          width={472}
          height={152}
        />
        <Image
          src="/images/error/404-dark.svg"
          alt="404"
          className="hidden dark:block"
          width={472}
          height={152}
        />

        <p className="mt-10 mb-6 text-base text-neutral-700 dark:text-neutral-400 sm:text-lg">
          We can’t seem to find the page you are looking for!
        </p>

        <Link
          href="/"
          className="inline-flex items-center justify-center rounded-lg border border-neutral-300 bg-white px-5 py-3.5 text-sm font-medium text-neutral-700 shadow-xs hover:bg-neutral-50 hover:text-neutral-800 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-400 dark:hover:bg-white/[0.03] dark:hover:text-neutral-200"
        >
          Back to Home Page
        </Link>
      </div>
      {/* <!-- Footer --> */}
      <p className="absolute text-sm text-center text-neutral-500 -translate-x-1/2 bottom-6 left-1/2 dark:text-neutral-400">
        &copy; {new Date().getFullYear()} - TailAdmin
      </p>
    </div>
  );
}
