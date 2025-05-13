package no.nav.foreldrepenger.web.server.abac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.sak.AbstractInvaliderSakKlient;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class InvaliderSakPersonCacheObserverKlient extends AbstractInvaliderSakKlient {

    private static final Logger LOG = LoggerFactory.getLogger(InvaliderSakPersonCacheObserverKlient.class);


    public void observerPersonerEndretEvent(@Observes SakensPersonerEndretEvent event) {
        invaliderSakCache(event.getSaksnummer());
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        // Førstegangssøknad kan ha annen forelder
        if (event.getMottattDokument().getDokumentType().erSøknadType()
            && FagsakYtelseType.FORELDREPENGER.equals(event.behandling().getFagsakYtelseType())) {
            invaliderSakCache(event.getSaksnummer());
        }
    }

    public void invaliderSakCache(Saksnummer saksnummer) {
        LOG.info("Invaliderer sak {}", saksnummer.getVerdi());
        super.invaliderSak(saksnummer.getVerdi());
    }

}
