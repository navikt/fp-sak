package no.nav.foreldrepenger.domene.uttak.svp;

import java.time.LocalDate;
import java.time.Period;

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
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FørsteLovligeUttaksdatoTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private Period søknadsfristEtterFørsteUttaksdag;

    FørsteLovligeUttaksdatoTjeneste() {
        //For CDI
    }

    /**
     * @param søknadsfristEtterFørsteUttaksdag - Maks antall måneder mellom søknadens mottattdato og første uttaksdag i søknaden.
     */
    @Inject
    public FørsteLovligeUttaksdatoTjeneste(UttakRepositoryProvider uttakRepositoryProvider,
                                           @KonfigVerdi(value = "svp.søknadfrist.etter.første.uttaksdag", defaultVerdi = "P3M") Period søknadsfristEtterFørsteUttaksdag) {
        this.uttaksperiodegrenseRepository = uttakRepositoryProvider.getUttaksperiodegrenseRepository();
        this.behandlingsresultatRepository = uttakRepositoryProvider.getBehandlingsresultatRepository();
        this.søknadsfristEtterFørsteUttaksdag = søknadsfristEtterFørsteUttaksdag;
    }

    public SøknadsfristResultat utledFørsteLovligeUttaksdato(UttakInput input, LocalDateInterval uttaksgrenser) {
        var behandlingId = input.getBehandlingReferanse().getBehandlingId();

        LocalDate søknadMottattDato = input.getSøknadMottattDato();
        //Sjekk søknadsfristregel
        var søknadsfristGrunnlag = new SøknadsfristGrunnlag.Builder()
            .medAntallMånederSøknadsfrist(søknadsfristEtterFørsteUttaksdag.getMonths()) // TODO, broken må rette uttak-regler den dagen det ikke er heltall antall måneder her.
            .medFørsteUttaksdato(uttaksgrenser.getFomDato())
            .medSøknadMottattDato(søknadMottattDato)
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
