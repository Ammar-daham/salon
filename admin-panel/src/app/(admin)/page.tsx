import type { Metadata } from "next";
import React from "react";


export const metadata: Metadata = {
  title: "Home page | Admin Dashboard",
  description: "This is Home for Admin Dashboard",
};

export default function Home() {
  return (
    <div className="grid grid-cols-12 gap-4 md:gap-6">
      <div className="col-span-12 space-y-6 xl:col-span-7">
        
      </div>

      <div className="col-span-12 xl:col-span-5">
      </div>

      <div className="col-span-12">
      </div>

      <div className="col-span-12 xl:col-span-5">
      </div>

      <div className="col-span-12 xl:col-span-7">
      </div>
    </div>
  );
}
