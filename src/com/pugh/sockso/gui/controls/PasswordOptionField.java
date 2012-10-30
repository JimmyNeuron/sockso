package com.pugh.sockso.gui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPasswordField;
import javax.swing.text.DefaultFormatter;

import com.pugh.sockso.Properties;
import com.pugh.sockso.Utils;

public class PasswordOptionField extends JPasswordField implements KeyListener {

	private Thread thread;
    private Properties p;
    protected String name;
    
    public PasswordOptionField( Properties p, String name ) {
    	this.p = p;
        this.name = name;
        setText( p.get(name) );
        addKeyListener( this );
    }
    
	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased( KeyEvent evt ) {
        if ( thread != null ) {
            thread.interrupt();
            thread = null;
        }
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep( 3000 );
                    p.set( name, Utils.md5(new String(getPassword())) );
                    p.save();
                }
                catch ( InterruptedException e ) {}
            }
        };
        thread.start();
    }

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

}
