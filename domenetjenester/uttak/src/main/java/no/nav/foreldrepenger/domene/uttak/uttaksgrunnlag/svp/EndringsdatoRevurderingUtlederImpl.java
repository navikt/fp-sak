package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.svp;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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
        var behandlingId = input.getBehandlingReferanse().getBehandlingId();
        var uttakResultat = uttakResultatRepository.hentHvisEksisterer(behandlingId);
        if (uttakResultat.isPresent() && uttakResultat.get().finnFørsteUttaksdato().isPresent()) {
            return uttakResultat.get().finnFørsteUttaksdato().get();
        }

        //Finn første tilretteleggingsbehovsdato dersom det ikke finnes uttaksperioder.
        SvangerskapspengerGrunnlag svpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var grunnlagOpt = svpGrunnlag.getGrunnlagEntitet();
        if (grunnlagOpt.isPresent()) {
            var førsteTilretteleggingBehovDato = new TilretteleggingFilter(
                grunnlagOpt.get()).getFørsteTilretteleggingsbehovdatoFiltrert();
            if (førsteTilretteleggingBehovDato.isPresent()) {
                return førsteTilretteleggingBehovDato.get();
            }
        }

        throw FastsettUttaksgrunnlagFeil.kunneIkkeUtledeEndringsdato(behandlingId);
    }

}
