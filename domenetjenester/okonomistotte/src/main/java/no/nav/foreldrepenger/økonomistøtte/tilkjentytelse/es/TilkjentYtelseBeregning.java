package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse.es;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.økonomistøtte.tilkjentytelse.YtelseTypeTilkjentYtelseTjeneste;
import no.nav.vedtak.exception.TekniskException;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class TilkjentYtelseBeregning implements YtelseTypeTilkjentYtelseTjeneste {

    private BehandlingVedtakRepository behandlingVedtakRepository;


    TilkjentYtelseBeregning() {
        //for CDI proxy
    }

    @Inject
    public TilkjentYtelseBeregning(BehandlingVedtakRepository behandlingVedtakRepository) {
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    @Override
    public List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Vedtak er ikke fattet enda. Denne tjenesten er kun designet for bruk etter at vedta er fattet."));

        var behandlingsresultat = vedtak.getBehandlingsresultat();
        var beregningResultat = behandlingsresultat.getBeregningResultat();
        if (beregningResultat == null) {
            return Collections.emptyList();
        }
        var beregning = beregningResultat.getSisteBeregning()
            .orElseThrow(() -> {
                var msg = String.format("Behandlingen %s har ikke beregning", behandlingId);
                return new TekniskException("FP-598399", msg);
            });

        return MapperForTilkjentYtelse.mapTilkjentYtelse(beregning, vedtak.getVedtaksdato());
    }

    @Override
    public boolean erOpphør(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.isBehandlingsresultatOpphørt();
    }

    @Override
    public Boolean erOpphørEtterSkjæringstidspunkt(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return null; //ikke aktuelt for ES //NOSONAR
    }
}
