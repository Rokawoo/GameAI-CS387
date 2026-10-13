 package board;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.vecmath.Vector2d;

import core.Boid;
import core.Line;
import core.Object;

public class Board extends JPanel implements ActionListener{
	
	private Timer timer;
	private Boid boid;
	private ArrayList<Object> objects;
	private Graphics2D graphics2d;
	private ArrayList<Vector2d> checkpoints;

	public Board() {
		
		setFocusable(true);
		setBackground(Color.WHITE);
		setDoubleBuffered(true);
		loadRandomObjects(20);
		loadPredefinedPath();
		boid = new Boid(this, new Vector2d(20, 50), 10, checkpoints);
		
		timer = new Timer(40, this);
		timer.start();
		
	}

	public void paint(Graphics g) {
		super.paint(g);
		graphics2d = (Graphics2D) g;
		drawRays();
		drawCircles();
		drawRayHits();
		drawInfoRect();
		drawCheckPointNumbers();
		Toolkit.getDefaultToolkit().sync();
		g.dispose();
	}

	private void drawInfoRect() {
		graphics2d.setColor(Color.LIGHT_GRAY);
		graphics2d.fillRect(0, 0, 800, 30);
		graphics2d.setColor(Color.BLACK);
		graphics2d.drawString("Current behavior: " + "Object Avoidance", 10, 20);
		graphics2d.drawString("Current checkpoint: " + boid.getCurrentCheckpointIndex(), 250, 20);
		graphics2d.drawString("Distance to the target:" + boid.getDistanceToTarget(), 600, 20);
	}

	private void drawCheckPointNumbers() {
		for (int checkpointIndex = 0; checkpointIndex < checkpoints.size(); checkpointIndex++) {
			// target = green
			graphics2d.setColor(checkpointIndex == boid.getCurrentCheckpointIndex() ? Color.GREEN.darker() : Color.BLACK);
			graphics2d.fillOval((int) checkpoints.get(checkpointIndex).getX() - 3, (int) checkpoints.get(checkpointIndex).getY() - 3, 6, 6);
			graphics2d.drawString(
				"(" + checkpointIndex + ")",
				(int) checkpoints.get(checkpointIndex).getX(),
				(int) checkpoints.get(checkpointIndex).getY());
		}
	}

	private void drawRays() {
		graphics2d.setColor(Color.LIGHT_GRAY);
		for (Line ray : boid.getRays())
			ray.draw(graphics2d);
		// blue line to target
		Vector2d target = checkpoints.get(boid.getCurrentCheckpointIndex());
		graphics2d.setColor(Color.BLUE);
		graphics2d.drawLine(boid.getX(), boid.getY(), (int) target.getX(), (int) target.getY());
		// path its taking around an obstacle = green
		if (boid.getAvoidRay() != null) {
			graphics2d.setColor(Color.GREEN.darker());
			boid.getAvoidRay().draw(graphics2d);
		}
	}

	// red outline = obstacle r + ray circle r
	// what the ray line actually hits
	private void drawRayHits() {
		graphics2d.setColor(Color.RED);
		for (Object hit : boid.getRayHits()) {
			int r = hit.getRadius() + boid.getRayRadius();
			graphics2d.drawOval(hit.getX() - r, hit.getY() - r, r * 2, r * 2);
		}
	}

	private void drawCircles() {
		graphics2d.setColor(Color.BLUE);
		graphics2d.fillOval(boid.getX()-boid.getRadius(), boid.getY()-boid.getRadius(), boid.getRadius()*2, boid.getRadius()*2);
		graphics2d.setColor(Color.RED);
		for(int objectIndex = 0; objectIndex < objects.size(); objectIndex++) {
			int radius = objects.get(objectIndex).getRadius();
			graphics2d.fillOval(objects.get(objectIndex).getX()-radius, objects.get(objectIndex).getY()-radius, radius*2, radius*2);
		}
	}
	
	
	private void loadPredefinedPath() {
		checkpoints = new ArrayList<Vector2d>();
		checkpoints.add(new Vector2d(10, 50));
		checkpoints.add(new Vector2d(770, 100));
		checkpoints.add(new Vector2d(10, 200));
		checkpoints.add(new Vector2d(770, 300));
		checkpoints.add(new Vector2d(10, 400));
		checkpoints.add(new Vector2d(770, 500));
		checkpoints.add(new Vector2d(10, 600));
		checkpoints.add(new Vector2d(770, 700));
	}

	private void loadRandomObjects(int numberOfObjects) {
        Random random = new Random();
        objects = new ArrayList<Object>();
		
        for (int i = 0; i < numberOfObjects; i++) {	
			int x = random.nextInt(600)+75;
			int y = random.nextInt(600)+50;
			int radius = random.nextInt(10)+15;
			objects.add(new Object(new Vector2d(x,y), radius));
		}
	}
	
	public ArrayList<Object> getObjects() {
		return objects;
	}
	
	public void actionPerformed(ActionEvent e) {
		boid.update();
		repaint();
	}
}
