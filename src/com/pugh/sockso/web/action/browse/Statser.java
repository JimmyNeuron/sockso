package com.pugh.sockso.web.action.browse;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.pugh.sockso.Properties;
import com.pugh.sockso.Utils;
import com.pugh.sockso.db.Database;
import com.pugh.sockso.music.Album;
import com.pugh.sockso.templates.web.browse.TStats;
import com.pugh.sockso.web.Request;
import com.pugh.sockso.web.action.BaseAction;

public class Statser extends BaseAction {
	private static final Logger log = Logger.getLogger( Statser.class );
	
	@Override
	public void handleRequest() throws IOException, SQLException {
        showStatser( getAlbums() );
	}
	
	protected void showStatser( final Vector<Album> albums ) throws IOException, SQLException {

        final TStats tpl = new TStats();
        
        final Properties p = getProperties();

        tpl.setAlbums( albums );
        tpl.setProperties( p );

        getResponse().showHtml( tpl );
        
    }

	protected Vector<Album> getAlbums( ) throws SQLException {
        
        ResultSet rs = null;
        PreparedStatement st = null;
                
        try {
            
            final Database db = getDatabase();
            final String sql = " select ar.id as artistid, ar.name as artist, al.id as albumid, al.name as album, al.year as year " +
                               " from albums al " +
                                   " inner join artists ar " +
                                   " on al.artist_id = ar.id " +
                               " where al.year <> '' " +
                               " order by al.year asc ";

            st = db.prepare( sql );

            rs = st.executeQuery();

            final Vector<Album> albums = new Vector<Album>();
            while ( rs.next() )
                albums.addElement( new Album(
                    rs.getInt("artistid"), rs.getString("artist"),
                    rs.getInt("albumid"), rs.getString("album"), rs.getString("year")
                ));

            return albums;
        }
        finally {
            Utils.close( rs );
            Utils.close( st );
        }
	}
}
