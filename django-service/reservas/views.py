
# reservas/views.py
from django.conf import settings
from django.shortcuts import render, redirect
from django.http import JsonResponse
from requests.auth import HTTPBasicAuth
import requests

# -----------------------------
# Helpers: llamadas al microservicio Java
# -----------------------------
def get_reservas():
    url = f"{settings.JAVA_API_BASE}/reservas"
    resp = requests.get(url, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
    if resp.ok:
        return resp.json()
    print("Error get_reservas:", resp.status_code, resp.text)
    return []

def obtener_reserva(reserva_id):
    all_res = get_reservas()
    for r in all_res:
        if str(r.get("id")) == str(reserva_id):
            return r
    raise requests.HTTPError(f"Reserva {reserva_id} no encontrada")


def crear_reserva_api(data):
    url = f"{settings.JAVA_API_BASE}/reservas"
    resp = requests.post(url, json=data, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
    if resp.status_code in (200, 201):
        return resp.json()
    print("Error crear_reserva_api:", resp.status_code, resp.text)
    return None

def actualizar_reserva_api(reserva_id, data):
    url = f"{settings.JAVA_API_BASE}/reservas/{reserva_id}"
    resp = requests.put(url, json=data, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
    if resp.ok:
        return resp.json()
    print("Error actualizar_reserva_api:", resp.status_code, resp.text)
    return None

def eliminar_reserva_api(reserva_id):
    url = f"{settings.JAVA_API_BASE}/reservas/{reserva_id}"
    resp = requests.delete(url, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
    if resp.status_code not in (200, 204):
        print("Error eliminar_reserva_api:", resp.status_code, resp.text)

# -----------------------------
# Helpers internos
# -----------------------------
def normalize_datetime_input(value, fallback=None):
    """
    Normaliza input de fecha/hora para enviar a backend Java.
    - value: string del form, ejemplo "2025-10-10T09:00"
    - fallback: si value está vacío, se retorna fallback
    Devuelve string "YYYY-MM-DDTHH:MM:SS" o None
    """
    if not value:
        return fallback
    val = value.strip()
    if len(val) == 16:  # YYYY-MM-DDTHH:MM
        return val + ":00"
    return val

# -----------------------------
# Vistas Django
# -----------------------------
def lista_reservas(request):
    reservas = get_reservas()
    return render(request, "reservas/lista.html", {"reservas": reservas})


def nueva_reserva(request):
    # Traer listas opcionales para autocomplete
    try:
        personas = requests.get(f"{settings.JAVA_API_BASE}/personas",
                                auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        personas = []
    try:
        salas = requests.get(f"{settings.JAVA_API_BASE}/salas",
                             auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        salas = []
    try:
        articulos = requests.get(f"{settings.JAVA_API_BASE}/articulos",
                                 auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        articulos = []

    if request.method == 'POST':
        start_raw = request.POST.get("fechaHoraInicio")
        end_raw = request.POST.get("fechaHoraFin")
        start = normalize_datetime_input(start_raw)
        end = normalize_datetime_input(end_raw)

        data = {
            "persona": request.POST.get("persona"),
            "sala": request.POST.get("sala"),
            "articulo": request.POST.get("articulo"),
            "fechaHoraInicio": start,
            "fechaHoraFin": end,
        }

        # debug logs
        print("POST", f"{settings.JAVA_API_BASE}/reservas", "->", data)
        resp = requests.post(f"{settings.JAVA_API_BASE}/reservas", json=data,
                             auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
        print("Status:", resp.status_code, "Body:", resp.text)
        if not resp.ok:
            return render(request, "reservas/reserva_form.html", {"error": f"{resp.status_code} {resp.text}",
                                                                  "personas": personas,
                                                                  "salas": salas,
                                                                  "articulos": articulos})
        return redirect('lista_reservas')

    return render(request, "reservas/reserva_form.html", {
        "personas": personas,
        "salas": salas,
        "articulos": articulos
    })


def editar_reserva(request, id):
    try:
        reserva = obtener_reserva(id)
    except requests.RequestException as e:
        print("Error obtener_reserva:", e)
        return redirect('lista_reservas')

    # Traer listas para autocomplete
    try:
        personas = requests.get(f"{settings.JAVA_API_BASE}/personas",
                                auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        personas = []
    try:
        salas = requests.get(f"{settings.JAVA_API_BASE}/salas",
                             auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        salas = []
    try:
        articulos = requests.get(f"{settings.JAVA_API_BASE}/articulos",
                                 auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD)).json()
    except Exception:
        articulos = []

    if request.method == "POST":
        start_raw = request.POST.get("fechaHoraInicio")
        end_raw = request.POST.get("fechaHoraFin")
        start_fallback = reserva.get("fechaHoraInicio") if isinstance(reserva, dict) else None
        end_fallback = reserva.get("fechaHoraFin") if isinstance(reserva, dict) else None
        start = normalize_datetime_input(start_raw, fallback=start_fallback)
        end = normalize_datetime_input(end_raw, fallback=end_fallback)

        data = {
            "persona": request.POST.get("persona"),
            "sala": request.POST.get("sala"),
            "articulo": request.POST.get("articulo"),
            "fechaHoraInicio": start,
            "fechaHoraFin": end,
        }

        # debug logs
        url = f"{settings.JAVA_API_BASE}/reservas/{id}"
        print("PUT", url)
        print("Sent JSON:", data)
        resp = requests.put(url, json=data, auth=HTTPBasicAuth(settings.JAVA_API_USER, settings.JAVA_API_PASSWORD))
        print("Status:", resp.status_code)
        print("Response text:", resp.text)

        if not resp.ok:
            return render(request, "reservas/reserva_form.html", {
                "reserva": reserva,
                "error": f"Error actualizando: {resp.status_code} {resp.text}",
                "personas": personas,
                "salas": salas,
                "articulos": articulos
            })

        return redirect('lista_reservas')

    return render(request, "reservas/reserva_form.html", {
        "reserva": reserva,
        "personas": personas,
        "salas": salas,
        "articulos": articulos
    })


def eliminar_reserva(request, id):
    try:
        eliminar_reserva_api(id)
    except Exception as e:
        print("Error eliminar_reserva:", e)
    return redirect('lista_reservas')


