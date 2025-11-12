from django.urls import path
from . import views

from django.urls import path
from . import views
from predictivo.views import predictivo

urlpatterns = [
    path('', views.lista_reservas, name='lista_reservas'),  # ahora responde a /reservas/
    path('nueva/', views.nueva_reserva, name='nueva_reserva'),
    path('editar/<int:id>/', views.editar_reserva, name='editar_reserva'),
    path('eliminar/<int:id>/', views.eliminar_reserva, name='eliminar_reserva'),
]
