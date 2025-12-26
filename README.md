# CryptoTrackerKotlin

CryptoTracker es una aplicación Android en Kotlin para gestionar portafolios de criptomonedas, permitiendo registrar, editar y eliminar movimientos financieros, así como calcular holdings de forma consistente y trazable.

El proyecto está diseñado bajo principios de Clean Architecture, con fuerte énfasis en:
	•	lógica de dominio aislada,
	•	casos de uso explícitos,
	•	pruebas unitarias desde el inicio,
	•	y una futura integración con persistencia local (Room).


  Objetivo del proyecto

Construir un MVP robusto y extensible que permita:
	•	Registrar movimientos de criptomonedas (compra, venta, depósito, retiro, swap, transferencias).
	•	Mantener el cálculo correcto de holdings por cartera y activo.
	•	Garantizar consistencia de datos mediante reglas de dominio.
	•	Facilitar pruebas unitarias y evolución del modelo sin fricción.
