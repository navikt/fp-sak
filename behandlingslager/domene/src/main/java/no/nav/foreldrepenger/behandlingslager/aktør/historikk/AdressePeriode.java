package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;

public record AdressePeriode(Gyldighetsperiode gyldighetsperiode, Adresseinfo adresse) {

    public boolean overlappMedLikAdresse(AdressePeriode other) {
        return gyldighetsperiode().overlapper(other.gyldighetsperiode()) && Adresseinfo.likeAdresser(adresse(), other.adresse());
    }

}
