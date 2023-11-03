package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GrunnlagDto(Map<LocalDate, List<Grunnlag>> grunnlagPerIdentdato) {

    public record Grunnlag(InfotrygdKode status,
                           InfotrygdKode tema,
                           Integer dekningsgrad,
                           LocalDate fødselsdatoBarn,
                           InfotrygdKode arbeidskategori,
                           List<Arbeidsforhold> arbeidsforhold,
                           Periode periode,
                           InfotrygdKode behandlingstema,
                           LocalDate identdato,
                           LocalDate iverksatt,
                           LocalDate opphørFom,
                           Integer gradering,
                           LocalDate opprinneligIdentdato,
                           LocalDate registrert,
                           String saksbehandlerId,
                           List<Vedtak> vedtak) { }

    public record Arbeidsforhold(String arbeidsgiverOrgnr, Integer inntekt, InfotrygdKode inntektsperiode, Boolean refusjon, LocalDate refusjonTom) {
    }


    public record Vedtak(Periode periode, int utbetalingsgrad, String arbeidsgiverOrgnr, Boolean erRefusjon, Integer dagsats) {
    }


    public record InfotrygdKode(String kode, String termnavn) {
    }

    public record Periode(LocalDate fom, LocalDate tom) {
    }

}
