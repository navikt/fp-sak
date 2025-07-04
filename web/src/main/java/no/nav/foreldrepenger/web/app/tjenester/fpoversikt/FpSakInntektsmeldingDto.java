package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record FpSakInntektsmeldingDto(Boolean erAktiv, BigDecimal stillingsprosent, BigDecimal inntektPrMnd, BigDecimal refusjonPrMnd,
                               String arbeidsgiverNavn, String arbeidsgiverIdent, String journalpostId, LocalDateTime mottattTidspunkt,
                               LocalDate startDatoPermisjon, List<Naturalytelse> bortfalteNaturalytelser, List<Refusjon> refusjonsperioder) {
    record Naturalytelse(LocalDate fomDato, LocalDate tomDato, BigDecimal beløpPerMnd, NaturalytelseType type) {
    }

    record Refusjon(BigDecimal refusjonsbeløpMnd, LocalDate fomDato) {
    }

    enum NaturalytelseType {
        ELEKTRISK_KOMMUNIKASJON,
        AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
        LOSJI,
        KOST_DØGN,
        BESØKSREISER_HJEMMET_ANNET,
        KOSTBESPARELSE_I_HJEMMET,
        RENTEFORDEL_LÅN,
        BIL,
        KOST_DAGER,
        BOLIG,
        SKATTEPLIKTIG_DEL_FORSIKRINGER,
        FRI_TRANSPORT,
        OPSJONER,
        TILSKUDD_BARNEHAGEPLASS,
        ANNET,
        BEDRIFTSBARNEHAGEPLASS,
        YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
        YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
        INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING
    }
}

