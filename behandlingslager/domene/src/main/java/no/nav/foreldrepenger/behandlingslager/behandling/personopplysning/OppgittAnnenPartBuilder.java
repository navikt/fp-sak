package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.io.Serializable;

public class OppgittAnnenPartBuilder implements Serializable {
    private OppgittAnnenPartEntitet søknadAnnenPartMal;

    public OppgittAnnenPartBuilder() {
        søknadAnnenPartMal = new OppgittAnnenPartEntitet();
    }

    public OppgittAnnenPartBuilder(OppgittAnnenPartEntitet oppgittAnnenPart) {
        if (oppgittAnnenPart != null) {
            søknadAnnenPartMal = new OppgittAnnenPartEntitet(oppgittAnnenPart);
        } else {
            søknadAnnenPartMal = new OppgittAnnenPartEntitet();
        }
    }

    public OppgittAnnenPartBuilder medAktørId(AktørId aktørId) {
        søknadAnnenPartMal.setAktørId(aktørId);
        return this;
    }

    public OppgittAnnenPartBuilder medNavn(String navn) {
        søknadAnnenPartMal.setNavn(navn);
        return this;
    }

    public OppgittAnnenPartBuilder medBegrunnelse(String begrunnelse) {
        søknadAnnenPartMal.setBegrunnelse(begrunnelse);
        return this;
    }

    public OppgittAnnenPartBuilder medType(SøknadAnnenPartType type) {
        søknadAnnenPartMal.setType(type);
        return this;
    }

    public OppgittAnnenPartBuilder medUtenlandskFnr(String utenlandskFnr) {
        søknadAnnenPartMal.setUtenlandskPersonident(utenlandskFnr);
        return this;
    }

    public OppgittAnnenPartBuilder medUtenlandskFnrLand(Landkoder utenlandskFnrLand) {
        søknadAnnenPartMal.setUtenlandskPersonidentLand(utenlandskFnrLand);
        return this;
    }

    public OppgittAnnenPartBuilder medÅrsak(String årsak) {
        søknadAnnenPartMal.setÅrsak(årsak);
        return this;
    }

    public OppgittAnnenPartEntitet build() {
        return søknadAnnenPartMal;
    }
}
