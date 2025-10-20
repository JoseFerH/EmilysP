# limitphone

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

## Carrusel de descanso

Durante el período de descanso configurado en la aplicación se mostrará un
carrusel de imágenes tipo tutorial. Para que funcione correctamente debes:

1. Colocar entre 4 y 8 imágenes en formato `.png` dentro de la carpeta
   `assets/tutorial/`.
2. Nombrar los archivos siguiendo el patrón `tutorial_N.png`, por ejemplo:
   `tutorial_1.png`, `tutorial_2.png`, ..., `tutorial_5.png`.
3. Mantener, por defecto, cinco imágenes (`tutorial_1.png` a `tutorial_5.png`).

Si necesitas cambiar la cantidad de imágenes mostradas, edita el valor de la
constante `kTutorialImageCount` en `lib/main.dart` y añade o elimina archivos en
`assets/tutorial/` para que coincidan con el nuevo número configurado.

## Recursos adicionales

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

Para más ayuda sobre Flutter visita la
[documentación en línea](https://docs.flutter.dev/), la cual ofrece tutoriales,
ejemplos y referencias completas de la API.
