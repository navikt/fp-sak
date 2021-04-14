package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;

@ApplicationScoped
public class HarEtablertYtelseFP {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public HarEtablertYtelseFP(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
            UttakInputTjeneste uttakInputTjeneste,
            RelatertBehandlingTjeneste relatertBehandlingTjeneste,
            ForeldrepengerUttakTjeneste uttakTjeneste,
            BehandlingVedtakRepository behandlingVedtakRepository) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    HarEtablertYtelseFP() {
        // CDI
    }

    public boolean vurder(Behandling revurdering,
            boolean finnesInnvilgetIkkeOpphørtVedtak,
            UttakResultatHolder uttakResultatHolder) {
        var annenpartUttak = getAnnenPartUttak(revurdering.getFagsak().getSaksnummer());
        if (erDagensDatoEtterSistePeriodeIUttak(uttakResultatHolder, annenpartUttak)) {
            var uttakInputOriginalBehandling = uttakInputTjeneste.lagInput(revurdering.getOriginalBehandlingId().orElseThrow());
            if (stønadskontoSaldoTjeneste.erSluttPåStønadsdager(uttakInputOriginalBehandling)) {
                return false;
            }
        }
        return finnesInnvilgetIkkeOpphørtVedtak;
    }

    private UttakResultatHolder getAnnenPartUttak(Saksnummer saksnummer) {
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummer);
        if (annenpartBehandling.isPresent() && erTilknyttetLøpendeFagsak(annenpartBehandling.get())) {
            var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(annenpartBehandling.get().getId());
            return new UttakResultatHolderFP(uttakTjeneste.hentUttakHvisEksisterer(annenpartBehandling.get().getId()), vedtak.orElse(null));
        }
        return new UttakResultatHolderFP(Optional.empty(), null);
    }

    private boolean erTilknyttetLøpendeFagsak(Behandling behandling) {
        return behandling.getFagsak().getStatus().equals(FagsakStatus.LØPENDE);
    }

    private boolean erDagensDatoEtterSistePeriodeIUttak(UttakResultatHolder uttakResultatHolder,
            UttakResultatHolder uttakResultatHolderAnnenPart) {
        var dagensDato = LocalDate.now();
        var sisteDagISøkersUttak = uttakResultatHolder.getSisteDagAvSistePeriode();
        var sisteDagIAnnenPartsUttak = uttakResultatHolderAnnenPart.getSisteDagAvSistePeriode();

        if (sisteDagIAnnenPartsUttak.isAfter(sisteDagISøkersUttak)) {
            return dagensDato.isAfter(sisteDagIAnnenPartsUttak);
        }
        return dagensDato.isAfter(sisteDagISøkersUttak);
    }
}
