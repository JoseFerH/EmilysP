import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class BreakTutorialDialog extends StatefulWidget {
  const BreakTutorialDialog({required this.images, super.key});

  final List<String> images;

  @override
  State<BreakTutorialDialog> createState() => _BreakTutorialDialogState();
}

class _BreakTutorialDialogState extends State<BreakTutorialDialog> {
  late final PageController _pageController;
  late List<TransformationController> _transformationControllers;
  late List<bool> _pageZoomStates;
  int _currentPage = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
    _initialiseZoomControllers();
  }

  @override
  void dispose() {
    for (final controller in _transformationControllers) {
      controller.dispose();
    }
    _pageController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(covariant BreakTutorialDialog oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!listEquals(oldWidget.images, widget.images)) {
      _reinitialiseZoomControllers();
      if (_currentPage >= widget.images.length) {
        _currentPage = widget.images.isEmpty
            ? 0
            : math.max(0, widget.images.length - 1);
      }
    }
  }

  void _initialiseZoomControllers() {
    _transformationControllers = List<TransformationController>.generate(
      widget.images.length,
      (_) => TransformationController(),
    );
    _pageZoomStates = List<bool>.filled(widget.images.length, false);
  }

  void _reinitialiseZoomControllers() {
    for (final controller in _transformationControllers) {
      controller.dispose();
    }
    _initialiseZoomControllers();
  }

  void _resetZoom(int index) {
    if (index < 0 || index >= _transformationControllers.length) {
      return;
    }
    _transformationControllers[index].value = Matrix4.identity();
    _updateZoomState(index);
  }

  void _updateZoomState(int index) {
    if (index < 0 || index >= _transformationControllers.length) {
      return;
    }
    final bool isZoomed =
        _transformationControllers[index].value.getMaxScaleOnAxis() > 1.01;
    if (_pageZoomStates[index] != isZoomed) {
      setState(() {
        _pageZoomStates[index] = isZoomed;
      });
    }
  }

  ScrollPhysics _pagePhysics() {
    final bool isZoomed =
        _currentPage < _pageZoomStates.length && _pageZoomStates[_currentPage];
    return isZoomed
        ? const NeverScrollableScrollPhysics()
        : const PageScrollPhysics();
  }

  @override
  Widget build(BuildContext context) {
    final mediaSize = MediaQuery.of(context).size;
    final maxDialogWidth = mediaSize.width * 0.98;
    final maxDialogHeight = mediaSize.height * 0.95;
    const aspectRatio = 16 / 9;
    const double chromeHeight = 150;

    double imageWidth = maxDialogWidth;
    double imageHeight = imageWidth / aspectRatio;

    final desiredHeight = imageHeight + chromeHeight;
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

    final bool hasImages = widget.images.isNotEmpty;
    final bool canGoBack = hasImages && _currentPage > 0;
    final bool canGoForward =
        hasImages && _currentPage < widget.images.length - 1;

    return Dialog(
      backgroundColor: Colors.black87,
      insetPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: maxDialogWidth,
          maxHeight: maxDialogHeight,
        ),
        child: SafeArea(
          minimum: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
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
                        if (hasImages)
                          PageView.builder(
                            controller: _pageController,
                            physics: _pagePhysics(),
                            itemCount: widget.images.length,
                            onPageChanged: (index) {
                              setState(() {
                                _currentPage = index;
                              });
                              _resetZoom(index);
                            },
                            itemBuilder: (context, index) {
                              final controller =
                                  _transformationControllers[index];
                              return Container(
                                color: Colors.black,
                                alignment: Alignment.center,
                                child: GestureDetector(
                                  onDoubleTap: () => _resetZoom(index),
                                  child: InteractiveViewer(
                                    clipBehavior: Clip.hardEdge,
                                    transformationController: controller,
                                    minScale: 1,
                                    maxScale: 5,
                                    boundaryMargin: const EdgeInsets.symmetric(
                                      horizontal: 64,
                                      vertical: 48,
                                    ),
                                    onInteractionStart: (_) =>
                                        _updateZoomState(index),
                                    onInteractionUpdate: (_) =>
                                        _updateZoomState(index),
                                    onInteractionEnd: (_) =>
                                        _updateZoomState(index),
                                    child: Image.asset(
                                      widget.images[index],
                                      fit: BoxFit.contain,
                                      errorBuilder:
                                          (context, error, stackTrace) {
                                        return const Center(
                                          child: Text(
                                            'Imagen no encontrada',
                                            style: TextStyle(
                                                color: Colors.white70),
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                                ),
                              );
                            },
                          )
                        else
                          const Center(
                            child: Text(
                              'No hay imágenes para mostrar',
                              style: TextStyle(color: Colors.white70),
                            ),
                          ),
                        if (hasImages)
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
                        if (hasImages)
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
                                color: canGoForward
                                    ? Colors.white
                                    : Colors.white24,
                              ),
                            ),
                          ),
                      ],
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
                FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: Colors.blueAccent,
                    foregroundColor: Colors.white,
                    padding:
                        const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                  ),
                  onPressed: () => Navigator.of(context).maybePop(),
                  child: const Text('¡Listo para continuar!'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}