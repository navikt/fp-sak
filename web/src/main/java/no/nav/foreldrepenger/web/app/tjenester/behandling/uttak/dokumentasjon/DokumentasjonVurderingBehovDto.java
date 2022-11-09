package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.domene.uttak.fakta.dokumentasjon.DokumentasjonVurderingBehov;

public record DokumentasjonVurderingBehovDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull Type type, @NotNull Årsak årsak,
                                             @NotNull Vurdering vurdering) {

    static DokumentasjonVurderingBehovDto from(DokumentasjonVurderingBehov o) {
        return new DokumentasjonVurderingBehovDto(o.oppgittPeriode().getFom(), o.oppgittPeriode().getTom(), Type.from(o.behov().type()),
            Årsak.from(o.behov().årsak()), Vurdering.from(o.vurdering()));
    }

    enum Type {
        UTSETTELSE,
        TIDLIG_OPPSTART_FAR,
        OVERFØRING,
        AKTIVITETSKRAV,
        ;

        static Type from(DokumentasjonVurderingBehov.Behov.Type type) {
            if (type == null) {
                return null;
            }
            return switch (type) {
                case UTSETTELSE -> Type.UTSETTELSE;
                case TIDLIG_OPPSTART_FAR -> TIDLIG_OPPSTART_FAR;
                case OVERFØRING -> OVERFØRING;
                case AKTIVITETSKRAV -> AKTIVITETSKRAV;
            };
        }
    }

    enum Årsak {
        INNLEGGELSE_SØKER,
        INNLEGGELSE_BARN,
        HV_OVELSE,
        NAV_TILTAK,
        SYKDOM_SØKER,
        ;

        static Årsak from(DokumentasjonVurderingBehov.Behov.Årsak årsak) {
            if (årsak == null) {
                return null;
            }
            return switch (årsak) {
                case INNLEGGELSE_SØKER -> Årsak.INNLEGGELSE_SØKER;
                case INNLEGGELSE_BARN -> Årsak.INNLEGGELSE_BARN;
                case HV_OVELSE -> Årsak.HV_OVELSE;
                case NAV_TILTAK -> Årsak.NAV_TILTAK;
                case SYKDOM_SØKER -> Årsak.SYKDOM_SØKER;
            };
        }
    }

    enum Vurdering {
        GODKJENT,
        IKKE_GODKJENT,
        IKKE_DOKUMENTERT;

        static Vurdering from(DokumentasjonVurdering dokumentasjonVurdering) {
            if (dokumentasjonVurdering == null) {
                return null;
            }
            return switch (dokumentasjonVurdering) {
                case SYKDOM_SØKER_GODKJENT, INNLEGGELSE_SØKER_GODKJENT, INNLEGGELSE_BARN_GODKJENT, HV_OVELSE_GODKJENT, NAV_TILTAK_GODKJENT,
                    TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT, MORS_AKTIVITET_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_GODKJENT, SYKDOM_ANNEN_FORELDER_GODKJENT, ALENEOMSORG_GODKJENT, BARE_SØKER_RETT_GODKJENT ->
                    GODKJENT;
                case SYKDOM_SØKER_IKKE_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT, HV_OVELSE_IKKE_GODKJENT, NAV_TILTAK_IKKE_GODKJENT,
                    MORS_AKTIVITET_IKKE_GODKJENT, TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT, SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT, ALENEOMSORG_IKKE_GODKJENT, BARE_SØKER_RETT_IKKE_GODKJENT ->
                    IKKE_GODKJENT;
                case MORS_AKTIVITET_IKKE_DOKUMENTERT -> IKKE_DOKUMENTERT;
            };
        }
    }
}
