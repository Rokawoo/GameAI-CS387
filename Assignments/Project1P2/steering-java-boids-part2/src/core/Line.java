package core;

import java.awt.Graphics2D;

/**
 *
 * @author Ehsan Khosroshahi
 */

public class Line{
    
	double m_x , m_y;
	double m_endx, m_endy;
	
    public Line(double x, double y, double endx, double endy){
        m_x = x;
        m_y = y;
        m_endx = endx;
        m_endy = endy;
    }
    
      
    public void draw(Graphics2D g) {
        g.drawLine((int)m_x, (int)m_y, (int)m_endx, (int)m_endy);     
        
    }

}
