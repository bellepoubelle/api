package fr.bellepoubelle.api.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import fr.bellepoubelle.api.model.Account;
import fr.bellepoubelle.api.model.Operator;

/**
 * 
 */
@Stateless
@Path("/accounts")
public class AccountEndpoint {
	@PersistenceContext(unitName = "api-persistence-unit")
	private EntityManager em;

	private static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response create(@HeaderParam("Authorization") String authToken, Account entity) {
		if (entity == null || entity.getPassword() == null || entity.getEmail() == null
				|| !isValidEmail(entity.getEmail()) || !isValidPassword(entity.getPassword())) {
			System.out.println(2);
			return Response.status(Status.BAD_REQUEST).build();
		}

		String passwordHashed = Crypt.crypt(entity.getPassword());

		entity.setPassword(passwordHashed);
		String uniqueToken = generateUniqueToken();
		entity.setUniqueToken(uniqueToken);
		entity.setTokenAge(new Date());
		entity.setRegisteredSince(new Date());
		entity.setEnabled(true);

		Account account = getCurrentAccount(authToken);
		if (account != null && account.getRole().equals("admin") && account.isActivated() && account.isEnabled()) {
			if (!entity.getRole().equals("regUser") && !entity.getRole().equals("manager")
					&& !entity.getRole().equals("admin")) {
				return Response.status(Status.FORBIDDEN).build();
			}
			entity.setActivated(true);
		} else if (account != null && account.getRole().equals("manager") && account.isActivated()
				&& account.isEnabled()) {
			if (!entity.getRole().equals("regUser") && !entity.getRole().equals("manager")) {
				return Response.status(Status.FORBIDDEN).build();
			}
			if (entity.getOperator() != account.getOperator()) {
				return Response.status(Status.FORBIDDEN).build();
			}
			entity.setActivated(true);
		} else {
			entity.setRole("regUser");
			entity.setActivated(false);
		}

		Operator operator = getOperator(entity.getOperator());
		if (operator == null) {
			return Response.status(Status.FORBIDDEN).build();
		}

		em.persist(entity);

		Account newAccount = getAccount(entity.getId());
		if (newAccount == null) {
			return Response.status(Status.CONFLICT).build();
		}
		if (!newAccount.isActivated()) {
			Mailing.sendMail(entity.getEmail(), "BellePoubelle - Activate your account",
					getActivationContent(uniqueToken, entity.getId(), entity.getFirstName(), entity.getLastName()));
		} else {
			Mailing.sendMail(entity.getEmail(), "BellePoubelle - Welcome",
					getWelcomeContent(entity.getFirstName(), entity.getLastName()));
		}

		return Response
				.created(URI
						.create("https://api.bellepoubelle.fr/rest/v1.0/accounts/" + (String.valueOf(entity.getId()))))
				.build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	@Produces("application/json")
	public Response deleteById(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id) {

		Account account = getCurrentAccount(authToken);
		if (account == null || !account.getRole().equals("admin") || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.FORBIDDEN).build();
		}

		Account entity = em.find(Account.class, id);
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		em.remove(entity);
		return Response.noContent().build();
	}

	@GET
	@Path("/me")
	@Produces("application/json")
	public Response findCurrentAccount(@HeaderParam("Authorization") String authToken) {
		Account entity = getCurrentAccount(authToken);
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		if (!entity.isActivated() || !entity.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		em.detach(entity);
		entity.setPassword(null);
		entity.setTokenAge(null);
		entity.setUniqueToken(null);

		return Response.ok(entity).build();
	}

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces("application/json")
	public Response findById(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id) {

		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		TypedQuery<Account> findByIdQuery = em.createQuery(
				"SELECT DISTINCT a FROM Account a WHERE a.id = :entityId AND a.enabled = true ORDER BY a.id",
				Account.class);
		findByIdQuery.setParameter("entityId", id);
		Account entity;
		try {
			entity = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			entity = null;
		}
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		if ((account.getRole().equals("regUser") && account.getId() != entity.getId())
				|| (account.getRole().equals("manager") && account.getOperator() != entity.getOperator())) {
			return Response.status(Status.FORBIDDEN).build();
		}

		em.detach(entity);
		entity.setPassword(null);
		entity.setTokenAge(null);
		entity.setUniqueToken(null);

		return Response.ok(entity).build();
	}

	@GET
	@Path("/{id:[0-9][0-9]*}/token/{token}")
	@Produces({ "application/json", "text/html" })
	public Response getTokenPage(@PathParam("id") Long id, @PathParam("token") String token) {

		URI uri;
		try {
			uri = new URI("https://bellepoubelle.fr");
		} catch (URISyntaxException e1) {
			return Response.status(Status.CONFLICT).build();
		}

		Account account = getAccount(id);
		if (account == null) {
			return Response.status(Status.FORBIDDEN).build();
		}
		if (token == null || token.equals("") || account.getUniqueToken() == null
				|| !account.getUniqueToken().equals(token)) {
			return Response.status(Status.FORBIDDEN).build();
		}

		// check token age
		if (account.getTokenAge() == null || account.getTokenAge().after(new Date())) {
			return Response.status(Status.FORBIDDEN).build();
		}
		LocalDate tokenDate = LocalDate
				.parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date(account.getTokenAge().getTime())));
		LocalDate now = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		tokenDate.plusDays(2);
		if (tokenDate.isAfter(now)) {
			return Response.status(Status.FORBIDDEN).build();
		}

		if (!account.isActivated()) {
			account.setActivated(true);
			account.setUniqueToken(null);
			account.setTokenAge(null);
			try {
				account = em.merge(account);
				Mailing.sendMail(account.getEmail(), "BellePoubelle - Welcome",
						getWelcomeContent(account.getFirstName(), account.getLastName()));
				return Response.temporaryRedirect(uri).build();
			} catch (OptimisticLockException e) {
				return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
			}
		} else if (account.getNewEmail() != null && !account.getNewEmail().equals("")
				&& isValidEmail(account.getNewEmail())) {
			account.setEmail(account.getNewEmail());
			account.setNewEmail(null);
			account.setUniqueToken(null);
			account.setTokenAge(null);
			try {
				account = em.merge(account);
				return Response.temporaryRedirect(uri).build();
			} catch (OptimisticLockException e) {
				return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
			}
		} else {
			return Response.status(Status.FORBIDDEN).build();
		}
	}

	@GET
	@Produces("application/json")
	public List<Account> listAll(@HeaderParam("Authorization") String authToken,
			@QueryParam("operator") Long operatorId, @QueryParam("start") Integer startPosition,
			@QueryParam("max") Integer maxResult) {

		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			List<Account> results = null;
			return results;
		}

		TypedQuery<Account> findAllQuery;

		if (account.getRole().equals("regUser")) {
			findAllQuery = em.createQuery(
					"SELECT DISTINCT a FROM Account a WHERE a.id = :accountId AND a.enabled = true ORDER BY a.id",
					Account.class);
			findAllQuery.setParameter("accountId", account.getId());
		} else if (account.getRole().equals("manager")) {
			if (operatorId != null) {
				Operator operator = getOperator(operatorId);
				if (operator != null && account.getOperator().equals(operator.getId())) {
					findAllQuery = em.createQuery(
							"SELECT DISTINCT a FROM Account a WHERE a.operator = :operatorId AND a.enabled = true ORDER BY a.id",
							Account.class);
					findAllQuery.setParameter("operatorId", operatorId);
				} else {
					List<Account> results = null;
					return results;
				}
			} else {
				findAllQuery = em.createQuery(
						"SELECT DISTINCT a FROM Account a WHERE a.operator = :accountOperator AND a.enabled = true ORDER BY a.id",
						Account.class);
				findAllQuery.setParameter("accountOperator", account.getOperator());
			}
		} else if (account.getRole().equals("admin")) {
			if (operatorId != null) {
				findAllQuery = em.createQuery(
						"SELECT DISTINCT a FROM Account a WHERE a.operator = :operatorId AND a.enabled = true ORDER BY a.id",
						Account.class);
				findAllQuery.setParameter("operatorId", operatorId);
			} else {
				findAllQuery = em.createQuery("SELECT DISTINCT a FROM Account a WHERE a.enabled = true ORDER BY a.id",
						Account.class);
			}
		} else {
			List<Account> results = null;
			return results;
		}

		if (startPosition != null) {
			findAllQuery.setFirstResult(startPosition);
		}
		if (maxResult != null) {
			findAllQuery.setMaxResults(maxResult);
		}

		final List<Account> results = findAllQuery.getResultList();

		for (Account entry : results) {
			em.detach(entry);
			entry.setPassword(null);
			entry.setTokenAge(null);
			entry.setUniqueToken(null);
		}

		return results;
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response update(@HeaderParam("Authorization") String authToken, @PathParam("id") Long id, Account entity) {

		Account account = getCurrentAccount(authToken);
		if (account == null || !account.isActivated() || !account.isEnabled()) {
			return Response.status(Status.UNAUTHORIZED).build();
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
		if (em.find(Account.class, id) == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		if ((account.getRole().equals("regUser") && account.getId() != entity.getId())
				|| (account.getRole().equals("manager") && account.getOperator() != entity.getOperator())) {
			return Response.status(Status.FORBIDDEN).build();
		}

		Account accountToUpdate = getAccount(entity.getId());
		if (accountToUpdate == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		// If role updated -> Check if current account is an administrator
		if (entity.getRole() != null && !entity.getRole().equals("")
				&& !entity.getRole().equals(accountToUpdate.getRole()) && !account.getRole().equals("admin")) {
			return Response.status(Status.FORBIDDEN).build();
		}

		// Don't allow changes for email || uniqueToken || tokenAge
		if ((entity.getEmail() != null && !entity.getEmail().equals("")
				&& !entity.getEmail().equals(accountToUpdate.getEmail()))
				|| (entity.getUniqueToken() != null && !entity.getUniqueToken().equals("")
						&& !entity.getUniqueToken().equals(accountToUpdate.getUniqueToken()))
				|| (entity.getTokenAge() != null && !entity.getTokenAge().equals(accountToUpdate.getTokenAge()))) {
			return Response.status(Status.FORBIDDEN).build();
		}

		// If password updated -> HASH new password
		if (entity.getPassword() != null && !entity.getPassword().equals("")
				&& !entity.getPassword().equals(accountToUpdate.getPassword())) {
			String passwordHashed = Crypt.crypt(entity.getPassword());

			if (passwordHashed != accountToUpdate.getPassword()) {
				// Validate password
				if (!isValidPassword(entity.getPassword())) {
					return Response.status(Status.BAD_REQUEST).build();
				}
				entity.setPassword(passwordHashed);
			}
		}

		// If newEmail updated -> generate TOKEN and send MAIL to newEmail
		if (entity.getNewEmail() != null && !entity.getNewEmail().equals("")
				&& !entity.getNewEmail().equals(accountToUpdate.getNewEmail())) {
			// Validate newEmail
			if (!isValidEmail(entity.getNewEmail())) {
				return Response.status(Status.BAD_REQUEST).build();
			}

			String uniqueToken = generateUniqueToken();
			entity.setUniqueToken(uniqueToken);
			entity.setTokenAge(new Date());
			Mailing.sendMail(entity.getNewEmail(), "BellePoubelle - Confirm your mail address",
					getEmailChangeContent(uniqueToken, entity.getId(), entity.getFirstName(), entity.getLastName()));
		}

		// If activated updated to true -> send Welcome Mail
		if (entity.isActivated() && !accountToUpdate.isActivated()) {
			Mailing.sendMail(entity.getEmail(), "BellePoubelle - Welcome",
					getWelcomeContent(entity.getFirstName(), entity.getLastName()));
		}

		// Don't update Password to NULL
		if (entity.getPassword() == null) {
			entity.setPassword(accountToUpdate.getPassword());
		}

		try {
			entity = em.merge(entity);
		} catch (OptimisticLockException e) {
			return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
		}

		return Response.noContent().build();
	}

	private String generateUniqueToken() {
		return UUID.randomUUID().toString();
	}

	private String getActivationContent(String uniqueToken, Long accountID, String firstName, String lastName) {
		String name = "";
		if (firstName != null && lastName != null) {
			name = " " + firstName + " " + lastName;
		}

		String mailContent = "Hello" + name + "," + "<br><br>"
				+ "thanks for your registration on BellePoubelle.<br><br>" + "Follow the link to confirm your account: "
				+ "<a href=\"" + "https://api.bellepoubelle.fr/rest/v1.0/accounts/" + accountID + "/token/" + uniqueToken
				+ "\">Click here</a>";
		return mailContent;
	}

	private String getEmailChangeContent(String uniqueToken, Long accountID, String firstName, String lastName) {
		String name = "";
		if (firstName != null && lastName != null) {
			name = " " + firstName + " " + lastName;
		}

		String mailContent = "Hello" + name + "," + "<br><br>"
				+ "your mail address on BellePoubelle has changed.<br><br>"
				+ "Follow the link to confirm your new mail address: " + "<a href=\""
				+ "https://api.bellepoubelle.fr/rest/v1.0/accounts/" + accountID + "/token/" + uniqueToken
				+ "\">Click here</a>";
		return mailContent;
	}

	private String getWelcomeContent(String firstName, String lastName) {
		String name = "";
		if (firstName != null && lastName != null) {
			name = " " + firstName + " " + lastName;
		}

		String mailContent = "Hello" + name + "," + "<br><br>"
				+ "your account for BellePoubelle has been activated.<br>"
				+ "You can now log in with your email address.";
		return mailContent;
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

		TypedQuery<Account> findByIdQuery = em.createQuery(
				"SELECT DISTINCT a FROM Account a WHERE a.email = :entityEmail ORDER BY a.id", Account.class);
		findByIdQuery.setParameter("entityEmail", email);
		Account account;
		try {
			account = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			account = null;
		}

		if (account != null && account.getEmail().equals(email)
				&& account.getPassword().equals(Crypt.crypt(password, account.getPassword()))) {
			return account;
		} else {
			throw new WebApplicationException(401);
		}
	}

	private Operator getOperator(Long id) {
		TypedQuery<Operator> findByIdQuery = em
				.createQuery("SELECT DISTINCT o FROM Operator o WHERE o.id = :entityId ORDER BY o.id", Operator.class);
		findByIdQuery.setParameter("entityId", id);
		Operator entity;
		try {
			entity = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			entity = null;
		}
		return entity;
	}

	private Account getAccount(Long id) {
		TypedQuery<Account> findByIdQuery = em
				.createQuery("SELECT DISTINCT a FROM Account a WHERE a.id = :entityId ORDER BY a.id", Account.class);
		findByIdQuery.setParameter("entityId", id);
		Account entity;
		try {
			entity = findByIdQuery.getSingleResult();
		} catch (NoResultException nre) {
			entity = null;
		}
		return entity;
	}

	private boolean isValidEmail(String email) {
		Pattern pattern = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
		Matcher matcher = pattern.matcher(email);
		return (matcher.matches() && email.length() < 255);
	}

	private boolean isValidPassword(String password) {
		Pattern pattern = Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,32}$");
		Matcher matcher = pattern.matcher(password);
		return (matcher.matches() && password.length() >= 8);
	}
}
