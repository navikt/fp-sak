package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SøknadsfristTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private Period søknadsfristEtterFørsteUttaksdag;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

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
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    public Optional<AksjonspunktDefinisjon> vurderSøknadsfristForForeldrepenger(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(kontekst.getBehandlingId());
        YtelseFordelingAggregat fordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandling.getId());
        List<OppgittPeriodeEntitet> oppgittePerioder = fordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        //Ingen perioder betyr behandling ut ny søknad. Trenger ikke å sjekke søknadsfrist på nytt ettersom uttaksperiodegrense er kopiert fra forrige behandling
        if (oppgittePerioder.isEmpty()) {
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
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), uttaksperiodegrense);


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
            repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), uttaksperiodegrenseBuilder.build());
            oppdaterYtelseFordelingMedMottattDato(behandling.getId(), adapter.getMottattDato());
        }
    }

    private void oppdaterYtelseFordelingMedMottattDato(Long behandlingId, LocalDate mottattDato) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var eksisterendeJustertFordeling = ytelseFordelingAggregat.getJustertFordeling().orElseThrow();
        var nyeJustertFordelingPerioder = eksisterendeJustertFordeling.getOppgittePerioder().stream()
            .map(p -> {
                var builder = OppgittPeriodeBuilder.fraEksisterende(p);
                if (Objects.equals(p.getPeriodeKilde(), FordelingPeriodeKilde.SØKNAD)) {
                    builder.medMottattDato(mottattDato);
                }
                return builder.build();
            })
            .collect(Collectors.toList());
        var nyJustertFordeling = new OppgittFordelingEntitet(nyeJustertFordelingPerioder, eksisterendeJustertFordeling.getErAnnenForelderInformert());
        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.of(ytelseFordelingAggregat))
            .medJustertFordeling(nyJustertFordeling);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
    }

    public LocalDate finnSøknadsfristForPeriodeMedStart(LocalDate periodeStart) {
        return periodeStart.plus(søknadsfristEtterFørsteUttaksdag).with(TemporalAdjusters.lastDayOfMonth());
    }
}
