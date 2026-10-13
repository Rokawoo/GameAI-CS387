package core;

import javax.vecmath.Vector2d;

public class CollisonCircle {
	private Vector2d collisionPosition;
	private int radius;
	
	public CollisonCircle(Vector2d CenterPosition, int Radius) {
		collisionPosition = CenterPosition;
		radius = Radius;
	}
	
	public Vector2d getCenterPosition() {
		return collisionPosition;
	}
	
	
	public int getRadius() {
		return radius;
	}
	
	public void move(Vector2d v) {
		collisionPosition.add(v);
	}
	
	public boolean collision(CollisonCircle C) {
		Vector2d distanceVector = new Vector2d(); 
		distanceVector = (Vector2d) collisionPosition.clone();	
		distanceVector.sub(C.getCenterPosition());	
		double distance = distanceVector.length();
	    if (radius + C.getRadius() <= distance  ) return false;
	        else return true;
	    }
	
}

