package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;

@ApplicationScoped
public class SjekkOmDetFinnesTilkjentYtelse {

    private BeregningsresultatRepository beregningsresultatRepository;

    SjekkOmDetFinnesTilkjentYtelse() {
        // for CDI proxy
    }

    @Inject
    public SjekkOmDetFinnesTilkjentYtelse(BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public boolean harTilkjentYtelse(Long behandlingId) {
        BeregningsresultatEntitet beregningsresultat = hentTilkjentYtelse(behandlingId);
        return harTilkjentYtelse(beregningsresultat);
    }

    public TilkjentYtelseDiff tilkjentYtelseDiffMotForrige(Behandling behandling) {
        BeregningsresultatEntitet tilkjentYtelse = hentTilkjentYtelse(behandling.getId());
        BeregningsresultatEntitet forrigeTilkjentYtelse = hentTilkjentYtelseForForrigeBehandling(behandling);

        boolean harNyTilkjentYtelse = harTilkjentYtelse(tilkjentYtelse);
        boolean harForrigeTilkjentYtelse = harTilkjentYtelse(forrigeTilkjentYtelse);

        if (!harForrigeTilkjentYtelse) {
            return harNyTilkjentYtelse
                ? TilkjentYtelseDiff.ENDRET_FRA_TOM
                : TilkjentYtelseDiff.INGEN_ENDRING;
        }
        if (!harNyTilkjentYtelse) {
            return TilkjentYtelseDiff.ENDRET_TIL_TOM;
        }
        return hentEndringsdato(behandling.getId()).isPresent()
            ? TilkjentYtelseDiff.ANNEN_ENDRING
            : TilkjentYtelseDiff.INGEN_ENDRING;
    }

    private BeregningsresultatEntitet hentTilkjentYtelse(Long behandlingId) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId).orElse(null);
    }

    private BeregningsresultatEntitet hentTilkjentYtelseForForrigeBehandling(Behandling behandling) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            return null;
        }
        Long forrigeBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Mangler original behandling for revurdering, behandlingId=" + behandling.getId()));

        return hentTilkjentYtelse(forrigeBehandlingId);
    }

    private boolean harTilkjentYtelse(BeregningsresultatEntitet ty) {
        if (ty == null) {
            return false;
        }
        return ty.getBeregningsresultatPerioder().stream()
            .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> andel.getDagsats() > 0);
    }

    private Optional<LocalDate> hentEndringsdato(Long behandlingId) {
        BeregningsresultatEntitet tilkjentYtelse = hentTilkjentYtelse(behandlingId);
        return tilkjentYtelse != null
            ? tilkjentYtelse.getEndringsdato()
            : Optional.empty();
    }

    public enum TilkjentYtelseDiff {
        INGEN_ENDRING,
        ENDRET_TIL_TOM,
        ENDRET_FRA_TOM,
        ANNEN_ENDRING
    }
}
