# Laboratorio práctico — DeepBlue Rescue

## Consumo del API REST con React y TanStack Query

**Duración:** 1 hora
**Nivel:** Básico
**Tecnologías:** React, Vite, JavaScript, TanStack Query v5, `useState`, `useReducer`, hooks personalizados
**Modalidad:** Individual o parejas

---

# 1. Cómo trabajar este laboratorio

Todo el código está completo: **copia cada bloque en el archivo indicado**. El objetivo no es teclear, sino **entender** lo que pegas. Después de cada paso hay un *Checkpoint*: no avances hasta que se cumpla, y responde las preguntas **"Para pensar"** en tu cuaderno (son parte de la entrega).

Los archivos se indican así:

> 📄 **`src/api/client.js`** → crear el archivo con exactamente este contenido.

---

# 2. Contexto

En los laboratorios anteriores construiste el backend de **DeepBlue Rescue**. Hoy construyes la interfaz que lo consume:

```text
React (navegador)
     ↓  fetch
TanStack Query  (caché, loading, error, mutaciones)
     ↓  HTTP /api/...
Spring Boot  (Controller → Service → Repository → PostgreSQL)
```

Al finalizar tendrás un panel que permite:

1. listar casos de rescate filtrando por estado;
2. ver el animal de un caso, sus tratamientos y si puede recibir tratamiento;
3. cambiar el estado de un caso;
4. registrar un tratamiento mostrando los errores de validación del backend.

---

# 3. Objetivos

1. Configurar `QueryClientProvider` y un proxy de desarrollo hacia el backend.
2. Crear una capa `api/` que traduzca el `ErrorResponse` del backend a un error de JavaScript.
3. Usar `useQuery` con `queryKey` dependiente de parámetros y con `enabled`.
4. Encapsular consultas en **hooks personalizados**.
5. Usar `useMutation` e **invalidar** la caché tras un cambio.
6. Manejar un formulario con `useReducer`.
7. Mostrar estados `isPending`, `isError` y errores `400 / 404 / 409`.

---

# 4. Requisitos previos

- Node 20+ y npm.
- El backend DeepBlue ejecutándose en `http://localhost:8080` **con datos** (al menos un caso con su animal y un especialista).
- Verifica antes de empezar:

```bash
curl "http://localhost:8080/api/rescue-cases?status=ADMITTED"
```

Si responde `[]`, prueba con otro estado (`UNDER_EVALUATION`, `IN_REHABILITATION`, …) o inserta datos. **Anota** un `caseCode`, un `animalCode` y un `specialistCode` reales; los usarás en las pruebas.

## Contrato del API

| Operación | Endpoint | Respuesta |
|---|---|---|
| Casos por estado | `GET /api/rescue-cases?status=ADMITTED` | `200` lista |
| Cambiar estado | `PATCH /api/rescue-cases/{caseCode}/status` body `{ "status": "..." }` | `200` / `400` / `404` / `409` |
| Animal | `GET /api/animals/{animalCode}` | `200` / `404` |
| Tratamientos del animal | `GET /api/animals/{animalCode}/treatments` | `200` lista |
| Elegibilidad | `GET /api/animals/{animalCode}/treatment-eligibility` | `200` `{ animalCode, eligible }` |
| Registrar tratamiento | `POST /api/treatments` | `201` / `400` / `404` / `409` |

Todos los errores tienen la misma forma (`ErrorResponse`):

```json
{
  "timestamp": "2026-10-22T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "details": { "description": "Description is required" }
}
```

---

# 5. Estructura final del proyecto

```text
src/
├── api/
│   ├── client.js
│   ├── rescueCases.js
│   ├── animals.js
│   └── treatments.js
├── hooks/
│   ├── useRescueCases.js
│   ├── useAnimal.js
│   └── useRegisterTreatment.js
├── components/
│   ├── ErrorMessage.jsx
│   ├── CaseList.jsx
│   ├── CaseDetail.jsx
│   ├── ChangeStatus.jsx
│   ├── TreatmentList.jsx
│   └── TreatmentForm.jsx
├── App.jsx
├── index.css
└── main.jsx
```

Crea las carpetas `api`, `hooks` y `components` dentro de `src` cuando las necesites.

---

# 6. Paso 1 — Proyecto, proxy y QueryClient  *(≈ 8 min)*

## 1.1 Crear el proyecto

```bash
npm create vite@latest deepblue-ui -- --template react
cd deepblue-ui
npm install
npm install @tanstack/react-query
```

> Si el asistente pregunta por *rolldown-vite* o *experimental*, responde **No**. Si te ofrece TypeScript, elige **JavaScript**.

## 1.2 Proxy hacia el backend

El navegador bloquea llamadas a otro origen (CORS). En lugar de modificar el backend, Vite reenviará `/api` hacia Spring Boot.

> 📄 **`vite.config.js`** (reemplaza todo el contenido)

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
```

## 1.3 Registrar el QueryClient

> 📄 **`src/main.jsx`** (reemplaza todo el contenido)

```jsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.jsx'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
```

> `retry: false` evita que un `404` se reintente tres veces antes de mostrarse.

## 1.4 Estilos

> 📄 **`src/index.css`** (reemplaza todo el contenido)

```css
* { box-sizing: border-box; }

body {
  margin: 0;
  font-family: system-ui, sans-serif;
  background: #f4f7fa;
  color: #1b2a3a;
}

main.layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 { margin: 0; padding: 16px 24px; background: #0b3d66; color: white; font-size: 1.3rem; }
h2, h3 { margin-top: 0; }

section, aside {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.12);
}

label { display: block; margin-bottom: 10px; font-size: .9rem; font-weight: 600; }
input, select, textarea { display: block; width: 100%; padding: 6px; margin-top: 4px; font: inherit; }

button { padding: 6px 12px; cursor: pointer; }
button:disabled { opacity: .5; cursor: not-allowed; }

.case-item {
  display: block; width: 100%; text-align: left; margin-bottom: 8px;
  background: #eef4fa; border: 1px solid #c7d6e5; border-radius: 6px;
}
.case-item.active { background: #0b3d66; color: white; }

.error { color: #b00020; background: #fdecee; padding: 8px; border-radius: 6px; margin-top: 8px; }
.ok { color: #0a6b2c; font-weight: 700; }
.bad { color: #b00020; font-weight: 700; }

table { width: 100%; border-collapse: collapse; font-size: .9rem; }
th, td { text-align: left; padding: 6px; border-bottom: 1px solid #dde5ec; }
```

## 1.5 App mínima (se completará después)

> 📄 **`src/App.jsx`** (reemplaza todo el contenido)

```jsx
export default function App() {
  return <h1>DeepBlue Rescue</h1>
}
```

## ✅ Checkpoint 1

```bash
npm run dev
```

1. `http://localhost:5173` muestra la franja azul "DeepBlue Rescue".
2. `http://localhost:5173/api/rescue-cases?status=ADMITTED` muestra el **JSON del backend** (no la página de React). Si no, revisa que Spring Boot esté arriba y reinicia `npm run dev`.

**Para pensar:** ¿por qué la URL del `fetch` será `/api/...` y no `http://localhost:8080/api/...`?

---

# 7. Paso 2 — Capa `api/` y manejo de errores  *(≈ 7 min)*

Las funciones de `api/` solo hacen HTTP: **no** usan hooks ni React. Devuelven datos o lanzan un error con la información del `ErrorResponse`.

> 📄 **`src/api/client.js`**

```js
export class ApiError extends Error {
  constructor({ status, message, details }) {
    super(message)
    this.status = status
    this.details = details ?? {}
  }
}

export async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!response.ok) {
    // El backend responde ErrorResponse; si no (backend caído), usamos un mensaje genérico
    const body = await response.json().catch(() => null)
    throw new ApiError({
      status: body?.status ?? response.status,
      message: body?.message ?? 'No se pudo contactar el servidor',
      details: body?.details,
    })
  }

  return response.json()
}
```

> 📄 **`src/api/rescueCases.js`**

```js
import { request } from './client'

export const getCasesByStatus = (status) =>
  request(`/api/rescue-cases?status=${status}`)

export const changeCaseStatus = (caseCode, status) =>
  request(`/api/rescue-cases/${caseCode}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
```

> 📄 **`src/api/animals.js`**

```js
import { request } from './client'

export const getAnimal = (animalCode) =>
  request(`/api/animals/${animalCode}`)

export const getAnimalTreatments = (animalCode) =>
  request(`/api/animals/${animalCode}/treatments`)

export const getTreatmentEligibility = (animalCode) =>
  request(`/api/animals/${animalCode}/treatment-eligibility`)
```

> 📄 **`src/api/treatments.js`**

```js
import { request } from './client'

export const registerTreatment = (payload) =>
  request('/api/treatments', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
```

## ✅ Checkpoint 2

**Para pensar:** ¿por qué `request` **lanza** el error en lugar de devolverlo? *(Pista: TanStack Query decide `isError` según si la promesa se rechaza.)*

---

# 8. Paso 3 — Lista de casos con un hook personalizado  *(≈ 12 min)*

> 📄 **`src/hooks/useRescueCases.js`**

```js
import { useQuery } from '@tanstack/react-query'
import { getCasesByStatus } from '../api/rescueCases'

export function useRescueCases(status) {
  return useQuery({
    queryKey: ['rescue-cases', status],
    queryFn: () => getCasesByStatus(status),
  })
}
```

La `queryKey` incluye `status`: cada estado tiene su propia caché, y cambiar el filtro dispara una consulta nueva automáticamente.

> 📄 **`src/components/ErrorMessage.jsx`**

```jsx
export default function ErrorMessage({ error }) {
  if (!error) return null

  const details = Object.entries(error.details ?? {})

  return (
    <div role="alert" className="error">
      <strong>Error {error.status}:</strong> {error.message}
      {details.length > 0 && (
        <ul>
          {details.map(([field, msg]) => (
            <li key={field}>
              {field}: {msg}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
```

> 📄 **`src/components/CaseList.jsx`**

```jsx
import { useState } from 'react'
import { useRescueCases } from '../hooks/useRescueCases'
import ErrorMessage from './ErrorMessage'

export const STATUSES = [
  'ADMITTED',
  'UNDER_EVALUATION',
  'IN_REHABILITATION',
  'READY_FOR_RELEASE',
  'RELEASED',
  'CLOSED',
]

export default function CaseList({ selected, onSelect }) {
  const [status, setStatus] = useState('ADMITTED')
  const { data, isPending, isError, error } = useRescueCases(status)

  return (
    <aside>
      <h2>Casos de rescate</h2>

      <label>
        Estado
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      {isPending && <p>Cargando...</p>}
      {isError && <ErrorMessage error={error} />}
      {data && data.length === 0 && <p>No hay casos con este estado</p>}

      {data?.map((c) => (
        <button
          key={c.caseCode}
          className={`case-item ${selected?.caseCode === c.caseCode ? 'active' : ''}`}
          onClick={() => onSelect(c)}
        >
          <strong>{c.caseCode}</strong> — {c.rescueLocation}
          <br />
          <small>{c.rescueDate}</small>
        </button>
      ))}
    </aside>
  )
}
```

> 📄 **`src/App.jsx`** (reemplaza todo el contenido)

```jsx
import { useState } from 'react'
import CaseList from './components/CaseList'
import CaseDetail from './components/CaseDetail'

export default function App() {
  const [selected, setSelected] = useState(null)

  return (
    <>
      <h1>DeepBlue Rescue</h1>
      <main className="layout">
        <CaseList selected={selected} onSelect={setSelected} />
        {selected ? (
          <CaseDetail caseItem={selected} onChanged={setSelected} />
        ) : (
          <p>Selecciona un caso</p>
        )}
      </main>
    </>
  )
}
```

> 📄 **`src/components/CaseDetail.jsx`** (versión temporal para que compile)

```jsx
export default function CaseDetail({ caseItem }) {
  return <section>Detalle de {caseItem.caseCode}</section>
}
```

## ✅ Checkpoint 3

- Al cargar ves los casos `ADMITTED` (o "No hay casos...").
- Cambia el filtro a otro estado y vuelve al primero: **no** debe aparecer "Cargando..." de nuevo (caché). Compruébalo en la pestaña *Network* de las DevTools.
- Detén el backend y recarga: debes ver un mensaje de error en rojo, no una pantalla en blanco. Vuelve a iniciarlo.

**Para pensar:** ¿qué pasaría si `queryKey` fuera solo `['rescue-cases']` sin `status`?

---

# 9. Paso 4 — Detalle del animal con `enabled`  *(≈ 10 min)*

> 📄 **`src/hooks/useAnimal.js`**

```js
import { useQuery } from '@tanstack/react-query'
import {
  getAnimal,
  getAnimalTreatments,
  getTreatmentEligibility,
} from '../api/animals'

export function useAnimal(animalCode) {
  const enabled = Boolean(animalCode)

  const animal = useQuery({
    queryKey: ['animal', animalCode],
    queryFn: () => getAnimal(animalCode),
    enabled,
  })

  const treatments = useQuery({
    queryKey: ['animal', animalCode, 'treatments'],
    queryFn: () => getAnimalTreatments(animalCode),
    enabled,
  })

  const eligibility = useQuery({
    queryKey: ['animal', animalCode, 'eligibility'],
    queryFn: () => getTreatmentEligibility(animalCode),
    enabled,
  })

  return { animal, treatments, eligibility }
}
```

> Las claves comparten el prefijo `['animal', animalCode]`. Esto permitirá invalidar todo lo del animal de una vez en el Paso 6.

> 📄 **`src/components/TreatmentList.jsx`**

```jsx
import ErrorMessage from './ErrorMessage'

export default function TreatmentList({ query }) {
  const { data, isPending, isError, error } = query

  return (
    <section>
      <h3>Tratamientos</h3>

      {isPending && <p>Cargando...</p>}
      {isError && <ErrorMessage error={error} />}
      {data && data.length === 0 && <p>Sin tratamientos registrados</p>}

      {data && data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Especialista</th>
              <th>Descripción</th>
            </tr>
          </thead>
          <tbody>
            {data.map((t) => (
              <tr key={t.id}>
                <td>{t.performedAt}</td>
                <td>{t.type}</td>
                <td>{t.specialistCode}</td>
                <td>{t.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
```

> 📄 **`src/components/CaseDetail.jsx`** (reemplaza la versión temporal)

```jsx
import { useAnimal } from '../hooks/useAnimal'
import ErrorMessage from './ErrorMessage'
import TreatmentList from './TreatmentList'

export default function CaseDetail({ caseItem, onChanged }) {
  const { animal, treatments, eligibility } = useAnimal(caseItem.animalCode)

  return (
    <div>
      <section>
        <h2>Caso {caseItem.caseCode}</h2>
        <p>
          <strong>Estado:</strong> {caseItem.status} <br />
          <strong>Centro:</strong> {caseItem.centerCode} <br />
          <strong>Lugar:</strong> {caseItem.rescueLocation} <br />
          <strong>Fecha:</strong> {caseItem.rescueDate}
        </p>
      </section>

      <section>
        <h3>Animal {caseItem.animalCode}</h3>
        {animal.isPending && <p>Cargando...</p>}
        {animal.isError && <ErrorMessage error={animal.error} />}

        {/* Muestra cualquier campo que devuelva tu API de animales */}
        {animal.data && (
          <ul>
            {Object.entries(animal.data).map(([key, value]) => (
              <li key={key}>
                <strong>{key}:</strong> {String(value)}
              </li>
            ))}
          </ul>
        )}

        {eligibility.data && (
          <p>
            Puede recibir tratamiento:{' '}
            <span className={eligibility.data.eligible ? 'ok' : 'bad'}>
              {eligibility.data.eligible ? 'Sí' : 'No'}
            </span>
          </p>
        )}
      </section>

      <TreatmentList query={treatments} />
    </div>
  )
}
```

## ✅ Checkpoint 4

- Al elegir un caso aparecen animal, elegibilidad y tratamientos.
- Un animal sin tratamientos muestra "Sin tratamientos registrados".
- Cambia entre dos casos y vuelve al primero: en *Network* no se repite la consulta (caché).

**Para pensar:** ¿para qué sirve `enabled`? ¿Qué pasaría sin él si `animalCode` fuera `undefined`?

---

# 10. Paso 5 — Cambiar el estado con `useMutation`  *(≈ 10 min)*

Una **query** lee datos; una **mutation** los modifica. Tras modificar, hay que avisar a la caché que sus datos quedaron viejos.

> 📄 **`src/components/ChangeStatus.jsx`**

```jsx
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeCaseStatus } from '../api/rescueCases'
import { STATUSES } from './CaseList'
import ErrorMessage from './ErrorMessage'

export default function ChangeStatus({ caseItem, onChanged }) {
  const queryClient = useQueryClient()
  const [next, setNext] = useState(caseItem.status)

  const mutation = useMutation({
    mutationFn: (status) => changeCaseStatus(caseItem.caseCode, status),
    onSuccess: (updatedCase) => {
      // Todas las listas por estado quedaron desactualizadas
      queryClient.invalidateQueries({ queryKey: ['rescue-cases'] })
      onChanged(updatedCase)
    },
  })

  return (
    <section>
      <h3>Cambiar estado</h3>

      <label>
        Nuevo estado
        <select value={next} onChange={(e) => setNext(e.target.value)}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      <button disabled={mutation.isPending} onClick={() => mutation.mutate(next)}>
        {mutation.isPending ? 'Aplicando...' : 'Aplicar'}
      </button>

      <ErrorMessage error={mutation.error} />
    </section>
  )
}
```

Conéctalo en `CaseDetail`. Haz estos **dos cambios** en **`src/components/CaseDetail.jsx`**:

1. Añade el import junto a los demás:

```jsx
import ChangeStatus from './ChangeStatus'
```

2. Justo después del cierre `</section>` de la primera sección (la del caso, antes de la sección del animal) añade:

```jsx
      <ChangeStatus
        key={caseItem.caseCode}
        caseItem={caseItem}
        onChanged={onChanged}
      />
```

> `key={caseItem.caseCode}` reinicia el estado del `<select>` al cambiar de caso.

## Pruebas — anota lo que ves

| Prueba | Resultado esperado |
|---|---|
| `ADMITTED → UNDER_EVALUATION` | `200`: el caso desaparece de la lista `ADMITTED` y aparece en `UNDER_EVALUATION` |
| `ADMITTED → RELEASED` (salto inválido) | `409` con el mensaje de la regla de negocio, en rojo |

## ✅ Checkpoint 5

- El cambio exitoso actualiza la lista **sin recargar la página**.
- El `409` se muestra y la aplicación sigue funcionando.

**Para pensar:** ¿qué hace `invalidateQueries({ queryKey: ['rescue-cases'] })` y por qué afecta a *todas* las listas por estado?

---

# 11. Paso 6 — Registrar tratamiento con `useReducer`  *(≈ 10 min)*

> 📄 **`src/hooks/useRegisterTreatment.js`**

```js
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { registerTreatment } from '../api/treatments'

export function useRegisterTreatment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerTreatment,
    onSuccess: (created) => {
      // Refresca tratamientos y elegibilidad del animal
      queryClient.invalidateQueries({
        queryKey: ['animal', created.animalCode],
      })
    },
  })
}
```

> 📄 **`src/components/TreatmentForm.jsx`**

```jsx
import { useReducer } from 'react'
import { useRegisterTreatment } from '../hooks/useRegisterTreatment'
import ErrorMessage from './ErrorMessage'

const TREATMENT_TYPES = [
  'WOUND_CARE',
  'HYDRATION',
  'MEDICATION',
  'SURGERY',
  'NUTRITION',
  'PHYSIOTHERAPY',
  'OBSERVATION',
]

const initialState = {
  specialistCode: '',
  performedAt: '',
  type: 'WOUND_CARE',
  description: '',
}

function formReducer(state, action) {
  switch (action.type) {
    case 'field':
      return { ...state, [action.name]: action.value }
    case 'reset':
      return initialState
    default:
      return state
  }
}

export default function TreatmentForm({ animalCode }) {
  const [form, dispatch] = useReducer(formReducer, initialState)
  const mutation = useRegisterTreatment()

  const handleChange = (e) =>
    dispatch({ type: 'field', name: e.target.name, value: e.target.value })

  const handleSubmit = (e) => {
    e.preventDefault()
    mutation.mutate(
      { animalCode, ...form },
      { onSuccess: () => dispatch({ type: 'reset' }) },
    )
  }

  return (
    <section>
      <h3>Registrar tratamiento</h3>

      <form onSubmit={handleSubmit}>
        <label>
          Código del especialista
          <input
            name="specialistCode"
            value={form.specialistCode}
            onChange={handleChange}
          />
        </label>

        <label>
          Fecha y hora
          <input
            type="datetime-local"
            name="performedAt"
            value={form.performedAt}
            onChange={handleChange}
          />
        </label>

        <label>
          Tipo
          <select name="type" value={form.type} onChange={handleChange}>
            {TREATMENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>

        <label>
          Descripción
          <textarea
            name="description"
            rows={3}
            value={form.description}
            onChange={handleChange}
          />
        </label>

        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Guardando...' : 'Registrar'}
        </button>
      </form>

      <ErrorMessage error={mutation.error} />
      {mutation.isSuccess && <p className="ok">Tratamiento registrado ✔</p>}
    </section>
  )
}
```

Conéctalo en **`src/components/CaseDetail.jsx`**:

1. Añade el import:

```jsx
import TreatmentForm from './TreatmentForm'
```

2. Añade esta línea justo después de `<TreatmentList query={treatments} />`:

```jsx
      <TreatmentForm key={caseItem.animalCode} animalCode={caseItem.animalCode} />
```

## Pruebas — anota lo que ves

Usa el `specialistCode` real que anotaste al inicio.

| Prueba | Resultado esperado |
|---|---|
| Descripción de menos de 10 caracteres | `400`, `details.description` visible |
| `specialistCode` vacío | `400`, `details.specialistCode` visible |
| Fecha en el futuro | `400`, "Treatment date cannot be in the future" |
| Fecha vacía | `400` ("Required..." o JSON inválido, según el handler) — observa el mensaje |
| `specialistCode` = `SP-999` (inexistente) | `404` |
| Datos válidos (descripción ≥ 10 caracteres) | `201`: el tratamiento aparece en la tabla sin recargar y el formulario se limpia |

## ✅ Checkpoint 6

- Un tratamiento válido aparece automáticamente en la tabla.
- Los errores `400` aparecen **por campo**.
- La elegibilidad se vuelve a consultar sola (mira *Network*).

**Para pensar:** ¿por qué usamos `useReducer` y no un `useState` por campo? ¿Por qué `invalidateQueries` usa la clave `['animal', animalCode]` sin más sufijos?

---

# 12. Código final de `CaseDetail.jsx` (referencia)

Si tu archivo no funciona después de los pasos 5 y 6, compáralo con este:

> 📄 **`src/components/CaseDetail.jsx`**

```jsx
import { useAnimal } from '../hooks/useAnimal'
import ChangeStatus from './ChangeStatus'
import ErrorMessage from './ErrorMessage'
import TreatmentForm from './TreatmentForm'
import TreatmentList from './TreatmentList'

export default function CaseDetail({ caseItem, onChanged }) {
  const { animal, treatments, eligibility } = useAnimal(caseItem.animalCode)

  return (
    <div>
      <section>
        <h2>Caso {caseItem.caseCode}</h2>
        <p>
          <strong>Estado:</strong> {caseItem.status} <br />
          <strong>Centro:</strong> {caseItem.centerCode} <br />
          <strong>Lugar:</strong> {caseItem.rescueLocation} <br />
          <strong>Fecha:</strong> {caseItem.rescueDate}
        </p>
      </section>

      <ChangeStatus
        key={caseItem.caseCode}
        caseItem={caseItem}
        onChanged={onChanged}
      />

      <section>
        <h3>Animal {caseItem.animalCode}</h3>
        {animal.isPending && <p>Cargando...</p>}
        {animal.isError && <ErrorMessage error={animal.error} />}

        {animal.data && (
          <ul>
            {Object.entries(animal.data).map(([key, value]) => (
              <li key={key}>
                <strong>{key}:</strong> {String(value)}
              </li>
            ))}
          </ul>
        )}

        {eligibility.data && (
          <p>
            Puede recibir tratamiento:{' '}
            <span className={eligibility.data.eligible ? 'ok' : 'bad'}>
              {eligibility.data.eligible ? 'Sí' : 'No'}
            </span>
          </p>
        )}
      </section>

      <TreatmentList query={treatments} />

      <TreatmentForm key={caseItem.animalCode} animalCode={caseItem.animalCode} />
    </div>
  )
}
```

---

# 13. Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El navegador muestra HTML en vez de JSON en `/api/...` | Spring Boot apagado o `vite.config.js` mal; reinicia `npm run dev` |
| `Cannot find module './components/CaseDetail'` | Falta crear el archivo o la carpeta está mal nombrada (mayúsculas) |
| Siempre "Cargando..." | El backend responde pero `fetch` falla; revisa la pestaña *Network* |
| Lista vacía siempre | No hay datos en ese estado; prueba otros estados o inserta datos |
| `400` al enviar fecha válida | Formato; `datetime-local` envía `2026-10-22T10:30`, debe estar en pasado o presente |
| Pantalla en blanco | Abre la consola del navegador (F12) y lee el primer error |

---

# 14. Entrega y checklist final

```text
Configuración
[ ] Proxy /api funcionando
[ ] QueryClientProvider en main.jsx

Funcionalidad
[ ] Lista filtrada por estado con loading / error / vacío
[ ] Detalle con animal, elegibilidad y tratamientos
[ ] Cambio de estado con invalidación de caché (409 mostrado)
[ ] Formulario con useReducer (400 por campo, 404 mostrado, 201 refresca la tabla)

Comprensión (respuestas escritas a los "Para pensar")
[ ] Checkpoints 1, 2, 3, 4, 5 y 6
```

**Entregable:** carpeta `.zip` del proyecto (**sin** `node_modules`) y un archivo de texto con tus respuestas a los *Para pensar* y las observaciones de las tablas de pruebas de los Pasos 5 y 6.

---

# 15. Retos opcionales (si terminas antes)

1. **Insignias:** muestra el `status` del caso con un color distinto por estado.
2. **Transiciones:** en `ChangeStatus`, ofrece solo el siguiente estado válido (revisa las reglas de tu `RescueCaseService`).
3. **Validación previa:** deshabilita el botón *Registrar* si la descripción tiene menos de 10 caracteres, manteniendo el error del backend como segunda red de seguridad.
4. **Devtools:** `npm install @tanstack/react-query-devtools` y agrega `<ReactQueryDevtools />` dentro del `QueryClientProvider` para ver las claves en caché.
5. **Ruta por caso:** con `react-router-dom`, navega a `/cases/:caseCode` usando `GET /api/rescue-cases/{caseCode}`, el único endpoint de casos que aún no usas.

---

# 16. Mapa mental

```text
Componente
    ↓ usa
Hook personalizado  (useRescueCases, useAnimal, useRegisterTreatment)
    ↓ envuelve
useQuery / useMutation  ── queryKey, enabled, invalidateQueries
    ↓ llama
api/*.js  ── request() lanza ApiError
    ↓
fetch → proxy Vite → Spring Boot
```

> **Idea principal:** los componentes solo muestran estados (`isPending`, `isError`, `data`). Cómo se obtienen los datos vive en los hooks, y cómo viajan por HTTP vive en `api/`.
