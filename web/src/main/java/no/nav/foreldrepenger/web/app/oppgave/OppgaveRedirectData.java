package no.nav.foreldrepenger.web.app.oppgave;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

public class OppgaveRedirectData {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveRedirectData.class);

    private Saksnummer saksnummer;
    private UUID behandlingUuid;
    private String feilmelding;

    static OppgaveRedirectData hent(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                    FagsakRepository fagsakRepository,
                                    BehandlingRepository behandlingRepository,
                                    OppgaveIdDto oppgaveId, SaksnummerDto saksnummerDto) {
        if (oppgaveId == null && saksnummerDto == null) {
            var feilmelding = "Sak kan ikke åpnes, da referanse mangler.";
            LOG.warn(feilmelding);
            return OppgaveRedirectData.medFeilmelding(feilmelding);
        }
        if (oppgaveId == null) {
            var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
            return hentForFagsak(fagsakRepository,saksnummer);
        }
        if (saksnummerDto == null) {
            return hentForOppgave(oppgaveBehandlingKoblingRepository, behandlingRepository, oppgaveId.getVerdi());
        }

        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isEmpty()) {
            return finnerIkkeSaksnummerRedirect(saksnummer);
        }

        var oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId.getVerdi(), saksnummer);
        if (oppgave.isPresent()) {
            var oppgaveSaksnummer = oppgave.get().getSaksnummer();
            if (oppgaveSaksnummer != null && oppgaveSaksnummer.equals(saksnummer)) {
                var behandlingUuid = hentBehandlingUuid(behandlingRepository, oppgave.get().getBehandlingId());
                return OppgaveRedirectData.medSaksnummerOgBehandlingUuid(oppgaveSaksnummer, behandlingUuid.orElse(null));
            }
            var feilmelding = String.format("Oppgaven med %s er ikke registrert på sak %s", oppgaveId.getVerdi(),
                saksnummer.getVerdi());
            LOG.error(feilmelding);
            return OppgaveRedirectData.medFeilmelding(feilmelding);
        }

        return OppgaveRedirectData.medSaksnummer(saksnummer);
    }

    private static OppgaveRedirectData hentForFagsak(FagsakRepository fagsakRepository, Saksnummer saksnummer) {
        var sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
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

    private static OppgaveRedirectData hentForOppgave(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                                      BehandlingRepository behandlingRepository,
                                                      String oppgaveId) {
        var oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        return oppgave.map(oppgaveBehandlingKobling -> {
            var behandlingUuid = hentBehandlingUuid(behandlingRepository, oppgaveBehandlingKobling.getBehandlingId());
            return OppgaveRedirectData.medSaksnummerOgBehandlingUuid(oppgaveBehandlingKobling.getSaksnummer(), behandlingUuid.orElse(null));
        })
            .orElseGet(() -> {
                var feilmelding = "Det finnes ingen oppgave med denne referansen:" + oppgaveId;
                LOG.warn(feilmelding);
                return OppgaveRedirectData.medFeilmelding(feilmelding);
            });
    }

    private static Optional<UUID> hentBehandlingUuid(BehandlingRepository behandlingRepository, Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        return Optional.of(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }

    private static OppgaveRedirectData medSaksnummerOgBehandlingUuid(Saksnummer saksnummer, UUID behandlingUuid) {
        var data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        data.behandlingUuid = behandlingUuid;
        return data;
    }

    private static OppgaveRedirectData medSaksnummer(Saksnummer saksnummer) {
        var data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        return data;
    }

    static OppgaveRedirectData medFeilmelding(String feilmelding) {
        var data = new OppgaveRedirectData();
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

    UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    String getFeilmelding() {
        return feilmelding;
    }

    boolean harBehandlingId() {
        return behandlingUuid != null;
    }

    boolean harFeilmelding() {
        return feilmelding != null;
    }
}
