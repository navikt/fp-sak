package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.DokVurderingKopierer;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.TidligstMottattOppdaterer;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperiodeFilter;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class SøknadDataFraTidligereVedtakTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FpUttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public SøknadDataFraTidligereVedtakTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                FpUttakRepository uttakRepository,
                                                BehandlingRepository behandlingRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
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
                .anyMatch(p -> p.getAktiviteter().stream().map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto).anyMatch(UttakPeriodeType.FORELDREPENGER::equals));
        // Skal ikke legge inn fri utsettelse for å markere start på endring i søknaden for førstegangsbehandlinger eller BFHR
        var beholdSenestePeriode = !behandling.erRevurdering() || foreldrepenger && !RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType());
        return VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling.getId(), nysøknad, forrigeUttak, beholdSenestePeriode);
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

    public List<OppgittPeriodeEntitet> oppdaterMedGodkjenteDokumentasjonsVurderinger(Behandling behandling, List<OppgittPeriodeEntitet> oppgittePerioderFraSøknad) {
        if (oppgittePerioderFraSøknad.isEmpty() || RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return oppgittePerioderFraSøknad;
        }

        // Vedtaksperioder fra forrige uttaksresultat
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer);

        // Kopier kun godkjent vurdering for søknadsperioder. Vedtaksperioder vil innholde alle vurderinger
        return DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(oppgittePerioderFraSøknad, List.of(), forrigeUttak);
    }

}
