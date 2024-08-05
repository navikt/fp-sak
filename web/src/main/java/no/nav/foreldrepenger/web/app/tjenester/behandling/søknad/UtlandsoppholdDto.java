package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;

public record UtlandsoppholdDto(String landNavn, LocalDate fom, LocalDate tom) {
    public static UtlandsoppholdDto mapFra(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
        return new UtlandsoppholdDto(utlandsopphold.getLand().getNavn(), utlandsopphold.getPeriodeFom(), utlandsopphold.getPeriodeTom());
    }
}
