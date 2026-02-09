import PageBreadcrumb from "@/components/common/PageBreadCrumb";
import BusinessInputs from "@/components/form/add-business/BusinessInputs";
import ContactInputs from "@/components/form/add-business/ContactInputs";
import InputStates from "@/components/form/add-business/InputStates";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Business Form | Admin panel - Dashboard",
  description:
    "This is an admin dashboard, where admins can mange businesses",
};

export default function AddBusiness() {
  return (
    <div>
      <PageBreadcrumb pageTitle="Add Business" />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <div className="space-y-6">
          <BusinessInputs />
          <InputStates />
        </div>
        <div className="space-y-6">
          <ContactInputs />
        </div>
      </div>
    </div>
  );
}
