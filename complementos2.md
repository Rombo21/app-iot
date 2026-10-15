Aplicaciones Móviles

DESARROLLO DE APLICACIONES EN ANDROID
CREAR SP LASHSCREEN  –   ESTA TICO , DINAMICO

Jorge Cortés Gallardo

CREAR SPLASHSCREEN

Las splashscreens son un elemento visual de la aplicación que contribuyen a que las
mejores aplicaciones se sientan como aplicaciones de primera categoría.

Siendo conscientes de que cada pequeño detalle del diseño es importante, y que es
un aspecto donde los programadores de aplicaciones suelen mostrarse más torpes.

La  guía  de  diseño  más  relevante  en  la  actualidad  es  Material  Design,  y  en  sus
recomendaciones  destacamos  aspectos  como  el  aspecto  por  capas,  los  botones
las  transformaciones  y  animaciones.  Cuando  un  desarrollador
flotantes  o
implementa  todas  estas  características,  las  aplicaciones  acaban  resultando  más
vistosas e intuitivas, lo cual contribuye a la experiencia final. Pero en estas líneas de
diseño  existe  una  recomendación  que  suele  pasar  desapercibida,  y  es  la  de  usar
Splash Screens, también conocidas como pantallas de inicio.

¿Por qué son importantes las SplashScreens?

Básicamente por el mismo motivo que es importante cada aspecto del diseño visual
de  la  aplicación.  La  respuesta  más  sencilla  sería  esa,  pero  en  esta  ocasión
expondremos más detalles. El hardware de nuestros dispositivos no es perfecto, y
según el tamaño de la aplicación, puede tardar unos segundos en abrirse. De este
modo, el propósito de una SplashScreen es el de hacer de punto intermedio entre
nuestro escritorio y la aplicación en cuestión.

¿En que consisten las SplashScreens entonces?

Pues son un recurso tan sencillo como un fondo simple -generalmente blanco o del
color principal de la aplicación – junto con el logotipo de nuestra aplicación y un breve
texto – como el nombre de nuestra aplicación, por ejemplo – y ya está.

CREAR SPLASHSCREEN ESTATICO

Crearemos un nuevo proyecto, por lo que nuestro primer Layout en que pondremos
solo  nuestra  imagen  que  para  el  ejemplo  será  el  logo  de  la  Aplicación  como  se
muestra a continuación.

Layout o Actuvity principal que será el splash de nuestra App.

Para  poder  realizar  este  primer  ejemplo  deberemos  tener  dos  Activity  con  sus
respectivos XML

Lo  principal  que  debe  tener  nuestro  splashscreen  es  que  no  debe  tener  la  barra
superior  con  el  nombre  de  la  aplicación  y  que  sea  fullscreen,  para  que  esto  sea
posible,  deberemos  agregar  las  siguientes  líneas  en  nuestro  archivo  de  java
MainActivity.java, en el método onCreate();

// Hacemos que nuestro activity se ajuste tanto al ancho y alto de la
pantalla del dispositivo
 this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
WindowManager.LayoutParams.FLAG_FULLSCREEN);

Utilizaremos  la  Opción  de  esperar  un  tiempo  (5  segundos)  para  poder  mostrar  el
splash y luego cargar otro activity, para eso  utilizaremos el metodo y función que
tiene Android para realizar esta acción que se llama Handler().

Handler(Looper.getMainLooper()).postDelayed({

    // Cargamos el Activity principal

    val intent = Intent(this, Principal::class.java)

    startActivity(intent)

    // Cerramos el Activity Principal que en este caso es el
MainActivity

    finish()

}, 5000) // Tiempo de espera en milisegundos

CREAR SPLASHSCREEN DINAMICO
(https://lottiefiles.com/)

Realizaremos ahora un ejemplo mas vistoso creando un splash con una animación,
para ellos ocuparemos los recursos de Lottie.

Las animaciones de Lottie por su parte son vectoriales. A groso modo, en lugar de
guardar  una  información  de  cada  pixel,  se  guarda  la  información  para  generar  la
forma y su animación. Si es un diseño sencillo  -y generalmente los stickers suelen
serlo- el resultado es un archivo de mucho menor tamaño que un archivo GIF.

Ejemplo de imagen vectorial (un SVG con el logotipo de Twitter)

Lottie  es  una  librería  que  permite  exportar  animaciones  creadas  en  Adobe  After
Effects mediante un plugin propio: Bodymovin. Este plugin genera un archivo JSON,
que es básicamente un documento de texto que contiene toda la información para
generar  la  imagen  y  su  animación.  Como  documento  de  texto  que  es,  su  tamaño
puede incrementarse rápidamente si es una animación compleja, larga o con muchos
elementos, ocupando en torno a los 100 KB.

Para  poder  incorporar  una  animación  en  nuestra  aplicación  descargaremos  la  que
más les guste del repositorio de lottie  https://lottiefiles.com/featured.

Seleccionan  la  animación  que  mas  les  gusto  y  deben  descargar  la  animación  en
formato JSON.

Crear Carpeta Assets en el directorio del proyecto, ejemplo

"AndroidStudioProjects\NOMBRE_PROYECTO\app\src\main"

Y guardamos el archivo JSON en la Carpeta Assets, que ara nuestro ejemplo es :

Una  vez  creada  la  carpeta  con  el  archivo  se  darán  cuenta  que  en la  estructura  de
archivos del proyecto figurara la carpeta con el archivo.

Tal  como  cada  vez  que  se  necesita  utilizar  una  librería  externa  deberemos
implementarla  según  indica  la  documentación  de  cada  librería,  para  nuestro  caso
debemos implementar la librería de Lottie.

Editaremos el archivo

Y agregaremos la siguiente línea en las dependencias

implementation 'com.airbnb.android:lottie:1.5.3'

Ahora deberemos agregar la animación en la parte grafica que sería el archivo XML,
que para nuestro caso seria

Debajo  del  Código  de  nuestro  logo  copiaremos  las  etiquetas  necesarias  para
incorporar la animación:

<com.airbnb.lottie.LottieAnimationView
android:layout_width="wrap_content"
android:layout_height="wrap_content"
app:lottie_autoPlay="true"
app:lottie_loop="true"
app:lottie_fileName="10962-spider-2.json"/>

En la etiqueta lottie_fileName, hacemos referencia al nombre de nuestra animación
descargada.

Y lo que podemos hacer es simplemente cambiar el tiempo en el que queremos que
se muestre la animación que para mi ejemplo debe ser 7 segundos.

Handler(Looper.getMainLooper()).postDelayed({

    // Cargamos el Activity principal

    val intent = Intent(this, Principal::class.java)

    startActivity(intent)

    // Cerramos el Activity Principal que en este caso es el
MainActivity

    finish()

}, 5000) // Tiempo de espera en milisegundos

Ahora compilamos y lanzamos nuestra aplicación y podrás ver tu splash animado.

