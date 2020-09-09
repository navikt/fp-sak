package no.nav.foreldrepenger.økonomi.tilkjentytelse.es;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.YtelseTypeTilkjentYtelseTjeneste;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

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
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Vedtak er ikke fattet enda. Denne tjenesten er kun designet for bruk etter at vedta er fattet."));

        Behandlingsresultat behandlingsresultat = vedtak.getBehandlingsresultat();
        LegacyESBeregningsresultat beregningResultat = behandlingsresultat.getBeregningResultat();
        if (beregningResultat == null) {
            return Collections.emptyList();
        }
        LegacyESBeregning beregning = beregningResultat.getSisteBeregning()
            .orElseThrow(() -> TilkjentYtelseTjenesteFeil.FACTORY.manglerBeregning(behandlingId).toException());

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

    @Override
    public LocalDate hentEndringstidspunkt(Long behandlingId) {
        return null; //alltid null for ES
    }


    interface TilkjentYtelseTjenesteFeil extends DeklarerteFeil {
        static final TilkjentYtelseTjenesteFeil FACTORY =  FeilFactory.create(TilkjentYtelseTjenesteFeil.class);
        @TekniskFeil(feilkode = "FP-598399", feilmelding = "Behandlingen %s har ikke beregning", logLevel = LogLevel.WARN)
        Feil manglerBeregning(long behandlingId);
    }
}
