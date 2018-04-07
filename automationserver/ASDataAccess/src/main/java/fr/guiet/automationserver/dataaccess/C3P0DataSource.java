package fr.guiet.automationserver.dataaccess;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3P0DataSource {
	
   private static C3P0DataSource _dataSource;
   private ComboPooledDataSource _comboPooledDataSource;

   private C3P0DataSource(String jdbcUrl, String user, String password) {
      try {
         _comboPooledDataSource = new ComboPooledDataSource();
         _comboPooledDataSource
            .setDriverClass("org.postgresql.Driver");
         _comboPooledDataSource
            .setJdbcUrl(jdbcUrl);
         _comboPooledDataSource.setUser(user);
         _comboPooledDataSource.setPassword(password);
         
         _comboPooledDataSource.setMinPoolSize(5); 
         _comboPooledDataSource.setAcquireIncrement(5); 
         _comboPooledDataSource.setMaxPoolSize(20);
      }
         catch (PropertyVetoException ex1) {
         ex1.printStackTrace();
      	}      
   }

   public static C3P0DataSource getInstance(String jdbcUrl, String user, String password) {
      if (_dataSource == null)
         _dataSource = new C3P0DataSource(jdbcUrl, user, password);
      return _dataSource;
   }

   public Connection getConnection() {
      Connection con = null;
      try {
         con = _comboPooledDataSource.getConnection();
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return con;
   }
}