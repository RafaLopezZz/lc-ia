# Skill Registry — lc-ia_0.2

<!-- Registro generado por SDD init. SKILL.md permanece como fuente de verdad. -->

Last updated: 2026-08-02

## Sources scanned

- C:\Users\rlpmu\.config\opencode\skills
- C:\Users\rlpmu\.config\agents\skills
- C:\Users\rlpmu\.config\kilo\skills
- C:\Users\rlpmu\.pi\agent\skills
- C:\Users\rlpmu\.agents\skills
- C:\Users\rlpmu\.kimi\skills
- C:\Users\rlpmu\.claude\skills
- C:\Users\rlpmu\.gemini\skills
- C:\Users\rlpmu\.gemini\antigravity\skills
- C:\Users\rlpmu\.cursor\skills
- C:\Users\rlpmu\.copilot\skills
- C:\Users\rlpmu\.codex\skills
- C:\Users\rlpmu\.codeium\windsurf\skills
- C:\Users\rlpmu\.qwen\skills
- C:\Users\rlpmu\.kiro\skills
- C:\Users\rlpmu\.openclaw\skills
- Directorios de skills del proyecto definidos por el contrato de escaneo

## Contract

**Uso exclusivo del delegador.** Este registro es un índice, no un resumen. El agente que delegue selecciona las skills pertinentes y pasa las rutas exactas de sus archivos `SKILL.md`.

`SKILL.md` permanece como fuente de verdad. No deben inyectarse resúmenes ni reglas compactadas por defecto.

## Skills

| Skill | Trigger / description | Scope | Path |
| --- | --- | --- | --- |
| `branch-pr` | Create Gentle AI pull requests with issue-first checks. Trigger: creating, opening, or preparing PRs for review. | user | `C:\Users\rlpmu\.config\opencode\skills\branch-pr\SKILL.md` |
| `chained-pr` | Trigger: PRs over 400 lines, stacked PRs, review slices. Split oversized changes into chained PRs that protect review focus. | user | `C:\Users\rlpmu\.config\opencode\skills\chained-pr\SKILL.md` |
| `cognitive-doc-design` | Design docs that reduce cognitive load. Trigger: writing guides, READMEs, RFCs, onboarding, architecture, or review-facing docs. | user | `C:\Users\rlpmu\.config\opencode\skills\cognitive-doc-design\SKILL.md` |
| `comment-writer` | Write warm, direct collaboration comments. Trigger: PR feedback, issue replies, reviews, Slack messages, or GitHub comments. | user | `C:\Users\rlpmu\.config\opencode\skills\comment-writer\SKILL.md` |
| `go-testing` | Trigger: Go tests, go test coverage, Bubbletea teatest, golden files. Apply focused Go testing patterns. | user | `C:\Users\rlpmu\.config\opencode\skills\go-testing\SKILL.md` |
| `issue-creation` | Create and triage GitHub issues from repository evidence. Trigger: issue creation, bug reports, feature requests, or issue approval. | user | `C:\Users\rlpmu\.config\opencode\skills\issue-creation\SKILL.md` |
| `judgment-day` | Trigger: judgment day, dual review, adversarial review, juzgar. Run explicit blind dual review with at most two scoped fix/re-judgment rounds. | user | `C:\Users\rlpmu\.config\opencode\skills\judgment-day\SKILL.md` |
| `laravel-web-bug-triage` | Diagnostica errores en aplicaciones Laravel con Blade clásico, formularios, sesiones, rutas, redirects y CSRF. | user | `C:\Users\rlpmu\.copilot\skills\laravel-web-bug-triage\SKILL.md` |
| `skill-creator` | Trigger: new skills, agent instructions, documenting AI usage patterns. Create LLM-first skills with valid frontmatter. | user | `C:\Users\rlpmu\.config\opencode\skills\skill-creator\SKILL.md` |
| `skill-improver` | Trigger: improve skills, audit skills, refactor skills, skill quality. Audit and upgrade existing LLM-first skills. | user | `C:\Users\rlpmu\.config\opencode\skills\skill-improver\SKILL.md` |
| `work-unit-commits` | Plan commits as reviewable work units. Trigger: implementation, commit splitting, chained PRs, or keeping tests and docs with code. | user | `C:\Users\rlpmu\.config\opencode\skills\work-unit-commits\SKILL.md` |

## Loading protocol

1. Comparar la tarea y los archivos objetivo con `Trigger / description`.
2. Pasar solo las rutas coincidentes bajo `## Skills to load before work`.
3. Exigir la lectura de esas rutas antes de leer, escribir, revisar, probar o crear artefactos.
4. Si no existe coincidencia, continuar sin inyección y declarar `skill_resolution: none`.

## Scan notes

- Skills indexadas: 11.
- Skills omitidas por contrato: `_shared`, `skill-registry` y todas las `sdd-*`.
- Skills de proyecto encontradas: ninguna.
- Duplicados globales: deduplicados por nombre; se conservó la primera fuente según el orden de escaneo.
