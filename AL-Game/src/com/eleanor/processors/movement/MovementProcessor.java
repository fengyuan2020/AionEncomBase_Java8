package com.eleanor.processors.movement;

import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.eleanor.processors.AGameProcessor;
import com.eleanor.processors.movement.motor.AMovementMotor;
import com.eleanor.processors.movement.motor.ReturnMotor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovementProcessor extends AGameProcessor {

    private static final Logger log = LoggerFactory.getLogger(MovementProcessor.class);

    // Потокобезопасная карта для хранения связи между существом и его текущим двигателем движения.
    private final ConcurrentHashMap<Creature, AMovementMotor> registeredCreatures = new ConcurrentHashMap<>();

    // Конструктор.  Вызываем конструктор суперкласса (AGameProcessor) с указанием количества потоков.
    public MovementProcessor() {
        super(12); // Используем 12 потоков для обработки движения.
    }

    /**
     * Применяет новый двигатель движения к существу.
     *
     * @param creature Существо, к которому применяется двигатель.
     * @param newMotor Новый двигатель движения.
     * @return true, если двигатель успешно применен, false - если произошла ошибка.
     */
    private boolean applyMotor(Creature creature, AMovementMotor newMotor) {
        // Попытка добавить новый двигатель в карту.  Если существо уже зарегистрировано,
        // старый двигатель будет возвращен.
        AMovementMotor oldMotor = registeredCreatures.put(creature, newMotor);

        // Проверка на попытку заменить двигатель на самого себя.  Это, скорее всего, ошибка в логике.
        if (oldMotor == newMotor) {
            log.error("Attempt to replace same movement motor for creature: {}", creature);
            return false; // Важно не продолжать выполнение в этом случае
        }

        // Если был старый двигатель, останавливаем его и запускаем новый.
        if (oldMotor != null) {
            try {
                oldMotor.stop();
            } catch (Exception e) {
                log.error("Error stopping old motor for creature: {}", creature, e);
            }

            try {
                newMotor.start();
            } catch (Exception e) {
                log.error("Error starting new motor for creature: {}", creature, e);
                // В случае ошибки запуска нового двигателя, откатываем изменения и возвращаем false
                registeredCreatures.put(creature, oldMotor);  // Восстанавливаем старый двигатель
                return false;
            }
        } else {
            // Если это первый двигатель для существа, запускаем его.
            try {
                newMotor.start();
            } catch (Exception e) {
                log.error("Error starting new motor for creature: {}", creature, e);
                registeredCreatures.remove(creature); // Удаляем существо из карты, т.к. двигатель не запустился
                return false;
            }
        }

        return true;
    }

    /**
     * Применяет двигатель возврата к NPC.
     *
     * @param creature NPC, к которому применяется двигатель.
     * @param spot     Точка, к которой нужно вернуть NPC.
     * @return ReturnMotor Двигатель возврата или null, если не удалось применить двигатель.
     */
    public ReturnMotor applyReturn(Npc creature, Vector3f spot) {
        // Создаем двигатель возврата.
        ReturnMotor returnMotor = new ReturnMotor(creature, spot, this);

        // Пытаемся применить двигатель.
        if (applyMotor(creature, returnMotor)) {
            return returnMotor;
        }

        // Если не удалось применить двигатель, возвращаем null.
        return null;
    }
}