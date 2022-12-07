package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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

    @Inject
    public VurderSøknadsfristTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    VurderSøknadsfristTjeneste() {
        // For CDI
    }

    public Optional<AksjonspunktDefinisjon> vurder(Long behandlingId) {
        var oppgittePerioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittFordeling)
            .map(OppgittFordelingEntitet::getPerioder).orElse(List.of());
        // Ingen perioder betyr behandling uten ny søknad.
        // Trenger ikke å sjekke søknadsfrist på nytt ettersom uttaksperiodegrense er kopiert fra forrige behandling
        if (oppgittePerioder.isEmpty()) {
            if (uttaksperiodegrenseRepository.hentHvisEksisterer(behandlingId).isEmpty()) {
                throw new IllegalStateException("Forventet at uttaksperiodegrense er kopiert fra original behandling");
            }
            return Optional.empty();
        }

        var søknadMottattDato = søknadRepository.hentSøknad(behandlingId).getMottattDato();
        var tidligsteLovligeUttakDato = Søknadsfrister.tidligsteDatoDagytelse(søknadMottattDato);

        var uttaksperiodegrense = new Uttaksperiodegrense(søknadMottattDato);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var førsteUttaksdato = finnFørsteUttaksdato(oppgittePerioder, søknadMottattDato).orElse(null);
        var forTidligUttak = førsteUttaksdato != null && førsteUttaksdato.isBefore(tidligsteLovligeUttakDato);
        return forTidligUttak ? Optional.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST) : Optional.empty();
    }

    private Optional<LocalDate> finnFørsteUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate søknadMottattDato) {
        return SøknadsperiodeFristTjenesteImpl.perioderSkalVurderes(oppgittePerioder, søknadMottattDato).stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }
}
