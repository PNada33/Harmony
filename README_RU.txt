Harmony 1.16.5 — FiguraCosmetic Container Fix

Это исправленная версия. Предыдущий архив Harmony_FiguraCosmetic_BackSwords.zip нужно заменить этим.

Правильная структура:
- В обычной категории Render видна только одна новая функция: FiguraCosmetic.
- Katana НЕ отображается отдельной функцией в Render.
- Pet, PatPatPat, ChinaHat, Raincoat, WavePlayer, JumpCircle и Particles также скрыты из обычных списков ClickGUI.
- Все перечисленные функции доступны карточками только внутри FiguraCosmetic.
- Исправление действует в Windowed ClickGUI, MiniDropDown и поиске функций.

Работа FiguraCosmetic:
1. Открой обычный ClickGUI Harmony.
2. В категории Render нажми FiguraCosmetic.
3. Откроется визуальная библиотека с вкладками.
4. Внутри неё можно включать и выключать Pet, PatPatPat, ChinaHat, Raincoat, Katana, WavePlayer, JumpCircle и Particles.

Вкладки:
- Петы: Pet, PatPatPat.
- Аксессуары: ChinaHat, Raincoat.
- BackSwords: Katana.
- Скрипты: WavePlayer, JumpCircle, Particles.

Катана остаётся внутренним модулем, потому что ей нужен жизненный цикл Harmony и настройки рендера, но в обычном ClickGUI она больше не показывается.

Установка:
Скопировать папки src и out из архива в корень проекта Harmony с заменой файлов. Архив содержит исходники и готовые .class в правильной структуре.

Проверка:
- изменённые классы успешно скомпилированы: JAVAC_EXIT=0;
- проверено наличие единственного публичного пункта FiguraCosmetic;
- проверена фильтрация дочерних косметических функций во всех вариантах ClickGUI;
- прямую кнопку Figura Cosmetic из предыдущего DropDown удалили;
- ZIP проверен через unzip -t.
