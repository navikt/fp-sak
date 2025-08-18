package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class EøsUttakAnnenpartTjeneste {

    private EøsUttakRepository eøsUttakRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public EøsUttakAnnenpartTjeneste(EøsUttakRepository eøsUttakRepository,
                                     HistorikkinnslagRepository historikkinnslagRepository,
                                     YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                     BehandlingRepository behandlingRepository) {
        this.eøsUttakRepository = eøsUttakRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    EøsUttakAnnenpartTjeneste() {
        //CDI
    }

    Optional<AksjonspunktDefinisjon> utledUttakIEøsForAnnenpartAP(BehandlingReferanse ref) {
        return trengerEøsAvklaring(ref) ? Optional.of(AksjonspunktDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART) : Optional.empty();
    }

    void fjernEøsUttak(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        eøsUttakRepository.hentGrunnlag(behandlingId).ifPresent(grunnlag -> {
            historikkinnslagRepository.lagre(historikkinnslagForFjerningAvEøsUttak(ref));
            eøsUttakRepository.deaktiverAktivtGrunnlagHvisFinnes(behandlingId);
        });
    }

    private boolean trengerEøsAvklaring(BehandlingReferanse ref) {
        var erRevurdering = ref.erRevurdering();
        var avklartAnnenForelderHarRettEØS = ytelseFordelingTjeneste.hentAggregat(ref.behandlingId()).avklartAnnenForelderHarRettEØS();

        var førstegangsbehandlingMedAvklartRettIEøs = !erRevurdering && avklartAnnenForelderHarRettEØS;
        var eøsGrunnlag = eøsUttakRepository.hentGrunnlag(ref.behandlingId());
        var revurderingMedAvklartRettIEøsUtenEøsUttakGrunnlag =
            erRevurdering && avklartAnnenForelderHarRettEØS && eøsGrunnlag.isEmpty(); //Eldre behandlinger har ikke eøsuttakgrunnlag
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var endringssøknadMedEøs = behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) && eøsGrunnlag.isPresent();
        return førstegangsbehandlingMedAvklartRettIEøs || revurderingMedAvklartRettIEøsUtenEøsUttakGrunnlag || endringssøknadMedEøs;
    }

    private static Historikkinnslag historikkinnslagForFjerningAvEøsUttak(BehandlingReferanse ref) {
        return new Historikkinnslag.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .medTittel("Ryddet bort annen forelders uttak i eøs ettersom avklart rettighetstype tilsier at dette ikke lenger er en EØS-sak")
            .build();
    }
}
