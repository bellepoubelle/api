package fr.bellepoubelle.api.rest;

import java.net.URI;
import java.util.Base64;
import java.util.Date;
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

import fr.bellepoubelle.api.misc.Mailing;
import fr.bellepoubelle.api.misc.Push;
import fr.bellepoubelle.api.model.Device;
import fr.bellepoubelle.api.model.Operator;
import fr.bellepoubelle.api.model.Post;
import fr.bellepoubelle.api.model.Account;

/**
 * 
 */
@Stateless
@Path("/posts")
public class PostEndpoint {
	@PersistenceContext(unitName = "api-persistence-unit")
	private EntityManager em;
	
	private static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";

	@POST
	@Consumes("application/json")
	public Response create(@HeaderParam("Authorization") String authToken, Post entity) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (account.getRole().equals("regUser")) {
			return Response.status(Status.FORBIDDEN).build();
		} else if (account.getRole().equals("manager")) {
			entity.setOperator(account.getOperator());
		}
		
		entity.setAuthor(account.getId());
		entity.setEnabled(true);
		entity.setDate(new Date());
		
		em.persist(entity);
		
		// send push notifications and e-mails to users
		Operator operator = getOperator(entity.getOperator());
		List<Account> receivers = getAccountsByOperator(entity.getOperator());
		for (Account receiver : receivers) {

			// e-mail
			Mailing.sendMail(receiver.getEmail(), entity.getTitle(), entity.getContent());

			// push notifications
			List<Device> devices = getDevicesByAccount(receiver.getId());
			for (Device device : devices) {
				try {
					Push.toFCM(device.getToken(), operator.getName(), entity.getTitle());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		return Response
				.created(URI.create("https://api.bellepoubelle.fr/rest/v1.0/posts/" + (String.valueOf(entity.getId())))).build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	public Response deleteById(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (account.getRole().equals("regUser")) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Post entity = em.find(Post.class, id);
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		if (account.getRole().equals("manager") && !entity.getOperator().equals(account.getOperator())) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		em.remove(entity);
		return Response.noContent().build();
	}

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces("application/json")
	public Response findById(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id) {
		TypedQuery<Post> findByIdQuery = em
				.createQuery(
						"SELECT DISTINCT p FROM Post p WHERE p.id = :entityId AND p.enabled = true ORDER BY p.id",
						Post.class);
		findByIdQuery.setParameter("entityId", id);
		Post entity;
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
	public List<Post> listAll(@HeaderParam("Authorization") String authToken, @QueryParam("start") Integer startPosition,
			@QueryParam("max") Integer maxResult) {
		TypedQuery<Post> findAllQuery = em.createQuery(
				"SELECT DISTINCT p FROM Post p WHERE p.enabled = true ORDER BY p.id DESC", Post.class);
		if (startPosition != null) {
			findAllQuery.setFirstResult(startPosition);
		}
		if (maxResult != null) {
			findAllQuery.setMaxResults(maxResult);
		}
		final List<Post> results = findAllQuery.getResultList();
		return results;
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes("application/json")
	public Response update(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id, Post entity) {
		
		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (account.getRole().equals("regUser")) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		if (entity == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (id == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (!id.equals(entity.getId())) {
			return Response.status(Status.CONFLICT).entity(entity).build();
		}
		if (em.find(Post.class, id) == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		if (account.getRole().equals("manager") && !entity.getOperator().equals(account.getOperator())) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		try {
			entity = em.merge(entity);
		} catch (OptimisticLockException e) {
			return Response.status(Response.Status.CONFLICT)
					.entity(e.getEntity()).build();
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
	
	private Operator getOperator(Long id) {
		TypedQuery<Operator> findByIdQuery = em
				.createQuery("SELECT DISTINCT o FROM Operator o WHERE o.id = :operatorId ORDER BY o.id", Operator.class);
		findByIdQuery.setParameter("operatorId", id);
		Operator operator;
		try {
			operator = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			operator = null;
		}
		return operator;
	}
	
	private List<Account> getAccountsByOperator(Long operator) {
		TypedQuery<Account> findAllQuery;
		findAllQuery = em.createQuery("SELECT DISTINCT a FROM Account a WHERE a.operator = :operator AND a.enabled = true ORDER BY a.id", Account.class);
		findAllQuery.setParameter("operator", operator);
		final List<Account> results = findAllQuery.getResultList();
		return results;
	}
	
	private List<Device> getDevicesByAccount(Long account) {
		TypedQuery<Device> findAllQuery;
		findAllQuery = em.createQuery("SELECT DISTINCT d FROM Device d WHERE d.owner = :account AND d.android = true ORDER BY d.id", Device.class);
		findAllQuery.setParameter("account", account);
		final List<Device> results = findAllQuery.getResultList();
		return results;
	}
}
