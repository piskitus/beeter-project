package edu.upc.eetac.dsa.marc.beeter.api;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
 
//es un singleton
public class DataSourceSPA {
	private DataSource dataSource; //Referencia al dataSource
	private static DataSourceSPA instance; //Referencia al Singleton, por eso es privado y estático

	private DataSourceSPA() { //Constructor privado, porque es un Singleton
		super();
		Context envContext = null;
		try {
			envContext = new InitialContext();
			Context initContext = (Context) envContext.lookup("java:/comp/env");
			dataSource = (DataSource) initContext.lookup("jdbc/beeterdb"); //Referencia al DataSource se obtiene
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
	}

	public final static DataSourceSPA getInstance() { //Para obetener la instancia del Singleton (sólo habrá 1 única instancia)
		if (instance == null)
			instance = new DataSourceSPA();
		return instance;
	}

	public DataSource getDataSource() { //Obtener el dataSource en sí
		return dataSource;
	}
}