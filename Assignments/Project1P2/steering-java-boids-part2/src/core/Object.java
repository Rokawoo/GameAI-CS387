package core;

import javax.vecmath.Vector2d;


public class Object {
	private Vector2d centerPosition;
	private int radius;
	private CollisonCircle collisonCircle;
	
	
	public Object(Vector2d Position, int Radius) {
		centerPosition = Position;
		radius = Radius;
		collisonCircle = new CollisonCircle(Position, Radius);
	}
	
	
	public int getX() {
		return (int) centerPosition.getX();
	}

	public int getY() {
		return (int) centerPosition.getY();
	}
	
	public int getRadius() {
		return radius;
	}
	
	public Vector2d getCenterPosition() {
		return centerPosition;
	}
	
	public CollisonCircle getCollisonCircle() {
		return collisonCircle;
	}
	
}
