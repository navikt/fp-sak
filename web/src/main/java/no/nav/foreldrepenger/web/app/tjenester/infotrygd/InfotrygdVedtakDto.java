package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InfotrygdVedtakDto(@NotNull List<SakDto> saker, @NotNull List<VedtakKjede> vedtakKjeder) {

    public record VedtakKjede(@NotNull LocalDate opprinneligIdentdato, @NotNull InfotrygdKode behandlingstema, @NotNull List<Vedtak> vedtak) {}

    public record Vedtak(InfotrygdKode behandlingstema,
                         @NotNull LocalDate identdato, // IS10: ArbUfør
                         LocalDate opphørFom, // IS10: ArbUførTom
                         @NotNull LocalDate opprinneligIdentdato, // IS10: ArbUførOpprinnelig
                         @NotNull Periode periode, // IS10: UtbetaltFom - UtbetaltTom
                         @NotNull LocalDate registrert,  // IS10: RegDato
                         @NotNull String saksbehandlerId, // IS10: BrukerId
                         InfotrygdKode arbeidskategori, // IS10: ArbKat
                         List<Arbeidsforhold> arbeidsforhold,
                         @NotNull Integer dekningsgrad, // IS10: Fdato
                         LocalDate fødselsdatoBarn, // IS10: Fdato
                         Integer gradering, // IS18 tidskonto prosent
                         List<Utbetaling> utbetalinger) { }

    // Fra IS13 Inntekt
    public record Arbeidsforhold(@NotNull String arbeidsgiverOrgnr, @NotNull Integer inntekt, InfotrygdKode inntektsperiode,
                                 @NotNull Boolean refusjon, LocalDate refusjonTom,
                                 @NotNull LocalDate identdato,@NotNull LocalDate opprinneligIdentdato) { }

    // Fra IS15 Utbetaling
    public record Utbetaling(@NotNull Periode periode, @NotNull int utbetalingsgrad, String arbeidsgiverOrgnr, @NotNull Boolean erRefusjon, @NotNull Integer dagsats,
                             @NotNull LocalDate identdato, @NotNull LocalDate opprinneligIdentdato) { }


    public record InfotrygdKode(@NotNull String kode, @NotNull String termnavn) { }

    public record Periode(@NotNull LocalDate fom, @NotNull LocalDate tom) { }

    // TODO [JOHANNES] -- frontend antar alle felter her er NotNull
    public record SakDto(String resultat, @NotNull LocalDate registrert, String sakId, String type, @NotNull LocalDate vedtatt, String valg, String undervalg, String nivaa) {

    }
}
