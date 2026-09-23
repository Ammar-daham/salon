import GridShape from "@/components/common/GridShape";
import ThemeTogglerTwo from "@/components/common/ThemeTogglerTwo";

import React from "react";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="relative p-6 bg-surface-raised z-1 sm:p-0">
      <div className="relative flex lg:flex-row w-full h-screen justify-center flex-col sm:p-0">
        {children}
        <div className="lg:w-1/2 w-full h-full bg-primary-900 dark:bg-primary-950 lg:grid items-center hidden">
          <div className="relative items-center justify-center flex z-1">
            {/* <!-- ===== Common Grid Shape Start ===== --> */}
            <GridShape />
            <div className="flex flex-col items-center max-w-xs text-center">
              <span className="mb-3 font-semibold tracking-tight text-white text-display">
                Salon
              </span>
              <p className="text-primary-100/80">
                Booking, staff and clients for beauty and wellness businesses.
              </p>
            </div>
          </div>
        </div>
        <div className="fixed bottom-6 right-6 z-50 hidden sm:block">
          <ThemeTogglerTwo />
        </div>
      </div>
    </div>
  );
}
