"use client";

import { useParams } from "next/navigation";
import ServicesManager from "@/features/services/ServicesManager";

/** The Services tab of a salon's detail page — the same manager as /services,
 *  pinned to this salon. */
export default function BusinessServicesView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);

	return <ServicesManager businessId={Number.isNaN(id) ? null : id} />;
}
