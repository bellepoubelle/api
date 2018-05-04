package fr.bellepoubelle.api.rest;

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

import javax.ws.rs.core.UriBuilder;

import fr.bellepoubelle.api.model.Poubelle;
import fr.bellepoubelle.api.model.Sensor;

/**
 * 
 */
@Stateless
@Path("/poubelles")
public class PoubelleEndpoint {
	@PersistenceContext(unitName = "api-persistence-unit")
	private EntityManager em;
	
	private static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";

	@POST
	@Consumes("application/json")
	public Response create(Poubelle entity) {
		em.persist(entity);
		return Response.created(
				UriBuilder.fromResource(PoubelleEndpoint.class)
						.path(String.valueOf(entity.getId())).build()).build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	public Response deleteById(@PathParam("id") Long id) {
		Poubelle entity = em.find(Poubelle.class, id);
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		em.remove(entity);
		return Response.noContent().build();
	}

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces("application/json")
	public Response findById(@PathParam("id") Long id) {
		TypedQuery<Poubelle> findByIdQuery = em
				.createQuery(
						"SELECT DISTINCT p FROM Poubelle p WHERE p.id = :entityId ORDER BY p.id",
						Poubelle.class);
		findByIdQuery.setParameter("entityId", id);
		Poubelle entity;
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
	@Path("/{id:[0-9][0-9]*}/fillingLevel/{fillingLevel:[0-9][0-9]*}")
	@Produces({ "application/json", "text/html" })
	public Response getFillingLevelPage(@PathParam("id") Long id, @PathParam("fillingLevel") int fillingLevel) {
		/**
		System.out.println("1");
		
		Sensor sensor = getCurrentSensor(authToken);
		if (sensor == null || !sensor.getId().equals(id)) {
			System.out.println("2");
			return Response.status(Status.UNAUTHORIZED).build();
		} */
		
		System.out.println("3");
		
		Poubelle entity = getPoubelle(id);
		
		System.out.println("4");

		if (entity == null) {
			System.out.println("5");
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (id == null) {
			System.out.println("6");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		System.out.println("7");
		
		entity.setFillingLevel(fillingLevel);
		
		if (!id.equals(entity.getId())) {
			System.out.println("8");
			return Response.status(Status.CONFLICT).entity(entity).build();
		}
		if (em.find(Poubelle.class, id) == null) {
			System.out.println("9");
			return Response.status(Status.NOT_FOUND).build();
		}
		try {
			System.out.println("10");
			entity = em.merge(entity);
		} catch (OptimisticLockException e) {
			System.out.println("11");
			return Response.status(Response.Status.CONFLICT)
					.entity(e.getEntity()).build();
		}

		System.out.println("12");
		return Response.noContent().build();

	}

	@GET
	@Produces("application/json")
	public List<Poubelle> listAll(@QueryParam("start") Integer startPosition,
			@QueryParam("max") Integer maxResult) {
		TypedQuery<Poubelle> findAllQuery = em.createQuery(
				"SELECT DISTINCT p FROM Poubelle p ORDER BY p.id",
				Poubelle.class);
		if (startPosition != null) {
			findAllQuery.setFirstResult(startPosition);
		}
		if (maxResult != null) {
			findAllQuery.setMaxResults(maxResult);
		}
		final List<Poubelle> results = findAllQuery.getResultList();
		return results;
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes("application/json")
	public Response update(@PathParam("id") Long id, Poubelle entity) {
		if (entity == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (id == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (!id.equals(entity.getId())) {
			return Response.status(Status.CONFLICT).entity(entity).build();
		}
		if (em.find(Poubelle.class, id) == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		try {
			entity = em.merge(entity);
		} catch (OptimisticLockException e) {
			return Response.status(Response.Status.CONFLICT)
					.entity(e.getEntity()).build();
		}

		return Response.noContent().build();
	}
	
	private Poubelle getPoubelle(Long id) {
		TypedQuery<Poubelle> findByIdQuery = em
				.createQuery("SELECT DISTINCT p FROM Poubelle p WHERE p.id = :entityId ORDER BY p.id", Poubelle.class);
		findByIdQuery.setParameter("entityId", id);
		Poubelle entity;
		try {
			entity = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			entity = null;
		}
		return entity;
	}
	
	private Sensor getCurrentSensor(String authToken) {
		if (authToken == null || authToken.equals("")) {
			return null;
		}
		authToken = authToken.replaceFirst(AUTHORIZATION_HEADER_PREFIX, "");
		String decodedString = new String(Base64.getDecoder().decode(authToken));
		StringTokenizer tokenizer = new StringTokenizer(decodedString, ":");
		String id = tokenizer.nextToken();
		String password = tokenizer.nextToken();

		TypedQuery<Sensor> findByIdQuery = em.createQuery(
				"SELECT DISTINCT s FROM Sensor s WHERE s.id = :entityId ORDER BY s.id", Sensor.class);
		findByIdQuery.setParameter("entityId", id);
		Sensor sensor;
		try {
			sensor = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			sensor = null;
		}

		if (sensor != null && sensor.getId().equals(id)
				&& sensor.getPassword().equals(Crypt.crypt(password, sensor.getPassword()))) {
			return sensor;
		} else {
			throw new WebApplicationException(401);
		}
	}
}
