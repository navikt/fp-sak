package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SøknadsfristTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private Period søknadsfristEtterFørsteUttaksdag;

    SøknadsfristTjeneste() {
        //For CDI
    }

    /**
     * @param søknadsfristEtterFørsteUttaksdag - Maks antall måneder mellom søknadens mottattdato og første uttaksdag i søknaden.
     */
    @Inject
    public SøknadsfristTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                              @KonfigVerdi(value = "fp.søknadfrist.etter.første.uttaksdag", defaultVerdi = "P3M") Period søknadsfristEtterFørsteUttaksdag) {
        this.repositoryProvider = repositoryProvider;
        this.søknadsfristEtterFørsteUttaksdag = søknadsfristEtterFørsteUttaksdag;
    }

    public Optional<AksjonspunktDefinisjon> vurderSøknadsfristForForeldrepenger(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(kontekst.getBehandlingId());
        YtelseFordelingAggregat fordelingAggregat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());
        List<OppgittPeriodeEntitet> oppgittePerioder = fordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        //Ingen perioder betyr behandling ut ny søknad. Trenger ikke å sjekke søknadsfrist på nytt ettersom uttaksperiodegrense er kopiert fra forrige behandling
        if (oppgittePerioder.isEmpty()){
            return Optional.empty();
        }

        SøknadEntitet søknad = repositoryProvider.getSøknadRepository().hentSøknad(behandling);
        SøknadsfristRegelAdapter regelAdapter = new SøknadsfristRegelAdapter();
        SøknadsfristResultat resultat = regelAdapter.vurderSøknadsfristFor(søknad, oppgittePerioder, søknadsfristEtterFørsteUttaksdag);

        var behandlingsresultat = hentBehandlingsresultat(behandling);
        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medFørsteLovligeUttaksdag(resultat.getTidligsteLovligeUttak())
            .medMottattDato(søknad.getMottattDato())
            .medSporingInput(resultat.getInnsendtGrunnlag())
            .medSporingRegel(resultat.getEvalueringResultat())
            .build();
        repositoryProvider.getUttakRepository().lagreUttaksperiodegrense(behandling.getId(), uttaksperiodegrense);


        Optional<String> årsakKode = resultat.getÅrsakKodeIkkeVurdert();
        if (!resultat.isRegelOppfylt() && årsakKode.isPresent()) {
            AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(årsakKode.get());

            return Optional.of(aksjonspunktDefinisjon);
        }
        return Optional.empty();
    }

    public void lagreVurderSøknadsfristResultat(Behandling behandling, VurderSøknadsfristAksjonspunktDto adapter) {

        if (adapter.getMottattDato() != null) {
            LocalDate mottattDato = adapter.getMottattDato();
            LocalDate førsteLovligeUttaksdag = mottattDato.with(DAY_OF_MONTH, 1).minus(søknadsfristEtterFørsteUttaksdag);
            var behandlingsresultat = hentBehandlingsresultat(behandling);
            Uttaksperiodegrense.Builder uttaksperiodegrenseBuilder = new Uttaksperiodegrense.Builder(behandlingsresultat)
                .medMottattDato(adapter.getMottattDato())
                .medFørsteLovligeUttaksdag(førsteLovligeUttaksdag);
            repositoryProvider.getUttakRepository().lagreUttaksperiodegrense(behandling.getId(), uttaksperiodegrenseBuilder.build());
        }
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
    }

    public LocalDate finnSøknadsfristForPeriodeMedStart(LocalDate periodeStart) {
        return periodeStart.plus(søknadsfristEtterFørsteUttaksdag).with(TemporalAdjusters.lastDayOfMonth());
    }
}
