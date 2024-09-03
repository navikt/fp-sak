package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record SkjæringstidspunktDto(LocalDate dato, boolean utenMinsterett) {

    public static Optional<SkjæringstidspunktDto> fraSkjæringstidspunkt(Skjæringstidspunkt skjæringstidspunkt) {
        var dato = Optional.ofNullable(skjæringstidspunkt).flatMap(Skjæringstidspunkt::getSkjæringstidspunktHvisUtledet);
        return dato.map(d -> new SkjæringstidspunktDto(d, skjæringstidspunkt.utenMinsterett()));
    }
}
