package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;

@FagsakYtelseTypeRef
@ApplicationScoped
public class TilkjentYtelseBeregning implements YtelseTypeTilkjentYtelseTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    TilkjentYtelseBeregning() {
        //for CDI proxy
    }

    @Inject
    public TilkjentYtelseBeregning(BeregningsresultatRepository beregningsresultatRepository, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId) {
        Optional<BeregningsresultatEntitet> resultatOpt = hentResultatFP(behandlingId);
        if (resultatOpt.isEmpty()) {
            return Collections.emptyList();
        }
        BeregningsresultatEntitet resultat = resultatOpt.get();
        return MapperForTilkjentYtelse.mapTilkjentYtelse(resultat);
    }

    @Override
    public boolean erOpphør(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.isBehandlingsresultatOpphørt();
    }

    @Override
    public Boolean erOpphørEtterSkjæringstidspunkt(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (!behandlingsresultat.isBehandlingsresultatOpphørt()) {
            return null; //ikke relevant //NOSONAR
        }

        var uttak = uttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        return uttak.map(uttakResultatEntitet ->
            uttakResultatEntitet.getGjeldendePerioder().stream()
                .anyMatch(ForeldrepengerUttakPeriode::isInnvilget))
            .orElse(false);
    }

    @Override
    public LocalDate hentEndringstidspunkt(Long behandlingId) {
        return hentResultatFP(behandlingId)
            .flatMap(BeregningsresultatEntitet::getEndringsdato)
            .orElse(null);
    }

    private Optional<BeregningsresultatEntitet> hentResultatFP(Long behandlingId) {
        return beregningsresultatRepository.hentBeregningsresultat(behandlingId);
    }
}
