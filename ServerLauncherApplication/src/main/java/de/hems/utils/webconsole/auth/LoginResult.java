package de.hems.utils.webconsole.auth;

/**
 * How a login attempt ended.
 */
public class LoginResult {

    /**
     * The possible outcomes. The password is only ever compared in {@link #OK} and
     * {@link #INVALID_CREDENTIALS} - every other outcome is decided before the password is looked at.
     */
    public enum Status {
        /** The login worked. */
        OK(200),
        /** The request did not carry a user name or a password. */
        INCOMPLETE(400),
        /** No authenticator code was supplied, so the password was not checked at all. */
        TOKEN_REQUIRED(400),
        /** The authenticator code was wrong, so the password was not checked. */
        INVALID_TOKEN(401),
        /** That code was already used for a login, so the password was not checked. */
        TOKEN_ALREADY_USED(401),
        /** Code and password did not match an account. */
        INVALID_CREDENTIALS(401),
        /** Another attempt is still inside its grace period. */
        GRACE_ACTIVE(429),
        /** Nobody can log in because no account is configured. */
        NO_ACCOUNT(503);

        private final int httpStatus;

        Status(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public boolean isSuccess() {
            return this == OK;
        }
    }

    private final Status status;
    private final String message;
    private final Session session;
    /** How long the caller has to wait before trying again, in seconds. */
    private final long retryAfterSeconds;

    private LoginResult(Status status, String message, Session session, long retryAfterSeconds) {
        this.status = status;
        this.message = message;
        this.session = session;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static LoginResult success(Session session) {
        return new LoginResult(Status.OK, "Angemeldet als " + session.getUsername(), session, 0L);
    }

    public static LoginResult failure(Status status, String message) {
        return new LoginResult(status, message, null, 0L);
    }

    public static LoginResult grace(long retryAfterSeconds) {
        return new LoginResult(Status.GRACE_ACTIVE,
                "Bitte " + retryAfterSeconds + " Sekunden warten, der letzte Login läuft noch.",
                null, retryAfterSeconds);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Session getSession() {
        return session;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
