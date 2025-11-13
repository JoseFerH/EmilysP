import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

import 'widgets/break_tutorial_dialog.dart';
import 'dart:async';
import 'dart:math' as math;

/// Bandera para activar/desactivar las integraciones nativas de bloqueo.
///
/// Para la demostración actual se deshabilita cualquier llamada nativa y se
/// utiliza únicamente la pantalla interna de bloqueo.
const bool kUseNativeLocking = false;

/// Número predeterminado de imágenes que se mostrarán en el carrusel de
/// descanso. Modifica este valor para ajustar la cantidad de imágenes que
/// deben cargarse desde `assets/tutorial/`.
const int kTutorialImageCount = 6;

/// Canal de método para comunicación con código nativo
const platform = MethodChannel('com.limitphone/lock');

/// Instancia global del plugin de notificaciones
final FlutterLocalNotificationsPlugin notificationsPlugin =
    FlutterLocalNotificationsPlugin();

/// Estado global del bloqueo
class LockState {
  static final LockState _instance = LockState._internal();
  factory LockState() => _instance;
  LockState._internal();

  bool isLocked = false;
  bool isBreakActive = false;
  Timer? checkTimer;
  Timer? reopenTimer;
  final StreamController<bool> _lockController = StreamController<bool>.broadcast();
  final StreamController<bool> _breakController =
      StreamController<bool>.broadcast();
  Stream<bool> get lockStream => _lockController.stream;
  Stream<bool> get breakStream => _breakController.stream;

  void setLocked(bool locked) {
    if (isLocked != locked) {
      isLocked = locked;
      _lockController.add(locked);
    }

    if (locked) {
      _startReopenTimer();
      _startNativeLockService();
    } else {
      _stopReopenTimer();
      _stopNativeLockService();
    }
  }

  void setBreakActive(bool isActive) {
    if (isBreakActive != isActive) {
      isBreakActive = isActive;
      _breakController.add(isActive);
    }
  }

  Future<void> _startNativeLockService() async {
    if (!kUseNativeLocking) {
      debugPrint('Bloqueo nativo deshabilitado: _startNativeLockService');
      return;
    }
    try {
      await platform.invokeMethod('startLockService');
      debugPrint('Servicio nativo de bloqueo iniciado');
    } on PlatformException catch (e) {
      debugPrint('Error de plataforma al iniciar servicio: ${e.message}');
    } catch (e) {
      debugPrint('Error al iniciar servicio nativo: $e');
    }
  }

  Future<void> _stopNativeLockService() async {
    if (!kUseNativeLocking) {
      debugPrint('Bloqueo nativo deshabilitado: _stopNativeLockService');
      return;
    }
    try {
      await platform.invokeMethod('stopLockService');
      debugPrint('Servicio nativo de bloqueo detenido');
    } on PlatformException catch (e) {
      debugPrint('Error de plataforma al detener servicio: ${e.message}');
    } catch (e) {
      debugPrint('Error al detener servicio nativo: $e');
    }
  }

  void _startReopenTimer() {
    if (!kUseNativeLocking) {
      return;
    }
    _stopReopenTimer();
    // Intentar reabrir la app cada 3 segundos si está bloqueada
    reopenTimer = Timer.periodic(const Duration(seconds: 3), (timer) {
      if (isLocked) {
        _bringAppToFront();
      }
    });
  }

  void _stopReopenTimer() {
    reopenTimer?.cancel();
    reopenTimer = null;
  }

  void _bringAppToFront() {
    if (!kUseNativeLocking) {
      debugPrint('Bloqueo nativo deshabilitado: _bringAppToFront');
      return;
    }
    // Intentar traer la app al frente usando el servicio nativo
    try {
      platform.invokeMethod('bringToFront');
    } catch (e) {
      debugPrint('Error al traer app al frente: $e');
    }
  }

  void dispose() {
    _lockController.close();
    _breakController.close();
    checkTimer?.cancel();
    reopenTimer?.cancel();
  }
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Inicializar zona horaria local
  tz.initializeTimeZones();
  tz.setLocalLocation(tz.getLocation('America/Guatemala'));

  // Configuración inicial para Android con canal de notificación
  const AndroidInitializationSettings androidInit =
      AndroidInitializationSettings('@mipmap/ic_launcher');
  
  const InitializationSettings initSettings =
      InitializationSettings(android: androidInit);

  await notificationsPlugin.initialize(
    initSettings,
    onDidReceiveNotificationResponse: (NotificationResponse response) {
      debugPrint('Notificación recibida: ${response.payload}');
      // Traer la app al frente cuando se toque la notificación
      LockState()._bringAppToFront();
    },
  );

  // Crear el canal de notificaciones para Android
  const AndroidNotificationChannel channel = AndroidNotificationChannel(
    'limitphone_work_channel',
    'Alarmas de Trabajo',
    description: 'Notificaciones para gestionar tu horario laboral',
    importance: Importance.max,
    playSound: true,
    enableVibration: true,
    showBadge: true,
  );

  final plugin = notificationsPlugin.resolvePlatformSpecificImplementation<
      AndroidFlutterLocalNotificationsPlugin>();
  
  if (plugin != null) {
    await plugin.createNotificationChannel(channel);
  }

  runApp(const LimitPhoneApp());
}

/// Aplicación principal
class LimitPhoneApp extends StatefulWidget {
  const LimitPhoneApp({super.key});

  @override
  State<LimitPhoneApp> createState() => _LimitPhoneAppState();
}

class _LimitPhoneAppState extends State<LimitPhoneApp> with WidgetsBindingObserver {
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    // Esperar un momento antes de verificar el estado
    await Future.delayed(const Duration(milliseconds: 500));
    await _checkAndUpdateLockState();
    _startLockChecker();
    setState(() {
      _isInitialized = true;
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    
    // Cuando la app vuelve a estar visible, verificar si debe estar bloqueada
    if (state == AppLifecycleState.resumed) {
      _checkAndUpdateLockState();
    }
    
    // Si la app va a segundo plano y está bloqueada, mostrar notificación persistente
    if (state == AppLifecycleState.paused && LockState().isLocked) {
      _showPersistentLockNotification();
    }
  }

  Future<void> _showPersistentLockNotification() async {
    const androidDetails = AndroidNotificationDetails(
      'limitphone_work_channel',
      'Alarmas de Trabajo',
      channelDescription: 'Notificaciones para gestionar tu horario laboral',
      importance: Importance.max,
      priority: Priority.high,
      playSound: false,
      enableVibration: false,
      ongoing: true, // Notificación persistente
      autoCancel: false,
      icon: '@mipmap/ic_launcher',
      largeIcon: DrawableResourceAndroidBitmap('@mipmap/ic_launcher'),
    );

    await notificationsPlugin.show(
      888,
      '🔒 Teléfono Bloqueado',
      'Estás en horario de trabajo. Toca para volver a la app.',
      const NotificationDetails(android: androidDetails),
    );
  }

  void _startLockChecker() {
    // Verificar el estado cada minuto
    LockState().checkTimer = Timer.periodic(const Duration(minutes: 1), (timer) async {
      await _checkAndUpdateLockState();
    });
  }

  Future<void> _checkAndUpdateLockState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final alarmsEnabled = prefs.getBool('alarmsEnabled') ?? false;
      
      if (!alarmsEnabled) {
        LockState().setLocked(false);
        await notificationsPlugin.cancel(888); // Cancelar notificación persistente
        return;
      }

      final workStartHour = prefs.getInt('workStartHour');
      final workStartMinute = prefs.getInt('workStartMinute');
      final workEndHour = prefs.getInt('workEndHour');
      final workEndMinute = prefs.getInt('workEndMinute');
      final breakStartHour = prefs.getInt('breakStartHour');
      final breakStartMinute = prefs.getInt('breakStartMinute');
      final breakDuration = prefs.getInt('breakDuration') ?? 30;

      if (workStartHour == null || workStartMinute == null ||
          workEndHour == null || workEndMinute == null ||
          breakStartHour == null || breakStartMinute == null) {
        LockState().setLocked(false);
        await notificationsPlugin.cancel(888);
        return;
      }

      final now = DateTime.now();
      final currentMinutes = now.hour * 60 + now.minute;

      final workStartMinutes = workStartHour * 60 + workStartMinute;
      final workEndMinutes = workEndHour * 60 + workEndMinute;
      final breakStartMinutes = breakStartHour * 60 + breakStartMinute;
      const totalMinutesInDay = 24 * 60;
      final normalizedBreakEndMinutes =
          (breakStartMinutes + breakDuration) % totalMinutesInDay;
      final breakCrossesMidnight =
          breakStartMinutes + breakDuration >= totalMinutesInDay;

      bool isWithinWorkSchedule;
      if (workStartMinutes <= workEndMinutes) {
        // Horario normal (no cruza medianoche)
        isWithinWorkSchedule =
            currentMinutes >= workStartMinutes && currentMinutes < workEndMinutes;
      } else {
        // Horario que cruza medianoche
        isWithinWorkSchedule =
            currentMinutes >= workStartMinutes || currentMinutes < workEndMinutes;
      }

      bool isDuringBreak = false;
      if (breakDuration > 0 && isWithinWorkSchedule) {
        if (!breakCrossesMidnight) {
          isDuringBreak = currentMinutes >= breakStartMinutes &&
              currentMinutes < normalizedBreakEndMinutes;
        } else {
          isDuringBreak = currentMinutes >= breakStartMinutes ||
              currentMinutes < normalizedBreakEndMinutes;
        }
      }

      final bool shouldLock = isWithinWorkSchedule && !isDuringBreak;
      final bool isBreakActive = isWithinWorkSchedule && isDuringBreak;

      debugPrint('Estado de bloqueo: $shouldLock');
      debugPrint('Hora actual: ${now.hour}:${now.minute}');
      debugPrint('Trabajo: $workStartHour:$workStartMinute - $workEndHour:$workEndMinute');
      debugPrint('Descanso: $breakStartHour:$breakStartMinute - ${breakDuration}min');
      debugPrint('Descanso activo: $isBreakActive');

      LockState().setLocked(shouldLock);
      LockState().setBreakActive(isBreakActive);

      if (shouldLock) {
        await _showPersistentLockNotification();
      } else {
        await notificationsPlugin.cancel(888);
      }
    } catch (e) {
      debugPrint('Error en _checkAndUpdateLockState: $e');
      // En caso de error, no bloquear
      LockState().setLocked(false);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    LockState().dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Mostrar splash mientras inicializa
    if (!_isInitialized) {
      return MaterialApp(
        title: 'LimitPhone',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.blueAccent),
          useMaterial3: true,
        ),
        debugShowCheckedModeBanner: false,
        home: const Scaffold(
          body: Center(
            child: CircularProgressIndicator(),
          ),
        ),
      );
    }

    return MaterialApp(
      title: 'LimitPhone',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blueAccent),
        useMaterial3: true,
      ),
      debugShowCheckedModeBanner: false,
      home: StreamBuilder<bool>(
        stream: LockState().lockStream,
        initialData: LockState().isLocked,
        builder: (context, snapshot) {
          if (snapshot.data == true) {
            return const LockScreen();
          }
          return const HomePage();
        },
      ),
    );
  }
}

/// Pantalla de bloqueo
class LockScreen extends StatefulWidget {
  const LockScreen({super.key});

  @override
  State<LockScreen> createState() => _LockScreenState();
}

class _LockScreenState extends State<LockScreen> {
  final TextEditingController _passwordController = TextEditingController();
  bool _obscurePassword = true;
  String _errorMessage = '';
  bool _isUnlocking = false; // NUEVO: estado de desbloqueo

  @override
  void initState() {
    super.initState();
    _enableImmersiveMode();
    
    // Deshabilitar botón de retroceso
    SystemChannels.platform.setMethodCallHandler((call) async {
      if (call.method == 'SystemNavigator.pop') {
        return; // Bloquear el botón de atrás
      }
    });
  }

  void _enableImmersiveMode() {
    // Ocultar barras del sistema (barra de estado y navegación)
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.immersiveSticky,
      overlays: [], // Sin overlays = oculta todo
    );
  }

  Future<void> _checkPassword() async {
    if (_isUnlocking) {
      debugPrint('⚠️ Ya se está procesando un desbloqueo...');
      return;
    }

    final prefs = await SharedPreferences.getInstance();
    final savedPassword = prefs.getString('emergencyPassword') ?? '123';
    
    if (_passwordController.text == savedPassword) {
      setState(() {
        _isUnlocking = true;
        _errorMessage = '';
      });

      try {
        // Restaurar las barras del sistema antes de mostrar la pantalla principal
        debugPrint('🔓 Restaurando barras del sistema...');
        SystemChrome.setEnabledSystemUIMode(
          SystemUiMode.edgeToEdge,
          overlays: SystemUiOverlay.values,
        );

        // Desbloquear la app
        debugPrint('🔓 Desbloqueando app...');
        LockState().setLocked(false);

        // Limpiar UI
        _passwordController.clear();
        if (mounted) {
          setState(() {
            _errorMessage = '';
            _isUnlocking = false;
          });
        }

        // Cancelar notificación persistente
        await notificationsPlugin.cancel(888);

        debugPrint('✅ Desbloqueo completado exitosamente');
        
        // Feedback de éxito
        HapticFeedback.mediumImpact();
      } catch (e) {
        debugPrint('❌ Error durante desbloqueo: $e');
        if (mounted) {
          setState(() {
            _errorMessage = '⚠️ Error al desbloquear. Intenta nuevamente.';
            _isUnlocking = false;
          });
        }
        HapticFeedback.heavyImpact();
      }
    } else {
      setState(() {
        _errorMessage = '❌ Contraseña incorrecta';
        _isUnlocking = false;
      });
      _passwordController.clear();
      
      // Vibrar para indicar error
      HapticFeedback.heavyImpact();
    }
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async => false, // Bloquear navegación hacia atrás
      child: Scaffold(
        backgroundColor: Colors.red.shade900,
        body: GestureDetector(
          onTap: () {
            // Mantener modo inmersivo cuando se toca la pantalla
            _enableImmersiveMode();
          },
          child: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.red.shade900,
                  Colors.red.shade700,
                ],
              ),
            ),
            child: SafeArea(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      // Icono de bloqueo
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.1),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.lock_clock,
                          size: 80,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 32),
                      
                      // Título
                      const Text(
                        '🔒 HORARIO DE TRABAJO',
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      
                      // Mensaje
                      Text(
                        'El teléfono está restringido durante\ntu horario laboral',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.white.withOpacity(0.9),
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      
                      const SizedBox(height: 24),
                      
                      // Hora actual
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 24,
                          vertical: 16,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: StreamBuilder(
                          stream: Stream.periodic(const Duration(seconds: 1)),
                          builder: (context, snapshot) {
                            final now = DateTime.now();
                            return Column(
                              children: [
                                Text(
                                  '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}',
                                  style: const TextStyle(
                                    fontSize: 48,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                    letterSpacing: 4,
                                  ),
                                ),
                                Text(
                                  '${now.day}/${now.month}/${now.year}',
                                  style: TextStyle(
                                    fontSize: 16,
                                    color: Colors.white.withOpacity(0.7),
                                  ),
                                ),
                              ],
                            );
                          },
                        ),
                      ),
                      const SizedBox(height: 48),
                      
                      // Campo de contraseña de emergencia
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Column(
                          children: [
                            const Text(
                              '⚠️ Salida de Emergencia',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 8),
                            TextField(
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 18,
                              ),
                              textAlign: TextAlign.center,
                              decoration: InputDecoration(
                                hintText: 'Contraseña de emergencia',
                                hintStyle: TextStyle(
                                  color: Colors.white.withOpacity(0.5),
                                ),
                                filled: true,
                                fillColor: Colors.white.withOpacity(0.1),
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: BorderSide.none,
                                ),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _obscurePassword
                                        ? Icons.visibility
                                        : Icons.visibility_off,
                                    color: Colors.white.withOpacity(0.7),
                                  ),
                                  onPressed: () {
                                    setState(() {
                                      _obscurePassword = !_obscurePassword;
                                    });
                                  },
                                ),
                              ),
                              onSubmitted: (_) => _checkPassword(),
                            ),
                            if (_errorMessage.isNotEmpty) ...[
                              const SizedBox(height: 12),
                              Text(
                                _errorMessage,
                                style: const TextStyle(
                                  color: Colors.yellowAccent,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                            const SizedBox(height: 16),
                            SizedBox(
                              width: double.infinity,
                              child: ElevatedButton(
                                onPressed: _isUnlocking ? null : _checkPassword,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.white,
                                  foregroundColor: Colors.red.shade900,
                                  padding: const EdgeInsets.symmetric(vertical: 16),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                ),
                                child: _isUnlocking
                                    ? Row(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        children: [
                                          SizedBox(
                                            width: 20,
                                            height: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              valueColor: AlwaysStoppedAnimation<Color>(
                                                Colors.red.shade900,
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 12),
                                          const Text(
                                            'DESBLOQUEANDO...',
                                            style: TextStyle(
                                              fontSize: 16,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ],
                                      )
                                    : const Text(
                                        'DESBLOQUEAR',
                                        style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      
                      // Mensaje motivacional
                      Text(
                        '💪 ¡Mantén el enfoque en tu trabajo!',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.white.withOpacity(0.7),
                          fontStyle: FontStyle.italic,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _passwordController.dispose();
    // Restaurar las barras del sistema cuando se cierre
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: SystemUiOverlay.values,
    );
    super.dispose();
  }
}

/// Pantalla principal
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  TimeOfDay? _workStart;
  TimeOfDay? _workEnd;
  TimeOfDay? _breakStart;
  int _breakDuration = 30;
  bool _hasExactAlarmPermission = false;
  bool _hasNotificationPermission = false;
  String _emergencyPassword = '123';
  final TextEditingController _passwordController = TextEditingController();
  StreamSubscription<bool>? _breakSubscription;
  bool _tutorialShownForCurrentBreak = false;
  late final List<String> _tutorialImages;

  @override
  void initState() {
    super.initState();
    _tutorialImages = List.generate(
      kTutorialImageCount,
      (index) => 'assets/tutorial/tutorial_${index + 1}.png',
    );
    _restoreSystemUI();
    _checkPermissions();
    _loadSavedSettings();
    _breakSubscription = LockState().breakStream.listen(_handleBreakChange);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _handleBreakChange(LockState().isBreakActive);
    });
  }

  void _handleBreakChange(bool isOnBreak) {
    if (!mounted) return;

    if (isOnBreak) {
      if (!_tutorialShownForCurrentBreak) {
        _tutorialShownForCurrentBreak = true;
        _showBreakTutorial();
      }
    } else {
      _tutorialShownForCurrentBreak = false;
    }
  }

  Future<void> _showBreakTutorial() async {
    if (!mounted) return;

    await showDialog(
      context: context,
      barrierDismissible: true,
      builder: (_) => BreakTutorialDialog(images: _tutorialImages),
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setState) {
            final mediaSize = MediaQuery.of(context).size;
            final maxDialogWidth = mediaSize.width * 0.95;
            final maxDialogHeight = mediaSize.height * 0.9;
            const aspectRatio = 16 / 9;
            const double chromeHeight = 160;

            double imageWidth = maxDialogWidth;
            double imageHeight = imageWidth / aspectRatio;

            final double desiredHeight = imageHeight + chromeHeight;
            if (desiredHeight > maxDialogHeight) {
              final availableForImage = math.max(0.0, maxDialogHeight - chromeHeight);
              if (availableForImage > 0) {
                imageHeight = availableForImage;
                imageWidth = imageHeight * aspectRatio;
                if (imageWidth > maxDialogWidth) {
                  imageWidth = maxDialogWidth;
                  imageHeight = imageWidth / aspectRatio;
                }
              } else {
                imageWidth = math.min(maxDialogWidth, maxDialogHeight);
                imageHeight = imageWidth / aspectRatio;
              }
            }

            return AlertDialog(
              backgroundColor: Colors.black87,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              contentPadding: const EdgeInsets.all(16),
              insetPadding: const EdgeInsets.symmetric(
                horizontal: 16,
                vertical: 24,
              ),
              content: ConstrainedBox(
                constraints: BoxConstraints(
                  maxWidth: maxDialogWidth,
                  maxHeight: maxDialogHeight,
                ),
                child: SizedBox(
                  width: imageWidth,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text(
                            '☕ Disfruta tu descanso',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          IconButton(
                            onPressed: () => Navigator.of(context).maybePop(),
                            icon: const Icon(Icons.close, color: Colors.white70),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(16),
                        child: SizedBox(
                          width: imageWidth,
                          height: imageHeight,
                          child: Stack(
                            children: [
                              PageView.builder(
                                controller: pageController,
                                itemCount: _tutorialImages.length,
                                onPageChanged: (index) {
                                  setState(() {
                                    currentPage = index;
                                  });
                                },
                                itemBuilder: (context, index) {
                                  return Container(
                                    color: Colors.black,
                                    alignment: Alignment.center,
                                    child: Image.asset(
                                      _tutorialImages[index],
                                      fit: BoxFit.cover,
                                      errorBuilder: (context, error, stackTrace) {
                                        return const Center(
                                          child: Text(
                                            'Imagen no encontrada',
                                            style: TextStyle(
                                              color: Colors.white70,
                                            ),
                                          ),
                                        );
                                      },
                                    ),
                                  );
                                },
                              ),
                              Align(
                                alignment: Alignment.centerLeft,
                                child: IconButton(
                                  padding: const EdgeInsets.all(16),
                                  iconSize: 28,
                                  splashRadius: 24,
                                  onPressed: currentPage > 0
                                      ? () {
                                          pageController.previousPage(
                                            duration: const Duration(milliseconds: 300),
                                            curve: Curves.easeInOut,
                                          );
                                        }
                                      : null,
                                  icon: Icon(
                                    Icons.arrow_back_ios,
                                    color: currentPage > 0
                                        ? Colors.white
                                        : Colors.white24,
                                  ),
                                ),
                              ),
                              Align(
                                alignment: Alignment.centerRight,
                                child: IconButton(
                                  padding: const EdgeInsets.all(16),
                                  iconSize: 28,
                                  splashRadius: 24,
                                  onPressed:
                                      currentPage < _tutorialImages.length - 1
                                          ? () {
                                              pageController.nextPage(
                                                duration: const Duration(milliseconds: 300),
                                                curve: Curves.easeInOut,
                                              );
                                            }
                                          : null,
                                  icon: Icon(
                                    Icons.arrow_forward_ios,
                                    color: currentPage < _tutorialImages.length - 1
                                        ? Colors.white
                                        : Colors.white24,
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),
                          FilledButton(
                            style: FilledButton.styleFrom(
                              backgroundColor: Colors.blueAccent,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(
                                horizontal: 24,
                                vertical: 12,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: List.generate(
                          _tutorialImages.length,
                          (index) => AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            margin: const EdgeInsets.symmetric(horizontal: 4),
                            width: currentPage == index ? 12 : 8,
                            height: currentPage == index ? 12 : 8,
                            decoration: BoxDecoration(
                              color: currentPage == index
                                  ? Colors.white
                                  : Colors.white38,
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      FilledButton(
                        style: FilledButton.styleFrom(
                          backgroundColor: Colors.blueAccent,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 24,
                            vertical: 12,
                          ),
                        ),
                        onPressed: () => Navigator.of(context).maybePop(),
                        child: const Text('¡Listo para continuar!'),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );
  }

  void _restoreSystemUI() {
    // Restaurar las barras del sistema en la página principal
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: SystemUiOverlay.values,
    );
  }

  Future<void> _checkPermissions() async {
    try {
      final plugin = notificationsPlugin
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>();

      if (plugin == null) {
        return;
      }

      var hasNotificationPermission =
          await plugin.areNotificationsEnabled() ?? true;

      if (!hasNotificationPermission) {
        hasNotificationPermission =
            await plugin.requestNotificationsPermission() ?? false;
      }

      var hasExactAlarmPermission =
          await plugin.canScheduleExactNotifications() ?? false;

      if (!hasExactAlarmPermission) {
        await plugin.requestExactAlarmsPermission();
        hasExactAlarmPermission =
            await plugin.canScheduleExactNotifications() ?? false;
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _hasNotificationPermission = hasNotificationPermission;
        _hasExactAlarmPermission = hasExactAlarmPermission;
      });
    } catch (e) {
      debugPrint('Error al verificar permisos: $e');
    }
  }

  Future<void> _loadSavedSettings() async {
    final prefs = await SharedPreferences.getInstance();

    final workStartHour = prefs.getInt('workStartHour');
    final workStartMinute = prefs.getInt('workStartMinute');
    final workEndHour = prefs.getInt('workEndHour');
    final workEndMinute = prefs.getInt('workEndMinute');
    final breakStartHour = prefs.getInt('breakStartHour');
    final breakStartMinute = prefs.getInt('breakStartMinute');
    final breakDuration = prefs.getInt('breakDuration');
    final savedPassword = prefs.getString('emergencyPassword');

    if (!mounted) {
      return;
    }

    setState(() {
      if (workStartHour != null && workStartMinute != null) {
        _workStart = TimeOfDay(hour: workStartHour, minute: workStartMinute);
      }
      if (workEndHour != null && workEndMinute != null) {
        _workEnd = TimeOfDay(hour: workEndHour, minute: workEndMinute);
      }
      if (breakStartHour != null && breakStartMinute != null) {
        _breakStart = TimeOfDay(hour: breakStartHour, minute: breakStartMinute);
      }
      if (breakDuration != null) {
        _breakDuration = breakDuration;
      }
      if (savedPassword != null) {
        _emergencyPassword = savedPassword;
        _passwordController.text = savedPassword;
      }
    });
  }

  Future<void> _saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    if (_workStart != null) {
      await prefs.setInt('workStartHour', _workStart!.hour);
      await prefs.setInt('workStartMinute', _workStart!.minute);
    }
    if (_workEnd != null) {
      await prefs.setInt('workEndHour', _workEnd!.hour);
      await prefs.setInt('workEndMinute', _workEnd!.minute);
    }
    if (_breakStart != null) {
      await prefs.setInt('breakStartHour', _breakStart!.hour);
      await prefs.setInt('breakStartMinute', _breakStart!.minute);
    }
    await prefs.setInt('breakDuration', _breakDuration);
    await prefs.setString('emergencyPassword', _emergencyPassword);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('LimitPhone'),
        centerTitle: true,
        elevation: 2,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              '⏰ Configuración de Horarios',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),

            if (!_hasExactAlarmPermission || !_hasNotificationPermission)
              Card(
                color: Colors.orange[100],
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.warning, color: Colors.orange),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Permisos requeridos',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        !_hasNotificationPermission
                            ? 'Necesitas habilitar notificaciones y alarmas exactas.'
                            : 'Necesitas habilitar alarmas exactas.',
                        style: const TextStyle(fontSize: 14),
                      ),
                      const SizedBox(height: 12),
                      ElevatedButton.icon(
                        icon: const Icon(Icons.settings),
                        label: const Text('Verificar Permisos'),
                        onPressed: _checkPermissions,
                      ),
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 20),

            Card(
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '💼 Horario de Trabajo',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: _buildTimeButton(
                            label: 'Inicio',
                            time: _workStart,
                            onPressed: () => _pickTime('workStart'),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: _buildTimeButton(
                            label: 'Fin',
                            time: _workEnd,
                            onPressed: () => _pickTime('workEnd'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            Card(
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '☕ Período de Descanso',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    _buildTimeButton(
                      label: 'Inicio del descanso',
                      time: _breakStart,
                      onPressed: () => _pickTime('breakStart'),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        const Text('Duración:', style: TextStyle(fontSize: 16)),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Slider(
                            value: _breakDuration.toDouble(),
                            min: 15,
                            max: 120,
                            divisions: 21,
                            label: '$_breakDuration min',
                            onChanged: (value) {
                              setState(() {
                                _breakDuration = value.toInt();
                              });
                            },
                          ),
                        ),
                        Text(
                          '$_breakDuration min',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    if (_breakStart != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          'Fin del descanso: ${_calculateBreakEnd().format(context)}',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Contraseña de emergencia
            Card(
              elevation: 3,
              color: Colors.orange[50],
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.emergency, color: Colors.orange),
                        SizedBox(width: 8),
                        Text(
                          '🔑 Contraseña de Emergencia',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _passwordController,
                      decoration: InputDecoration(
                        hintText: 'Contraseña (default: 123)',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                        prefixIcon: const Icon(Icons.lock),
                      ),
                      onChanged: (value) {
                        _emergencyPassword = value.isEmpty ? '123' : value;
                      },
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Esta contraseña te permitirá desbloquear el teléfono en caso de emergencia.',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 30),

            ElevatedButton.icon(
              icon: const Icon(Icons.alarm_add, size: 28),
              label: const Text(
                'Activar Alarmas y Bloqueo',
                style: TextStyle(fontSize: 18),
              ),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
              onPressed: (_canSchedule() && _hasExactAlarmPermission && _hasNotificationPermission) 
                  ? _scheduleAllAlarms 
                  : null,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              icon: const Icon(Icons.alarm_off, size: 28),
              label: const Text(
                'Cancelar Alarmas y Bloqueo',
                style: TextStyle(fontSize: 18),
              ),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
              ),
              onPressed: _cancelAllAlarms,
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              icon: const Icon(Icons.notifications_active),
              label: const Text('Probar Notificación'),
              onPressed: _testNotification,
            ),
            const SizedBox(height: 30),

            if (_canSchedule())
              Card(
                color: Colors.blue[50],
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        '📋 Resumen de Alarmas',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 12),
                      _buildAlarmSummary(
                        '🟢 Inicio de trabajo (🔒 BLOQUEADO)',
                        _workStart!.format(context),
                      ),
                      _buildAlarmSummary(
                        '🟡 Inicio de descanso (🔓 LIBRE)',
                        _breakStart!.format(context),
                      ),
                      _buildAlarmSummary(
                        '🟠 Fin de descanso (🔒 BLOQUEADO)',
                        _calculateBreakEnd().format(context),
                      ),
                      _buildAlarmSummary(
                        '🔴 Fin de trabajo (🔓 LIBRE)',
                        _workEnd!.format(context),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimeButton({
    required String label,
    required TimeOfDay? time,
    required VoidCallback onPressed,
  }) {
    return OutlinedButton(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        padding: const EdgeInsets.all(16),
      ),
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 12)),
          const SizedBox(height: 4),
          Text(
            time?.format(context) ?? '--:--',
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAlarmSummary(String label, String time) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(label, style: const TextStyle(fontSize: 13)),
          ),
          Text(
            time,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  Future<void> _pickTime(String type) async {
    final now = TimeOfDay.now();
    final picked = await showTimePicker(
      context: context,
      initialTime: now,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            timePickerTheme: TimePickerThemeData(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        switch (type) {
          case 'workStart':
            _workStart = picked;
            break;
          case 'workEnd':
            _workEnd = picked;
            break;
          case 'breakStart':
            _breakStart = picked;
            break;
        }
      });
    }
  }

  TimeOfDay _calculateBreakEnd() {
    if (_breakStart == null) return const TimeOfDay(hour: 0, minute: 0);
    
    final totalMinutes = _breakStart!.hour * 60 + _breakStart!.minute + _breakDuration;
    return TimeOfDay(
      hour: (totalMinutes ~/ 60) % 24,
      minute: totalMinutes % 60,
    );
  }

  bool _canSchedule() {
    return _workStart != null && _workEnd != null && _breakStart != null;
  }

  Future<void> _testNotification() async {
    try {
      const androidDetails = AndroidNotificationDetails(
        'limitphone_work_channel',
        'Alarmas de Trabajo',
        channelDescription: 'Notificaciones para gestionar tu horario laboral',
        importance: Importance.max,
        priority: Priority.high,
        playSound: true,
        enableVibration: true,
        icon: '@mipmap/ic_launcher',
        largeIcon: DrawableResourceAndroidBitmap('@mipmap/ic_launcher'),
      );

      await notificationsPlugin.show(
        999,
        '🔔 Notificación de Prueba',
        'Si ves esto, las notificaciones funcionan correctamente.',
        const NotificationDetails(android: androidDetails),
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Notificación de prueba enviada'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error en notificación de prueba: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Error: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _scheduleAllAlarms() async {
    if (!_hasExactAlarmPermission || !_hasNotificationPermission) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('⚠️ Debes otorgar todos los permisos necesarios'),
            backgroundColor: Colors.orange,
          ),
        );
      }
      return;
    }

    try {
      await _saveSettings();
      await notificationsPlugin.cancelAll();

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('alarmsEnabled', true);

      final now = tz.TZDateTime.now(tz.local);

      debugPrint('Programando alarmas...');
      debugPrint('Hora actual: $now');

      // 1. Alarma de inicio de trabajo (BLOQUEA)
      await _scheduleNotification(
        id: 1,
        time: _workStart!,
        title: '💼 ¡Inicio de Trabajo!',
        body: 'Tu jornada laboral comienza ahora. El teléfono se bloqueará. ¡Adelante!',
        now: now,
      );

      // 2. Alarma de inicio de descanso (DESBLOQUEA)
      await _scheduleNotification(
        id: 2,
        time: _breakStart!,
        title: '☕ ¡Hora de Descanso!',
        body: 'Tómate $_breakDuration minutos para descansar. Teléfono desbloqueado.',
        now: now,
      );

      // 3. Alarma de fin de descanso (BLOQUEA)
      await _scheduleNotification(
        id: 3,
        time: _calculateBreakEnd(),
        title: '🔄 ¡Fin del Descanso!',
        body: 'Tu descanso ha terminado. El teléfono se bloqueará nuevamente.',
        now: now,
      );

      // 4. Alarma de fin de trabajo (DESBLOQUEA)
      await _scheduleNotification(
        id: 4,
        time: _workEnd!,
        title: '🎉 ¡Fin de Trabajo!',
        body: 'Tu jornada laboral ha terminado. Teléfono desbloqueado. ¡Excelente trabajo!',
        now: now,
      );

      // Verificar estado inmediato
      await Future.delayed(const Duration(milliseconds: 500));
      if (context.mounted) {
        (context.findAncestorStateOfType<_LimitPhoneAppState>())?._checkAndUpdateLockState();
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Alarmas y sistema de bloqueo activados correctamente'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 3),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error al programar alarmas: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Error: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    }
  }

  Future<void> _scheduleNotification({
    required int id,
    required TimeOfDay time,
    required String title,
    required String body,
    required tz.TZDateTime now,
  }) async {
    var scheduled = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      time.hour,
      time.minute,
      0,
    );

    if (scheduled.isBefore(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }

    debugPrint('Programando alarma $id: ${time.hour}:${time.minute}');
    debugPrint('Fecha programada: $scheduled');

    const androidDetails = AndroidNotificationDetails(
      'limitphone_work_channel',
      'Alarmas de Trabajo',
      channelDescription: 'Notificaciones para gestionar tu horario laboral',
      importance: Importance.max,
      priority: Priority.high,
      playSound: true,
      enableVibration: true,
      icon: '@mipmap/ic_launcher',
      largeIcon: DrawableResourceAndroidBitmap('@mipmap/ic_launcher'),
      fullScreenIntent: true,
      category: AndroidNotificationCategory.alarm,
    );

    await notificationsPlugin.zonedSchedule(
      id,
      title,
      body,
      scheduled,
      const NotificationDetails(android: androidDetails),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );

    debugPrint('Alarma $id programada exitosamente');
  }

  Future<void> _cancelAllAlarms() async {
    try {
      await notificationsPlugin.cancelAll();

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('alarmsEnabled', false);
      
      LockState().setLocked(false);
      
      debugPrint('Todas las alarmas canceladas y bloqueo desactivado');
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('🚫 Alarmas canceladas y sistema de bloqueo desactivado'),
            backgroundColor: Colors.orange,
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error al cancelar alarmas: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('⚠️ Error al cancelar: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  void dispose() {
    _breakSubscription?.cancel();
    _passwordController.dispose();
    super.dispose();
  }
}

