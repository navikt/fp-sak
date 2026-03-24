package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;

public record VarselRevurderingAksjonspunktDto(String begrunnelse, LocalDate frist, String venteÅrsakKode) {

}
