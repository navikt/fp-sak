package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import java.time.LocalDate;
import java.util.List;

public record InfotrygdVedtakDto(List<VedtakKjede> vedtakKjeder) {

    public record VedtakKjede(LocalDate opprinneligIdentdato, InfotrygdKode behandlingstema, List<Vedtak> vedtak) {}

    public record Vedtak(InfotrygdKode behandlingstema,
                         LocalDate identdato, // IS10: ArbUfør
                         LocalDate opphørFom, // IS10: ArbUførTom
                         LocalDate opprinneligIdentdato, // IS10: ArbUførOpprinnelig
                         Periode periode, // IS10: UtbetaltFom - UtbetaltTom
                         LocalDate registrert,  // IS10: RegDato
                         String saksbehandlerId, // IS10: BrukerId
                         InfotrygdKode arbeidskategori, // IS10: ArbKat
                         List<Arbeidsforhold> arbeidsforhold,
                         Integer dekningsgrad, // IS10: Fdato
                         LocalDate fødselsdatoBarn, // IS10: Fdato
                         Integer gradering, // IS18 tidskonto prosent
                         List<Utbetaling> utbetalinger) { }

    // Fra IS13 Inntekt
    public record Arbeidsforhold(String arbeidsgiverOrgnr, Integer inntekt, InfotrygdKode inntektsperiode,
                                 Boolean refusjon, LocalDate refusjonTom,
                                 LocalDate identdato, LocalDate opprinneligIdentdato) { }

    // Fra IS15 Utbetaling
    public record Utbetaling(Periode periode, int utbetalingsgrad, String arbeidsgiverOrgnr, Boolean erRefusjon, Integer dagsats,
                             LocalDate identdato, LocalDate opprinneligIdentdato) { }


    public record InfotrygdKode(String kode, String termnavn) { }

    public record Periode(LocalDate fom, LocalDate tom) { }

}
