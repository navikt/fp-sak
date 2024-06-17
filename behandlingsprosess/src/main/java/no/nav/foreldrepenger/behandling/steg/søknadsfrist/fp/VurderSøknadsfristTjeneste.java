package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SøknadsperiodeFristTjenesteImpl;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class VurderSøknadsfristTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VurderSøknadsfristTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    VurderSøknadsfristTjeneste() {
        // For CDI
    }

    public Optional<AksjonspunktDefinisjon> vurder(Long behandlingId) {
        var oppgittePerioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittFordeling)
            .map(OppgittFordelingEntitet::getPerioder)
            .orElse(List.of());
        // Ingen perioder betyr behandling uten ny søknad, ergo ingen søknadsfrist å sjekke.
        if (oppgittePerioder.isEmpty()) {
            return Optional.empty();
        }

        var søknadMottattDato = søknadRepository.hentSøknad(behandlingId).getMottattDato();

        var eksisterendePeriodegrense = uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingId).map(Uttaksperiodegrense::getMottattDato);

        // Midlertidig: se bort fra tilfelle som har kopiert forrige i KOFAK/revurdering
        var harKopiertPeriodegrenseFraOriginal = finnPeriodegrenseOriginalbehandling(behandlingId).filter(
            d -> eksisterendePeriodegrense.filter(d::equals).isPresent()).isPresent();

        // Behold periodegrense som allerede er satt dersom tilbakehopp
        var brukperiodegrense = harKopiertPeriodegrenseFraOriginal ? søknadMottattDato : eksisterendePeriodegrense.orElse(søknadMottattDato);

        var tidligsteLovligeUttakDato = Søknadsfrister.tidligsteDatoDagytelse(brukperiodegrense);

        var uttaksperiodegrense = new Uttaksperiodegrense(brukperiodegrense);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var førsteUttaksdato = finnFørsteUttaksdato(oppgittePerioder, søknadMottattDato).orElse(null);
        var forTidligUttak = førsteUttaksdato != null && førsteUttaksdato.isBefore(tidligsteLovligeUttakDato);
        return forTidligUttak ? Optional.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST) : Optional.empty();
    }

    private Optional<LocalDate> finnFørsteUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate søknadMottattDato) {
        return SøknadsperiodeFristTjenesteImpl.perioderSkalVurderes(oppgittePerioder, søknadMottattDato)
            .stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnPeriodegrenseOriginalbehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId)
            .getOriginalBehandlingId()
            .flatMap(uttaksperiodegrenseRepository::hentHvisEksisterer)
            .map(Uttaksperiodegrense::getMottattDato);
    }
}
