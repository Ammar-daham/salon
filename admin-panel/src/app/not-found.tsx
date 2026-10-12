import Link from "next/link";

export default function NotFound() {
	return (
		<div className="flex min-h-screen flex-col items-center justify-center bg-surface px-6 text-center">
			<p className="text-sm font-medium text-ink-subtle">Error 404</p>
			<h1 className="mt-2 text-h1 font-semibold text-ink">Page not found</h1>
			<p className="mt-2 max-w-md text-sm text-ink-muted">
				There&apos;s nothing at this address. It may have been mistyped, or the page may have
				moved.
			</p>
			<Link
				href="/"
				className="mt-6 inline-flex items-center justify-center rounded-lg bg-primary-solid px-5 py-3 text-sm font-medium text-white transition-colors hover:bg-primary-700"
			>
				Back to dashboard
			</Link>
		</div>
	);
}
