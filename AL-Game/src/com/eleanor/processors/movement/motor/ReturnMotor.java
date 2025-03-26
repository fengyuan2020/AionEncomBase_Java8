package com.eleanor.processors.movement.motor;

import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.eleanor.processors.movement.MovementProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReturnMotor extends AMovementMotor {

    private static final Logger log = LoggerFactory.getLogger(ReturnMotor.class);

    private ScheduledFuture<?> task; // Переименовано _task в task
    private final Vector3f targetPosition; // Добавлено final для неизменяемости

    public ReturnMotor(Npc owner, Vector3f spot, MovementProcessor processor) {
        super(owner, processor);
        this.targetPosition = spot.clone(); // Клонируем, чтобы избежать изменения внешней переменной
    }

    @Override
    public void start() {
        if (task != null) {
            log.warn("ReturnMotor already started for NPC {}", _owner.getObjectId());
            return; // Предотвращаем повторный запуск
        }

        recalculateMovementParams();

        float speed = _owner.getGameStats().getMovementSpeedFloat();
        long movementTime = (long) (100.0f / speed * 1000.0f);

        // Отправляем пакет SM_MOVE.
        PacketSendUtility.broadcastPacket(_owner,
                new SM_MOVE(_owner.getObjectId(), _owner.getX(), _owner.getY(), _owner.getZ(),
                        targetPosition.x, targetPosition.y, targetPosition.z, _targetHeading,
                        _targetMask));

        // Планируем задачу на MovementProcessor
        task = _processor.schedule(new Runnable() {
            @Override
            public void run() {
                // Обновляем позицию NPC.
                World.getInstance().updatePosition(_owner, targetPosition.x, targetPosition.y, targetPosition.z,
                        _targetHeading, false);

                // Вызываем события AI.
                try {
                    _owner.getAi2().onGeneralEvent(AIEventType.MOVE_ARRIVED);
                    _owner.getAi2().onGeneralEvent(AIEventType.BACK_HOME);
                } catch (Exception e) {
                    log.error("Error during AI event execution for NPC {}", _owner.getObjectId(), e);
                }
            }
        }, movementTime);
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel(true); // Отменяем запланированную задачу
            task = null;
        }
    }
}