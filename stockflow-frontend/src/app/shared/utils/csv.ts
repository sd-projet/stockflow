export type CsvCell = string | number | boolean | null | undefined;

function escapeCell(value: CsvCell): string {
  if (value === null || value === undefined) {
    return '';
  }

  let text = String(value);

  // Empêche l'interprétation d'une cellule texte comme formule dans Excel.
  if (typeof value === 'string' && /^[=+\-@\t\r]/.test(text)) {
    text = `'${text}`;
  }

  return /[";\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

/**
 * Télécharge un fichier CSV (séparateur « ; » et BOM UTF-8,
 * pour une ouverture correcte dans Excel en français).
 */
export function downloadCsv(
  filename: string,
  headers: string[],
  rows: CsvCell[][]
): void {
  const lines = [headers, ...rows].map(row =>
    row.map(escapeCell).join(';')
  );

  const blob = new Blob(['﻿' + lines.join('\r\n')], {
    type: 'text/csv;charset=utf-8'
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');

  link.href = url;
  link.download = filename;
  link.click();

  URL.revokeObjectURL(url);
}
