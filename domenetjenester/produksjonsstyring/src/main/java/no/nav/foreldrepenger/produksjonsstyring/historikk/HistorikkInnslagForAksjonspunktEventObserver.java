package no.nav.foreldrepenger.produksjonsstyring.historikk;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Observerer Aksjonspunkt*Events og registrerer HistorikkInnslag for enkelte
 * hendelser (eks. gjenoppta og behandling på vent)
 */
@ApplicationScoped
public class HistorikkInnslagForAksjonspunktEventObserver {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public HistorikkInnslagForAksjonspunktEventObserver(HistorikkinnslagRepository historikkinnslagRepository,
                                                        BehandlingRepository behandlingRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * @param event
     */
    public void oppretteHistorikkForBehandlingPåVent(@Observes AksjonspunktStatusEvent event) {
        for (var aksjonspunkt : event.getAksjonspunkter()) {
            if (aksjonspunkt.erOpprettet() && AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(event.getBehandlingId(), event.getFagsakId(), "Behandlingen er satt på vent", null,
                    Venteårsak.VENT_ÅPEN_BEHANDLING);
            } else if (aksjonspunkt.erOpprettet() && aksjonspunkt.getFristTid() != null) {
                var frist = aksjonspunkt.getFristTid();
                var venteårsak = aksjonspunkt.getVenteårsak();
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(event.getBehandlingId(), event.getFagsakId(), "Behandlingen er satt på vent", frist,
                    venteårsak);
            }
        }
    }

    private void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId,
                                                                      Long fagsakId,
                                                                      String tittel,
                                                                      LocalDateTime fristTid,
                                                                      Venteårsak venteårsak) {
        var historikkinnslagBuilder = new Historikkinnslag.Builder();
        if (fristTid != null) {
            historikkinnslagBuilder.medTittel(tittel + " til " + HistorikkinnslagLinjeBuilder.format(fristTid.toLocalDate()));
        } else {
            historikkinnslagBuilder.medTittel(tittel);
        }
        if (venteårsak != null) {
            historikkinnslagBuilder.addLinje(venteårsak.getNavn());
        }
        var erSystemBruker = Optional.ofNullable(KontekstHolder.getKontekst().getIdentType()).filter(IdentType::erSystem).isPresent() ||
            Optional.ofNullable(KontekstHolder.getKontekst().getUid()).map(String::toLowerCase).filter(s -> s.startsWith("srv")).isPresent();
        historikkinnslagBuilder
            .medAktør(erSystemBruker ? HistorikkAktør.VEDTAKSLØSNINGEN : HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandlingId)
            .medFagsakId(fagsakId);
        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
    }

    public void oppretteHistorikkForGjenopptattBehandling(@Observes AksjonspunktStatusEvent event) {
        for (var aksjonspunkt : event.getAksjonspunkter()) {
            if (aksjonspunkt.erUtført() && AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(event.getBehandlingId(), event.getFagsakId(), "Køet behandling er gjenopptatt",
                    null, null);
            } else if (aksjonspunkt.erUtført() && aksjonspunkt.getFristTid() != null) {
                // Unngå dobbelinnslag (innslag ved manuellTaAvVent) + konvensjon med påVent->SBH=null og manuellGjenoppta->SBH=ident
                var manueltTattAvVent = Optional.ofNullable(behandlingRepository.hentBehandlingReadOnly(event.getBehandlingId()))
                    .map(Behandling::getAnsvarligSaksbehandler).isPresent();
                if (!manueltTattAvVent) {
                    opprettHistorikkinnslagForVenteFristRelaterteInnslag(event.getBehandlingId(), event.getFagsakId(), "Behandlingen er gjenopptatt",
                        null, null);
                }
            }
        }
    }

}
