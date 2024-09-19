package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record UtlandsoppholdDto(String landNavn, LocalDate fom, LocalDate tom) {
    public static UtlandsoppholdDto mapFra(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
        return new UtlandsoppholdDto(Landkoder.navnLesbart(utlandsopphold.getLand()), utlandsopphold.getPeriodeFom(), utlandsopphold.getPeriodeTom());
    }
}
