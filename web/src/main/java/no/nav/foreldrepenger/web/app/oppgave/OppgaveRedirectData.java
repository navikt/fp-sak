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

public class OppgaveRedirectData {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveRedirectData.class);

    private Saksnummer saksnummer;
    private Long behandlingId;
    private String feilmelding;

    static OppgaveRedirectData hent(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                    FagsakRepository fagsakRepository,
                                    OppgaveIdDto oppgaveId, SaksnummerDto saksnummerDto) {
        if (oppgaveId == null && saksnummerDto == null) {
            var feilmelding = "Sak kan ikke åpnes, da referanse mangler.";
            LOG.warn(feilmelding);
            return OppgaveRedirectData.medFeilmelding(feilmelding);
        } else if (oppgaveId == null) {
            Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
            return hentForFagsak(fagsakRepository,saksnummer);
        } else if (saksnummerDto == null) {
            return hentForOppgave(oppgaveBehandlingKoblingRepository, oppgaveId.getVerdi());
        }

        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isEmpty()) {
            return finnerIkkeSaksnummerRedirect(saksnummer);
        }

        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId.getVerdi(), saksnummer);
        if (oppgave.isPresent()) {
            var oppgaveSaksnummer = oppgave.get().getSaksnummer();
            if (oppgaveSaksnummer != null && oppgaveSaksnummer.equals(saksnummer)) {
                return OppgaveRedirectData.medSaksnummerOgBehandlingId(oppgaveSaksnummer, oppgave.get().getBehandlingId());
            }
            var feilmelding = String.format("Oppgaven med %s er ikke registrert på sak %s", oppgaveId.getVerdi(),
                saksnummer.getVerdi());
            LOG.error(feilmelding);
            return OppgaveRedirectData.medFeilmelding(feilmelding);
        }

        return OppgaveRedirectData.medSaksnummer(saksnummer);
    }

    private static OppgaveRedirectData hentForFagsak(FagsakRepository fagsakRepository, Saksnummer saksnummer) {
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isPresent()) {
            return OppgaveRedirectData.medSaksnummer(saksnummer);
        }
        return finnerIkkeSaksnummerRedirect(saksnummer);
    }

    private static OppgaveRedirectData finnerIkkeSaksnummerRedirect(Saksnummer saksnummer) {
        var feilmelding = "Det finnes ingen sak med dette saksnummeret: " + saksnummer.getVerdi();
        LOG.warn(feilmelding);
        return OppgaveRedirectData.medFeilmelding(feilmelding);
    }

    private static OppgaveRedirectData hentForOppgave(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        return oppgave.map(oppgaveBehandlingKobling -> OppgaveRedirectData.medSaksnummerOgBehandlingId(oppgaveBehandlingKobling.getSaksnummer(), oppgaveBehandlingKobling.getBehandlingId()))
            .orElseGet(() -> {
                var feilmelding = "Det finnes ingen oppgave med denne referansen:" + oppgaveId;
                LOG.warn(feilmelding);
                return OppgaveRedirectData.medFeilmelding(feilmelding);
            });
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
            data.feilmelding = "Kunne ikke encode feilmelding " + feilmelding;
            LOG.error(data.feilmelding, e);
        }
        return data;
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
