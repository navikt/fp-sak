package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.RegisterVurdering;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public record DokumentasjonVurderingBehovDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull @Valid DokumentasjonVurderingBehov.Behov.Type type,
                                             @NotNull @Valid DokumentasjonVurderingBehov.Behov.Årsak årsak, @Valid Vurdering vurdering,
                                             @Valid MorsStillingsprosent morsStillingsprosent,
                                             Set<@Valid AktivitetskravGrunnlagArbeid> aktivitetskravGrunnlag) {

    static DokumentasjonVurderingBehovDto from(DokumentasjonVurderingBehov o, Set<AktivitetskravArbeidPeriodeEntitet> e) {
        return new DokumentasjonVurderingBehovDto(o.oppgittPeriode().getFom(), o.oppgittPeriode().getTom(), o.behov().type(), o.behov().årsak(),
            Vurdering.from(o.vurdering(), o.registerVurdering()), map(o.oppgittPeriode().getDokumentasjonVurdering()),
            e.stream().map(AktivitetskravGrunnlagArbeid::from).collect(Collectors.toSet()));
    }

    private static MorsStillingsprosent map(DokumentasjonVurdering dokumentasjonVurdering) {
        if (dokumentasjonVurdering == null) {
            return null;
        }
        return dokumentasjonVurdering.morsStillingsprosent();
    }

    enum Vurdering {
        GODKJENT,
        GODKJENT_AUTOMATISK,
        IKKE_GODKJENT,
        IKKE_DOKUMENTERT;

        static Vurdering from(DokumentasjonVurdering dokumentasjonVurdering, RegisterVurdering registerVurdering) {
            if (dokumentasjonVurdering == null) {
                return switch (registerVurdering) {
                    case MORS_AKTIVITET_GODKJENT -> GODKJENT_AUTOMATISK;
                    case MORS_AKTIVITET_IKKE_GODKJENT -> null;
                    case null -> null;
                };
            }
            return switch (dokumentasjonVurdering.type()) {
                case SYKDOM_SØKER_GODKJENT, INNLEGGELSE_SØKER_GODKJENT, INNLEGGELSE_BARN_GODKJENT, HV_OVELSE_GODKJENT, NAV_TILTAK_GODKJENT,
                     TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT, MORS_AKTIVITET_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_GODKJENT,
                     SYKDOM_ANNEN_FORELDER_GODKJENT, ALENEOMSORG_GODKJENT, BARE_SØKER_RETT_GODKJENT -> GODKJENT;
                case SYKDOM_SØKER_IKKE_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT, HV_OVELSE_IKKE_GODKJENT,
                     NAV_TILTAK_IKKE_GODKJENT, MORS_AKTIVITET_IKKE_GODKJENT, TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT,
                     INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT, SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT, ALENEOMSORG_IKKE_GODKJENT,
                     BARE_SØKER_RETT_IKKE_GODKJENT -> IKKE_GODKJENT;
                case MORS_AKTIVITET_IKKE_DOKUMENTERT -> IKKE_DOKUMENTERT;
            };
        }
    }

    record AktivitetskravGrunnlagArbeid(@NotNull @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER) String orgNummer,
                                        @NotNull @DecimalMin("0.00") @DecimalMax("2000.00") BigDecimal stillingsprosent,
                                        @NotNull @Valid Permisjon permisjon) {
        static AktivitetskravGrunnlagArbeid from(AktivitetskravArbeidPeriodeEntitet e) {
            var stilling = Optional.ofNullable(e.getSumStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO);
            var permisjonsprosent = Optional.ofNullable(e.getSumPermisjonsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO);
            var permisjonstype = Optional.ofNullable(e.getPermisjonsbeskrivelseType()).orElse(AktivitetskravPermisjonType.UDEFINERT);
            return new AktivitetskravGrunnlagArbeid(e.getOrgNummer().getId(), stilling, new Permisjon(permisjonsprosent, permisjonstype));
        }
    }

    private record Permisjon(@NotNull @DecimalMin("0.00") @DecimalMax("2000.00") BigDecimal prosent,
                             @NotNull @ValidKodeverk AktivitetskravPermisjonType type) {

    }
}
