package com.eleanor.processors.movement;

import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.geo.GeoService;
import com.eleanor.utils.GeomUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathfindHelper {

    private static final Logger log = LoggerFactory.getLogger(PathfindHelper.class); // Добавлен логгер

    private static final int VISIBLE_ANGLE = 180;
    private static final int PATHFIND_ANGLE_STEP = 20;

    /**
     * Выбирает шаг для движения к цели, учитывая геодату и препятствия.
     *
     * @param source Существо, выполняющее поиск пути.
     * @param target Целевая точка.
     * @return Vector3f Следующая точка для движения или null, если путь не найден.
     */
    public static Vector3f selectStep(Creature source, Vector3f target) {
        int mapId = source.getPosition().getMapId();
        int instanceId = source.getPosition().getInstanceId();
        float zOffset = source.getObjectTemplate().getBoundRadius().getUpper() / 2.0f;

        // Ограничение zOffset сверху.
        if (zOffset > 2.2f) {
            zOffset = 2.2f;
        }

        // Настройка смещения по высоте в зависимости от цели (если это игрок).
        float newZOffset = Math.max(0.6f, source.getObjectTemplate().getBoundRadius().getUpper() * 0.7f);
        if (source.getTarget() instanceof Player) {
            newZOffset = 1.5f;
        }

        Vector3f sourcePoint = new Vector3f(source.getX(), source.getY(), source.getZ());
        Vector3f targetPoint = target.clone(); // Важно клонировать, чтобы не изменить исходный target
        double futureDistance = GeomUtil.getDistance3D(sourcePoint.x, sourcePoint.y, sourcePoint.z, targetPoint.x, targetPoint.y, targetPoint.z);

        int offset = VISIBLE_ANGLE / 2;
        int rounds = VISIBLE_ANGLE / PATHFIND_ANGLE_STEP + 1;

        Vector3f closetsPoint = null;
        double minimalDistance = Short.MAX_VALUE;

        for (int i = 0; i < rounds; ++i) {
            // Вычисляем повёрнутую точку
            Vector3f rotated = rotate(source, sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y, futureDistance, i * PATHFIND_ANGLE_STEP - offset, targetPoint.z);

            // Проверяем допустимость высоты и наличие повёрнутой точки
            if (targetPoint.z - rotated.z > source.getObjectTemplate().getBoundRadius().getUpper() || rotated.z == 0.0f) {
                continue;
            }

            double newRotatedDistance = MathUtil.getDistance(sourcePoint.x, sourcePoint.y, sourcePoint.z, rotated.x, rotated.y, rotated.z);

            // Проверяем проходимость через геодату
            boolean canPassTemp = GeoService.getInstance().canPass(mapId, sourcePoint.x, sourcePoint.y, sourcePoint.z + zOffset, rotated.x, rotated.y, rotated.z + newZOffset, (float) newRotatedDistance, instanceId);
            if (!canPassTemp) {
                continue;
            }

            // Вычисляем расстояние от повёрнутой точки до цели
            double canPassDistance = MathUtil.getDistance(targetPoint.x, targetPoint.y, targetPoint.z, rotated.x, rotated.y, rotated.z);

            // Если нашли более близкую точку, обновляем её
            if (minimalDistance > canPassDistance) {
                minimalDistance = canPassDistance;
                closetsPoint = rotated;
            }
        }

        return closetsPoint;
    }

    /**
     * Выбирает шаг для следования за видимым объектом.
     *
     * @param source Существо, выполняющее следование.
     * @param target Объект, за которым нужно следовать.
     * @return Vector3f Следующая точка для движения или null, если невозможно следовать.
     */
    public static Vector3f selectFollowStep(Creature source, VisibleObject target) {
        int mapId = source.getPosition().getMapId();
        int instanceId = source.getPosition().getInstanceId();

        // Проверяем, находится ли цель в той же карте и инстансе
        if (target.getPosition().getMapId() != mapId || target.getPosition().getInstanceId() != instanceId) {
            return null;
        }

        Vector3f point = new Vector3f(target.getX(), target.getY(), target.getZ());

        // Assert - хорошее средство для проверки условий, которые всегда должны быть верны.
        // Если условие не выполняется, это указывает на ошибку в логике.
        // Однако, Assert может быть отключен в production-среде, поэтому его не стоит использовать
        // для проверок, которые необходимо выполнять всегда.  Вместо этого, используйте if/else с логированием.
        if (point.x == 0.0f || point.y == 0.0f) {
            log.warn("Target position is invalid (x={}, y={})", point.x, point.y);
            return null; // Или другое разумное действие
        }

        return selectStep(source, point);
    }

    /**
     * Вычисляет повёрнутую точку вокруг центра.
     *
     * @param owner    Существо, для которого выполняется вращение.
     * @param cx       Координата X центра вращения.
     * @param cy       Координата Y центра вращения.
     * @param x1       Координата X исходной точки.
     * @param y1       Координата Y исходной точки.
     * @param radius   Радиус вращения.
     * @param degrees  Угол вращения в градусах.
     * @param defaultZ Высота по умолчанию.
     * @return Vector3f Повёрнутая точка.
     */
    private static Vector3f rotate(Creature owner, float cx, float cy, float x1, float y1, double radius, float degrees, float defaultZ) {
        // Вычисляем угол в радианах от центра до исходной точки
        double beginDeg = Math.toDegrees(Math.atan2(y1 - cy, x1 - cx));

        // Добавляем угол вращения к исходному углу
        degrees += beginDeg;

        // Вычисляем координаты повёрнутой точки
        double x = cx + radius * Math.cos(Math.toRadians(degrees));
        double y = cy + radius * Math.sin(Math.toRadians(degrees));

        // Получаем высоту из геодаты
        double z = GeoService.getInstance().getZ(owner.getWorldId(), (float) x, (float) y, defaultZ, 100.0f, owner.getInstanceId());

        return new Vector3f((float) x, (float) y, (float) z);
    }

    /**
     * Получает случайную точку в заданном диапазоне от существа.
     *  <p>
     *  **ВНИМАНИЕ:** Этот метод кажется неполным, так как возвращает null.  Он также не использует вычисленные значения.
     *  Его нужно доработать.
     *
     * @param source   Существо, от которого нужно получить случайную точку.
     * @param minRange Минимальное расстояние.
     * @param maxRange Максимальное расстояние.
     * @return Vector3f Случайная точка или null, если не удалось её вычислить.  **(Текущая реализация всегда возвращает null!)**
     */
    public static Vector3f getRandomPoint(Creature source, float minRange, float maxRange) {
        Vector3f origin = new Vector3f(source.getX(), source.getY(), source.getZ());

        // Проверяем, что minRange и maxRange корректны.  Если это не так, логируем ошибку и возвращаем null.
        if (minRange <= 0.0f || maxRange <= minRange) {
            log.warn("Invalid range: minRange={}, maxRange={}", minRange, maxRange);
            return null;
        }

        int searchAngle = 360;
        int angleStep = 60;

        // Генерируем случайное расстояние в заданном диапазоне
        float randDist = (float) (Math.random() * (maxRange - minRange) + minRange);

        // Генерируем случайный угол
        int randAngle = (int) (Math.random() * 360.0);

        // Обходим углы с заданным шагом
        for (int i = 0; i < searchAngle; i += angleStep) {
            // Вычисляем повёрнутую точку
            Vector2f rotated2D = GeomUtil.getNextPoint2D(new Vector2f(origin.x, origin.y), randAngle + i, randDist);
            // **ВНИМАНИЕ:**  Результат вычисления rotated2D нигде не используется!
            // Это делает цикл бесполезным.  Нужно либо возвращать случайную точку, либо
            // вычислять несколько точек и выбирать лучшую (например, учитывая проходимость).
        }

        // **ВНИМАНИЕ:** Этот метод всегда возвращает null!
        // Нужно изменить логику, чтобы возвращать вычисленную случайную точку.
        return null;
    }
}