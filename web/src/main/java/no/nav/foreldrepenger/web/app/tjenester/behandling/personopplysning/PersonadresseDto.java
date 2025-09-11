package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.foreldrepenger.web.app.util.StringUtils.formaterMedStoreOgSmåBokstaver;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record PersonadresseDto(@NotNull LocalDate fom, @NotNull LocalDate tom, AdresseType adresseType, String adresselinje1, String adresselinje2,
                               String adresselinje3, String postNummer, String poststed, String land) {

    public static PersonadresseDto tilDto(PersonAdresseEntitet adresse) {
        return new PersonadresseDto(adresse.getPeriode().getFomDato(), adresse.getPeriode().getTomDato(), adresse.getAdresseType(),
            formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje1()), formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje2()),
            formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje3()), adresse.getPostnummer(),
            formaterMedStoreOgSmåBokstaver(adresse.getPoststed()), Landkoder.navnLesbart(adresse.getLand()));
    }

    @Override
    public String toString() {
        return "PersonadresseDto{" + "fom=" + fom + ", tom=" + tom + ", adresseType=" + adresseType + '}';
    }
}
