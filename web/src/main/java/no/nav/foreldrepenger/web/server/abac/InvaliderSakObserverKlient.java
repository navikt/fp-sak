package no.nav.foreldrepenger.web.server.abac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.sak.AbstractInvaliderSakKlient;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class InvaliderSakObserverKlient extends AbstractInvaliderSakKlient {

    private static final Logger LOG = LoggerFactory.getLogger(InvaliderSakObserverKlient.class);


    public void observerStoppetEvent(@Observes SakensPersonerEndretEvent event) {
        invaliderSak(event.getSaksnummer());
    }

    public void invaliderSak(Saksnummer saksnummer) {
        LOG.info("Invaliderer sak {}", saksnummer.getVerdi());
        super.invaliderSak(saksnummer.getVerdi());
    }

}
