package no.nav.foreldrepenger.produksjonsstyring.opprettgsak;

import static no.nav.vedtak.log.util.LoggerUtils.removeLineBreaks;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSAktor;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSOpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSOpprettSakResponse;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSSak;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSSakEksistererAlleredeException;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSSikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSUgyldigInputException;
import no.nav.tjeneste.virksomhet.sak.v1.binding.FinnSakForMangeForekomster;
import no.nav.tjeneste.virksomhet.sak.v1.binding.FinnSakUgyldigInput;
import no.nav.tjeneste.virksomhet.sak.v1.meldinger.FinnSakRequest;
import no.nav.tjeneste.virksomhet.sak.v1.meldinger.FinnSakResponse;
import no.nav.vedtak.felles.integrasjon.behandlesak.klient.BehandleSakConsumer;
import no.nav.vedtak.felles.integrasjon.sak.SakConsumer;


@ApplicationScoped
public class OpprettGSakTjeneste {


    private static final String FORELDREPENGER_KODE = "FOR";
    private static final String VL_FAGSYSTEM_KODE = Fagsystem.FPSAK.getOffisiellKode();
    private static final String MED_FAGSAK_KODE = "MFS";

    private Logger logger = LoggerFactory.getLogger(OpprettGSakTjeneste.class);

    private BehandleSakConsumer behandleSakConsumer;
    private SakConsumer sakConsumer;

    public OpprettGSakTjeneste() {
        //For CDI
    }

    @Inject
    public OpprettGSakTjeneste(BehandleSakConsumer behandleSakConsumer, SakConsumer sakConsumer) {
        this.behandleSakConsumer = behandleSakConsumer;
        this.sakConsumer = sakConsumer;
    }

    public Saksnummer opprettSakIGsak(@SuppressWarnings("unused") Long fagsakId, Personinfo bruker) {

        WSSak sak = new WSSak();
        sak.setFagomrade(FORELDREPENGER_KODE);
        sak.setSaktype(MED_FAGSAK_KODE);

        WSAktor aktoer = new WSAktor();
        aktoer.setIdent(bruker.getPersonIdent().getIdent());
        sak.getGjelderBrukerListe().add(aktoer);

        sak.setFagsystem(VL_FAGSYSTEM_KODE);

        WSOpprettSakRequest opprettSakRequest = new WSOpprettSakRequest();
        opprettSakRequest.setSak(sak);

        try {
            WSOpprettSakResponse response = behandleSakConsumer.opprettSak(opprettSakRequest);
            logger.info(removeLineBreaks("Sak opprettet i GSAK med saksnummer: {}"), removeLineBreaks(response.getSakId())); //NOSONAR
            return new Saksnummer(response.getSakId());
        } catch (WSSakEksistererAlleredeException e) {
            throw OpprettGSakFeil.FACTORY.kanIkkeOppretteIGsakFordiSakAlleredeEksisterer(e).toException();
        } catch (WSUgyldigInputException opprettSakUgyldigInput) {
            throw OpprettGSakFeil.FACTORY.kanIkkeOppretteIGsakFordiInputErUgyldig(opprettSakUgyldigInput).toException();
        } catch (WSSikkerhetsbegrensningException e) {
            throw OpprettGSakFeil.FACTORY.opprettSakSikkerhetsbegrensning(e).toException();
        }
    }

    public Optional<Saksnummer> finnGsak(Long fagsakId) {
        FinnSakRequest finnSakRequest = new FinnSakRequest();

        no.nav.tjeneste.virksomhet.sak.v1.informasjon.Fagsystemer fagsystem = new no.nav.tjeneste.virksomhet.sak.v1.informasjon.Fagsystemer();
        fagsystem.setValue(VL_FAGSYSTEM_KODE);
        finnSakRequest.setFagsystem(fagsystem);

        String saksnummer = VL_FAGSYSTEM_KODE + fagsakId;
        finnSakRequest.setFagsystemSakId(saksnummer);

        try {
            FinnSakResponse response = sakConsumer.finnSak(finnSakRequest);
            int size = response.getSakListe().size();
            switch (size) {
                case 0:
                    return Optional.empty();
                case 1:
                    return Optional.of(new Saksnummer(response.getSakListe().get(0).getSakId()));
                default:
                    // Skal ikke kunne oppstå siden Saksnummer er unik
                    throw OpprettGSakFeil.FACTORY.finnSakIkkeUniktResultat(saksnummer, size).toException();
            }
        } catch (FinnSakForMangeForekomster finnSakForMangeForekomster) {
            // Skal ikke kunne oppstå siden Saksnummer er unik
            throw OpprettGSakFeil.FACTORY.finnSakForMangeForekomster(saksnummer, finnSakForMangeForekomster).toException();
        } catch (FinnSakUgyldigInput finnSakUgyldigInput) {
            // Skal ikke kunne oppstå her siden vi setter fagsystem og Saksnummer
            throw OpprettGSakFeil.FACTORY.finnSakUgyldigInput(finnSakUgyldigInput).toException();
        }
    }

    public Saksnummer opprettEllerFinnGsak(Long fagsakId, Personinfo bruker) {
        Saksnummer saksnummer;
        try {
            saksnummer = opprettSakIGsak(fagsakId, bruker);
        } catch (SakEksistererAlleredeException ignored) { //NOSONAR
            Optional<Saksnummer> gsakId = finnGsak(fagsakId);
            if (gsakId.isPresent()) {
                saksnummer = gsakId.get();
            } else {
                throw OpprettGSakFeil.FACTORY.fantIkkeSakenSomGsakSaAlleredeEksisterer(fagsakId).toException();
            }
        }
        return saksnummer;
    }
}
