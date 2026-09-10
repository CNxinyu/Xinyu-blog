export type QueryValue = string | string[] | undefined;

export function firstValue(value: QueryValue) {
  return Array.isArray(value) ? value[0] : value;
}

export function normalizePage(value: QueryValue) {
  const parsed = Number.parseInt(firstValue(value) ?? "1", 10);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : 1;
}

export function pageHref(pathname: string, page: number, query?: Record<string, string>) {
  const params = new URLSearchParams(query);
  if (page > 1) params.set("page", String(page));
  else params.delete("page");
  const suffix = params.toString();
  return suffix ? `${pathname}?${suffix}` : pathname;
}

export function formatDate(value: string | null) {
  if (!value) return "尚未记录";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(date);
}
