# LC-IA — Mapeo CSV → Jira Cloud

Usa la importación CSV de Jira y, en la pantalla de mapeo, configura explícitamente:

| Columna CSV | Campo Jira |
|---|---|
| Summary | Summary / Resumen (campo de sistema obligatorio) |
| Work Type | Work type / Tipo de incidencia |
| Issue ID | Work item ID / Issue ID |
| Parent | Parent |
| Description | Description |
| Priority | Priority |
| Labels | Labels |

## Importante

- No dejes `Summary` como *Don't map this field* / sin asignar.
- Si aparecen dos campos llamados `Summary`, selecciona el campo de sistema de Jira, no un custom field homónimo.
- Mapea `Issue ID` y `Parent` si quieres conservar la relación Epic → Story/Task.
- Los Epics ya aparecen antes que sus hijos en el CSV.
- Valida la importación antes de ejecutarla.
