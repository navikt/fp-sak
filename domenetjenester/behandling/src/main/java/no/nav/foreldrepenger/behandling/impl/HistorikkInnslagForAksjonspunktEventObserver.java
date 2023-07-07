package no.nav.foreldrepenger.behandling.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Observerer Aksjonspunkt*Events og registrerer HistorikkInnslag for enkelte
 * hendelser (eks. gjenoppta og behandling på vent)
 */
@ApplicationScoped
public class HistorikkInnslagForAksjonspunktEventObserver {

    private HistorikkRepository historikkRepository;

    @Inject
    public HistorikkInnslagForAksjonspunktEventObserver(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    /**
     * @param aksjonspunkterFunnetEvent
     */
    public void oppretteHistorikkForBehandlingPåVent(@Observes AksjonspunktStatusEvent aksjonspunkterFunnetEvent) {
        var ktx = aksjonspunkterFunnetEvent.getKontekst();
        for (var aksjonspunkt : aksjonspunkterFunnetEvent.getAksjonspunkter()) {
            if (aksjonspunkt.erOpprettet() && AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), HistorikkinnslagType.BEH_KØET, null,
                    Venteårsak.VENT_ÅPEN_BEHANDLING);
            } else if (aksjonspunkt.erOpprettet() && aksjonspunkt.getFristTid() != null) {
                var frist = aksjonspunkt.getFristTid();
                var venteårsak = aksjonspunkt.getVenteårsak();
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), HistorikkinnslagType.BEH_VENT, frist,
                    venteårsak);
            }
        }
    }

    private void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId,
            Long fagsakId,
            HistorikkinnslagType historikkinnslagType,
            LocalDateTime fristTid,
            Venteårsak venteårsak) {
        var builder = new HistorikkInnslagTekstBuilder();
        if (fristTid != null) {
            builder.medHendelse(historikkinnslagType, fristTid.toLocalDate());
        } else {
            builder.medHendelse(historikkinnslagType);
        }
        if (venteårsak != null) {
            builder.medÅrsak(venteårsak);
        }
        var historikkinnslag = new Historikkinnslag();
        var erSystemBruker = Optional.ofNullable(KontekstHolder.getKontekst().getIdentType()).filter(IdentType::erSystem).isPresent() ||
            Optional.ofNullable(KontekstHolder.getKontekst().getUid()).map(String::toLowerCase).filter(s -> s.startsWith("srv")).isPresent();
        historikkinnslag.setAktør(erSystemBruker ? HistorikkAktør.VEDTAKSLØSNINGEN : HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandlingId);
        historikkinnslag.setFagsakId(fagsakId);
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    public void oppretteHistorikkForGjenopptattBehandling(@Observes AksjonspunktStatusEvent aksjonspunkterFunnetEvent) {
        for (var aksjonspunkt : aksjonspunkterFunnetEvent.getAksjonspunkter()) {
            var ktx = aksjonspunkterFunnetEvent.getKontekst();
            if (aksjonspunkt.erUtført() && AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), HistorikkinnslagType.KØET_BEH_GJEN,
                    null, null);
            } else if (aksjonspunkt.erUtført() && aksjonspunkt.getFristTid() != null) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), HistorikkinnslagType.BEH_GJEN, null,
                    null);
            }
        }
    }

}
