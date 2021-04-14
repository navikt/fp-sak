package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse;

import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

class MapperForInntektskategori {

    private static final Map<Inntektskategori, TilkjentYtelseV1.Inntektskategori> INNTEKTSKATEGORI_MAP = Map.of(
        Inntektskategori.ARBEIDSTAKER, TilkjentYtelseV1.Inntektskategori.ARBEIDSTAKER,
        Inntektskategori.FRILANSER, TilkjentYtelseV1.Inntektskategori.FRILANSER,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, TilkjentYtelseV1.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        Inntektskategori.DAGPENGER, TilkjentYtelseV1.Inntektskategori.DAGPENGER,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, TilkjentYtelseV1.Inntektskategori.ARBEIDSAVKLARINGSPENGER,
        Inntektskategori.SJØMANN, TilkjentYtelseV1.Inntektskategori.SJØMANN,
        Inntektskategori.DAGMAMMA, TilkjentYtelseV1.Inntektskategori.DAGMAMMA,
        Inntektskategori.JORDBRUKER, TilkjentYtelseV1.Inntektskategori.JORDBRUKER,
        Inntektskategori.FISKER, TilkjentYtelseV1.Inntektskategori.FISKER,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, TilkjentYtelseV1.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER
    );

    private MapperForInntektskategori() {
        //for å unngå instansiering, slik at SonarQube blir glad
    }

    static TilkjentYtelseV1.Inntektskategori mapInntektskategori(Inntektskategori inntektskategori) {
        var resultat = INNTEKTSKATEGORI_MAP.get(inntektskategori);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: Inntektskategorien " + inntektskategori + " er ikke støttet i mapping");
    }
}
