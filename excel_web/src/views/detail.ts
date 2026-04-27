import { html } from "hono/html";
import type { ExcelFull } from "../types";
import { layout } from "./layout";

const CATEGORIES = [
  "BasicFormula",
  "Function",
  "Lookup",
  "PivotTable",
  "Chart",
  "Macro",
  "Format",
  "Database",
] as const;

export function detailView(
  full: ExcelFull,
  flash?: { kind: "ok" | "error"; msg: string },
) {
  const r = full.excel;
  const tags = r.tags_csv ? r.tags_csv.split(",").map((t) => t.trim()).filter(Boolean) : [];

  const body = html`
    <p><a href="/admin">← Daftar tutorial</a></p>
    <h1>${r.title}</h1>

    <div class="meta-row">
      <span><span class="badge badge-${r.status}">${r.status}</span></span>
      <span>ID: <code>${r.id}</code></span>
      ${r.generated_at ? html`<span>Teks: ${formatTime(r.generated_at)}</span>` : ""}
      ${r.image_generated_at ? html`<span>Gambar: ${formatTime(r.image_generated_at)}</span>` : ""}
      ${r.published_at ? html`<span>Published: ${formatTime(r.published_at)}</span>` : ""}
    </div>

    ${flash
      ? html`<div class="alert alert-${flash.kind === "ok" ? "ok" : "error"}">${flash.msg}</div>`
      : ""}

    <div class="panel">
      <h2>Aksi</h2>
      <div class="actions">
        <form class="inline" method="post" action="/admin/excels/${r.id}/generate-text">
          <button class="btn" type="submit">${r.generated_at ? "Regenerate teks" : "Generate teks"}</button>
        </form>
        <form class="inline" method="post" action="/admin/excels/${r.id}/generate-image">
          <button class="btn" type="submit">${r.image_key ? "Regenerate gambar" : "Generate gambar"}</button>
        </form>
        <form class="inline" method="post" action="/admin/excels/${r.id}/generate-all">
          <button class="btn btn-primary" type="submit">Generate keduanya</button>
        </form>
        ${r.status !== "published" && r.generated_at && r.image_key
          ? html`<form class="inline" method="post" action="/admin/excels/${r.id}/publish">
              <button class="btn btn-primary" type="submit">Publish</button>
            </form>`
          : ""}
        ${r.status === "published"
          ? html`<form class="inline" method="post" action="/admin/excels/${r.id}/unpublish">
              <button class="btn btn-danger" type="submit">Unpublish</button>
            </form>`
          : ""}
      </div>
      <p class="small" style="margin-top: 12px">
        Generate butuh ~10–30 detik (teks) dan ~3–8 detik (gambar). Token dihemat: hanya admin yang bisa trigger.
      </p>
    </div>

    <div class="panel">
      <h2>Gambar</h2>
      ${full.imageUrl
        ? html`<img class="image-preview" src="${full.imageUrl}" alt="${r.title}" />`
        : html`<div class="empty-image">Belum ada gambar</div>`}
    </div>

    <div class="panel">
      <h2>Metadata (boleh edit manual)</h2>
      <form method="post" action="/admin/excels/${r.id}/update">
        <label>Judul</label>
        <input type="text" name="title" value="${r.title}" required />
        <div class="grid" style="margin-top: 12px">
          <div>
            <label>Kategori</label>
            <select name="category">
              ${CATEGORIES.map(
                (c) => html`<option value="${c}" ${c === r.category ? "selected" : ""}>${c}</option>`,
              )}
            </select>
          </div>
          <div>
            <label>Difficulty</label>
            <select name="difficulty">
              ${["Easy", "Medium", "Hard"].map(
                (d) => html`<option value="${d}" ${d === r.difficulty ? "selected" : ""}>${d}</option>`,
              )}
            </select>
          </div>
        </div>
        <div class="grid" style="margin-top: 12px">
          <div>
            <label>Estimasi belajar (menit)</label>
            <input type="number" name="cooking_time_minutes" value="${r.cooking_time_minutes}" min="1" max="720" />
          </div>
          <div>
            <label>Versi Excel minimal</label>
            <input type="number" name="servings" value="${r.servings}" min="2010" max="2100" />
          </div>
        </div>
        <label style="margin-top: 12px">Deskripsi</label>
        <textarea name="description">${r.description}</textarea>
        <label style="margin-top: 12px">Tags (pisah koma)</label>
        <input type="text" name="tags_csv" value="${tags.join(", ")}" />
        <div style="margin-top: 14px">
          <button class="btn btn-primary" type="submit">Simpan perubahan</button>
        </div>
      </form>
    </div>

    <div class="panel">
      <h2>Rumus/Fungsi (${full.ingredients.length})</h2>
      ${full.ingredients.length === 0
        ? html`<p class="small">Belum di-generate.</p>`
        : html`<ul class="ingredients">
            ${full.ingredients.map(
              (ing) =>
                html`<li><span>${ing.name}</span><span class="small">${ing.quantity} ${ing.unit}</span></li>`,
            )}
          </ul>`}
    </div>

    <div class="panel">
      <h2>Langkah (${full.steps.length})</h2>
      ${full.steps.length === 0
        ? html`<p class="small">Belum di-generate.</p>`
        : html`<ul class="steps">
            ${full.steps.map((s) => html`<li>${s.instruction}</li>`)}
          </ul>`}
      <p class="small" style="margin-top: 8px">
        Untuk edit bahan / langkah secara manual, lakukan via SQL D1 (UPDATE/INSERT
        <code>ingredients</code> / <code>excel_steps</code>). Form edit lengkap
        bisa ditambahkan kalau dirasa perlu — saat ini fokus alur generate-then-publish.
      </p>
    </div>
  `;
  return layout(r.title, body);
}

function formatTime(epochMs: number): string {
  return new Date(epochMs).toISOString().replace("T", " ").slice(0, 16);
}
