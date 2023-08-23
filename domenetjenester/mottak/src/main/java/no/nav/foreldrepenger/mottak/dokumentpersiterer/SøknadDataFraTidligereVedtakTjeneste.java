package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.DokVurderingKopierer;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.TidligstMottattOppdaterer;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeFilter;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SøknadDataFraTidligereVedtakTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FpUttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;
    private UtsettelseBehandling2021 utsettelseBehandling;

    @Inject
    public SøknadDataFraTidligereVedtakTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                FpUttakRepository uttakRepository,
                                                BehandlingRepository behandlingRepository,
                                                UtsettelseBehandling2021 utsettelseBehandling) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
        this.utsettelseBehandling = utsettelseBehandling;
        this.behandlingRepository = behandlingRepository;
    }

    SøknadDataFraTidligereVedtakTjeneste() {
        //CDI
    }

    public List<OppgittPeriodeEntitet> filtrerVekkPerioderSomErLikeInnvilgetUttak(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        if (nysøknad.isEmpty() || forrigeUttak == null || forrigeUttak.getGjeldendePerioder().getPerioder().isEmpty()) {
            return nysøknad;
        }
        var foreldrepenger = nysøknad.stream().map(OppgittPeriodeEntitet::getPeriodeType).anyMatch(UttakPeriodeType.FORELDREPENGER::equals) ||
            forrigeUttak.getGjeldendePerioder().getPerioder().stream()
                .anyMatch(p -> p.getAktiviteter().stream().map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto).anyMatch(StønadskontoType.FORELDREPENGER::equals));
        // Skal ikke legge inn utsettelse for BFHR
        var kreverSammenhengendeUttak = !behandling.erRevurdering() || utsettelseBehandling.kreverSammenhengendeUttak(behandling)
            || foreldrepenger && !RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType());
        return VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling.getId(), nysøknad, forrigeUttak, kreverSammenhengendeUttak);
    }

    public List<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(Behandling behandling, LocalDate mottattDato, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        var tidligereFordelinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
            .filter(Behandling::erYtelseBehandling)
            .map(Behandling::getId)
            .filter(b -> !b.equals(behandling.getId()))
            .map(this::fordelingForBehandling)
            .flatMap(Optional::stream)
            .toList();

        // Vedtaksperioder fra forrige uttaksresultat - bruker sammenhengende = true for å få med avslåtte
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer);

        return TidligstMottattOppdaterer.oppdaterTidligstMottattDato(nysøknad, mottattDato, tidligereFordelinger, forrigeUttak);
    }

    private Optional<OppgittFordelingEntitet> fordelingForBehandling(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getGjeldendeFordeling);
    }

    public List<OppgittPeriodeEntitet> oppdaterMedGodkjenteDokumentasjonsVurderinger(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty() || RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return nysøknad;
        }

        // Vedtaksperioder fra forrige uttaksresultat
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer);

        // Kopier kun godkjent vurdering for søknadsperioder. Vedtaksperioder vil innholde alle vurderinger
        return DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(nysøknad, List.of(), forrigeUttak);
    }

}
