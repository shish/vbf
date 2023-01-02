/**
 *   Visual BrainF*ck - As useful as Visual Basic, but lots more fun!
 *   Copyright (C) 2003 Shish
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 * 
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;

public class vbf extends Canvas
implements WindowListener, ActionListener, MouseListener, MouseMotionListener,
AdjustmentListener, Runnable {

    private int[] disk = new int[5000];
    private int[][] states = new int[6][5000];
    private int state = 0;
    private int head = 0, selected = 0;

    private Image buf, prbuf, prbufs;
    private Graphics g;
    private double prescale = 0.4;
    private int xDragStart=0, yDragStart=0;
    private int xDragLength=0, yDragLength=0;
    private int xoff=0, yoff=180;
    private int width=0, height=0;

    private Frame frame;
    private TextField edBox;
    private TextArea text;
    private FileDialog fd;
    private byte[] program = new byte[10];
    private byte lastCmd;
    private static final int MODE_B=0, MODE_BXX=1;
    private int mode = MODE_B;
    private Scrollbar scroller;

    private Thread ticker;
    private boolean running = false;
    private boolean paused = false;

    private String fname = null;
    private boolean dirty = false;
    private boolean preview = false;



	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	*                            Load program                             *
	\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public static void main(String[] args) {
	new vbf(args);
    }

    public vbf(String[] args) {
	frame = new Frame("Visual BrainF*ck");
	frame.setLayout(new GridLayout(1,0));
	frame.setIconImage(getToolkit().getImage("icon.bmp"));
	frame.add(makeLeft());
	frame.add(makeRight());
	frame.setMenuBar(makeMenu());
	frame.addWindowListener(this);
	center(frame);
	fd = new FileDialog(frame);
	frame.show();

	addMouseListener(this);
	addMouseMotionListener(this);

	if(args.length >= 1) {
	    program = readFile(args[0]).getBytes();
	    start();
	}
    }


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	*                         Paining & Such                              *
	\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void update(Graphics g) {
	paint(g);
    }

    public void paint(Graphics gtop) {
	if(buf == null || width != getSize().width || height != getSize().height) {
	    buf = createImage(width=getSize().width, height=getSize().height);
	    g = buf.getGraphics();
	}

	if(preview) {
	    g.setColor(Color.black);
	    g.fillRect(0, 0, width, height);
	    g.drawImage(prbufs, xoff, yoff, this);
	    gtop.drawImage(buf, 0, 0, this);
	    return;
	}

	g.setColor(Color.black);
	g.fillRect(0, 0, width, height);

	g.setColor(Color.gray);
	for(int i=(xoff%10); i<width; i+=10) {
	    g.drawLine(i, 0, i, height);
	}
	for(int i=(yoff%10); i<height; i+=10) {
	    g.drawLine(0, i, width, i);
	}

	if((xDragStart | yDragStart) != 0) {
	    g.drawLine(width/2, height/2, xoff, yoff);
	}

	for(int i=0; i<disk.length; i++) {
	    if(disk[i] >= 0) {
		g.setColor(i==head? Color.green:Color.red);
		if(i == selected) g.setColor(Color.yellow);
		g.fillRect(xoff+(i*2), yoff, 2, disk[i]);
	    }
	}

	g.setColor(Color.red);
	g.drawString("Head at "+String.valueOf(head), 5, 25);
	g.drawString("Value is "+String.valueOf(disk[head]), 5, 40);
	g.drawString("Cmd is "+(char)lastCmd, 5, 55);

	gtop.drawImage(buf, 0, 0, this);
    }

    private void print() {
	PrintJob pj = getToolkit().getPrintJob(frame, "vbf-print", null);
	Graphics gr = pj.getGraphics();
	// this LIES! it gives the screen dimension instead of A4
	// maybe need to set "A4" in the properties bit of getPrintJob?
	Dimension d = pj.getPageDimension();
	print(gr, 701, 601);
	pj.end();
    }

    private void printPreview() {
	if(preview) {
	    preview = false;
	    //scroller.setMaximum(800);
	    xoff = 0;
	    yoff = getSize().height/2;
	    repaint();
	}
	else {
	    preview = true;
	    xoff = 20;
	    yoff = 10;
	    prbuf = createImage(700, 900);
	    Graphics gr = prbuf.getGraphics();
	    print(gr, 701, 601);
		/* SCALE_DEFAULT, SCALE_FAST, SCALE_SMOOTH,
		SCALE_REPLICATE, SCALE_AREA_AVERAGING; */
	    prbuf = prbuf.getScaledInstance(210, 270, Image.SCALE_SMOOTH);
	    prbufs = prbuf.getScaledInstance(
		(int)(701.0*prescale),
		(int)(900.0*prescale),
		Image.SCALE_SMOOTH);
	    repaint();
	}
    }

    private void print(Graphics gr, int pw, int ph) {
	gr.setColor(Color.white);
	gr.fillRect(0, 0, width, height);

	gr.setColor(Color.gray);
	for(int i=0; i<ph; i+=10) { //horiz lines
	    gr.drawLine(0, i, pw, i);
	}
	for(int i=0; i<pw; i+=10) { //vert lines
	    gr.drawLine(i, 0, i, ph);
	}

	int end = 0;
	for(int i=disk.length-1; i>0; i--) {
	    if(disk[i] != 0) {
		end=i;
		break;
	    }
	}

	int txoff = 0;
	int tyoff = 150;
	g.setColor(Color.red);
	for(int i=0; i<disk.length; i++) {
	    if(disk[i] > 0) {
		g.fillRect(txoff, tyoff, 2, disk[i]);
	    }
	    txoff += 2;
	    if(txoff > pw) {
		txoff = 0;
		tyoff += 300;
	    }
	}

	gr.setColor(Color.black);
	gr.drawString("Program Listing", 0, 630);
	StringTokenizer st = new StringTokenizer(new String(program), "\n");
	tyoff = 650;
	while(st.hasMoreTokens()) {
	    gr.drawString(st.nextToken(), 0, tyoff+=12);
	}
    }

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	*                         Run the prog                                *
	\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void run() {
	try {
	    for(int i=0; i<disk.length; i++) disk[i] = 0;
	    interpret(program);
	}
	catch(Exception e) {
	    System.out.print("Error interpreting '"+(char)lastCmd+"': ");
	    e.printStackTrace(System.err);
	}
    }

    private void start() {
	running = true;
	if(ticker == null) (ticker = new Thread(this)).start();
    }
    private void stop() {
	running = false;
	ticker = null;
	head = 0;
	disk = new int[5000];
	repaint();
    }
    private void pause() {
	paused = !paused;
    }
    private void step() {
	alert("Not Implemented", "This feature isn't ready yet");
    }

    public void interpret(byte[] prog) throws IOException {
	int point = 0;
	if(prog == null) return;
	while(point < prog.length) {
	    if(!running) break;
	    try {while(paused) Thread.sleep(100);}
	    catch(Exception e) {}

	    lastCmd = prog[point];
	    switch(prog[point]) {
		case '>':
		    try {head++; Thread.sleep(10);}
		    catch(Exception e) {System.err.println("Error in '>': "+e);}
		    break;

		case '<':
		    try {head--; Thread.sleep(10);}
		    catch(Exception e) {System.err.println("Error in '<': "+e);}
		    break;

		case '+':
		    try {disk[head]++; Thread.sleep(10);}
		    catch(Exception e) {System.err.println("Error in '+': "+e);}
		    break;

		case '-':
		    try {disk[head]--; Thread.sleep(10);}
		    catch(Exception e) {System.err.println("Error in '-': "+e);}
		    break;

		case '.':
		    try {System.out.print((char)disk[head]);}
		    catch(Exception e) {System.err.println("Error in '.': "+e);}
		    break;

		case ',':
		    try {disk[head] = System.in.read();}
		    catch(Exception e) {System.err.println("Error in ',': "+e);}
		    break;

		case '[':
		    try {
			int start = point+1;
			int end = point+1;
			int ins=1;
			while(end < prog.length && running) {
			    if(prog[end] == '[') ins++;
			    if(prog[end] == ']') ins--;
			    if(ins == 0) break;
			    end++;
			}
			byte[] sub = new byte[end-start];
			System.arraycopy(prog, start, sub, 0, sub.length);
			while(disk[head] != 0) interpret(sub);
			point = end;
		    }
		    catch(Exception e) {System.err.println("Error in '[': "+e);}
		    break;

		case ']': alert("Bad Brackets",
			"The interpreter ran into an extra ']', "+
			"it is reccomended you restart the program"); break;
	    }
	    point++;
	    repaint();
	}
    }


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	*                             Actions                                 *
	\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void actionPerformed(ActionEvent ae) {
	String com = ae.getActionCommand();
	if(com.equals("file-new")) {
	    fname = null;
	    stop();
	    program = new byte[5000];
	    text.setText("");
	}
	else if(com.equals("file-open")) {
	    fname = getFile(FileDialog.LOAD);
	    if(fname == null) {
		alert("File Not Found", "File is null");
		return;
	    }
	    stop();
	    String file = readFile(fname);
	    program = file.getBytes();
	    text.setText(new String(file));
	}
	else if(com.equals("file-save")) {
	    if(fname == null) fname = getFile(FileDialog.SAVE);
	    if(fname != null) {
		writeFile(fname, new String(program));
		dirty = false;
	    }
	}
	else if(com.equals("file-print")) {print();}
	else if(com.equals("file-printpre")) {printPreview();}
	else if(com.equals("file-exit")) {if(!dirty) System.exit(0);}

	else if(com.equals("run-run")) {start();}
	else if(com.equals("run-pause")) {pause();}
	else if(com.equals("run-stop")) {stop();}
	else if(com.equals("run-step")) {step();}

	else if(com.equals("tools-check")) {
	    int open = 0;
	    for(int i=0; i<program.length; i++) {
		if(program[i] == '[') open++;
		else if(program[i] == ']') open--;
	    }
	    if(open == 0) alert("Brackets Match", "Brackets are OK");
	    else alert("Bracket Error", "Brackets do not match");
	}

	else if(com.equals("mode-b")) {mode = MODE_B;}
	else if(com.equals("mode-b++")) {mode = MODE_BXX;}

	else if(com.equals("state-save")) {states[state] = disk;}
	else if(com.equals("state-load")) {disk = states[state];}
	else if(com.equals("state-1")) {state = 0;}
	else if(com.equals("state-2")) {state = 1;}
	else if(com.equals("state-3")) {state = 2;}
	else if(com.equals("state-4")) {state = 3;}
	else if(com.equals("state-5")) {state = 4;}
	else if(com.equals("state-6")) {state = 5;}

	else if(com.equals("move-left")) {
	    if(selected > 0) selected--;
	    edBox.setText(String.valueOf(disk[selected]));
	}
	else if(com.equals("move-right")) {
	    if(selected < disk.length-1) selected++;
	    edBox.setText(String.valueOf(disk[selected]));
	}
	else {
	    try {
		disk[selected] = Integer.parseInt(com);
	    }
	    catch(Exception e) {
		alert("Bad Number", "The number "+com+" is invalid");
	    }
	}
    }

    private String getFile(int prompt) {
	fd.setMode(prompt);
	fd.show();
	return fd.getFile();
    }

    private String readFile(String fname) {
	try {
	    StringBuffer sb = new StringBuffer();
	    BufferedReader br = new BufferedReader(new FileReader(fname));
	    String s;
	    while(true) {
		s = br.readLine();
		if(s == null) break;
		sb.append(s);
		sb.append("\n");
	    }
	    br.close();
	    return sb.toString();
	}
	catch(FileNotFoundException e) {
	    alert("File Not Found", "File "+fname+" not found");
	}
	catch(Exception e) {
	    alert("Error Reading File", "Error loading "+fname+" - "+e);
	    e.printStackTrace(System.err);
	}
	return null;
    }

    private void writeFile(String fname, String data) {
	try {
	    FileOutputStream fos = new FileOutputStream(fname);
	    fos.write(data.getBytes());
	    fos.flush();
	    fos.close();
	}
	catch(Exception e) {
	    alert("Error Writing File", "Couldn't Write File - "+e);
	}
    }


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	*                       AWT Frame / Panel Maker                       *
	\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private void alert(String title, String text) {
	//System.err.println("*** "+title+"\n* "+text);
	Dialog di = new Dialog(frame, title);
	di.addWindowListener(this);
	di.setLayout(new BorderLayout());
	di.add(new Label(text), BorderLayout.CENTER);

	//Panel p1 = new Panel(new FlowLayout(FlowLayout.CENTER));
	//p1.add(new Button("OK"));
	//di.add(p1, BorderLayout.SOUTH);

	center(di);
	di.show();
    }

    private Panel makeLeft() {
	Panel p3 = new Panel(new GridLayout(1,0));
	p3.add(makeButton("<", "move-left"));
	p3.add(makeButton(">", "move-right"));

	Panel p2 = new Panel(new BorderLayout());
	edBox = new TextField("0");
	edBox.addActionListener(this);
	p2.add(edBox, BorderLayout.CENTER);
	p2.add(p3, BorderLayout.EAST);

	Panel p4 = new Panel(new BorderLayout());
	p4.add(this, BorderLayout.CENTER);
	scroller = new Scrollbar(Scrollbar.HORIZONTAL);
	scroller.setMaximum(800); //in theory should be (disk.length*2) to get 1 to 1 scroll
	scroller.addAdjustmentListener(this);
	p4.add(scroller, BorderLayout.SOUTH);

	Panel p = new Panel(new BorderLayout());
	p.add(p4, BorderLayout.CENTER);
	p.add(p2, BorderLayout.SOUTH);
	return p;
    }

    private Panel makeRight() {
	//Panel p2 = new Panel(new CardLayout());
	//p2.add(new Button("Run"), "SET AREA");
	//p2.add(new Button("Step"), "STEP");

	Panel p = new Panel(new BorderLayout());
	p.add(text = new TextArea(10, 50), BorderLayout.CENTER);
	//p.add(p2, BorderLayout.SOUTH);
	return p;
    }

    public Dimension getPreferredSize() {
	return new Dimension(300, 400);
    }

    private MenuBar makeMenu() {
	MenuBar mb = new MenuBar();
	Menu me;

	me = new Menu("File");
	me.add(makeMenuItem("New",  "file-new"));
	me.add(makeMenuItem("Open", "file-open"));
	me.add(makeMenuItem("Save", "file-save"));
	me.addSeparator();
	me.add(makeMenuItem("Print Preview", "file-printpre"));
	me.add(makeMenuItem("Print", "file-print"));
	me.addSeparator();
	me.add(makeMenuItem("Exit", "file-exit"));
	mb.add(me);

	me = new Menu("Run");
	me.add(makeMenuItem("Start", "run-run"));
	me.add(makeMenuItem("Pause", "run-pause"));
	me.add(makeMenuItem("Stop", "run-stop"));
	me.addSeparator();
	me.add(makeMenuItem("Step", "run-step"));
	mb.add(me);

	me = new Menu("Tools");
	me.add(makeMenuItem("Bracket Check", "tools-check"));
	mb.add(me);

	me = new Menu("Mode");
	me.add(makeMenuItem("B", "mode-b"));
	me.add(makeMenuItem("B++", "mode-b++"));
	mb.add(me);

	me = new Menu("State");
	me.add(makeMenuItem("Save", "state-save"));
	me.add(makeMenuItem("Load", "state-load"));
	me.addSeparator();
	me.add(makeMenuItem("1", "state-1"));
	me.add(makeMenuItem("2", "state-2"));
	me.add(makeMenuItem("3", "state-3"));
	me.add(makeMenuItem("4", "state-4"));
	me.add(makeMenuItem("5", "state-5"));
	me.add(makeMenuItem("6", "state-6"));
	mb.add(me);

	return mb;
    }

    private MenuItem makeMenuItem(String text, String com) {
	MenuItem mi = new MenuItem(text);
	mi.setActionCommand(com);
	mi.addActionListener(this);
	return mi;
    }

    private Button makeButton(String text, String com) {
	Button b = new Button(text);
	b.setActionCommand(com);
	b.addActionListener(this);
	return b;
    }

    private static void center(Window win) {
	int x,y,h,w;
	Dimension d;
	win.pack();
	d = win.getSize();
	w = d.width;
	h = d.height;
	d = Toolkit.getDefaultToolkit().getScreenSize();
	x = d.width;
	y = d.height;
	win.setLocation( (x-w)/2 , (y-h)/2 );
    }

    private int round(int num, int mod) {
	if(num % mod == 0) return num;
	int dif = num%mod;
	if(dif > mod/2) num += mod-dif;
	else num -= dif;
	return num;
    }

    public void adjustmentValueChanged(AdjustmentEvent ae) {
	if(preview) {
	    xoff = 20;
	    yoff = 10;
	    prescale = (double)(ae.getValue()+1)/4.0;
	    if(prescale > 1.0) return; //maybe 2.0?
	    prbufs = prbuf.getScaledInstance(
		(int)(700.0*prescale),
		(int)(900.0*prescale),
		Image.SCALE_SMOOTH);
	    repaint();
	    return;
	}
	else {
	    yoff = getSize().height/2;
	    xoff = -ae.getValue();
	}
	repaint();
    }

    public void mousePressed(MouseEvent me) {
	//frame.setCursor(Frame.MOVE_CURSOR);
	xDragStart = me.getX() + xoff;
	yDragStart = me.getY() + yoff;
	repaint();
    }
    public void mouseReleased(MouseEvent me) {
	//frame.setCursor(Frame.DEFAULT_CURSOR);
	xoff = round(xoff, 10);
	yoff = round(yoff, 10);
	xDragStart = 0; // to tell if we're moving or not
	yDragStart = 0;
	repaint();
    }
    public void mouseClicked(MouseEvent me) {
	int x = (me.getX()-xoff)/2;
	if(x >= 0 && x < disk.length) selected = x;
    }
    public void mouseEntered(MouseEvent me) {}
    public void mouseExited(MouseEvent me) {}

    public void mouseMoved(MouseEvent me) {}
    public void mouseDragged(MouseEvent me) {
	xDragLength = (me.getX() + xoff) - xDragStart;
	yDragLength = (me.getY() + yoff) - yDragStart;
	xoff += xDragLength;
	yoff += yDragLength;
	//if(xoff > 10*TWIDTH)  xoff = 10*TWIDTH;
	//if(yoff > 10*THEIGHT) yoff = 10*THEIGHT;
	//if(xoff < (11-BWIDTH)*TWIDTH)   xoff = (11-BWIDTH)*TWIDTH;
	//if(yoff < (11-BHEIGHT)*THEIGHT) yoff = (11-BHEIGHT)*THEIGHT;
	xDragStart = me.getX() + xoff;
	yDragStart = me.getY() + yoff;
	repaint();
    }

    public void windowOpened(WindowEvent we) {}
    public void windowClosing(WindowEvent we) {
	if(we.getWindow() == frame) System.exit(0);
	else we.getWindow().dispose();
    }
    public void windowClosed(WindowEvent we) {}

    public void windowMinimized(WindowEvent we) {}
    public void windowMaximized(WindowEvent we) {}

    public void windowActivated(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}

    public void windowIconified(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
}
