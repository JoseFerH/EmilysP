// WorkBreak Timer - main.dart
// Flutter single-file app (minimalista moderna)
// INSTRUCCIONES: Añade estas dependencias en tu pubspec.yaml:
//
// dependencies:
//   flutter:
//     sdk: flutter
//   flutter_local_notifications: ^13.0.0
//   shared_preferences: ^2.1.0
//   timezone: ^0.9.0
//   flutter_native_timezone: ^2.0.0
//
// Luego ejecuta: flutter pub get
// Compilar en Android Studio / VSCode

import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import 'package:flutter_native_timezone/flutter_native_timezone.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await _initTimezone();
  await NotificationService().init();
  runApp(const WorkBreakApp());
}

Future<void> _initTimezone() async {
  tz.initializeTimeZones();
  String localTimeZone = 'UTC';
  try {
    localTimeZone = await FlutterNativeTimezone.getLocalTimezone();
  } catch (_) {}
  tz.setLocalLocation(tz.getLocation(localTimeZone));
}

class WorkBreakApp extends StatelessWidget {
  const WorkBreakApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'WorkBreak Timer',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
      ),
      home: const HomeScreen(),
    );
  }
}

// -------------------- Models --------------------
class WorkSchedule {
  final TimeOfDay start;
  final TimeOfDay end;

  WorkSchedule({required this.start, required this.end});

  Map<String, dynamic> toJson() => {
        'start': {'hour': start.hour, 'minute': start.minute},
        'end': {'hour': end.hour, 'minute': end.minute},
      };

  factory WorkSchedule.fromJson(Map<String, dynamic> json) {
    final s = json['start'];
    final e = json['end'];
    return WorkSchedule(
      start: TimeOfDay(hour: s['hour'], minute: s['minute']),
      end: TimeOfDay(hour: e['hour'], minute: e['minute']),
    );
  }
}

class BreakInterval {
  final String id; // unique string id
  final TimeOfDay start;
  final TimeOfDay end;

  BreakInterval({required this.id, required this.start, required this.end});

  Map<String, dynamic> toJson() => {
        'id': id,
        'start': {'hour': start.hour, 'minute': start.minute},
        'end': {'hour': end.hour, 'minute': end.minute},
      };

  factory BreakInterval.fromJson(Map<String, dynamic> json) {
    final s = json['start'];
    final e = json['end'];
    return BreakInterval(
      id: json['id'],
      start: TimeOfDay(hour: s['hour'], minute: s['minute']),
      end: TimeOfDay(hour: e['hour'], minute: e['minute']),
    );
  }
}

// -------------------- Notification Service --------------------
class NotificationService {
  NotificationService._internal();
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();

  Future<void> init() async {
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iOS = DarwinInitializationSettings();
    const settings = InitializationSettings(android: android, iOS: iOS);
    await _plugin.initialize(settings);
  }

  Future<void> showScheduledNotification(
      {required int id,
      required String title,
      required String body,
      required tz.TZDateTime scheduled}) async {
    final android = AndroidNotificationDetails(
      'workbreak_channel',
      'WorkBreak Alarms',
      channelDescription: 'Alarmas para tiempos libres y fin de descanso',
      importance: Importance.max,
      priority: Priority.high,
      playSound: true,
      ticker: 'ticker',
    );
    final ios = DarwinNotificationDetails();
    final details = NotificationDetails(android: android, iOS: ios);

    await _plugin.zonedSchedule(
      id,
      title,
      body,
      scheduled,
      details,
      androidAllowWhileIdle: true,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  Future<void> cancel(int id) => _plugin.cancel(id);
  Future<void> cancelAll() => _plugin.cancelAll();
}

// -------------------- Storage --------------------
class Storage {
  static const _scheduleKey = 'work_schedule_v1';
  static const _breaksKey = 'breaks_v1';

  Future<void> saveSchedule(WorkSchedule schedule) async {
    final prefs = await SharedPreferences.getInstance();
    prefs.setString(_scheduleKey, jsonEncode(schedule.toJson()));
  }

  Future<WorkSchedule?> loadSchedule() async {
    final prefs = await SharedPreferences.getInstance();
    final s = prefs.getString(_scheduleKey);
    if (s == null) return null;
    return WorkSchedule.fromJson(jsonDecode(s));
  }

  Future<void> saveBreaks(List<BreakInterval> breaks) async {
    final prefs = await SharedPreferences.getInstance();
    final encoded = breaks.map((b) => jsonEncode(b.toJson())).toList();
    prefs.setStringList(_breaksKey, encoded);
  }

  Future<List<BreakInterval>> loadBreaks() async {
    final prefs = await SharedPreferences.getInstance();
    final list = prefs.getStringList(_breaksKey) ?? [];
    return list.map((s) => BreakInterval.fromJson(jsonDecode(s))).toList();
  }
}

// -------------------- Home Screen --------------------
class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  WorkSchedule? schedule;
  List<BreakInterval> breaks = [];
  final Storage _storage = Storage();

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    final s = await _storage.loadSchedule();
    final b = await _storage.loadBreaks();
    setState(() {
      schedule = s;
      breaks = b;
    });
  }

  Future<void> _saveSchedule(WorkSchedule s) async {
    await _storage.saveSchedule(s);
    setState(() => schedule = s);
  }

  Future<void> _addBreak(BreakInterval b) async {
    breaks.add(b);
    await _storage.saveBreaks(breaks);
    _scheduleBreakNotifications(b);
    setState(() {});
  }

  Future<void> _removeBreak(BreakInterval b) async {
    breaks.removeWhere((x) => x.id == b.id);
    await _storage.saveBreaks(breaks);
    // cancel two notifications (id and id+100000 to avoid clash)
    final int baseId = _idFromString(b.id);
    await NotificationService().cancel(baseId);
    await NotificationService().cancel(baseId + 100000);
    setState(() {});
  }

  int _idFromString(String id) {
    // deterministic numeric id from string (small, but unique enough)
    return id.hashCode & 0x7fffffff; // positive
  }

  tz.TZDateTime _nextInstanceOf(TimeOfDay t) {
    final now = tz.TZDateTime.now(tz.local);
    var scheduled = tz.TZDateTime(tz.local, now.year, now.month, now.day, t.hour, t.minute);
    if (scheduled.isBefore(now)) scheduled = scheduled.add(const Duration(days: 1));
    return scheduled;
  }

  Future<void> _scheduleBreakNotifications(BreakInterval b) async {
    final baseId = _idFromString(b.id);

    final startDt = _nextInstanceOf(b.start);
    final endDt = _nextInstanceOf(b.end);

    await NotificationService().showScheduledNotification(
      id: baseId,
      title: 'Inicio de tiempo libre 😌',
      body: 'Disfruta tu descanso — ${_formatTimeOfDay(b.start)} a ${_formatTimeOfDay(b.end)}',
      scheduled: startDt,
    );

    await NotificationService().showScheduledNotification(
      id: baseId + 100000,
      title: 'Fin del tiempo libre ⏱️',
      body: 'Fin del descanso — vuelve al trabajo',
      scheduled: endDt,
    );
  }

  String _formatTimeOfDay(TimeOfDay t) => t.format(context);

  // -------------------- UI --------------------
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('WorkBreak Timer'),
        centerTitle: true,
        elevation: 4,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildScheduleCard(),
            const SizedBox(height: 16),
            const Text('Tiempos libres programados', style: TextStyle(fontWeight: FontWeight.bold)),
            Expanded(child: _buildBreaksList()),
          ],
        ),
      ),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          FloatingActionButton.extended(
            heroTag: 'editSchedule',
            onPressed: () => _openScheduleEditor(),
            label: const Text('Horario'),
            icon: const Icon(Icons.schedule),
          ),
          const SizedBox(height: 12),
          FloatingActionButton.extended(
            heroTag: 'addBreak',
            onPressed: () => _openAddBreakDialog(),
            label: const Text('Agregar descanso'),
            icon: const Icon(Icons.add_alarm),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleCard() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Horario laboral', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                Text(schedule == null ? 'No configurado' : '${_formatTimeOfDay(schedule!.start)} — ${_formatTimeOfDay(schedule!.end)}'),
              ],
            ),
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: _openScheduleEditor,
            )
          ],
        ),
      ),
    );
  }

  Widget _buildBreaksList() {
    if (breaks.isEmpty) {
      return Center(child: Text('No hay descansos. Agrega uno — tu productividad te lo agradecerá. 😏'));
    }

    return ListView.builder(
      itemCount: breaks.length,
      itemBuilder: (context, idx) {
        final b = breaks[idx];
        return Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            title: Text('${_formatTimeOfDay(b.start)} — ${_formatTimeOfDay(b.end)}'),
            subtitle: const Text('Descanso programado'),
            trailing: IconButton(
              icon: const Icon(Icons.delete_forever),
              onPressed: () => _confirmRemoveBreak(b),
            ),
          ),
        );
      },
    );
  }

  Future<void> _openScheduleEditor() async {
    TimeOfDay start = schedule?.start ?? const TimeOfDay(hour: 8, minute: 0);
    TimeOfDay end = schedule?.end ?? const TimeOfDay(hour: 17, minute: 0);

    final res = await showModalBottomSheet<WorkSchedule>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (context) {
        return Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: ScheduleEditor(initialStart: start, initialEnd: end),
        );
      },
    );

    if (res != null) {
      await _saveSchedule(res);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Horario guardado')));
    }
  }

  Future<void> _openAddBreakDialog() async {
    final res = await showModalBottomSheet<BreakInterval>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (context) {
        return Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: AddBreakDialog(),
        );
      },
    );

    if (res != null) {
      await _addBreak(res);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Descanso agregado y alarmas programadas')));
    }
  }

  Future<void> _confirmRemoveBreak(BreakInterval b) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar descanso?'),
        content: const Text('¿Seguro que quieres eliminar este descanso y sus alarmas?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text('Eliminar')),
        ],
      ),
    );

    if (ok == true) await _removeBreak(b);
  }
}

// -------------------- Editors --------------------
class ScheduleEditor extends StatefulWidget {
  final TimeOfDay initialStart;
  final TimeOfDay initialEnd;
  const ScheduleEditor({Key? key, required this.initialStart, required this.initialEnd}) : super(key: key);

  @override
  _ScheduleEditorState createState() => _ScheduleEditorState();
}

class _ScheduleEditorState extends State<ScheduleEditor> {
  late TimeOfDay start;
  late TimeOfDay end;

  @override
  void initState() {
    super.initState();
    start = widget.initialStart;
    end = widget.initialEnd;
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Editar horario', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          ListTile(
            title: const Text('Inicio'),
            trailing: Text(start.format(context)),
            onTap: () async {
              final t = await showTimePicker(context: context, initialTime: start);
              if (t != null) setState(() => start = t);
            },
          ),
          ListTile(
            title: const Text('Fin'),
            trailing: Text(end.format(context)),
            onTap: () async {
              final t = await showTimePicker(context: context, initialTime: end);
              if (t != null) setState(() => end = t);
            },
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
              const SizedBox(width: 8),
              ElevatedButton(onPressed: () => Navigator.pop(context, WorkSchedule(start: start, end: end)), child: const Text('Guardar')),
            ],
          )
        ],
      ),
    );
  }
}

class AddBreakDialog extends StatefulWidget {
  @override
  _AddBreakDialogState createState() => _AddBreakDialogState();
}

class _AddBreakDialogState extends State<AddBreakDialog> {
  TimeOfDay start = TimeOfDay.now();
  TimeOfDay end = TimeOfDay.now();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Agregar descanso', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          ListTile(
            title: const Text('Inicio'),
            trailing: Text(start.format(context)),
            onTap: () async {
              final t = await showTimePicker(context: context, initialTime: start);
              if (t != null) setState(() => start = t);
            },
          ),
          ListTile(
            title: const Text('Fin'),
            trailing: Text(end.format(context)),
            onTap: () async {
              final t = await showTimePicker(context: context, initialTime: end);
              if (t != null) setState(() => end = t);
            },
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: () {
                  if (!_validate()) return;
                  final id = DateTime.now().millisecondsSinceEpoch.toString();
                  final br = BreakInterval(id: id, start: start, end: end);
                  Navigator.pop(context, br);
                },
                child: const Text('Agregar'),
              ),
            ],
          )
        ],
      ),
    );
  }

  bool _validate() {
    final s = DateTime(0, 0, 0, start.hour, start.minute);
    final e = DateTime(0, 0, 0, end.hour, end.minute);
    if (s.isAtSameMomentAs(e)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('El inicio y fin no pueden ser iguales')));
      return false;
    }
    return true;
  }
}

