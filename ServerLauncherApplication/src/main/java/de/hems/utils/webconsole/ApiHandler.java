package de.hems.utils.webconsole;

/**
 * What answers one route of the admin website.
 * <p>
 * Anything thrown out of here is turned into a json error answer by {@link WebServer}, so a handler only
 * has to describe the happy path.
 */
@FunctionalInterface
public interface ApiHandler {

    /**
     * Answers one request.
     *
     * @param ctx the request, together with the login it belongs to
     */
    void handle(ApiContext ctx) throws Exception;
}
