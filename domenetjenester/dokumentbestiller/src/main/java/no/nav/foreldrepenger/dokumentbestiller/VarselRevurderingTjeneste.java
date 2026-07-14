package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

@ApplicationScoped
public class VarselRevurderingTjeneste {

    private static final Period VENTEFRIST = Period.ofWeeks(4);
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private BehandlingRepository behandlingRepository;
    private MellomlagringRepository mellomlagringRepository;

    @Inject
    public VarselRevurderingTjeneste(BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                     DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                     BehandlingRepository behandlingRepository,
                                     MellomlagringRepository mellomlagringRepository) {
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.mellomlagringRepository = mellomlagringRepository;
    }

    VarselRevurderingTjeneste() {
        // CDI
    }

    public void bestillVarselRevurdering(BehandlingReferanse ref, VarselRevurderingAksjonspunktDto adapter) {
        var harRedigertBrev = mellomlagringRepository.harMellomlagring(ref.behandlingId(),
            MellomlagringType.fraDokumentMalType(DokumentMalType.VARSEL_OM_REVURDERING));
        var dokumentMal = harRedigertBrev ? DokumentMalType.FRITEKST_HTML : DokumentMalType.VARSEL_OM_REVURDERING;
        var journalførSom = harRedigertBrev ? DokumentMalType.VARSEL_OM_REVURDERING : null;
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(ref.behandlingUuid())
            .medSaksnummer(ref.saksnummer())
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSom)
            .build();
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        settBehandlingPaVent(ref, adapter.frist(), fraDto(adapter.venteÅrsakKode()));
    }

    private void settBehandlingPaVent(BehandlingReferanse ref, LocalDate frist, Venteårsak venteårsak) {
        behandlingRepository.taSkriveLås(ref.behandlingId());
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        behandlingProsesseringTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            bestemFristForBehandlingVent(frist), venteårsak);
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime()) : LocalDateTime.now().plus(VENTEFRIST);
    }

    private Venteårsak fraDto(String kode) {
        return Venteårsak.fraKode(kode);
    }
}
