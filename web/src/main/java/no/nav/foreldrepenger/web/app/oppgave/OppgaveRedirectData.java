package no.nav.foreldrepenger.web.app.oppgave;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.feil.Feil;

public class OppgaveRedirectData {

    private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveRedirectData.class);

    private Saksnummer saksnummer;
    private Long behandlingId;
    private String feilmelding;

    static OppgaveRedirectData hent(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                    FagsakRepository fagsakRepository,
                                    OppgaveIdDto oppgaveId, SaksnummerDto saksnummerDto) {
        if (oppgaveId == null && saksnummerDto == null) {
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.sakKanIkkeÅpnesDaReferanseMangler()));
        } else if (oppgaveId == null) {
            Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
            return hentForFagsak(fagsakRepository,saksnummer);
        } else if (saksnummerDto == null) {
            return hentForOppgave(oppgaveBehandlingKoblingRepository, oppgaveId.getVerdi());
        }

        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isEmpty()) {
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenFagsak(saksnummer.getVerdi())));
        }

        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId.getVerdi(), saksnummer);
        if (oppgave.isPresent()) {
            var oppgaveSaksnummer = oppgave.get().getSaksnummer();
            if (oppgaveSaksnummer != null && oppgaveSaksnummer.equals(saksnummer)) {
                return OppgaveRedirectData.medSaksnummerOgBehandlingId(oppgaveSaksnummer, oppgave.get().getBehandlingId());
            }
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.oppgaveErIkkeRegistrertPåSak(oppgaveId.getVerdi(), saksnummer.getVerdi())));
        }

        return OppgaveRedirectData.medSaksnummer(saksnummer);
    }

    private static OppgaveRedirectData hentForFagsak(FagsakRepository fagsakRepository, Saksnummer saksnummer) {
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isPresent()) {
            return OppgaveRedirectData.medSaksnummer(saksnummer);
        }
        return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenFagsak(saksnummer.getVerdi())));
    }

    private static OppgaveRedirectData hentForOppgave(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        return oppgave.map(oppgaveBehandlingKobling -> OppgaveRedirectData.medSaksnummerOgBehandlingId(oppgaveBehandlingKobling.getSaksnummer(), oppgaveBehandlingKobling.getBehandlingId()))
            .orElseGet(() -> OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenOppgaveMedDenneReferansen(oppgaveId))));
    }

    private static OppgaveRedirectData medSaksnummerOgBehandlingId(Saksnummer saksnummer, Long behandlingId) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        data.behandlingId = behandlingId;
        return data;
    }

    private static OppgaveRedirectData medSaksnummer(Saksnummer saksnummer) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        return data;
    }

    static OppgaveRedirectData medFeilmelding(String feilmelding) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        try {
            data.feilmelding = URLEncoder.encode(feilmelding, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            data.feilmelding = logg(OppgaveRedirectServletFeil.FACTORY.kunneIkkeEncodeFeilmelding(feilmelding, e));
        }
        return data;
    }

    static String logg(Feil feil) {
        feil.log(LOGGER);
        return feil.getFeilmelding();
    }

    Saksnummer getSaksnummer() {
        return saksnummer;
    }

    Long getBehandlingId() {
        return behandlingId;
    }

    String getFeilmelding() {
        return feilmelding;
    }

    boolean harBehandlingId() {
        return behandlingId != null;
    }

    boolean harFeilmelding() {
        return feilmelding != null;
    }
}
