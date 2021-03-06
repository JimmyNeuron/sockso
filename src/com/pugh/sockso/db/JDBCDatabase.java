
package com.pugh.sockso.db;

import com.pugh.sockso.Constants;
import com.pugh.sockso.Properties;
import com.pugh.sockso.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.File;

import org.apache.log4j.Logger;

public abstract class JDBCDatabase extends AbstractDatabase implements Database {

    private static final Logger log = Logger.getLogger( JDBCDatabase.class );
    
    /**
     *  queries the db
     * 
     */
    
    public ResultSet query( final String sql ) throws SQLException {
        
        return query( sql, true );
        
    }
    
    /**
     *  performs a query to fetch a ResultSet, and closes the statement object
     *  if we've been told to do so
     * 
     *  @param sql
     *  @param closeStatement
     * 
     *  @return
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    protected ResultSet query( final String sql, final boolean closeStatement ) throws SQLException {
        
        Statement st = null;
        ResultSet rs = null;
        
        log.debug( sql );
        
        try {
            st = getConnection().createStatement();
            rs = st.executeQuery( sql );
        }
        
        finally {
            if ( closeStatement ) {
                Utils.close( st );
            }
        }
        
        return rs;
        
    }
    
    /**
     *  updates the db
     * 
     */
    
    public int update( final String sql ) throws SQLException {
        
        Statement st = null;
        
        log.debug( sql );
        
        try {
            
            final Connection cnn = getConnection();
            
            st = cnn.createStatement();

            return st.executeUpdate( sql );

        }
        
        finally {
            Utils.close( st );
        }
        
    }

    /**
     * Execute an sql statement and supress any exceptions
     *
     * @param sql
     *
     * @return
     *
     */
    
    protected boolean safeUpdate( final String sql ) {

        try {
            update( sql );
            return true;
        }

        catch ( final SQLException e ) {
            log.debug( e );
        }

        return false;

    }


    /**
     *  returns a prepared statement for the specified sql
     * 
     *  @param sql
     * 
     *  @return
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    public PreparedStatement prepare( final String sql ) throws SQLException {
        
        final Connection cnn = getConnection();
        
        return cnn.prepareStatement( sql );
        
    }

    /**
     *  this method sets a property to a given value, checking whether it currently
     *  exists and all that boring stuff
     * 
     *  @param name
     *  @param value
     * 
     */
    
    protected boolean setProperty( final String name, final String value ) {

        try {

            if ( propertyExists(name) ) {
                updateProperty( name, value );
            }
            else {
                createProperty( name, value );
            }

            return true;
            
        }

        catch ( final SQLException e ) {
            log.error( e );
        }

        return false;

    }
    
    /**
     *  checks if a property exists and returns a boolean
     * 
     *  @param name
     * 
     *  @return
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    protected boolean propertyExists( final String name ) throws SQLException {

        PreparedStatement st = null;
        ResultSet rs = null;
        
        try {
       
            final String sql = " select 1 " +
                               " from properties p " +
                               " where p.name = ? ";

            st = prepare( sql );
            st.setString( 1, name );
            rs = st.executeQuery();

            return rs.next();

        }
        
        finally {
            Utils.close( rs );
            Utils.close( st );
        }
        
    }

    /**
     *  tries to update a property to a new value, returns a boolean indicating success
     * 
     *  @param name
     *  @param value
     * 
     *  @return
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    protected boolean updateProperty( final String name, final String value ) throws SQLException {

        PreparedStatement st = null;
        ResultSet rs = null;
        
        try {
        
            final String sql = " update properties " +
                               " set value = ? " +
                               " where name = ? ";
            
            st = prepare( sql );
            st.setString( 1, value );
            st.setString( 2, name );
            
            return st.execute();

        }
        
        finally {
            Utils.close( rs );
            Utils.close( st );
        }
        
    }
    
    /**
     *  tries to create a new property, returns a boolean indicating success
     * 
     *  @param name
     *  @param value
     * 
     *  @return
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    protected boolean createProperty( final String name, final String value ) throws SQLException {
        
        PreparedStatement st = null;
        ResultSet rs = null;
        
        try {
        
            final String sql = " insert into properties ( name, value ) " +
                               " values ( ?, ? ) ";
            
            st = prepare( sql );
            st.setString( 1, name );
            st.setString( 2, value );
            
            return st.execute();
            
        }
        
        finally {
            Utils.close( rs );
            Utils.close( st );
        }
        
    }

    /**
     *  sets default properties for the application.  this should really only
     *  be called once when the database is first created.
     * 
     *  @throws java.sql.SQLException
     * 
     */
    
    protected void setDefaultProperties() throws SQLException {

        update( "insert into properties ( name, value ) values ( '" +Constants.SERVER_PORT+ "', '4444' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.SERVER_BASE_PATH+ "', '/' )" );

        update( "insert into properties ( name, value ) values ( '" +Constants.WWW_TITLE+ "', 'Sockso' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.WWW_TAGLINE+ "', 'Personal Music Server' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.WWW_DOWNLOADS_DISABLE+ "', '" +Properties.NO+ "' )" );

        update( "insert into properties ( name, value ) values ( '" +Constants.COLLMAN_SCAN_INTERVAL+ "', 5 )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.COLLMAN_SCAN_ONSTART+ "', '" + Properties.YES + "' )" );

        update( "insert into properties ( name, value ) values ( '" +Constants.APP_CONFIRM_EXIT+ "', '" + Properties.YES + "' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.APP_START_MINIMIZED+ "', '" + Properties.NO + "' )" );

        update( "insert into properties ( name, value ) values ( '" +Constants.LASTFM_KEY+ "', '457dba6ea55d766e65eacd28a927b273' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.LASTFM_SECRET+ "', '610e2b9a553e39a8896a27c1130a523a' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.LASTFM_USERNAME+ "', '' )" );
        update( "insert into properties ( name, value ) values ( '" +Constants.LASTFM_PASSWORD+ "', '' )" );
    }
    
    /**
     *  returns the path to the database
     * 
     *  @return absolute path
     * 
     */

    protected static String getDefaultDatabasePath( final String dbType ) {

        return new File( Utils.getApplicationDirectory() ).getAbsolutePath() + "/database" +dbType;

    }


    /**
     * Checks the albums.year column
     *
     */
    protected void checkAlbumYearColumnExists() {

        final String sql = " alter table albums " +
                           " add year varchar(20) null";

        safeUpdate ( sql );

    }

    /**
     * Creates the users.is_active column if it doesn't exist
     *
     */
    protected void checkUserIsActiveColumnExists() {

        try {

            final String sql = " alter table users " +
                               " add is_active char(1) default '1' not null ";

            update( sql );
            
        }

        catch ( final SQLException e ) {
            log.info( e.getMessage() );
        }

    }

    /**
     * Creates the last.fm columns in users if they do not exist.
     *
     */
    protected void checkLastfmColumnsExist() {
    	try {

            final String sql = " alter table users " +
                               " add " +
            		           "   (lastfm_user varchar(50) not null, " +
                               "    lastfm_pass char(32) not null)";

            update( sql );
            
        }

        catch ( final SQLException e ) {
            log.info( e.getMessage() );
        }
    }
    
    /**
     * Adds Musicbrainz track id to tracks table.
     *
     */
    protected void checkMbTrackIdColumnExist() {
    	try {

            final String sql = " alter table tracks " +
                               " add " +
            		           "   (mbtrackid varchar(36) not null)";

            update( sql );
            
        }

        catch ( final SQLException e ) {
            log.info( e.getMessage() );
        }
    }
}
