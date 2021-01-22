package no.nav.foreldrepenger.produksjonsstyring.opprettgsak;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.sak.v1.LegacySakRestKlient;
import no.nav.vedtak.felles.integrasjon.sak.v1.SakJson;


@ApplicationScoped
public class OpprettGSakTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpprettGSakTjeneste.class);

    private LegacySakRestKlient restKlient;

    public OpprettGSakTjeneste() {
        //For CDI
    }

    @Inject
    public OpprettGSakTjeneste(LegacySakRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    public Saksnummer opprettArkivsak(AktørId aktørId) {
        var request = SakJson.getBuilder()
            .medAktoerId(aktørId.getId())
            .medApplikasjon(Fagsystem.FPSAK.getOffisiellKode())
            .medTema(Tema.FOR.getOffisiellKode());
        var sak = restKlient.opprettSak(request.build());
        var sakId = new Saksnummer(String.valueOf(sak.getId()));
        LOGGER.info("SAK REST opprettet sak {}", sakId.getVerdi());
        return sakId;
    }

    public Saksnummer opprettArkivsakFor(Saksnummer saksnummer, AktørId aktørId) {
        var request = SakJson.getBuilder()
            .medAktoerId(aktørId.getId())
            .medFagsakNr(saksnummer.getVerdi())
            .medApplikasjon(Fagsystem.FPSAK.getOffisiellKode())
            .medTema(Tema.FOR.getOffisiellKode());
        var sak = restKlient.opprettSak(request.build());
        var sakId = new Saksnummer(String.valueOf(sak.getId()));
        LOGGER.info("SAK REST opprettet sak {} for saksnummer {}", sakId.getVerdi(), saksnummer.getVerdi());
        return sakId;
    }

    public Optional<Saksnummer> finnArkivSakIdFor(Saksnummer saksnummer) {
        try {
            var sak = restKlient.finnForSaksnummer(saksnummer.getVerdi())
                .map(s -> new Saksnummer(String.valueOf(s.getId())));
            LOGGER.info("SAK REST fant sak {} for saksnummer {}", sak, saksnummer.getVerdi());
            return sak;
        } catch (Exception e) {
            LOGGER.warn("SAK REST feil ved finn sak {}", saksnummer.getVerdi(), e);
            throw new IllegalArgumentException("Utviklerfeil finnSak kalt med ugyldig saksnummer" + saksnummer.getVerdi());
        }
    }
}
