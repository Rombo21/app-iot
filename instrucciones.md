APLICACIONES MOVILES PARA IOT
PAUTA DE REQUERIMIENTO – PRIMERA ENTRAGA

IMPORTANTE
Se informa que esta evaluación considera exclusivamente los siguientes aspectos: Navegación entre las distintas
pantallas,  Fluidez  y  funcionamiento  de  las  interfaces,  Distribución  y  organización  visual  de  los  elementos,
Validación de los campos de entrada, Utilización e implementación de librerías externas.

En  esta  instancia  no  se  evaluará  la  integración  con  bases  de  datos  ni  el  consumo  de  API.  Por  lo  tanto,  las
funcionalidades  podrán  ser  simuladas,  siempre  que  permitan  demostrar  correctamente  la  navegación,  las
validaciones y el comportamiento esperado de la interfaz.

Desarrollar la siguiente aplicación móvil, considerando los siguientes requerimientos:

1.- SPLASH:  Diseñar e integrar un splash screen animado con Lottie que represente la
marca  y  optimice  la  experiencia  de  inicio  de  la  app  sin  degradar  el  rendimiento  ni  la
accesibilidad, redirigiendo automáticamente a la pantalla de Login (2) exactamente a los 7,0
segundos.

2.- LOGIN: Pantalla de Login con campos Usuario/Email y Contraseña, con las opciones
de  Ingresar,  Registrarse  y  Recuperar  Contraseña.  Validar  campos  en  blanco  y  formato  de
email. Mostrar mensajes con SweetAlert/SwitchAlert para: campos obligatorios.

3. y 4 - RECUPERAR CONTRASEÑAS: El usuario ingresa su emai, validar campo en
blanco y mostrar mensajes con SweetAlert/SwitchAlert, al presiona Recuperar, debe cargar
el formulario Crear contraseña con doble ingreso y validación de robustez: ≥8 caracteres,
mínimo  1  mayúscula,  1 minúscula, 1  número  y  1  carácter  especial.  Usar  SweetAlert  para
mensajes de éxito/errores.

5.- REGISTRAR USUARIO: El formulario (campos según la imagen) debe validar:
campos  vacíos,  formatos  (email),  y  contraseña  robusta  (punto  3  y  4))  con  confirmación
idéntica. Tras registrar Usar SweetAlert para: éxito, campos obligatorios, formatos inválidos,
contraseña débil/no coincide.

6.-  MENU  PRINCIPAL:
botones/tarjetas/imagenes con icono y texto:

  Debe

incluir

tres  opciones

visibles

como

1.  CRUD de Usuarios.
2.  Ver datos de sensores.
3.  Datos del desarrollador.

7.- MENU GESTIÓN DE USUARIO: centraliza la administración y debe mostrar dos
accesos  claros:  “Ingresar  usuario”  (abre  el  formulario  de  registro)  y  “Listar  usuarios”
(muestra la tabla de usuarios).

8.- REGISTRO DE USUARIO: El formulario (campos según la imagen) debe validar:
campos  vacíos,  formatos  (email),  y  contraseña  robusta  (punto  3  y  4))  con  confirmación
idéntica.  Tras  registrar,  persistir  y  redirigir  a  Login  (no  iniciar  sesión  automática).  Usar
SweetAlert  para:  éxito,  campos  obligatorios,  formatos  inválidos,  contraseña  débil/no
coincide.

9  y 10 .- LISTAR USUARIOS, MODIFICAR Y ELIMINAR: Listar Usuarios
mostrará una tabla con Nombre y Apellidos (opcional: Email) y un buscador que filtra por
nombre o apellidos (insensible a mayúsculas/acentos). Al seleccionar un usuario se abrirá
una ventana, para modificar Nombres, Apellidos y Email, con acciones Guardar y Eliminar.
Validaciones: campos obligatorios no vacíos; Nombres/Apellidos solo letras y espacios; Email
las notificaciones (éxito, error, confirmación de eliminación) se muestran con SweetAlert.

11.- DATOS DE SENSORES: Debe diseñar la siguiente opción considerando iconos e
imágenes, similares a la de la maqueta.

12.-  DATOS  DEL  DESARROLLADOR(ES):  Vista  solo  lectura  que  muestra  a  los
estudiantes  desarrolladores  en  tarjetas/lista  con:  foto/avatar,  Nombre  y  Apellidos,  Rol
(Frontend/Backend/QA/PM),  Correo  institucional,  Institución/Carrera/Sección  y  enlaces  a
GitHub/LinkedIn/portafolio.

IMPORTANTE: LA EVALUACIÓN DEBE SER ENTREGADA DESDE UN TELÉFONO MÓVIL FÍSICO.
NO SE ACEPTARÁN ENTREGAS REALIZADAS DESDE EMULADORES.

PUNTAJE DE LOS ITEMS

ESCALA DE NOTAS

