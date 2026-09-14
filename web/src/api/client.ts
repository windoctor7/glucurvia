import type { paths } from "./schema";

/** Tipos derivados del contrato (npm run gen). Ejemplos de uso para los agentes de web. */
export type CgmStatus = paths["/cgm/status"]["get"]["responses"]["200"]["content"]["application/json"];
export type ChatRequest = paths["/chat/messages"]["post"]["requestBody"]["content"]["application/json"];
export type ChatResponse = paths["/chat/messages"]["post"]["responses"]["200"]["content"]["application/json"];

const BASE = "/api/v1";

export async function getCgmStatus(): Promise<CgmStatus> {
  const r = await fetch(`${BASE}/cgm/status`);
  if (!r.ok) throw new Error(`cgm/status ${r.status}`);
  return (await r.json()) as CgmStatus;
}

export async function postChatMessage(body: ChatRequest): Promise<ChatResponse> {
  const r = await fetch(`${BASE}/chat/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(`chat/messages ${r.status}`);
  return (await r.json()) as ChatResponse;
}
