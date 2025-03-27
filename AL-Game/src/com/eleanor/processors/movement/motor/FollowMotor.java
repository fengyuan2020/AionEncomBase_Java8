package com.eleanor.processors.movement.motor;

import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.eleanor.processors.movement.MovementProcessor;
import com.eleanor.processors.movement.PathfindHelper;
import com.eleanor.utils.GeomUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FollowMotor extends AMovementMotor {

    private static final Logger log = LoggerFactory.getLogger(FollowMotor.class);

    private static final int TARGET_REVALIDATE_TIME = 300; // Время перепроверки цели (в миллисекундах)

    public VisibleObject target;  // Цель для следования
    private ScheduledFuture<?> task; // Задача планировщика для регулярного обновления движения
    private long lastMoveMs;        // Время последнего перемещения
    private Vector3f lastMovePoint;   // Последняя точка, в которой находился NPC
    private byte newTargetHeading;   // Новое направление движения NPC

    // Переменная для времени перепроверки пути.  Сделана final, т.к. должна быть константой.
    // Инициализируется при создании экземпляра FollowMotor.
    private final long pathfindRevalidationTime;

    public FollowMotor(MovementProcessor parentProcessor, Npc owner, VisibleObject target) {
        super(owner, parentProcessor);
        this.target = target;
        this.pathfindRevalidationTime = System.currentTimeMillis(); // Инициализация при создании экземпляра
    }

    @Override
    public void start() {
        if (task != null) {
            // log.warn("FollowMotor already started for NPC {}", _owner.getObjectId());
            return; // Предотвращаем повторный запуск
        }
        update(); // Запускаем процесс следования
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel(true); // Отменяем задачу
            task = null;
        }

        // Сбрасываем значения
        lastMoveMs = 0L;
        lastMovePoint = null;
        target = null;
    }

    /**
     * Обновляет движение NPC, определяя следующую точку и отправляя пакет движения.
     *
     * @return true, если обновление успешно выполнено, false в противном случае.
     */
    public boolean update() {
	try {
        // Проверяем условия, при которых обновление не требуется.
        if (target == null || (task != null && task.isCancelled()) || _owner.getLifeStats().isAlreadyDead() || _owner.getAi2().getState() == AIState.DIED) {
            // log.warn("Update: Условия остановки выполнены, останавливаем двигатель. target == {}, task.isCancelled() == {}, isAlreadyDead() == {}, AIState == {}", target, (task != null && task.isCancelled()), _owner.getLifeStats().isAlreadyDead(), _owner.getAi2().getState());
            stop(); // Останавливаем двигатель, если условия не выполнены
            return false;
        }
        // log.warn("Update:  Проверка before canMove");
        boolean canMoveResult = canMove();
        // log.warn("Update: canMove() вернул: {}", canMoveResult);
        if (!canMoveResult) {
            // log.warn("Update: canMove() вернул false, остановка.");
            stop(); // Останавливаем двигатель, если canMove вернул false
            return false;
        }


        // log.warn("Update: Проходим дальше.  target: {}", target);

        boolean directionChanged;
        lastMovePoint = new Vector3f(_owner.getX(), _owner.getY(), _owner.getZ());
        // log.warn("Update: lastMovePoint установлен в {}", lastMovePoint);

        // Проверяем, может ли NPC пройти к цели напрямую.
        boolean canPass = GeoService.getInstance().canPass(_owner, target);
        // log.warn("Update: canPass() вернул {}", canPass);

        // Выбираем следующую точку для движения.
        Vector3f nextTargetPosition = null;
        if (canMove() && !canPass && pathfindRevalidationTime < System.currentTimeMillis()) {
            // Если не может пройти и требуется перепроверка пути, используем PathfindHelper.
            // log.warn("Update: Используем PathfindHelper");
            nextTargetPosition = PathfindHelper.selectFollowStep(_owner, target);
            // log.warn("Update: PathfindHelper вернул {}", nextTargetPosition);
        } else if (canMove() && canPass) {
            // Если может пройти, вычисляем следующую точку на основе расстояния и направления.
            // log.warn("Update: NPC может пройти напрямую к цели");
            if (target == null) {
                log.error("target is null!");
                return false; // Добавлено, чтобы не было NullPointerException
            }

            float newZ = GeoService.getInstance().getZ(_owner.getWorldId(), target.getX(), target.getY(), target.getZ(), 100.0f, _owner.getInstanceId());
            // log.warn("Update: newZ: {}", newZ);
            Vector3f getTargetPos = new Vector3f(target.getX(), target.getY(), newZ);
            // log.warn("Update: getTargetPos: {}", getTargetPos);
            float range = (float) _owner.getGameStats().getAttackRange().getCurrent() / 1000.0f;
            // log.warn("Update: range: {}", range);

            // log.warn("Update: before GeomUtil.getDistance3D");
            double distance = GeomUtil.getDistance3D(lastMovePoint, getTargetPos.x, getTargetPos.y, getTargetPos.z) - Math.max(range, _owner.getCollision());
            // log.warn("Update: distance: {}", distance);
            // log.warn("Update: before GeomUtil.getDirection3D");
            Vector3f dir = GeomUtil.getDirection3D(lastMovePoint, getTargetPos);
            // log.warn("Update: dir: {}", dir);
            if (dir != null) { // Добавляем проверку на null
                // log.warn("Update: before GeomUtil.getNextPoint3D");
                nextTargetPosition = GeomUtil.getNextPoint3D(lastMovePoint, dir, (float) distance); //Строка 91!
                // log.warn("Update: nextTargetPosition: {}", nextTargetPosition);
            } else {
                // log.warn("Update: dir == null, остаемся на месте");
                nextTargetPosition = lastMovePoint; // Если направление не определено, остаемся на месте
            }
        } else if (pathfindRevalidationTime < System.currentTimeMillis()) {
            // Если не может двигаться и требуется перепроверка, устанавливаем целевую позицию в null.
            // log.warn("Update: Перепроверка пути не требуется, устанавливаем целевую позицию в null.");
            nextTargetPosition = null;
        }
        // log.warn("Update: nextTargetPosition: {}", nextTargetPosition);

        // Отправляем пакет движения, если целевая позиция была изменена.
        if (nextTargetPosition != null) {
            // log.warn("Update: nextTargetPosition != null, отправляем SM_MOVE");
            directionChanged = nextTargetPosition.x != this.targetDestX || nextTargetPosition.y != this.targetDestY || nextTargetPosition.z != this.targetDestZ;
            this.targetDestX = nextTargetPosition.x;
            this.targetDestY = nextTargetPosition.y;
            this.targetDestZ = nextTargetPosition.z;

            double distance = GeomUtil.getDistance3D(_owner.getX(), _owner.getY(), _owner.getZ(), nextTargetPosition.x, nextTargetPosition.y, nextTargetPosition.z);
            float speed = _owner.getGameStats().getMovementSpeedFloat();
            long movementTime = (long) (distance / (double) speed * 1000.0);
            //this.pathfindRevalidationTime = System.currentTimeMillis() + movementTime;  //Использовать pathfindRevalidationTime нельзя, оно final
            recalculateMovementParams();

            newTargetHeading = (byte) (Math.toDegrees(Math.atan2(nextTargetPosition.y - _owner.getY(), nextTargetPosition.x - _owner.getX())) / 3.0);

            if (directionChanged) {
                PacketSendUtility.broadcastPacket(_owner, new SM_MOVE(_owner.getObjectId(), _owner.getX(), _owner.getY(), _owner.getZ(), nextTargetPosition.x, nextTargetPosition.y, nextTargetPosition.z, newTargetHeading, _targetMask));
            }

            lastMoveMs = System.currentTimeMillis();
        } else {
            // // log.warn("Update: nextTargetPosition == null, не отправляем SM_MOVE");
        }

        // Планируем следующее обновление движения.
        Vector3f finalNextTargetPosition = nextTargetPosition;  //Создаём final копию
        task = _processor.schedule(new Runnable() {
            @Override
            public void run() {
                if (finalNextTargetPosition != null) {   //Используем final копию
                    Vector3f lastMove = FollowMotor.this.lastMovePoint;
                    Vector3f targetMove = new Vector3f(finalNextTargetPosition.x, finalNextTargetPosition.y, finalNextTargetPosition.z);
                    float speed = FollowMotor.this._owner.getGameStats().getMovementSpeedFloat();
                    long time = System.currentTimeMillis() - FollowMotor.this.lastMoveMs;
                    float distPassed = speed * ((float) time / 1000.0f);
                    if (lastMove == null) {
                        lastMove = new Vector3f(FollowMotor.this._owner.getX(), FollowMotor.this._owner.getY(), FollowMotor.this._owner.getZ());
                    }
                    float maxDist = lastMove.distance(targetMove);
                    if (distPassed <= 0.0f) {
                        return;
                    }
                    if (distPassed > maxDist) {
                        distPassed = maxDist;
                    }
                    Vector3f dir = GeomUtil.getDirection3D(lastMove, targetMove);
                    Vector3f position = GeomUtil.getNextPoint3D(lastMove, dir, distPassed);
                    if (FollowMotor.this._owner.getWorldId() != 300230000) {
                        float newZ = GeoService.getInstance().getZ(FollowMotor.this._owner.getWorldId(), position.x, position.y, position.z, 100.0f, FollowMotor.this._owner.getInstanceId());
                        position.z = lastMove.getZ() < newZ && Math.abs(lastMove.getZ() - newZ) > 1.0f ? newZ + FollowMotor.this._owner.getObjectTemplate().getBoundRadius().getUpper() - FollowMotor.this._owner.getObjectTemplate().getHeight() : newZ;
                    }
                    World.getInstance().updatePosition(FollowMotor.this._owner, position.x, position.y, position.z, FollowMotor.this.newTargetHeading, false);
                } else {
                    PacketSendUtility.broadcastPacket(FollowMotor.this._owner, new SM_MOVE(FollowMotor.this._owner.getObjectId(), FollowMotor.this._owner.getX(), FollowMotor.this._owner.getY(), FollowMotor.this._owner.getZ(), FollowMotor.this._owner.getX(), FollowMotor.this._owner.getY(), FollowMotor.this._owner.getZ(), FollowMotor.this._owner.getHeading(), (byte) 0));
                    //this.pathfindRevalidationTime = 0L;  //pathfindRevalidationTime нельзя менять, оно final
                }
                // Планируем следующее обновление сразу после выполнения текущего
                FollowMotor.this._processor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        FollowMotor.this.update();
                    }
                }, 0L);
            }
            }, TARGET_REVALIDATE_TIME);
				return true;
			} catch (NullPointerException e) {
				// // log.warn("NullPointerException caught in FollowMotor.update(): " + e.getMessage());
				return false;
        }
    }

    /**
     * Проверяет, может ли NPC двигаться (не находится под эффектом страха, может выполнять перемещения, не кастует).
     *
     * @return true, если NPC может двигаться, false в противном случае.
     */
    private boolean canMove() {
        boolean canMove = !_owner.getEffectController().isUnderFear() && _owner.canPerformMove() && _owner.getAi2().getSubState() != AISubState.CAST;
        // log.warn("canMove() - Под страхом: {},  canPerformMove(): {},  getSubState(): {}, canMove: {}", _owner.getEffectController().isUnderFear(), _owner.canPerformMove(), _owner.getAi2().getSubState(), canMove);

        return canMove;
    }
}