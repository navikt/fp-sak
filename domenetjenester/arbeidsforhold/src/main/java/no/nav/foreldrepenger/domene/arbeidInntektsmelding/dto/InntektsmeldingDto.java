package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.typer.Beløp;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

public record InntektsmeldingDto(BigDecimal inntektPrMnd,
                                 BigDecimal refusjonPrMnd,
                                 String arbeidsgiverIdent,
                                 String eksternArbeidsforholdId,
                                 String internArbeidsforholdId,
                                 String kontaktpersonNavn,
                                 String kontaktpersonNummer,
                                 String journalpostId,
                                 String dokumentId,
                                 LocalDate motattDato,
                                 LocalDateTime innsendingstidspunkt,
                                 AksjonspunktÅrsak årsak,
                                 String begrunnelse,
                                 ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                 String kildeSystem,
                                 LocalDate startDatoPermisjon,
                                 @Deprecated
                                 List<NaturalYtelse> aktiveNaturalytelser,
                                 List<Refusjon> refusjonsperioder,
                                 InntektsmeldingInnsendingsårsak innsendingsårsak,
                                 List<UUID> tilknyttedeBehandlingIder,
                                 Map<NaturalYtelseType, List<BortfaltNaturalytelse>> bortfalteNaturalytelser) {

    public record BortfaltNaturalytelse(LocalDate fom, LocalDate tom, Beløp beloepPerMnd) { }

    public static Map<NaturalYtelseType, List<BortfaltNaturalytelse>> mapToBortfalteNaturalytelser(List<NaturalYtelse> aktiveNaturalytelser) {
        Map<NaturalYtelseType, List<NaturalYtelse>> gruppertNaturalytelsePerioder = aktiveNaturalytelser.stream()
            .collect(Collectors.groupingBy(NaturalYtelse::getType));

        Map<NaturalYtelseType, List<BortfaltNaturalytelse>> bortfalteNaturalytelser = new HashMap<>();

        gruppertNaturalytelsePerioder.forEach((ytelseType, aktiveNaturalytelserForType) -> {
            List<NaturalYtelse> sortertePerioder = aktiveNaturalytelserForType.stream()
                .sorted(Comparator.comparing(NaturalYtelse::getTom, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(NaturalYtelse::getFom, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

            var perioderMedBortfaltYtelse = new ArrayList<BortfaltNaturalytelse>();

            for (int i = 0; i < sortertePerioder.size(); i++) {
                NaturalYtelse currentPeriod = sortertePerioder.get(i);
                NaturalYtelse nextPeriod = i + 1 < sortertePerioder.size() ? sortertePerioder.get(i + 1) : null;

                if (!currentPeriod.getTom().equals(TIDENES_ENDE)) {
                LocalDate nyFom = currentPeriod.getTom().plusDays(1);
                LocalDate nyTom = nextPeriod != null ? nextPeriod.getFom().minusDays(1) : TIDENES_ENDE;

                    perioderMedBortfaltYtelse.add(new BortfaltNaturalytelse(nyFom, nyTom, currentPeriod.getBeloepPerMnd()));
                }
            }
            bortfalteNaturalytelser.put(ytelseType, perioderMedBortfaltYtelse);
        });

        return bortfalteNaturalytelser;
    }
}
