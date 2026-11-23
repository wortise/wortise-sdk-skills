# Wortise SDK skills

Agent skills that help AI coding agents integrate the [Wortise](https://wortise.com) ads SDK
correctly: set it up, add ad placements, check the integration before release and diagnose
problems, following Wortise's requirements and best practices.

| Skill | Use |
|---|---|
| `wortise-sdk-integrate` | Add the SDK to an app, or update it: dependency, Google App ID, initialization, consent and test mode |
| `wortise-sdk-ad-formats` | Add or change banners, interstitials, rewarded, app open and native ads |
| `wortise-sdk-validate` | Audit an integration before release, build it and check the SDK logs |
| `wortise-sdk-troubleshoot` | Diagnose ads that do not load or show, crashes and build errors |

Platforms: Android, iOS, Flutter, React Native (bare and Expo) and Unity.

The skills are plain Markdown, with no scripts. They follow the
[Agent Skills](https://agentskills.io) format.

## Install

| Client | How |
| --- | --- |
| **Claude Code** | `/plugin install wortise-sdk-integration --marketplace wortise/wortise-sdk-skills` |
| **Codex** | `codex plugin marketplace add wortise/wortise-sdk-skills`, then `codex plugin add wortise-sdk-integration@wortise-sdk-skills` |
| **Cursor** | Add the plugin marketplace `wortise/wortise-sdk-skills` |
| **Any agent that reads skills** (Android Studio, Xcode, VS Code, Gemini CLI and others) | `npx skills add wortise/wortise-sdk-skills` |

Then add the [AGENTS.md snippet](AGENTS-snippet.md) to your project, so the agent uses the skills
every time it touches ad code. Agents load skills only some of the time, but always read
`AGENTS.md`.

The skills are instructions only: they run no scripts and send nothing anywhere. To check an
integration, the agent builds the app and reads the SDK logs with the tools of the project.

## Documentation

- [Wortise SDK documentation](https://docs.wortise.com)
- [Documentation for AI agents](https://docs.wortise.com/llms.txt)

## License

MIT (`LICENSE`).
