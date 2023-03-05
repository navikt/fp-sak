package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.svp;

import java.time.LocalDate;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.FastsettUttaksgrunnlagFeil;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class EndringsdatoRevurderingUtlederImpl implements EndringsdatoRevurderingUtleder {

    private SvangerskapspengerUttakResultatRepository uttakResultatRepository;

    EndringsdatoRevurderingUtlederImpl() {
        // CDI
    }

    @Inject
    public EndringsdatoRevurderingUtlederImpl(UttakRepositoryProvider repositoryProvider) {
        this.uttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
    }

    @Override
    public LocalDate utledEndringsdato(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var uttakResultat = uttakResultatRepository.hentHvisEksisterer(behandlingId);
        var førsteUttak = uttakResultat.flatMap(SvangerskapspengerUttakResultatEntitet::finnFørsteUttaksdato);
        if (førsteUttak.isPresent()) {
            return førsteUttak.get();
        }

        //Finn første tilretteleggingsbehovsdato dersom det ikke finnes uttaksperioder.
        SvangerskapspengerGrunnlag svpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeTilrettelegginger = svpGrunnlag.getGrunnlagEntitet()
            .map(SvpGrunnlagEntitet::hentTilretteleggingerSomSkalBrukes)
            .orElseThrow(() -> FastsettUttaksgrunnlagFeil.kunneIkkeUtledeEndringsdato(behandlingId));

        return gjeldendeTilrettelegginger.stream()
            .map(SvpTilretteleggingEntitet::getTilretteleggingFOMListe)
            .flatMap(Collection::stream)
            .map(TilretteleggingFOM::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow(() -> FastsettUttaksgrunnlagFeil.kunneIkkeUtledeEndringsdato(behandlingId));
    }
}
