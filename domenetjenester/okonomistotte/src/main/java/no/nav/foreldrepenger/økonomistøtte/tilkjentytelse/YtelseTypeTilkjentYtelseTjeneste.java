package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;

public interface YtelseTypeTilkjentYtelseTjeneste {

    List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId);

    boolean erOpphør(Behandlingsresultat behandlingsresultat);

    Boolean erOpphørEtterSkjæringstidspunkt(Behandling behandling, Behandlingsresultat behandlingsresultat);
}
