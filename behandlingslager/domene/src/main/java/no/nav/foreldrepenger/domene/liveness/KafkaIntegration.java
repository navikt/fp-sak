package no.nav.foreldrepenger.domene.liveness;

public interface KafkaIntegration {

    /**
     * Er integrasjonen i live (og ready).
     *
     * @return true / false
     */
    boolean isAlive();
}
