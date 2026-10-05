package core;
import java.util.ArrayList;
import javax.vecmath.Vector2d;
import enums.BehaviorEnum;

public class Boid {
	public static final Float MASS = 1f;
	public static final int MAX_SPEED = 2;
	public static final int MAX_ACCELERATION = 2;
	double distance = 10;
	
	private Vector2d centerPosition;
	private Vector2d velocity;
	private Vector2d steerForce;
	private ArrayList<Vector2d> checkpoints;
	private Vector2d currentCheckpoint;
	private int currentCheckpointIndex;
	private double distanceToTarget;

	public Boid(Vector2d initialPosition, ArrayList<Vector2d> checkpoints) {
		centerPosition = initialPosition;

		velocity = new Vector2d();
		steerForce = new Vector2d();
		this.checkpoints = checkpoints;
	}

	// E is target position
	// P is current position
	// V is current velocity
	// D is vector/distance to target
	// ND is normalized distance to target
	// L is length
	// MS is max speed
	// A is acceleration
	// MA is max acceleration
	// NA is normalized acceleration

	/**
	 * Steers the boid to move toward the target (http://www.red3d.com/cwr/steer/SeekFlee.html).
	 */
	public Vector2d seek(Vector2d targetPosition) {
		Vector2d D = new Vector2d(), ND = new Vector2d(), A = new Vector2d();

		D.sub(targetPosition, centerPosition);	//  D = E - P

		if (D.length() == 0) return new Vector2d(0, 0); //  Extra check: on top of target: no direction

		ND.normalize(D);						// ND = D / |D|
		A.scale(MAX_ACCELERATION, ND);			//  A = ND * maxAcceleration
		return A;
	}
	
	/**
	 * Steers the boid to move away from the target (http://www.red3d.com/cwr/steer/SeekFlee.html).
	 */
	public Vector2d flee(Vector2d targetPosition) {
		Vector2d D = new Vector2d(), ND = new Vector2d(), A = new Vector2d();

		D.sub(targetPosition, centerPosition);	//  D = E - P

		if (D.length() == 0) return new Vector2d(0, 0); //  Extra check: on top of target: no direction

		ND.normalize(D);   						// ND = D / |D|
		A.scale(-MAX_ACCELERATION, ND);			//  A = -ND * maxAcceleration
		return A;
	}
	
	/**
	 * Steers the boid to arrive slowly on the target (http://www.red3d.com/cwr/steer/Arrival.html).
	 */
	public Vector2d arrival(Vector2d targetPosition) {
		Vector2d D = new Vector2d(), ND = new Vector2d(), targetVelocity = new Vector2d(),
			A = new Vector2d(), NA = new Vector2d();
		double targetRadius = 30, slowRadius = targetRadius * 3,
			targetSpeed, time = 1;

		D.sub(targetPosition, centerPosition);	//  D = E - P
		double L = D.length();                  //  L = |D|
		if (L < targetRadius) {
			velocity.set(0, 0); 				// Not part of the function, but makes arrive smoother by stopping the ball
			return new Vector2d(0,0);           //  Return (0,0,0)
		}
		if (L < slowRadius) {					// I inverted the logic here to make more sense
			targetSpeed = MAX_SPEED * L / slowRadius; // TS = MS * L / SR 
		} else {
			targetSpeed = MAX_SPEED;
		}
		ND.normalize(D);						// ND = D / |D|
		targetVelocity.scale(targetSpeed, ND);	// targetVelocity = (D / |D|) * targetSpeed
		A.sub(targetVelocity, velocity);		// A = (targetVelocity - character.velocity)
		A.scale(1.000 / time);					// A = A / time
		if (A.length() > MAX_ACCELERATION) {
			NA.normalize(A);					// A = A / |A|
			NA.scale(MAX_ACCELERATION, NA);		// A = A * maxAcceleration
			A = NA;
		}

		return A;
	}
	/**
	 * Steers the boid to move along a predefined path. 
	 * It follows a set of checkpoint until arrive at the last one, then start over again.
	 */
	public Vector2d pathFollowing() {
		Vector2d D = new Vector2d();
		double L;

		while (currentCheckpointIndex < checkpoints.size()) {
			currentCheckpoint = checkpoints.get(currentCheckpointIndex);

			D.sub(currentCheckpoint, centerPosition); 	//  D = E - P
			L = D.length();								//  L = |D|	
			if (L < distance) {							//  short circuit if ball is already at waypoint
				currentCheckpointIndex += 1;
				if (currentCheckpointIndex == checkpoints.size()) {
					currentCheckpointIndex = 0;
				}
				continue;
			}

			return seek(currentCheckpoint);
		}

		return new Vector2d(0,0);
	}
	
	public void update(Vector2d targetPosition, BehaviorEnum behavior) {
	chooseBehavior(targetPosition, behavior);

		// distance to ball's target this frame
		Vector2d target = (behavior == BehaviorEnum.PATHFOLLOWING) ? currentCheckpoint : targetPosition;
		Vector2d toTarget = new Vector2d();
		toTarget.sub(target, centerPosition);
		distanceToTarget = toTarget.length();

		// acceleration = force / mass
		Vector2d acceleration = new Vector2d();
		if (steerForce.length()>0) {
			steerForce.normalize();
			acceleration.setX(steerForce.getX() / MASS);
			acceleration.setY(steerForce.getY() / MASS);
		}
		
		centerPosition.add(velocity);
		velocity.add(acceleration);
		if (velocity.length()>MAX_SPEED) {
			velocity.normalize();
			velocity.scale(MAX_SPEED);
		}
	}

	private void chooseBehavior(Vector2d targetPosition, BehaviorEnum behavior) {
		switch (behavior) {
		case SEEK:
			steerForce = seek(targetPosition);
			break;
		case FLEE:
			steerForce = flee(targetPosition);
			break;
		case ARRIVAL:
			steerForce = arrival(targetPosition);
			break;
		case PATHFOLLOWING:
			steerForce = pathFollowing();
			break;
		}
	}
	
	public Vector2d getSteerForce() {
		return steerForce;
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
}
