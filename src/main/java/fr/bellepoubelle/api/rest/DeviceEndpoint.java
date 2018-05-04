package fr.bellepoubelle.api.rest;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.Crypt;

import fr.bellepoubelle.api.misc.Push;
import fr.bellepoubelle.api.model.Device;
import fr.bellepoubelle.api.model.Account;

/**
 * 
 */
@Stateless
@Path("/devices")
public class DeviceEndpoint {
	@PersistenceContext(unitName = "api-persistence-unit")
	private EntityManager em;
	
	private static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";

	@POST
	@Consumes("application/json")
	public Response create(@HeaderParam("Authorization") String authToken, Device entity) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null) {
			entity.setOwner(null);
		} else {
			entity.setOwner(account.getId());
		}
		
		em.persist(entity);
		
		if (entity.isAndroid()) {
			try {
				Push.toFCM(entity.getToken(),"BellePoubelle","Device registered");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return Response
				.created(URI.create("https://api.bellepoubelle.fr/rest/v1.0/devices/" + (String.valueOf(entity.getId())))).build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	public Response deleteById(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		if (!account.getRole().equals("admin")) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Device entity = em.find(Device.class, id);
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		em.remove(entity);
		return Response.noContent().build();
	}

	@GET
	@Path("/{token}")
	@Produces("application/json")
	public Response findById(@HeaderParam("Authorization") String authToken, @PathParam("token") String token) {
		TypedQuery<Device> findByIdQuery = em
				.createQuery(
						"SELECT DISTINCT d FROM Device d WHERE d.token = :token ORDER BY d.id",
						Device.class);
		findByIdQuery.setParameter("token", token);
		Device entity;
		try {
			entity = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			entity = null;
		}
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok(entity).build();
	}

	@GET
	@Produces("application/json")
	public List<Device> listAll(@HeaderParam("Authorization") String authToken, @QueryParam("start") Integer startPosition,
			@QueryParam("max") Integer maxResult) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			List<Device> results = null;
			return results;
		}

		if (!account.getRole().equals("admin")) {
			List<Device> results = null;
			return results;
		}
		
		TypedQuery<Device> findAllQuery = em.createQuery(
				"SELECT DISTINCT d FROM Device d ORDER BY d.id", Device.class);
		if (startPosition != null) {
			findAllQuery.setFirstResult(startPosition);
		}
		if (maxResult != null) {
			findAllQuery.setMaxResults(maxResult);
		}
		final List<Device> results = findAllQuery.getResultList();
		return results;
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes("application/json")
	public Response update(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id, Device entity) {
				
		if (entity == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (id == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (!id.equals(entity.getId())) {
			return Response.status(Status.CONFLICT).entity(entity).build();
		}
		if (em.find(Device.class, id) == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Account account = getCurrentAccount(authToken);
		if (account == null) {
			entity.setOwner(null);
		} else {
			entity.setOwner(account.getId());
		}		
		
		try {
			entity = em.merge(entity);
		} catch (OptimisticLockException e) {
			return Response.status(Response.Status.CONFLICT)
					.entity(e.getEntity()).build();
		}
		
		if (entity.isAndroid()) {
			try {
				Push.toFCM(entity.getToken(),"MeterReader","Device updated");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return Response.noContent().build();
	}
	
	private Account getCurrentAccount(String authToken) {
		if (authToken == null || authToken.equals("")) {
			return null;
		}
		authToken = authToken.replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
		String decodedString = new String(Base64.getDecoder().decode(authToken));
		StringTokenizer tokenizer = new StringTokenizer(decodedString, ":");
		String email = tokenizer.nextToken();
		String password = tokenizer.nextToken();

		TypedQuery<Account> findByIdQuery = em
				.createQuery("SELECT DISTINCT a FROM Account a WHERE a.email = :entityEmail ORDER BY a.id", Account.class);
		findByIdQuery.setParameter("entityEmail", email);
		Account account;
		try {
			account = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			account = null;
		}
		if (account != null && account.getEmail().equals(email) && account.getPassword().equals(Crypt.crypt(password, account.getPassword()))) {
			return account;
		} else {
			throw new WebApplicationException(401);
		}
	}
}
