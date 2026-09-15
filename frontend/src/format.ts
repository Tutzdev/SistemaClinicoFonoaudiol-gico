export const timezone = "America/Sao_Paulo";
export const statuses = {
  AGENDADO: "Agendado",
  CONFIRMADO: "Confirmado",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
  NAO_COMPARECEU: "Não compareceu",
} as const;
export function dateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: timezone,
  }).format(new Date(value));
}
export function time(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(new Date(value));
}
export function date(value: string) {
  return value
    ? new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeZone: "UTC",
      }).format(new Date(`${value.slice(0, 10)}T12:00:00Z`))
    : "—";
}
export function localDate(value = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: timezone,
  }).format(value);
}
export function localInput(value: string) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: timezone,
  }).formatToParts(new Date(value));
  const part = (name: string) =>
    parts.find((p) => p.type === name)?.value || "";
  return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
}
// Resolve a clinic wall-clock time using IANA data, independently of the browser's timezone.
export function withOffset(value: string) {
  if (!value) return "";
  const [y, m, d, h, min] = value.match(/\d+/g)!.map(Number);
  const wall = Date.UTC(y, m - 1, d, h, min);
  let instant = wall;
  for (let i = 0; i < 3; i++) {
    const parts = new Intl.DateTimeFormat("en-CA", {
      timeZone: timezone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }).formatToParts(new Date(instant));
    const n = (name: string) =>
      Number(parts.find((p) => p.type === name)?.value);
    const displayed = Date.UTC(
      n("year"),
      n("month") - 1,
      n("day"),
      n("hour"),
      n("minute"),
    );
    instant += wall - displayed;
  }
  const offset = Math.round((wall - instant) / 60000);
  return `${value.slice(0, 16)}:00${offset >= 0 ? "+" : "-"}${String(Math.floor(Math.abs(offset) / 60)).padStart(2, "0")}:${String(Math.abs(offset) % 60).padStart(2, "0")}`;
}
export function nextDay(day: string, amount = 1) {
  return new Date(Date.parse(`${day}T12:00:00Z`) + amount * 86400000)
    .toISOString()
    .slice(0, 10);
}
export function isMinor(birthDate: string) {
  if (!birthDate) return false;
  const today = localDate();
  const year = Number(today.slice(0, 4)) - 18;
  return birthDate > `${year}${today.slice(4)}`;
}
export function whatsapp(number: string) {
  return `https://wa.me/${phoneUri(number).replace("+", "")}?text=${encodeURIComponent("Olá! Gostaria de informações sobre atendimento no Espaço Sinapse.")}`;
}
export function phoneUri(number: string) {
  const digits = number.replace(/\D/g, "");
  return `+${!number.trim().startsWith("+") && (digits.length === 10 || digits.length === 11) ? `55${digits}` : digits}`;
}
