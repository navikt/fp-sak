package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class FormidlingRestKlientProducer {

    private static final Environment ENV = Environment.current();

    @Produces
    @ApplicationScoped
    Dokument formidlingKlient() {
        if (ENV.isDev()) {
            return new FormidlingRestKlientGcp();
        }
        return new FormidlingRestKlient();
    }
}
