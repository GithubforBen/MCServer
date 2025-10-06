package de.hems;

public interface CommunicationListener<E> {
    public boolean checkE(Event event);
    void onEvent(E event);
}
