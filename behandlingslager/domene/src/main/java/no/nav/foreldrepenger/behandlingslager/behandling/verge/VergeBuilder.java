package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class VergeBuilder {
    private VergeEntitet kladd;

    public VergeBuilder() {
        kladd = new VergeEntitet();
    }

    public VergeBuilder gyldigPeriode(LocalDate gyldigFom, LocalDate gyldigTom) {
        if (gyldigTom != null) {
            kladd.gyldigPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gyldigFom, gyldigTom);
        } else {
            kladd.gyldigPeriode = DatoIntervallEntitet.fraOgMed(gyldigFom);
        }
        return this;
    }

    public VergeBuilder medVergeType(VergeType vergeType) {
        kladd.vergeType = vergeType;
        return this;
    }

    public VergeBuilder medBruker(NavBruker bruker) {
        kladd.bruker = bruker;
        return this;
    }

    public VergeBuilder medVergeOrganisasjon(VergeOrganisasjonEntitet vergeOrganisasjon) {
        vergeOrganisasjon.setVerge(kladd);
        kladd.vergeOrganisasjon = vergeOrganisasjon;
        return this;
    }

    public VergeEntitet build() {
        //verifiser oppbyggingen til objektet
        Objects.requireNonNull(kladd.vergeType, "vergeType");
        return kladd;
    }
}
