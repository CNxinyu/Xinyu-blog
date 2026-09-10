export default function SearchLoading() {
  return (
    <div className="state-page state-page--loading" aria-live="polite" aria-busy="true">
      <div className="loading-rune" aria-hidden="true" />
      <p>正在校准星盘…</p>
    </div>
  );
}
