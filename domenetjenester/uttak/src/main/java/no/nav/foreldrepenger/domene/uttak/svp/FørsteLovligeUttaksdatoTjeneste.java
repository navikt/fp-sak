package no.nav.foreldrepenger.domene.uttak.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristRegelOrkestrering;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;
import no.nav.foreldrepenger.regler.soknadsfrist.grunnlag.SøknadsfristGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class FørsteLovligeUttaksdatoTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    FørsteLovligeUttaksdatoTjeneste() {
        //For CDI
    }

    @Inject
    public FørsteLovligeUttaksdatoTjeneste(UttakRepositoryProvider uttakRepositoryProvider) {
        this.uttaksperiodegrenseRepository = uttakRepositoryProvider.getUttaksperiodegrenseRepository();
        this.behandlingsresultatRepository = uttakRepositoryProvider.getBehandlingsresultatRepository();
    }

    public SøknadsfristResultat utledFørsteLovligeUttaksdato(UttakInput input, LocalDateInterval uttaksgrenser) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();

        var søknadMottattDato = input.getSøknadMottattDato();
        //Sjekk søknadsfristregel
        var søknadsfristGrunnlag = new SøknadsfristGrunnlag.Builder()
            .førsteUttaksdato(uttaksgrenser.getFomDato())
            .søknadMottattDato(søknadMottattDato)
            .build();
        var resultat = new SøknadsfristRegelOrkestrering().vurderSøknadsfrist(søknadsfristGrunnlag);

        //Lagre søknadsfristresultat
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultatRepository.hent(behandlingId))
            .medFørsteLovligeUttaksdag(resultat.getTidligsteLovligeUttak())
            .medMottattDato(søknadMottattDato)
            .medSporingInput(resultat.getInnsendtGrunnlag())
            .medSporingRegel(resultat.getEvalueringResultat())
            .build();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);
        return resultat;
    }

}
