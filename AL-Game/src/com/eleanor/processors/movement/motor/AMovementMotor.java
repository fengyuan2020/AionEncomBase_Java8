package com.eleanor.processors.movement.motor;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.eleanor.processors.movement.MovementProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AMovementMotor {

    private static final Logger log = LoggerFactory.getLogger(AMovementMotor.class);

    protected final Npc _owner;             // NPC, которым управляет данный двигатель
    protected final MovementProcessor _processor;   // Процессор движения, используемый для планирования задач
    protected Vector3f _targetPosition;      // Целевая позиция, к которой движется NPC
    protected float targetDestX;            // Координата X целевой позиции
    protected float targetDestY;            // Координата Y целевой позиции
    protected float targetDestZ;            // Координата Z целевой позиции
    protected byte _targetMask;             // Маска движения, определяющая тип движения
    protected byte _targetHeading;          // Направление движения

    /**
     * Конструктор для AMovementMotor.
     *
     * @param owner     NPC, которым управляет данный двигатель.  Не может быть null.
     * @param processor Процессор движения, используемый для планирования задач. Не может быть null.
     */
    AMovementMotor(Npc owner, MovementProcessor processor) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }

        this._owner = owner;
        this._processor = processor;
    }

    /**
     * Запускает двигатель движения.  Должен быть реализован в подклассах.
     */
    public abstract void start();

    /**
     * Останавливает двигатель движения. Должен быть реализован в подклассах.
     */
    public abstract void stop();

    /**
     * Возвращает текущую целевую позицию.
     *
     * @return Текущая целевая позиция или null, если цель не задана.
     */
    public Vector3f getCurrentTarget() {
        return _targetPosition != null ? _targetPosition.clone() : null; // Возвращаем копию
    }

    /**
     * Возвращает маску движения.
     *
     * @return Маска движения.
     */
    public byte getMovementMask() {
        return _targetMask;
    }

    /**
     * Пересчитывает параметры движения, такие как направление и маска движения.
     */
    protected void recalculateMovementParams() {
        byte oldHeading = _owner.getHeading();

        // Вычисляем направление движения.
        if (_targetPosition == null) {
            log.warn("Target position is null for NPC {}", _owner.getObjectId());
            return; // Прерываем выполнение, если целевая позиция не задана.
        }
        _targetHeading = (byte) (Math.toDegrees(Math.atan2(_targetPosition.getY() - _owner.getY(),
                _targetPosition.getX() - _owner.getX())) / 3.0);

        _targetMask = 0;

        // Если направление изменилось, устанавливаем соответствующий бит в маске.
        if (oldHeading != _targetHeading) {
            _targetMask |= 0xFFFFFFE0; // Или _targetMask = (byte) (_targetMask | 0xFFFFFFE0);
        }

        Stat2 stat = _owner.getGameStats().getMovementSpeed();

        // Устанавливаем биты в маске в зависимости от состояния NPC.
        if (_owner.isInState(CreatureState.WEAPON_EQUIPPED)) {
            _targetMask |= (stat.getBonus() < 0 ? -30 : -28);
        } else if (_owner.isInState(CreatureState.WALKING) || _owner.isInState(CreatureState.ACTIVE)) {
            _targetMask |= (stat.getBonus() < 0 ? -24 : -22);
        }

        // Если NPC летит, устанавливаем соответствующий бит в маске.
        if (_owner.isFlying()) {
            _targetMask |= 4;
        }

        // Если NPC возвращается, устанавливаем соответствующий бит в маске.
        if (_owner.getAi2().getState() == AIState.RETURNING) {
            _targetMask |= 0xFFFFFFE2;
        }
    }
}