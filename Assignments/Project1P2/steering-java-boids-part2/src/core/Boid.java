package core;

import java.awt.Color;
import java.util.ArrayList;

import javax.vecmath.Vector2d;

import board.Board;


public class Boid {
	public static final Float MASS = 1f;
	public static final double MAX_SPEED = 4;
	public static final int MAX_ACCELERATION = 2;
	// max magnitude of the steering force applied in one tick
	public static final double MAX_FORCE = 1;
	double distance = 10;

	// how close the boid must get to a checkpoint before moving on to the next one
	private static final double ARRIVAL_RADIUS = 10;
	// how far ahead the rays are cast
	private static final double LOOK_AHEAD = 80;
	// distance between consecutive samples along a ray
	private static final double RAY_STEP = 2;
	// extra clearance kept between the boid and the obstacles
	private static final int SAFETY_MARGIN = 5;
	// angle between two consecutive candidate rays when searching for a free direction
	private static final double RAY_ANGLE_STEP = Math.toRadians(10);
	private static final int MAX_RAY_STEPS = (int) Math.round(Math.PI / RAY_ANGLE_STEP);
	// below this fraction of free distance the boid slows down
	private static final double MIN_SPEED_FACTOR = 0.15;

	private Board board;
	private Vector2d centerPosition;
	private Vector2d velocity;
	private Vector2d steerForce;
	private ArrayList<Vector2d> checkpoints;
	private ArrayList<Line> rays;
	private Vector2d currentCheckpoint;
	private int radius;
	private int currentCheckpointIndex;
	private double distanceToTarget;
	private CollisonCircle collisionCircle;
	// side the boid committed to while going around an obstacle: +1, -1, or 0 (none)
	private int avoidSide;

	public Boid(Board b, Vector2d initialPosition, int Radius, ArrayList<Vector2d> checkpoints) {
		board = b;
		centerPosition = initialPosition;
		radius = Radius;
		velocity = new Vector2d();
		steerForce = new Vector2d();
		distanceToTarget = 0d;
		this.checkpoints = checkpoints;
		currentCheckpoint = checkpoints.get(currentCheckpointIndex);
		// the collision circle gets its own copy of the position, otherwise both
		// update() and collisionCircle.move() would move the same vector each tick
		collisionCircle = new CollisonCircle((Vector2d) centerPosition.clone(), Radius);
		rays = new ArrayList<Line>();
		avoidSide = 0;
	}

	/**
	 * Path following + obstacle avoidance.
	 * The boid seeks the current checkpoint; once it reaches it, it moves on to the next
	 * one (wrapping around after the last). Before moving, rays are cast to find a free
	 * direction: first straight towards the checkpoint, and if that is blocked, rotating
	 * away from it in steps until a clear ray is found. Once the boid picks a side to go
	 * around an obstacle it sticks to it until the way to the checkpoint is clear again,
	 * so it does not oscillate between the two sides.
	 */
	public void objectAvoidance() {
		rays.clear();

		// path following: advance to the next checkpoint when the current one is reached
		Vector2d toTarget = new Vector2d(currentCheckpoint);
		toTarget.sub(centerPosition);
		distanceToTarget = toTarget.length();
		if (distanceToTarget < ARRIVAL_RADIUS) {
			currentCheckpointIndex = (currentCheckpointIndex + 1) % checkpoints.size();
			currentCheckpoint = checkpoints.get(currentCheckpointIndex);
			toTarget = new Vector2d(currentCheckpoint);
			toTarget.sub(centerPosition);
			distanceToTarget = toTarget.length();
		}

		double targetAngle = Math.atan2(toTarget.getY(), toTarget.getX());
		// no need to look past the checkpoint itself
		double rayLength = Math.max(Math.min(LOOK_AHEAD, distanceToTarget), radius);

		double chosenAngle;
		double chosenFree;
		double free = castRay(targetAngle, rayLength);
		if (free >= rayLength) {
			// the way to the checkpoint is clear
			avoidSide = 0;
			chosenAngle = targetAngle;
			chosenFree = free;
		} else {
			if (avoidSide == 0) avoidSide = pickSide(targetAngle, rayLength);

			// rotate away from the target on the committed side until a ray is clear
			chosenAngle = targetAngle;
			chosenFree = free;
			boolean found = false;
			for (int step = 1; step <= MAX_RAY_STEPS && !found; step++) {
				double angle = targetAngle + avoidSide * step * RAY_ANGLE_STEP;
				double f = castRay(angle, rayLength);
				if (f >= rayLength) {
					chosenAngle = angle;
					chosenFree = f;
					found = true;
				} else if (f > chosenFree) {
					// remember the most open direction in case nothing is fully clear
					chosenAngle = angle;
					chosenFree = f;
				}
			}
			if (!found) {
				// boxed in on this side: try the other side too
				for (int step = 1; step < MAX_RAY_STEPS; step++) {
					double angle = targetAngle - avoidSide * step * RAY_ANGLE_STEP;
					double f = castRay(angle, rayLength);
					if (f > chosenFree) {
						chosenAngle = angle;
						chosenFree = f;
					}
				}
			}
		}

		// seek along the chosen direction, slowing down if there is little room ahead
		double speed = MAX_SPEED * Math.max(MIN_SPEED_FACTOR, Math.min(1, chosenFree / rayLength));
		Vector2d desiredVelocity = new Vector2d(Math.cos(chosenAngle), Math.sin(chosenAngle));
		desiredVelocity.scale(speed);
		steerForce = desiredVelocity;
		steerForce.sub(velocity);
	}

	/**
	 * Chooses which side (+1 or -1) to go around an obstacle: the side with the free
	 * direction closest to the target, preferring the side the boid is already heading to.
	 */
	private int pickSide(double targetAngle, double rayLength) {
		double headingBias = 0;
		if (velocity.length() > 0.01) {
			double heading = Math.atan2(velocity.getY(), velocity.getX());
			headingBias = Math.signum(Math.sin(heading - targetAngle));
		}
		for (int step = 1; step <= MAX_RAY_STEPS; step++) {
			boolean leftClear = castRay(targetAngle + step * RAY_ANGLE_STEP, rayLength) >= rayLength;
			boolean rightClear = castRay(targetAngle - step * RAY_ANGLE_STEP, rayLength) >= rayLength;
			if (leftClear && rightClear) return headingBias < 0 ? -1 : 1;
			if (leftClear) return 1;
			if (rightClear) return -1;
		}
		return headingBias < 0 ? -1 : 1;
	}

	/**
	 * Raycast: moves a collision circle of the boid's size (plus a safety margin) along
	 * the ray in small increments and checks it against every obstacle.
	 * Returns the free distance along the ray (maxLength if nothing was hit).
	 * The ray is also stored in "rays" so that it can be drawn.
	 */
	private double castRay(double angle, double maxLength) {
		ArrayList<Object> objects = board.getObjects();
		double dx = Math.cos(angle);
		double dy = Math.sin(angle);
		double free = maxLength;
		for (double d = RAY_STEP; d <= maxLength; d += RAY_STEP) {
			Vector2d probePosition = new Vector2d(centerPosition.getX() + dx * d, centerPosition.getY() + dy * d);
			CollisonCircle probe = new CollisonCircle(probePosition, radius + SAFETY_MARGIN);
			boolean hit = false;
			for (int i = 0; i < objects.size() && !hit; i++)
				if (probe.collision(objects.get(i).getCollisonCircle())) hit = true;
			if (hit) {
				free = d - RAY_STEP;
				break;
			}
		}
		rays.add(new Line(centerPosition.getX(), centerPosition.getY(),
				centerPosition.getX() + dx * free, centerPosition.getY() + dy * free));
		return free;
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
		// acceleration = force / mass
		// (the force is truncated rather than normalized, so that small corrections
		//  stay small and the boid does not jitter around its desired direction)
		Vector2d acceleration = new Vector2d();
		if (steerForce.length()>MAX_FORCE) {
			steerForce.normalize();
			steerForce.scale(MAX_FORCE);
		}
		acceleration.setX(steerForce.getX() / MASS);
		acceleration.setY(steerForce.getY() / MASS);

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

	public ArrayList<Line> getRays() {
		return rays;
	}

	public CollisonCircle getCollisionCircle() {
		return collisionCircle;
	}

	public int getCurrentCheckpointIndex() {
		return currentCheckpointIndex;
	}
}
