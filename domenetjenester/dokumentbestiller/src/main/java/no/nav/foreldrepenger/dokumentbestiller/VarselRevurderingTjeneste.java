package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class VarselRevurderingTjeneste {

    private Period defaultVenteFrist;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VarselRevurderingTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                     BehandlingRepository behandlingRepository) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    VarselRevurderingTjeneste() {
        // CDI
    }

    public void håndterVarselRevurdering(BehandlingReferanse ref, VarselRevurderingAksjonspunktDto adapter) {
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(ref.behandlingUuid())
            .medSaksnummer(ref.saksnummer())
            .medDokumentMal(DokumentMalType.VARSEL_OM_REVURDERING)
            .medRevurderingÅrsak(RevurderingVarslingÅrsak.ANNET)
            .medFritekst(adapter.getFritekst())
            .build();
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        settBehandlingPaVent(ref, adapter.getFrist(), fraDto(adapter.getVenteÅrsakKode()));
    }

    private void settBehandlingPaVent(BehandlingReferanse ref, LocalDate frist, Venteårsak venteårsak) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
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
