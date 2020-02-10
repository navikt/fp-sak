package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class YtelseStørrelseBuilder {
    private final YtelseStørrelse ytelseStørrelse;

    YtelseStørrelseBuilder(YtelseStørrelse ytelseStørrelse) {
        this.ytelseStørrelse = ytelseStørrelse;
    }

    public static YtelseStørrelseBuilder ny() {
        return new YtelseStørrelseBuilder(new YtelseStørrelse());
    }

    public YtelseStørrelseBuilder medVirksomhet(String virksomhetOrgnr) {
        return medVirksomhet(virksomhetOrgnr == null ? null : new OrgNummer(virksomhetOrgnr));
    }

    public YtelseStørrelseBuilder medBeløp(BigDecimal verdi) {
        BigDecimal verdiNotNull = verdi != null ? verdi : new BigDecimal(0);
        this.ytelseStørrelse.setBeløp(new Beløp(verdiNotNull));
        return this;
    }

    public YtelseStørrelseBuilder medHyppighet(InntektPeriodeType frekvens) {
        this.ytelseStørrelse.setHyppighet(frekvens);
        return this;
    }

    public YtelseStørrelse build() {
        if (ytelseStørrelse.hasValues()) {
            return ytelseStørrelse;
        }
        throw new IllegalStateException();
    }

    public YtelseStørrelseBuilder medVirksomhet(OrgNummer orgNummer) {
        this.ytelseStørrelse.setVirksomhet(orgNummer);
        return this;
    }

}
