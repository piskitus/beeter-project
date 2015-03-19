package edu.upc.eetac.dsa.marc.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.digest.DigestUtils;

import edu.upc.eetac.dsa.marc.beeter.api.model.User;

@Path("/users")
public class UserResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
 
	private final static String GET_USER_BY_USERNAME_QUERY = "select * from users where username=?";
	private final static String INSERT_USER_INTO_USERS = "insert into users values(?, MD5(?), ?, ?)";
	private final static String INSERT_USER_INTO_USER_ROLES = "insert into user_roles values (?, 'registered')";
 
	@POST
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User createUser(User user) {
		validateUser(user);
 
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmtGetUsername = null;
		PreparedStatement stmtInsertUserIntoUsers = null;
		PreparedStatement stmtInsertUserIntoUserRoles = null;
		// Mirar si ya existe ese nombre de usuario
		try {
			stmtGetUsername = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmtGetUsername.setString(1, user.getUsername());
 
			ResultSet rs = stmtGetUsername.executeQuery();
			if (rs.next())
				throw new WebApplicationException(user.getUsername()
						+ " already exists.", Status.CONFLICT);//error 409
			rs.close();
			
			// No se hace commit automático para asegurar que se añade a las 2
			// tablas (users y userroles)
			conn.setAutoCommit(false);
			stmtInsertUserIntoUsers = conn
					.prepareStatement(INSERT_USER_INTO_USERS);
			stmtInsertUserIntoUserRoles = conn
					.prepareStatement(INSERT_USER_INTO_USER_ROLES);
 
			stmtInsertUserIntoUsers.setString(1, user.getUsername());
			stmtInsertUserIntoUsers.setString(2, user.getPassword());
			stmtInsertUserIntoUsers.setString(3, user.getName());
			stmtInsertUserIntoUsers.setString(4, user.getEmail());
			stmtInsertUserIntoUsers.executeUpdate();
 
			stmtInsertUserIntoUserRoles.setString(1, user.getUsername());
			stmtInsertUserIntoUserRoles.executeUpdate();
			
			// Hasta aquí está ejecutado pero no sobre la BD
			// El commit escribe los dos registros definitivamente en la BD
			conn.commit();
		} catch (SQLException e) {
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException e1) {
				}
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmtGetUsername != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUsers != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUserRoles != null)
					stmtGetUsername.close();
				
				// Para poder reutilizar la cnx, activamos el autocommit porque
				// sino no se llegará a cerrar y será inútil en el pool
				conn.setAutoCommit(true);
				conn.close();
			} catch (SQLException e) {
			}
		}
		user.setPassword(null);
		return user;
	}
 
	private void validateUser(User user) {
		if (user.getUsername() == null)
			throw new BadRequestException("username cannot be null.");
		if (user.getPassword() == null)
			throw new BadRequestException("password cannot be null.");
		if (user.getName() == null)
			throw new BadRequestException("name cannot be null.");
		if (user.getEmail() == null)
			throw new BadRequestException("email cannot be null.");
	}
 /////////////////////////////////////////////////////////////////////////
	@Path("/login")
	@POST
	@Produces(MediaType.BEETER_API_USER)
	@Consumes(MediaType.BEETER_API_USER)
	public User login(User user) {
		if (user.getUsername() == null || user.getPassword() == null)
			throw new BadRequestException(
					"username and password cannot be null.");
 
		String pwdDigest = DigestUtils.md5Hex(user.getPassword());
		String storedPwd = getUserFromDatabase(user.getUsername(), true)
				.getPassword();
 
		user.setLoginSuccessful(pwdDigest.equals(storedPwd));
		user.setPassword(null);//Para devolver todos los datos excepto el password
		return user;
	}
	
	// El bool password define si se quiere recuperar o no el pwd de la BD
	private User getUserFromDatabase(String username, boolean password) {
		User user = new User();
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
 
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, username);
 
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user.setUsername(rs.getString("username"));
				if (password)
					user.setPassword(rs.getString("userpass"));
				user.setEmail(rs.getString("email"));
				user.setName(rs.getString("name"));
			} else
				throw new NotFoundException(username + " not found.");
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
 
		return user;
	}
	////////////////////////////////////////////////////////
	
	// Método obtención usuario - cacheable
		@GET
		@Path("/{username}")
		@Produces(MediaType.BEETER_API_USER)
		public Response getUser(@PathParam("username") String username,
				@Context Request request) {
			CacheControl cc = new CacheControl();

			User user = getUserFromDatabase(username, false);

			// Como en este caso no tenemos un last modified que pueda variar,
			// podemos hacer un md5 entre el usuario (invariable) y el email
			// (variable) para ver si el recurso sigue siendo válido
			String eTagDigest = DigestUtils
					.md5Hex(user.getName() + user.getEmail());

			EntityTag eTag = new EntityTag(eTagDigest);

			// Verificar si coincide con el etag de la peticion http
			Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

			if (rb != null) {
				return rb.cacheControl(cc).tag(eTag).build();
			}
			rb = Response.ok(user).cacheControl(cc).tag(eTag); // ok = status 200OK

			return rb.build();
		}
///////////////////////////////////////////////////////////////
		private final static String UPDATE_STING_QUERY = "update users set name=ifnull(?, name), email=ifnull(?, email) where username=?";
		
		
		// Método cambiar datos usuario
		@PUT
		@Path("/{username}")
		@Consumes(MediaType.BEETER_API_USER)
		@Produces(MediaType.BEETER_API_USER)
		public User updateUser(@PathParam("username") String username, User user) {
			validateUser(username);

			validateUpdateUser(user);

			Connection conn = null;
			try {
				conn = ds.getConnection();
			} catch (SQLException e) {
				throw new ServerErrorException("Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}

			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(UPDATE_STING_QUERY);
				stmt.setString(1, user.getName());
				stmt.setString(2, user.getEmail());
				stmt.setString(3, username);

				int rows = stmt.executeUpdate();
				if (rows == 1)
					user = getUserFromDatabase(username, false);
				else {
					throw new NotFoundException("No existe usuario con username="
							+ username);
				}

			} catch (SQLException e) {
				throw new ServerErrorException(e.getMessage(),
						Response.Status.INTERNAL_SERVER_ERROR);
			} finally {
				try {
					if (stmt != null)
						stmt.close();
					conn.close();
				} catch (SQLException e) {
				}
			}

			return user;
		}
		// Método de validación de Update
		private void validateUpdateUser(User user) {
			if (user.getName() != null && user.getName().length() > 70)
				throw new BadRequestException(
						"Name can't be greater than 70 characters.");
			if (user.getEmail() != null && user.getEmail().length() > 255)
				throw new BadRequestException(
						"Email can't be greater than 255 characters.");
		}

		// Esto sirve para poder trabajar con cualquier parámetro de seguridad
		@Context
		private SecurityContext security;

		// Método de validación de usuario
		private void validateUser(String username) {
			if (!security.getUserPrincipal().getName().equals(username))
				throw new ForbiddenException("No permitido");
		}
		
//////////////////////////////////////////////////////////////////////
		private final static String DELETE_USER_QUERY = "delete from users where username=?";
		private final static String IGNORE_FK_QUERY = "SET FOREIGN_KEY_CHECKS=?";
		// Método borrar usuario
		@DELETE
		@Path("/{username}")
		public void deleteSting(@PathParam("username") String username) {
			validateUser(username);

			Connection conn = null;
			try {
				conn = ds.getConnection();
			} catch (SQLException e) {
				throw new ServerErrorException("Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}

			PreparedStatement stmt = null;
			try {
				// Deshabilitar detección de ForeinKey
				stmt = conn.prepareStatement(IGNORE_FK_QUERY);
				stmt.setInt(1, 0);
				stmt.executeUpdate();
				stmt.close();

//				stmt = conn.prepareStatement("ALTER TABLE stings");
//				stmt.executeUpdate();
//				stmt.close();
//				
//				stmt = conn.prepareStatement("drop foreign key(username)");
//				stmt.executeUpdate();
//				stmt.close();
				
				// Pero sí que borra los stings del usuario borrado
				stmt = conn.prepareStatement(DELETE_USER_QUERY);
				stmt.setString(1, username);

				int rows = stmt.executeUpdate();
				if (rows == 0)
					throw new NotFoundException("There's no user with username="
							+ username);

				// Habilitar detección de ForeinKey
				stmt = conn.prepareStatement(IGNORE_FK_QUERY);
				stmt.setInt(1, 1);
				stmt.executeUpdate();
				stmt.close();
			} catch (SQLException e) {
				throw new ServerErrorException(e.getMessage(),
						Response.Status.INTERNAL_SERVER_ERROR);
			} finally {
				try {
					if (stmt != null)
						stmt.close();
					conn.close();
				} catch (SQLException e) {
				}
			}
		}

}

