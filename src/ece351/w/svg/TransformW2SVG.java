/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Spring 2019 (1195)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.w.svg;

import java.io.PrintWriter;
import java.io.StringWriter;

import ece351.util.Debug;
import ece351.w.ast.WProgram;
import ece351.w.ast.Waveform;

public final class TransformW2SVG {

	public static String transform(final WProgram wp) {
		final StringWriter sw = new StringWriter();
		final PrintWriter out = new PrintWriter(sw);
		transform(wp, out);
		out.close();
		return sw.toString();
	}
	
	public static void transform(final WProgram wp, final PrintWriter out) {
		
		// header
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		out.println("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100%\" height=\"100%\" version=\"1.1\">");
		out.println("<style type=\"text/css\"><![CDATA[line{stroke:#006600;fill:#00 cc00;} text{font-size:\"large\";font-family:\"sans-serif\"}]]></style>");
		out.println();

		final int WIDTH = 100;
		
		int y_mid = 150;
		int y_prev = 150;
		int y_pos =150;
		final int y_off =50;

		// loop on waveforms
		// this line implicitly uses an iterator
		for (final Waveform w : wp.waveforms) {
			// reset the initial x position
			int x = 50;

			// write out the waveform name
			out.println(Pin.toSVG(w.name, x, y_mid));

			// advance the x position to start drawing
			x=100;

			// loop on bits
			for (final String bit : w.bits) {
				// set the y position according to the value of the bit
				// draw the vertical line
				// draw the horizontal line
				// get ready for the next bit
// TODO: longer code snippet
				if (bit == "0") {
					y_pos = y_mid + y_off;
				}
				else if (bit == "1") {
					y_pos = y_mid - y_off;	
				}
				
				if (y_prev != y_pos) {
					//draw the line
					out.println(Line.toSVG(x, y_prev, x, y_pos));
				} else {
					// connecting dot
					out.println(Line.toSVG(x, y_pos, x, y_pos));
				}
				
				y_prev = y_pos;
				out.println(Line.toSVG(x, y_pos, x+WIDTH, y_pos));
				x += WIDTH;
//throw new ece351.util.Todo351Exception();
			}
			
			// advance the y position for the next pin
// TODO: short code snippet
//throw new ece351.util.Todo351Exception();
			y_mid += 200;
			y_prev = y_mid;
			y_pos = y_mid;
		}

		// footer
		out.println("</svg>");
		
	}

	/**
	 * Extra exploration activity (optional).
	 * Try changing the value of this flag and see how it changes the performance
	 * of the test harnesses. Why does that happen? What is the difference
	 * between a DOM-style XML parser and a SAX-style XML parser?
	 */
	public static final boolean USE_DOM_XML_PARSER = true; // TODO: replace this stub

}
