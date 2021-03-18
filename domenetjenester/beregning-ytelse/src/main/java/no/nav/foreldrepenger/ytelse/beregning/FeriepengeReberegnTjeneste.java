package no.nav.foreldrepenger.ytelse.beregning;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class FeriepengeReberegnTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenesteInstance;
    private BehandlingRepository behandlingRepository;

    FeriepengeReberegnTjeneste() {
        // CDI
    }

    @Inject
    public FeriepengeReberegnTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                      @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                      BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregnFeriepengerTjenesteInstance = beregnFeriepengerTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public boolean harDiffUtenomPeriode(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsresultatEntitet> gjeldendeResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        return gjeldendeResultat.map(r -> utledNyttResultat(behandling, r, true)).orElse(false);
    }

    public boolean skalReberegneFeriepenger(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsresultatEntitet> gjeldendeResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        return gjeldendeResultat.map(r -> utledNyttResultat(behandling, r, false)).orElse(false);
    }

    private boolean utledNyttResultat(Behandling behandling, BeregningsresultatEntitet gjeldendeResultat, boolean loggAvvik) {
        BeregnFeriepengerTjeneste feriepengetjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjenesteInstance, behandling.getFagsakYtelseType()).orElseThrow();
        return feriepengetjeneste.avvikBeregnetFeriepengerBeregningsresultat(behandling, gjeldendeResultat, loggAvvik);
    }
}
