package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov;

public record DokumentasjonVurderingBehovDto(@NotNull LocalDate fom,
                                             @NotNull LocalDate tom,
                                             @NotNull DokumentasjonVurderingBehov.Behov.Type type,
                                             @NotNull @JsonDeserialize(using = ÅrsakDeserializer.class) DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                             @NotNull Vurdering vurdering) {

    static DokumentasjonVurderingBehovDto from(DokumentasjonVurderingBehov o) {
        return new DokumentasjonVurderingBehovDto(o.oppgittPeriode().getFom(), o.oppgittPeriode().getTom(), o.behov().type(),
            o.behov().årsak(), Vurdering.from(o.vurdering()));
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
                    TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT, MORS_AKTIVITET_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_GODKJENT, SYKDOM_ANNEN_FORELDER_GODKJENT,
                    ALENEOMSORG_GODKJENT, BARE_SØKER_RETT_GODKJENT -> GODKJENT;
                case SYKDOM_SØKER_IKKE_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT, HV_OVELSE_IKKE_GODKJENT,
                    NAV_TILTAK_IKKE_GODKJENT, MORS_AKTIVITET_IKKE_GODKJENT, TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT,
                    SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT, ALENEOMSORG_IKKE_GODKJENT, BARE_SØKER_RETT_IKKE_GODKJENT -> IKKE_GODKJENT;
                case MORS_AKTIVITET_IKKE_DOKUMENTERT -> IKKE_DOKUMENTERT;
            };
        }
    }

    private static class ÅrsakDeserializer extends JsonDeserializer<DokumentasjonVurderingBehov.Behov.Årsak> {

        @Override
        public DokumentasjonVurderingBehov.Behov.Årsak deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            var text = jsonParser.getCodec().<JsonNode>readTree(jsonParser).asText();

            if (Arrays.stream(DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.values()).anyMatch(n -> n.name().equals(text))) {
                return DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.valueOf(text);
            }
            if (Arrays.stream(DokumentasjonVurderingBehov.Behov.OverføringÅrsak.values()).anyMatch(n -> n.name().equals(text))) {
                return DokumentasjonVurderingBehov.Behov.OverføringÅrsak.valueOf(text);
            }
            if (Arrays.stream(DokumentasjonVurderingBehov.Behov.UttakÅrsak.values()).anyMatch(n -> n.name().equals(text))) {
                return DokumentasjonVurderingBehov.Behov.UttakÅrsak.valueOf(text);
            }
            throw new IllegalArgumentException("Ukjent årsak " + text);
        }
    }
}
