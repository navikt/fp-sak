package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class VarselRevurderingTjeneste {

    private Period defaultVenteFrist;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;

    VarselRevurderingTjeneste() {
        // CDI
    }

    @Inject
    public VarselRevurderingTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    public void håndterVarselRevurdering(Behandling behandling, VarselRevurderingAksjonspunktDto adapter) {
        var bestillBrevDto = new BestillBrevDto(behandling.getId(), behandling.getUuid(), DokumentMalType.VARSEL_OM_REVURDERING, adapter.getFritekst());
        bestillBrevDto.setArsakskode(RevurderingVarslingÅrsak.ANNET);
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
        settBehandlingPaVent(behandling, adapter.getFrist(), fraDto(adapter.getVenteÅrsakKode()));
    }

    private void settBehandlingPaVent(Behandling behandling, LocalDate frist, Venteårsak venteårsak) {
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            bestemFristForBehandlingVent(frist), venteårsak);
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    private Venteårsak fraDto(String kode) {
        return Venteårsak.fraKode(kode);
    }
}
