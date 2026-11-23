# AGENTS.md snippet

Coding agents load skills only some of the time, but they always read the project's
`AGENTS.md` (or `CLAUDE.md`). Paste this block into it so the agent uses the Wortise skills, or
the documentation when they are not installed, every time it touches ad code.

```markdown
## Wortise ads

This app shows ads with the Wortise SDK. Before writing, changing or reviewing any Wortise code,
use the Wortise skills (`wortise-sdk-integrate`, `wortise-sdk-ad-formats`, `wortise-sdk-validate`,
`wortise-sdk-troubleshoot`). If they are not installed, read the platform's guide and its
"Best practices" page, listed in https://docs.wortise.com/llms.txt.

- Use only the APIs the Wortise documentation describes; do not guess names from other ad SDKs.
- Initialize the SDK once at startup and resolve user consent before the first ad request.
- Test mode and test ad units (`test-...`) never reach a release build.
- Load ads ahead, show them only when available, never retry loads in a loop, and destroy them
  with their screens.
- Never tap real ads while testing.
```
