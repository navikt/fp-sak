package no.nav.foreldrepenger.ytelse.beregning.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class BeregnFeriepenger extends BeregnFeriepengerTjeneste {
    private SvangerskapspengerFeriekvoteTjeneste svangerskapspengerFeriekvoteTjeneste;

    BeregnFeriepenger() {
        // CDI
    }

    /**
     *
     * @param antallDagerFeriepenger - Antall dager i feriepengerperioden for svangerskapspenger
     */
    @Inject
    public BeregnFeriepenger(BehandlingRepositoryProvider repositoryProvider,
                             MapInputFraVLTilRegelGrunnlag inputTjeneste,
                             FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                             DekningsgradTjeneste dekningsgradTjeneste,
                             @KonfigVerdi(value = "svp.antall.dager.feriepenger", defaultVerdi = "64") int antallDagerFeriepenger,
                             SvangerskapspengerFeriekvoteTjeneste svangerskapspengerFeriekvoteTjeneste) {
        super(repositoryProvider, inputTjeneste, fagsakRelasjonTjeneste, dekningsgradTjeneste, antallDagerFeriepenger);
        this.svangerskapspengerFeriekvoteTjeneste = svangerskapspengerFeriekvoteTjeneste;
    }

    @Override
    protected int finnTigjengeligeFeriepengedager(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        return svangerskapspengerFeriekvoteTjeneste.beregnTilgjengeligFeriekvote(ref, beregningsresultat).orElse(antallDagerFeriepenger);
    }
}
