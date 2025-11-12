import os
os.environ['MPLBACKEND'] = 'Agg'   # antes de cualquier import de pyplot

from django.shortcuts import render
from django.conf import settings
from requests.auth import HTTPBasicAuth
import requests
import pandas as pd
import io, base64, traceback

import matplotlib
matplotlib.use('Agg')
from matplotlib.figure import Figure
from matplotlib.backends.backend_agg import FigureCanvasAgg as FigureCanvas
import seaborn as sns

from sklearn.linear_model import LinearRegression
from datetime import datetime, timedelta

# ---------- UMBRALES (con override por ENV) ----------
SLOPE_THRESHOLD = float(os.getenv("PRED_MIN_SLOPE", "0.05"))   # pendiente mínima (reservas/semana)
MIN_POINTS      = int(os.getenv("PRED_MIN_POINTS", "2"))       # semanas mínimas para evaluar

# ---------- Helpers ----------
def get_reservas():
    url = f"{settings.JAVA_API_BASE}/reservas"
    try:
        r = requests.get(url, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD), timeout=10)
        if r.ok:
            return r.json()
    except Exception as e:
        print("Error fetching reservas:", e)
    return []

def fig_to_base64(fig: Figure) -> str:
    buf = io.BytesIO()
    FigureCanvas(fig).print_png(buf)
    buf.seek(0)
    data = base64.b64encode(buf.read()).decode('utf-8')
    buf.close()
    fig.clf()
    return data

def linear_trend_forecast(series: pd.Series):
    try:
        y = series.values
        if len(y) < 2:
            return None
        x = pd.RangeIndex(len(y)).to_numpy().reshape(-1, 1)
        m = LinearRegression().fit(x, y)
        slope = float(m.coef_[0])
        last = float(y[-1])
        pred_next = float(m.predict([[len(y)]])[0])
        return {"slope": slope, "last": last, "pred_next": pred_next}
    except Exception:
        print("linear_trend_forecast() error:\n", traceback.format_exc())
        return None

def _ensure_time_index_sorted(pivot: pd.DataFrame) -> pd.DataFrame:
    """Convierte el índice (semanas) a datetime y lo ordena, cualquiera sea su tipo."""
    try:
        idx = pivot.index
        if isinstance(idx, pd.PeriodIndex):
            idx = idx.to_timestamp()              # inicio de la semana
        else:
            idx = pd.to_datetime(idx, errors="coerce")
        out = pivot.copy()
        out.index = idx
        out = out.sort_index()
        return out
    except Exception:
        print("_ensure_time_index_sorted() error:\n", traceback.format_exc())
        return pivot

def pick_top_trend(pivot_df: pd.DataFrame, top_n: int = 1):
    """Devuelve hasta top_n items con pendiente >= SLOPE_THRESHOLD (solo positivas)."""
    results = []
    for col in pivot_df.columns:
        ser = pivot_df[col].fillna(0)
        print(f"[TREND] item='{col}' semanas={ser.shape[0]} MIN_POINTS={MIN_POINTS}")
        if ser.shape[0] < MIN_POINTS:
            continue
        info = linear_trend_forecast(ser)
        if info:
            print(f"[TREND] item='{col}' slope={info['slope']:.4f} last={info['last']:.2f} "
                  f"pred_next={info['pred_next']:.2f} (threshold={SLOPE_THRESHOLD})")
        if info and info["slope"] >= SLOPE_THRESHOLD:
            results.append((col, info))
    results.sort(key=lambda x: x[1]["slope"], reverse=True)
    print(f"[TREND] top={[(n, round(i['slope'],4)) for n,i in results]}")
    return results[:top_n]

# ---------- Vista ----------
def predictivo(request):
    try:
        reservas = get_reservas()
        if not reservas:
            return render(request, "predictivo/predictivo.html",
                          {"error": "No hay datos para generar predicción."})

        df = pd.DataFrame(reservas)

        def extract_name(x):
            if isinstance(x, dict):
                return x.get("nombre") or x.get("name") or str(x)
            return x

        for col in ["persona", "sala", "articulo"]:
            if col in df.columns:
                df[col] = df[col].apply(extract_name)

        df["fechaHoraInicio"] = pd.to_datetime(df["fechaHoraInicio"])
        df["fecha"] = df["fechaHoraInicio"].dt.date
        df["hora"] = df["fechaHoraInicio"].dt.hour
        df["semana"] = df["fechaHoraInicio"].dt.to_period("W-MON")  # semanas que inician lunes

        sns.set(style="whitegrid")
        images = {}
        predictions_text = {
            "recursos": "No se detecta una tendencia clara entre recursos.",
            "salas": "No se detecta una tendencia clara entre salas."
        }

        # ---- Reservas por día + tendencia simple ----
        try:
            df_count = (df.groupby("fecha")
                          .size()
                          .reset_index(name="cantidad")
                          .sort_values("fecha"))
            df_count["dia_num"] = range(len(df_count))
            if len(df_count) >= 2:
                m = LinearRegression().fit(df_count[["dia_num"]], df_count["cantidad"])
                df_count["prediccion"] = m.predict(df_count[["dia_num"]])
            else:
                df_count["prediccion"] = df_count["cantidad"]

            fig = Figure(figsize=(10, 4))
            ax = fig.subplots()
            xlabels = df_count["fecha"].astype(str)
            ax.plot(xlabels, df_count["cantidad"], marker="o", label="Real")
            ax.plot(xlabels, df_count["prediccion"], marker="x", label="Predicción (trend)")
            ax.set_xticklabels(xlabels, rotation=45, ha="right")
            ax.set_title("Reservas por día (con tendencia)")
            ax.legend()
            fig.tight_layout()
            images["por_dia"] = fig_to_base64(fig)
        except Exception:
            print("Error gráfico por día:\n", traceback.format_exc())

        # ---- Barras varias (personas / salas / artículos) ----
        try:
            if "persona" in df.columns:
                top_personas = df["persona"].value_counts().head(10)
                fig = Figure(figsize=(8, 4)); ax = fig.subplots()
                sns.barplot(x=top_personas.values, y=top_personas.index, ax=ax)
                ax.set_title("Top 10 personas con más reservas"); ax.set_xlabel("Cantidad de reservas")
                fig.tight_layout(); images["por_persona"] = fig_to_base64(fig)
        except Exception:
            print("Error gráfico personas:\n", traceback.format_exc())

        try:
            if "sala" in df.columns:
                top_salas = df["sala"].value_counts().head(10)
                fig = Figure(figsize=(8, 4)); ax = fig.subplots()
                sns.barplot(x=top_salas.values, y=top_salas.index, ax=ax)
                ax.set_title("Salas más reservadas (histórico)"); ax.set_xlabel("Cantidad de reservas")
                fig.tight_layout(); images["por_sala"] = fig_to_base64(fig)
        except Exception:
            print("Error gráfico salas:\n", traceback.format_exc())

        try:
            if "articulo" in df.columns:
                top_articulos = df["articulo"].value_counts().head(10)
                fig = Figure(figsize=(8, 4)); ax = fig.subplots()
                sns.barplot(x=top_articulos.values, y=top_articulos.index, ax=ax)
                ax.set_title("Artículos más reservados (histórico)"); ax.set_xlabel("Cantidad de reservas")
                fig.tight_layout(); images["por_articulo"] = fig_to_base64(fig)
        except Exception:
            print("Error gráfico artículos:\n", traceback.format_exc())

        # ---- Demanda reciente por recurso (3 semanas) ----
        try:
            fecha_limite = datetime.now() - timedelta(weeks=3)
            df_rec = df[df["fechaHoraInicio"] >= fecha_limite]
            if "articulo" in df_rec.columns and not df_rec.empty:
                demanda = df_rec["articulo"].value_counts().reset_index()
                demanda.columns = ["Recurso", "Cantidad"]
                fig = Figure(figsize=(8, 4)); ax = fig.subplots()
                sns.barplot(x="Cantidad", y="Recurso", data=demanda, ax=ax)
                ax.set_title("Demanda reciente por recurso (últimas 3 semanas)")
                ax.set_xlabel("Cantidad de reservas")
                fig.tight_layout(); images["demanda_recurso"] = fig_to_base64(fig)
        except Exception:
            print("Error gráfico demanda reciente:\n", traceback.format_exc())

        # ---- Picos horarios ----
        try:
            fig = Figure(figsize=(8, 4)); ax = fig.subplots()
            sns.histplot(df["hora"], bins=24, kde=False, ax=ax)
            ax.set_title("Picos horarios de reservas"); ax.set_xlabel("Hora del día"); ax.set_ylabel("Cantidad de reservas")
            fig.tight_layout(); images["pico_horario"] = fig_to_base64(fig)
            hora_top = int(df["hora"].value_counts().idxmax())
            predictions_text["pico_horario"] = f"Horario pico: {hora_top}:00 - {hora_top+1}:00"
        except Exception:
            print("Error gráfico picos horarios:\n", traceback.format_exc())

        # ---- PREDICCIÓN semanal por recursos y salas ----
        try:
            print(f"[CFG] SLOPE_THRESHOLD={SLOPE_THRESHOLD}  MIN_POINTS={MIN_POINTS}")

            # Recursos (artículos)
            if "articulo" in df.columns:
                pivot_art = df.groupby(["semana", "articulo"]).size().unstack(fill_value=0)
                pivot_art = _ensure_time_index_sorted(pivot_art)
                print(f"[ART] semanas={list(pivot_art.index.strftime('%Y-%m-%d'))} cols={list(pivot_art.columns)}")
                top_art = pick_top_trend(pivot_art, top_n=3)
                if top_art:
                    name, info = top_art[0]
                    predictions_text["recursos"] = (
                        f"Predicción: '{name}' muestra tendencia positiva "
                        f"(pendiente={info['slope']:.2f}). Último={int(info['last'])}, "
                        f"Próx.periodo≈{max(0, int(round(info['pred_next'])))} reservas."
                    )

            # Salas
            if "sala" in df.columns:
                pivot_sala = df.groupby(["semana", "sala"]).size().unstack(fill_value=0)
                pivot_sala = _ensure_time_index_sorted(pivot_sala)
                print(f"[SALA] semanas={list(pivot_sala.index.strftime('%Y-%m-%d'))} cols={list(pivot_sala.columns)}")
                top_s = pick_top_trend(pivot_sala, top_n=3)
                if top_s:
                    name, info = top_s[0]
                    predictions_text["salas"] = (
                        f"Predicción: '{name}' podría seguir creciendo "
                        f"(pendiente={info['slope']:.2f}). Último={int(info['last'])}, "
                        f"Próx.periodo≈{max(0, int(round(info['pred_next'])))} reservas."
                    )

        except Exception:
            print("Error calculando predicciones:\n", traceback.format_exc())

        return render(request, "predictivo/predictivo.html", {"images": images, "predicciones": predictions_text})

    except Exception:
        print("Error en vista predictivo:\n", traceback.format_exc())
        return render(request, "predictivo/predictivo.html",
                      {"error": "Ocurrió un error al generar el dashboard."})
