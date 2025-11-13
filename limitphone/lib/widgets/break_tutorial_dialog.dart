import 'package:flutter/material.dart';

class BreakTutorialDialog extends StatefulWidget {
  const BreakTutorialDialog({required this.images, super.key});

  final List<String> images;

  @override
  State<BreakTutorialDialog> createState() => _BreakTutorialDialogState();
}

class _BreakTutorialDialogState extends State<BreakTutorialDialog> {
  late final PageController _pageController;
  int _currentPage = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mediaSize = MediaQuery.of(context).size;
    final bool isLargeScreen = mediaSize.width >= 900;
    final double widthFactor = isLargeScreen ? 0.8 : 0.95;
    final double maxHeightFactor = isLargeScreen ? 0.85 : 0.9;

    // Reserva suficiente espacio para los controles inferiores sin depender de
    // cálculos de dimensiones intrínsecas (que ya habían provocado errores en
    // merges anteriores). El `Flexible` junto con `AspectRatio` garantiza que
    // el carrusel conserve el formato panorámico de 16:9 dentro del alto
    // disponible del diálogo.
    const aspectRatio = 16 / 9;

    final bool canGoBack = _currentPage > 0;
    final bool canGoForward = _currentPage < widget.images.length - 1;

    return Dialog(
      backgroundColor: Colors.black87,
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      child: FractionallySizedBox(
        widthFactor: widthFactor,
        heightFactor: maxHeightFactor,
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxWidth: mediaSize.width,
          ),
          child: SafeArea(
            minimum: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.max,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        '☕ Disfruta tu descanso',
                        style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.of(context).maybePop(),
                      icon: const Icon(Icons.close, color: Colors.white70),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Flexible(
                  fit: FlexFit.loose,
                  child: AspectRatio(
                    aspectRatio: aspectRatio,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          PageView.builder(
                            controller: _pageController,
                            itemCount: widget.images.length,
                            onPageChanged: (index) {
                              setState(() {
                                _currentPage = index;
                              });
                            },
                            itemBuilder: (context, index) {
                              return Container(
                                color: Colors.black,
                                alignment: Alignment.center,
                                child: Image.asset(
                                  widget.images[index],
                                  fit: BoxFit.cover,
                                  errorBuilder: (context, error, stackTrace) {
                                    return const Center(
                                      child: Text(
                                        'Imagen no encontrada',
                                        style: TextStyle(color: Colors.white70),
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
                              onPressed: canGoBack
                                  ? () {
                                      _pageController.previousPage(
                                        duration: const Duration(milliseconds: 300),
                                        curve: Curves.easeInOut,
                                      );
                                    }
                                  : null,
                              icon: Icon(
                                Icons.arrow_back_ios,
                                color: canGoBack ? Colors.white : Colors.white24,
                              ),
                            ),
                          ),
                          Align(
                            alignment: Alignment.centerRight,
                            child: IconButton(
                              padding: const EdgeInsets.all(16),
                              iconSize: 28,
                              splashRadius: 24,
                              onPressed: canGoForward
                                  ? () {
                                      _pageController.nextPage(
                                        duration: const Duration(milliseconds: 300),
                                        curve: Curves.easeInOut,
                                      );
                                    }
                                  : null,
                              icon: Icon(
                                Icons.arrow_forward_ios,
                                color:
                                    canGoForward ? Colors.white : Colors.white24,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(
                    widget.images.length,
                    (index) => AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      width: _currentPage == index ? 12 : 8,
                      height: _currentPage == index ? 12 : 8,
                      decoration: BoxDecoration(
                        color: _currentPage == index
                            ? Colors.white
                            : Colors.white38,
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Align(
                  alignment: Alignment.center,
                  child: FilledButton(
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
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
