import { LibraryBig } from "lucide-react";

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-state">
      <LibraryBig aria-hidden="true" size={32} />
      <h2>{title}</h2>
      <p>{description}</p>
    </div>
  );
}
