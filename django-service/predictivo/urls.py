from django.urls import path
from .views import predictivo

urlpatterns = [
    path('', predictivo, name='predictivo'),
]
