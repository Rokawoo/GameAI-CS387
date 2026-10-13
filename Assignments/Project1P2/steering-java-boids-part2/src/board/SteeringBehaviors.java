package board;
import javax.swing.JFrame;

public class SteeringBehaviors extends JFrame {
	
    public SteeringBehaviors() {
    	Board board = new Board();
        add(board);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 750);
        setLocationRelativeTo(null);
        setTitle("Java Steering Behaviors");
        setResizable(false);
        setVisible(true);
    }

    public static void main(String[] args) {
        new SteeringBehaviors();
    }
}