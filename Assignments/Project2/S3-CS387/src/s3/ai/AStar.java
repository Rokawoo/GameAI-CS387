package s3.ai;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import s3.base.S3;
import s3.base.S3Map;
import s3.entities.S3PhysicalEntity;
import s3.util.Pair;


public class AStar {
	private static class Node {
    int x, y;
    int g, h;
    Node parent;

    Node(int x, int y, int g, int h, Node parent) {
        this.x = x; this.y = y; this.g = g; this.h = h; this.parent = parent;
    }

    int f() { return g + h; }
	}
	
	public static int pathDistance(double start_x, double start_y, double goal_x, double goal_y,
			S3PhysicalEntity i_entity, S3 the_game) {
		AStar a = new AStar(start_x,start_y,goal_x,goal_y,i_entity,the_game);
		List<Pair<Double, Double>> path = a.computePath();
		if (path!=null) return path.size();
		return -1;
	}

	private int startX, startY, goalX, goalY;
	private S3PhysicalEntity entity;
	private S3 game;

	/*
	Start.g = 0; Start.h = heuristic(Start)
	OPEN = [Start]; CLOSED = []
	WHILE OPEN is not empty
		N = OPEN.removeLowestF()
		IF goal(N) RETURN path to N
		CLOSED.add(N)
		FOR all children M of N not in CLOSED nor OPEN:
			M.parent = N; M.g = N.g + 1; M.h = heuristic(M)
			OPEN.add(M)

	*/
	public AStar(double start_x, double start_y, double goal_x, double goal_y,
			S3PhysicalEntity i_entity, S3 the_game) {
        
		// Casting
		startX 	= (int) start_x;
		startY 	= (int) start_y;
		goalX 	= (int) goal_x;
		goalY 	= (int) goal_y;
		entity 	= i_entity;
		game 	= the_game;
	}

	private int heuristic(int x, int y)  {return Math.abs(x - goalX) + Math.abs(y - goalY); }

	private boolean isFree(int x, int y) {
		S3Map map = game.getMap();
		// stay inside the map (account for the unit's size)
		if (x < 0 || y < 0
				|| x + entity.getWidth() > map.getWidth()
				|| y + entity.getLength() > map.getHeight())
			return false;

		int oldX = entity.getX(), oldY = entity.getY();
		entity.setX(x);
		entity.setY(y);
		boolean free = (game.anyLevelCollision(entity) == null);
		entity.setX(oldX);   // reset
		entity.setY(oldY);
		return free;
	}

	private List<Pair<Double, Double>> buildPath(Node goal) {
		LinkedList<Pair<Double, Double>> path = new LinkedList<>();
		Node cur = goal;
		while (cur.parent != null) {
			path.addFirst(new Pair<>((double) cur.x, (double) cur.y));
			cur = cur.parent;
		}
		return path;
	}

	public List<Pair<Double, Double>> computePath() {
		int w = game.getMap().getWidth();
		int hgt = game.getMap().getHeight();

		// OPEN = [Start]; CLOSED = []
		List<Node> open = new ArrayList<>();
		boolean[][] inOpen = new boolean[w][hgt];
		boolean[][] closed = new boolean[w][hgt];

		open.add(new Node(startX, startY, 0, heuristic(startX, startY), null));
		inOpen[startX][startY] = true;

		int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };

		// WHILE OPEN is not empty
		while (!open.isEmpty()) {
			// N = OPEN.removeLowestF()
			Node n = open.get(0);
			for (Node c : open)
				if (c.f() < n.f()) n = c;
			open.remove(n);

			// IF goal(N) RETURN path to N
			if (n.x == goalX && n.y == goalY) return buildPath(n);

			// CLOSED.add(N)
			closed[n.x][n.y] = true;

			// FOR all children M of N not in CLOSED nor OPEN
			for (int[] d : dirs) {
				int mx = n.x + d[0], my = n.y + d[1];
				if (mx < 0 || my < 0 || mx >= w || my >= hgt) continue;
				if (closed[mx][my] || inOpen[mx][my]) continue;
				if (!isFree(mx, my)) continue;

				// M.parent = N; M.g = N.g + 1; M.h = heuristic(M); OPEN.add(M)
				open.add(new Node(mx, my, n.g + 1, heuristic(mx, my), n));
				inOpen[mx][my] = true;
			}
		}
		return null;   // OPEN ran out: no path
	}


}
