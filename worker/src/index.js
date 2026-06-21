const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

const HTML_HEADERS = {
  "content-type": "text/html; charset=utf-8",
  "cache-control": "no-store"
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return json({ ok: true, latest: env.LATEST_VERSION });
    }

    const match = url.pathname.match(/^\/activate\/([A-Za-z0-9]{8})$/);
    if (!match) {
      return html(page("Farm", "Калькулятор не активирован - обратись к TL"), 404);
    }

    const code = match[1];
    const record = await readActivationCode(env, code);
    if (!record || record.active === false || record.version !== env.LATEST_VERSION) {
      return html(page("Farm", "Калькулятор не активирован - обратись к TL"), 403);
    }

    const oldVersion = url.searchParams.get("from");
    const message = oldVersion
      ? `Калькулятор обновлен ${escapeHtml(oldVersion)} -> ${escapeHtml(record.version)}`
      : `Калькулятор обновлен до ${escapeHtml(record.version)}`;
    const appUrl = activationUrl(env.APP_URL, record.version);

    return html(page("Farm", message, appUrl, record.version));
  }
};

async function readActivationCode(env, code) {
  const raw = await env.FARM_CODES.get(code);
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    return { version: raw.trim(), active: true };
  }
}

function activationUrl(appUrl, version) {
  const url = new URL(appUrl);
  url.pathname = `${url.pathname.replace(/\/$/, "")}/a/${encodeURIComponent(version)}/`;
  url.search = "";
  url.hash = "";
  return url.toString();
}

function page(title, message, appUrl, version) {
  const button = appUrl
    ? `<a class="button" href="${escapeAttr(appUrl)}">Открыть калькулятор</a>`
    : "";
  const repeatedActivationScript = appUrl && version
    ? `<script>
      (() => {
        try {
          const version = ${JSON.stringify(version)};
          const appUrl = ${JSON.stringify(appUrl)};
          const key = "farm.workerActivatedVersion";
          if (localStorage.getItem(key) === version) {
            location.replace(appUrl);
            return;
          }
          localStorage.setItem(key, version);
        } catch (_) {}
      })();
    </script>`
    : "";

  return `<!doctype html>
<html lang="ru">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
    <title>${escapeHtml(title)}</title>
    <style>
      :root {
        color-scheme: light dark;
        --accent: #7A8491;
        --bg: #f4f5f7;
        --surface: #ffffff;
        --text: #171a1f;
        --muted: #69717d;
        --line: rgba(23, 26, 31, .12);
      }
      @media (prefers-color-scheme: dark) {
        :root {
          --bg: #111315;
          --surface: #1b1f23;
          --text: #f4f6f8;
          --muted: #a7b0ba;
          --line: rgba(255, 255, 255, .12);
        }
      }
      * { box-sizing: border-box; }
      body {
        margin: 0;
        min-height: 100vh;
        display: grid;
        place-items: center;
        padding: 24px;
        background: var(--bg);
        color: var(--text);
        font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      }
      main {
        width: min(420px, 100%);
        display: grid;
        gap: 16px;
        justify-items: center;
        text-align: center;
        padding: 24px;
        border: 2px solid color-mix(in srgb, var(--accent) 62%, var(--line));
        border-radius: 12px;
        background: var(--surface);
      }
      h1 {
        margin: 0;
        font-size: 30px;
        line-height: 1.1;
      }
      p {
        margin: 0;
        color: var(--muted);
        font-size: 17px;
        line-height: 1.45;
      }
      .button {
        min-height: 46px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 0 18px;
        border-radius: 8px;
        background: var(--accent);
        color: #fff;
        font-weight: 800;
        text-decoration: none;
      }
    </style>
  </head>
  <body>
    <main>
      <h1>Farm</h1>
      <p>${message}</p>
      ${button}
    </main>
    ${repeatedActivationScript}
  </body>
</html>`;
}

function html(body, status = 200) {
  return new Response(body, { status, headers: HTML_HEADERS });
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[char]);
}

function escapeAttr(value) {
  return escapeHtml(value);
}
