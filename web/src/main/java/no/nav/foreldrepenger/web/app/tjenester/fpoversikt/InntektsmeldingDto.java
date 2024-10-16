package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@JsonInclude(JsonInclude.Include.NON_NULL)
record InntektsmeldingDto(Boolean erAktiv, BigDecimal inntektPrMnd,
                                 BigDecimal refusjonPrMnd,
                                 String arbeidsgiverNavn,
                          String kontaktpersonNavn,
                          String kontaktpersonNummer,
                                 JournalpostId journalpostId,
                                 LocalDateTime innsendingstidspunkt,
                                 LocalDateTime mottattTidspunkt,
                                 LocalDate startDatoPermisjon,
                                 List<NaturalYtelse> bortfalteNaturalytelser,
                                 List<Refusjon> refusjonsperioder
){
    record NaturalYtelse(
        LocalDate fomDato,
        LocalDate tomDato,
    BigDecimal beloepPerMnd,
    NaturalYtelseType type
    ) {}

    record Refusjon(
        BigDecimal refusjonsbeløpMnd,
        LocalDate fomDato
    ) {}
}

