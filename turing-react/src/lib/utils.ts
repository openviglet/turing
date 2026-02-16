import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const truncateMiddle = (
  text: string,
  maxLength: number = 10,
): string => {
  if (text.length <= maxLength) return text;

  const dots = "...";
  const charsToShow = maxLength - dots.length;
  const frontChars = Math.ceil(charsToShow / 2);
  const backChars = Math.floor(charsToShow / 2);

  return (
    text.substring(0, frontChars) +
    dots +
    text.substring(text.length - backChars)
  );
};

export const getFlagEmoji = (locale: string) => {
  // Pega a parte após o underline (ex: en_US -> US)
  const countryCode = locale.split("_")[1]?.toUpperCase();

  if (!countryCode?.length || countryCode.length !== 2) return "🌐"; // Fallback se não encontrar

  // Converte cada letra para o Regional Indicator Symbol
  return countryCode
    .split("")
    .map((char) => String.fromCodePoint((char.codePointAt(0) ?? 0) + 127397))
    .join("");
};
