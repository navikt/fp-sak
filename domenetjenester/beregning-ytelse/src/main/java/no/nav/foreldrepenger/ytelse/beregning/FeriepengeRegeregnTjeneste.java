package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class FeriepengeRegeregnTjeneste {
    private static final Logger logger = LoggerFactory.getLogger(FeriepengeRegeregnTjeneste.class);
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenesteInstance;
    private BehandlingRepository behandlingRepository;

    FeriepengeRegeregnTjeneste() {
        // CDI
    }

    @Inject
    public FeriepengeRegeregnTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                      @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                      BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregnFeriepengerTjenesteInstance = beregnFeriepengerTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public boolean harDiff(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        Optional<BeregningsresultatEntitet> gjeldendeResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(ref.getBehandlingId());
        if (gjeldendeResultat.isEmpty()) {
            logger.info("Finner ikke beregningsresultat for behandling " + ref.getBehandlingId());
            return false;
        }
        BeregningsresultatEntitet nyttResultat = utledNyttResultat(ref, behandling, gjeldendeResultat);
        return new Feriepengesammenligner(ref.getBehandlingId(), ref.getSaksnummer(), nyttResultat, gjeldendeResultat.get()).finnesAvvik();
    }

    private BeregningsresultatEntitet utledNyttResultat(BehandlingReferanse ref, Behandling behandling, Optional<BeregningsresultatEntitet> gjeldendeResultat) {
        BeregnFeriepengerTjeneste feriepengetjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjenesteInstance, ref.getFagsakYtelseType()).orElseThrow();
        BeregningsresultatEntitet kopi = BeregningsresultatEntitet.builder(gjeldendeResultat).build();
        feriepengetjeneste.beregnFeriepenger(behandling, kopi);
        return kopi;
    }
}
