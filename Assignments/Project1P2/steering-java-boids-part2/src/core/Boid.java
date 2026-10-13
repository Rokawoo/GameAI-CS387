package core;

import java.awt.Color;
import java.util.ArrayList;

import javax.vecmath.Vector2d;

import board.Board;


public class Boid {
	public static final Float MASS = 1f;
	public static final double MAX_SPEED = 4;
	public static final int MAX_ACCELERATION = 2;
	// max steer force
	public static final double MAX_FORCE = 1;
	double distance = 10;

	// close enough to checkpoint
	private static final double ARRIVAL_RADIUS = 10;
	// ray len
	private static final double LOOK_AHEAD = 80;
	// ticks w/ no progress = stuck
	private static final int STUCK_TICKS = 150;
	// ray march step
	private static final double RAY_STEP = 2;
	// extra room around obstacles
	private static final int SAFETY_MARGIN = 5;
	// angle between rays
	private static final double RAY_ANGLE_STEP = Math.toRadians(10);
	private static final int MAX_RAY_STEPS = (int) Math.round(Math.PI / RAY_ANGLE_STEP);
	// min speed % when its tight
	private static final double MIN_SPEED_FACTOR = 0.15;

	private Board board;
	private Vector2d centerPosition;
	private Vector2d velocity;
	private Vector2d steerForce;
	private ArrayList<Vector2d> checkpoints;
	private ArrayList<Line> rays;
	// obstacles hit, for drawing
	private ArrayList<Object> rayHits;
	// ray it's actually following when going around something, null if clear
	private Line avoidRay;
	private Vector2d currentCheckpoint;
	private int radius;
	private int currentCheckpointIndex;
	private double distanceToTarget;
	private CollisonCircle collisionCircle;
	// side going around: +1/-1, 0 none
	private int avoidSide;
	// stuck check stuff
	private double bestDistanceToTarget = Double.MAX_VALUE;
	private int ticksWithoutProgress;
	// forced side when escaping
	private int escapeSide;
	private int escapeTicks;

	public Boid(Board b, Vector2d initialPosition, int Radius, ArrayList<Vector2d> checkpoints) {
		board = b;
		centerPosition = initialPosition;
		radius = Radius;
		velocity = new Vector2d();
		steerForce = new Vector2d();
		distanceToTarget = 0d;
		this.checkpoints = checkpoints;
		currentCheckpoint = checkpoints.get(currentCheckpointIndex);
		// clone or update() moves it twice
		collisionCircle = new CollisonCircle((Vector2d) centerPosition.clone(), Radius);
		rays = new ArrayList<Line>();
		rayHits = new ArrayList<Object>();
		avoidSide = 0;
	}

	// path following + avoidance
	// seek checkpoint, next one when reached
	// blocked -> rotate 10deg til clear ray
	// keep same side til clear (no wobble)
	public void objectAvoidance() {
		rays.clear();
		rayHits.clear();

		// 1. reached checkpoint -> next
		if (distance(centerPosition, currentCheckpoint) < ARRIVAL_RADIUS) nextCheckpoint();
		Vector2d toTarget = new Vector2d(currentCheckpoint);
		toTarget.sub(centerPosition);
		distanceToTarget = toTarget.length();

		// 2. stuck?
		checkIfStuck();

		// 3. find open dir, no need to look past checkpoint
		double rayLength = Math.max(Math.min(LOOK_AHEAD, distanceToTarget), radius);
		double targetAngle = Math.atan2(toTarget.getY(), toTarget.getX());
		double moveAngle = findOpenDirection(targetAngle, rayLength);

		// 4. room ahead + actual heading (cant turn instantly), less room = slower
		double room = castRay(moveAngle, rayLength);
		// not going straight at target = avoiding, save that ray to draw green
		avoidRay = (moveAngle != targetAngle) ? rays.get(rays.size() - 1) : null;
		if (velocity.length() > 0.1)
			room = Math.min(room, castRay(Math.atan2(velocity.getY(), velocity.getX()), rayLength));
		double speed = MAX_SPEED * Math.max(MIN_SPEED_FACTOR, Math.min(1, room / rayLength));

		// 5. seek
		seek(moveAngle, speed);
	}

	private void nextCheckpoint() {
		currentCheckpointIndex = (currentCheckpointIndex + 1) % checkpoints.size();
		currentCheckpoint = checkpoints.get(currentCheckpointIndex);
		bestDistanceToTarget = Double.MAX_VALUE;
		escapeTicks = 0;
	}

	// no progress for a while -> try other side
	private void checkIfStuck() {
		if (distanceToTarget < bestDistanceToTarget - 1) {
			bestDistanceToTarget = distanceToTarget;
			ticksWithoutProgress = 0;
		} else if (++ticksWithoutProgress > STUCK_TICKS) {
			escapeSide = (escapeTicks > 0 ? -escapeSide : (avoidSide != 0 ? -avoidSide : 1));
			escapeTicks = 2 * STUCK_TICKS;
			avoidSide = escapeSide;
			bestDistanceToTarget = distanceToTarget;
			ticksWithoutProgress = 0;
		}
		if (escapeTicks > 0) escapeTicks--;
	}

	// clear -> straight. else sweep our side then other til clear
	// none clear -> most room
	private double findOpenDirection(double targetAngle, double rayLength) {
		double bestAngle = targetAngle;
		double bestFree = castRay(targetAngle, rayLength);
		if (bestFree >= rayLength) {
			avoidSide = 0;
			return targetAngle;
		}

		if (avoidSide == 0) avoidSide = pickSide(targetAngle, rayLength);
		for (int side : new int[] {avoidSide, -avoidSide}) {
			for (int step = 1; step <= MAX_RAY_STEPS; step++) {
				double angle = targetAngle + side * step * RAY_ANGLE_STEP;
				double free = castRay(angle, rayLength);
				if (free >= rayLength) return angle;
				if (free > bestFree) {
					bestAngle = angle;
					bestFree = free;
				}
			}
		}
		return bestAngle;
	}

	// pick side w/ open dir closest to target
	// tie -> side we're heading
	private int pickSide(double targetAngle, double rayLength) {
		if (escapeTicks > 0) return escapeSide;
		int headingSide = 1;
		if (velocity.length() > 0.01) {
			double heading = Math.atan2(velocity.getY(), velocity.getX());
			if (Math.sin(heading - targetAngle) < 0) headingSide = -1;
		}
		for (int step = 1; step <= MAX_RAY_STEPS; step++) {
			boolean leftClear = castRay(targetAngle + step * RAY_ANGLE_STEP, rayLength) >= rayLength;
			boolean rightClear = castRay(targetAngle - step * RAY_ANGLE_STEP, rayLength) >= rayLength;
			if (leftClear && rightClear) return headingSide;
			if (leftClear) return 1;
			if (rightClear) return -1;
		}
		return headingSide;
	}

	// seek: desired = dir * speed, steer = desired - vel
	private void seek(double angle, double speed) {
		Vector2d desiredVelocity = new Vector2d(Math.cos(angle), Math.sin(angle));
		desiredVelocity.scale(speed);
		steerForce = desiredVelocity;
		steerForce.sub(velocity);
	}

	// raycast - slide circle (boid r + margin) along ray, check vs obstacles
	// returns free dist (maxLength if no hit)
	// only a hit if moving closer to it, else inside margin = all rays blocked
	// saves ray + hit for drawing
	private double castRay(double angle, double maxLength) {
		ArrayList<Object> objects = board.getObjects();
		Vector2d dir = new Vector2d(Math.cos(angle), Math.sin(angle));
		Vector2d probePosition = new Vector2d();
		CollisonCircle probe = new CollisonCircle(probePosition, getRayRadius());

		double free = maxLength;
		Object hitObject = null;
		for (double d = RAY_STEP; d <= maxLength && hitObject == null; d += RAY_STEP) {
			// center + dir * d
			probePosition.scaleAdd(d, dir, centerPosition);
			for (Object obstacle : objects) {
				if (probe.collision(obstacle.getCollisonCircle())
						&& distance(probePosition, obstacle.getCenterPosition()) < distance(centerPosition, obstacle.getCenterPosition())) {
					hitObject = obstacle;
					free = d - RAY_STEP;
					break;
				}
			}
		}

		Vector2d rayEnd = new Vector2d();
		rayEnd.scaleAdd(free, dir, centerPosition);
		rays.add(new Line(centerPosition.getX(), centerPosition.getY(), rayEnd.getX(), rayEnd.getY()));
		if (hitObject != null && !rayHits.contains(hitObject)) rayHits.add(hitObject);
		return free;
	}

	private static double distance(Vector2d a, Vector2d b) {
		Vector2d difference = new Vector2d(a);
		difference.sub(b);
		return difference.length();
	}

	public void update() {
		ArrayList<Object> objects = board.getObjects();
		// check for collisions:
	    // note: this is an awfully inefficient method, if you ever do a real
	    //       game engine, you need to use a quad-tree or oct-tree to speed
	    //       this up.
		for (int i = 0; i < objects.size(); i++)
			if (collisionCircle.collision(objects.get(i).getCollisonCircle()))
				return;

		objectAvoidance();

		// cap force not normalize, no jitter
		if (steerForce.length() > MAX_FORCE) {
			steerForce.normalize();
			steerForce.scale(MAX_FORCE);
		}
		// acceleration = force / mass
		Vector2d acceleration = new Vector2d(steerForce);
		acceleration.scale(1 / MASS);

		centerPosition.add(velocity);
		// update the collision circle:
		collisionCircle.move(velocity);

		velocity.add(acceleration);
		if (velocity.length()>MAX_SPEED) {
			velocity.normalize();
			velocity.scale(MAX_SPEED);
		}
	}

	public Vector2d getCenterPosition() {
		return centerPosition;
	}

	public Vector2d getSteerForce() {
		return (Vector2d)steerForce.clone();
	}

	public int getX() {
		return (int) centerPosition.getX();
	}

	public int getY() {
		return (int) centerPosition.getY();
	}

	public int getDistanceToTarget() {
		return (int) distanceToTarget;
	}

	public int getRadius() {
		return radius;
	}

	// ray circle r (boid + margin)
	public int getRayRadius() {
		return radius + SAFETY_MARGIN;
	}

	public ArrayList<Line> getRays() {
		return rays;
	}

	public Line getAvoidRay() {
		return avoidRay;
	}

	public ArrayList<Object> getRayHits() {
		return rayHits;
	}

	public CollisonCircle getCollisionCircle() {
		return collisionCircle;
	}

	public int getCurrentCheckpointIndex() {
		return currentCheckpointIndex;
	}
}
