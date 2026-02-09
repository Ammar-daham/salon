"use client";
import React from "react";
import ComponentCard from "../../common/ComponentCard";
import Label from "../Label";
import Input from "../input/InputField";
import { EnvelopeIcon } from "../../../icons";
import PhoneInput from "../group-input/PhoneInput";

export default function ContactInputs() {
  const countries = [
    { code: "US", label: "+1" },
    { code: "FI", label: "+358" },
    { code: "IQ", label: "+964" }
  ];
  const handlePhoneNumberChange = (phoneNumber: string) => {
    console.log("Updated phone number:", phoneNumber);
  };
  return (
    <ComponentCard title="Contact Inputs">
      <div className="space-y-6">
        <div>
          <Label>Email</Label>
          <div className="relative">
            <Input
              placeholder="info@gmail.com"
              type="text"
              className="pl-[62px]"
            />
            <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
              <EnvelopeIcon />
            </span>
          </div>
        </div>
        <div>
          <Label>Phone</Label>
          <PhoneInput
            selectPosition="start"
            countries={countries}
            placeholder="+1 (555) 000-0000"
            onChange={handlePhoneNumberChange}
          />
        </div>
        <div>
          <Label>WhatsApp</Label>
          <PhoneInput
            selectPosition="start"
            countries={countries}
            placeholder="+1 (555) 000-0000"
            onChange={handlePhoneNumberChange}
          />
        </div>
        <div>
          <Label>Website</Label>
          <div className="relative">
            <Input
              placeholder="https://ammardaham.fi/"
              type="text"
              className="pl-[62px]"
            />
            <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
              <EnvelopeIcon />
            </span>
          </div>
        </div>
        <div>
          <Label>Instagram</Label>
          <div className="relative">
            <Input
              placeholder="https://instagram.com/yourbusiness"
              type="text"
              className="pl-[62px]"
            />
            <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
              <EnvelopeIcon />
            </span>
          </div>
        </div>
        <div>
          <Label>Facebook</Label>
          <div className="relative">
            <Input
              placeholder="https://www.facebook.com/yourbusiness"
              type="text"
              className="pl-[62px]"
            />
            <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
              <EnvelopeIcon />
            </span>
          </div>
        </div>
        <div>
          <Label>Tiktok</Label>
          <div className="relative">
            <Input
              placeholder="https://www.tiktok.com/@yourbusiness"
              type="text"
              className="pl-[62px]"
            />
            <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
              <EnvelopeIcon />
            </span>
          </div>
        </div>
      </div>
    </ComponentCard>
  );
}

// "use client";

// import React, { useState } from "react";
// import ComponentCard from "../../common/ComponentCard";
// import Label from "../Label";
// import Input from "../input/InputField";
// import { EnvelopeIcon, PlusIcon, TrashBinIcon } from "../../../icons";

// export default function InputGroup() {
//   const countries = [
//     { code: "US", label: "+1" },
//     { code: "FI", label: "+358" },
//     { code: "IQ", label: "+964" },
//   ];

//   /* -------------------- state -------------------- */
//   const [emails, setEmails] = useState<string[]>([""]);
//   const [phones, setPhones] = useState<{ country: string; number: string }[]>([
//     { country: "+1", number: "" },
//   ]);
//   const [whatsapps, setWhatsapps] = useState<{ country: string; number: string }[]>([
//     { country: "+1", number: "" },
//   ]);

//   const [websites, setWebsites] = useState<string[]>([""]);
//   const [instagram, setInstagram] = useState<string[]>([""]);
//   const [facebook, setFacebook] = useState<string[]>([""]);
//   const [tiktok, setTiktok] = useState<string[]>([""]);

//   /* -------------------- helpers -------------------- */
//   const updateAt = (list: any[], index: number, value: any) =>
//     list.map((item, i) => (i === index ? value : item));

//   const removeAt = (list: any[], index: number) =>
//     list.filter((_, i) => i !== index);

//   /* -------------------- render -------------------- */
//   return (
//     <ComponentCard title="Contact Inputs">
//       <div className="space-y-8">

//         {/* ================= EMAIL ================= */}
//         <MultiTextInput
//           label="Email"
//           placeholder="info@gmail.com"
//           values={emails}
//           setValues={setEmails}
//           icon={<EnvelopeIcon />}
//           type="email"
//         />

//         {/* ================= PHONE ================= */}
//         <MultiPhoneInput
//           label="Phone"
//           values={phones}
//           setValues={setPhones}
//           countries={countries}
//           placeholder="000-000-0000"
//         />

//         {/* ================= WHATSAPP ================= */}
//         <MultiPhoneInput
//           label="WhatsApp"
//           values={whatsapps}
//           setValues={setWhatsapps}
//           countries={countries}
//           placeholder="000-000-0000"
//         />

//         {/* ================= WEBSITE / SOCIAL ================= */}
//         <MultiUrlSection label="Website" placeholder="https://yourbusiness.com" values={websites} setValues={setWebsites} />
//         <MultiUrlSection label="Instagram" placeholder="instagram.com/yourbusiness" values={instagram} setValues={setInstagram} />
//         <MultiUrlSection label="Facebook" placeholder="facebook.com/yourbusiness" values={facebook} setValues={setFacebook} />
//         <MultiUrlSection label="TikTok" placeholder="tiktok.com/@yourbusiness" values={tiktok} setValues={setTiktok} />

//       </div>
//     </ComponentCard>
//   );
// }

// /* ========================================================= */
// /* ================= REUSABLE MULTI TEXT INPUT ============ */
// /* ========================================================= */
// function MultiTextInput({
//   label,
//   placeholder,
//   values,
//   setValues,
//   icon,
//   type = "text",
// }: {
//   label: string;
//   placeholder: string;
//   values: string[];
//   setValues: React.Dispatch<React.SetStateAction<string[]>>;
//   icon?: React.ReactNode;
//   type?: string;
// }) {
//   const updateAt = (list: any[], index: number, value: any) =>
//     list.map((item, i) => (i === index ? value : item));
//   const removeAt = (list: any[], index: number) =>
//     list.filter((_, i) => i !== index);

//   return (
//     <div className="space-y-3">
//       <Label>{label}</Label>
//       {values.map((val, i) => (
//         <div key={i} className="flex items-center gap-2">
//           <div className="relative flex-1">
//             <Input
//               type={type}
//               placeholder={placeholder}
//               defaultValue={val}
//               onChange={(e) => setValues(updateAt(values, i, e.target.value))}
//               className={icon ? "pl-[62px]" : ""}
//             />
//             {icon && (
//               <span className="absolute left-0 top-1/2 -translate-y-1/2 border-r border-gray-200 px-3.5 py-3 text-gray-500 dark:border-gray-800 dark:text-gray-400">
//                 {icon}
//               </span>
//             )}
//           </div>

//           {i === 0 ? (
//             <button type="button" onClick={() => setValues([...values, ""])} className="p-2 text-primary-600 hover:text-primary-800">
//               <PlusIcon />
//             </button>
//           ) : (
//             <button type="button" onClick={() => setValues(removeAt(values, i))} className="p-2 text-red-500 hover:text-red-600">
//               <TrashBinIcon />
//             </button>
//           )}
//         </div>
//       ))}
//     </div>
//   );
// }

// /* ========================================================= */
// /* ================= REUSABLE MULTI PHONE INPUT =========== */
// /* ========================================================= */
// function MultiPhoneInput({
//   label,
//   values,
//   setValues,
//   countries,
//   placeholder,
// }: {
//   label: string;
//   values: { country: string; number: string }[];
//   setValues: React.Dispatch<React.SetStateAction<{ country: string; number: string }[]>>;
//   countries: { code: string; label: string }[];
//   placeholder: string;
// }) {
//   const updateAt = (list: any[], index: number, value: any) =>
//     list.map((item, i) => (i === index ? value : item));
//   const removeAt = (list: any[], index: number) =>
//     list.filter((_, i) => i !== index);

//   return (
//     <div className="space-y-3">
//       <Label>{label}</Label>
//       {values.map((val, i) => (
//         <div key={i} className="flex items-center gap-2">
//           <div className="flex-1 flex gap-2">
//             <select
//               value={val.country}
//               onChange={(e) => setValues(updateAt(values, i, { ...val, country: e.target.value }))}
//               className="px-3 py-3 rounded-lg border bg-transparent text-gray-800 dark:text-white/90 dark:bg-gray-900 border-gray-300 dark:border-gray-700"
//             >
//               {countries.map((c) => (
//                 <option key={c.code} value={c.label}>
//                   {c.label}
//                 </option>
//               ))}
//             </select>

//             <Input
//               type="tel"
//               placeholder={placeholder}
//               defaultValue={val.number}
//               onChange={(e) => setValues(updateAt(values, i, { ...val, number: e.target.value }))}
//             />
//           </div>

//           {i === 0 ? (
//             <button type="button" onClick={() => setValues([...values, { country: countries[0].label, number: "" }])} className="p-2 text-primary-600 hover:text-primary-800">
//               <PlusIcon />
//             </button>
//           ) : (
//             <button type="button" onClick={() => setValues(removeAt(values, i))} className="p-2 text-red-500 hover:text-red-600">
//               <TrashBinIcon />
//             </button>
//           )}
//         </div>
//       ))}
//     </div>
//   );
// }

// /* ========================================================= */
// /* ================= MULTI URL SECTION ===================== */
// /* ========================================================= */
// function MultiUrlSection({
//   label,
//   placeholder,
//   values,
//   setValues,
// }: {
//   label: string;
//   placeholder: string;
//   values: string[];
//   setValues: React.Dispatch<React.SetStateAction<string[]>>;
// }) {
//   const updateAt = (list: any[], index: number, value: any) =>
//     list.map((item, i) => (i === index ? value : item));
//   const removeAt = (list: any[], index: number) =>
//     list.filter((_, i) => i !== index);

//   return (
//     <div className="space-y-3">
//       <Label>{label}</Label>
//       {values.map((val, i) => (
//         <div key={i} className="flex items-center gap-2">
//           <Input
//             placeholder={placeholder}
//             defaultValue={val}
//             onChange={(e) => setValues(updateAt(values, i, e.target.value))}
//           />

//           {i === 0 ? (
//             <button type="button" onClick={() => setValues([...values, ""])} className="p-2 text-primary-600 hover:text-primary-800">
//               <PlusIcon />
//             </button>
//           ) : (
//             <button type="button" onClick={() => setValues(removeAt(values, i))} className="p-2 text-red-500 hover:text-red-600">
//               <TrashBinIcon />
//             </button>
//           )}
//         </div>
//       ))}
//     </div>
//   );
// }
