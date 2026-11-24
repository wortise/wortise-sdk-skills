# Evals

Repeatable checks of the skills with
[`claude plugin eval`](https://code.claude.com/docs/en/plugin-evals). Each case runs Claude Code
with the plugin and without it, and scores the answers with the graders of the case.

| Case | What it checks |
|---|---|
| `audit-android`, `audit-ios`, `audit-flutter`, `audit-react-native`, `audit-unity` | Reviewing an app excerpt (in `app/`) with known integration mistakes: finds the release blockers and the lifecycle problems, and gives no wrong advice |
| `subtle-android`, `subtle-react-native`, `subtle-unity` | Reviewing an integration that is mostly right, with a few subtle mistakes (Compose banners, async checks without `await`, listeners never removed, scene changes): finds them and does not flag the correct code |
| `ask-for-ids` | Asks for the app key, the Google App ID and the audience instead of inventing them |
| `interstitial-at-launch` | Warns against an interstitial at launch and suggests an alternative |
| `rewarded-reward` | Grants the reward only on completion, with the real Android API |
| `flutter-test-mode` | Keeps test mode out of release builds |

## Run

From the root of the repo:

```bash
claude plugin eval . --scaffold --trust-plugin --no-publish -j 4
```

`--scaffold` is needed: each audit case copies its `app/` folder into the run's workspace with
`scaffold.sh`. Add `--case <name>` to run one case (the flag takes one glob, such as `'audit-*'`), and `--runs 1 --ablation none` for a quick
check of the plugin alone. A full run makes about 48 agent runs and costs about 8 USD.

Results go to `evals/results/` (not committed).

The app excerpts are fictional: their keys and IDs are made up, and their mistakes are on
purpose.
