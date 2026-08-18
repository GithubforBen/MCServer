package de.hems.utils.webconsole;

/**
 * One area of the admin website.
 * <p>
 * This is the extension point of the whole interface: a new panel is a new class implementing this, plus
 * one line in {@link WebServer} that registers it. The module says what it is called and which routes it
 * answers, and the frontend builds its navigation from the list of modules it gets from the server - so a
 * new module shows up in the browser without the page having to be touched.
 */
public interface WebModule {

    /**
     * @return the id used in urls and by the frontend to find the panel that renders this module
     */
    String getId();

    /**
     * @return the name shown in the navigation
     */
    String getTitle();

    /**
     * @return a short line describing what the module does
     */
    default String getDescription() {
        return "";
    }

    /**
     * @return whether the module is shown in the navigation of the website
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * Adds the routes of this module.
     *
     * @param server the server to register on
     */
    void register(WebServer server);
}
